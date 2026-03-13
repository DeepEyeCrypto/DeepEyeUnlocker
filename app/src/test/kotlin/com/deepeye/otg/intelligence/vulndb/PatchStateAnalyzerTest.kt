package com.deepeye.otg.intelligence.vulndb

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

// ──────────────────────────────────────────────────────────────
// PatchStateAnalyzer Unit Tests
// DeepEye OTG — CVE Intelligence Module Test Fixtures
// ──────────────────────────────────────────────────────────────

/**
 * In-memory fake DAO for unit testing without Room.
 * Duplicates the query semantics of [CveDao] using in-memory lists.
 */
class FakeCveDao : CveDao {
    private val entries = mutableListOf<CveEntry>()

    override suspend fun upsert(entry: CveEntry) {
        entries.removeAll { it.cveId == entry.cveId }
        entries.add(entry)
    }

    override suspend fun upsertAll(entryList: List<CveEntry>) {
        entryList.forEach { upsert(it) }
    }

    override suspend fun update(entry: CveEntry) {
        val idx = entries.indexOfFirst { it.cveId == entry.cveId }
        if (idx >= 0) entries[idx] = entry
    }

    override suspend fun delete(entry: CveEntry) {
        entries.removeAll { it.cveId == entry.cveId }
    }

    override suspend fun deleteById(cveId: String) {
        entries.removeAll { it.cveId == cveId }
    }

    override suspend fun deleteAll() {
        entries.clear()
    }

    override suspend fun getById(cveId: String): CveEntry? =
        entries.find { it.cveId == cveId }

    override fun observeAll() = throw UnsupportedOperationException("Use getAll() in tests")

    override suspend fun getAll(): List<CveEntry> = entries.toList()

    override suspend fun count(): Int = entries.size

    override suspend fun getByComponent(component: String): List<CveEntry> =
        entries.filter { it.component == component }

    override fun observeByComponent(component: String) =
        throw UnsupportedOperationException("Use getByComponent() in tests")

    override suspend fun getDistinctComponents(): List<String> =
        entries.map { it.component }.distinct().sorted()

    override suspend fun getAffectingVersion(version: String): List<CveEntry> =
        entries.filter { version in it.affectedVersions }
            .sortedByDescending { it.cvssScore }

    override fun observeAffectingVersion(version: String) =
        throw UnsupportedOperationException("Use getAffectingVersion() in tests")

    override suspend fun getFixedInVersion(version: String): List<CveEntry> =
        entries.filter { version in it.fixedVersions }
            .sortedByDescending { it.cvssScore }

    override suspend fun getByExploitationStatus(status: ExploitationStatus): List<CveEntry> =
        entries.filter { it.exploitationStatus == status }

    override suspend fun getActivelyExploited(): List<CveEntry> =
        entries.filter { it.exploitationStatus == ExploitationStatus.ACTIVE_EXPLOITATION }

    override fun observeActivelyExploited() =
        throw UnsupportedOperationException("Use getActivelyExploited() in tests")

    override suspend fun getHighConfidence(): List<CveEntry> =
        entries.filter { it.confidence in listOf(ConfidenceLevel.CONFIRMED, ConfidenceLevel.HIGH) }

    override suspend fun getUnreviewed(): List<CveEntry> =
        entries.filter { !it.reviewed }

    override suspend fun getByVulnType(type: VulnerabilityType): List<CveEntry> =
        entries.filter { it.vulnerabilityType == type }

    override suspend fun getUnpatchedForVersion(version: String): List<CveEntry> =
        entries.filter { version in it.affectedVersions && version !in it.fixedVersions }
            .sortedByDescending { it.cvssScore }

    override fun observeUnpatchedForVersion(version: String) =
        throw UnsupportedOperationException("Use getUnpatchedForVersion() in tests")

    override suspend fun search(query: String, limit: Int): List<CveEntry> =
        entries.filter {
            query in it.cveId || query in it.component ||
                    query in it.summary || query in it.notes
        }.take(limit)

    override suspend fun componentStats(): List<ComponentStat> =
        entries.groupBy { it.component }
            .map { (comp, list) -> ComponentStat(comp, list.size) }
            .sortedByDescending { it.cnt }

    override suspend fun exploitationStats(): List<ExploitationStat> =
        entries.groupBy { it.exploitationStatus }
            .map { (status, list) -> ExploitationStat(status, list.size) }
}

/**
 * Unit tests for [PatchStateAnalyzer].
 */
class PatchStateAnalyzerTest {

    private lateinit var fakeDao: FakeCveDao
    private lateinit var analyzer: PatchStateAnalyzer

    @Before
    fun setup() {
        fakeDao = FakeCveDao()
        analyzer = PatchStateAnalyzer(fakeDao)
        analyzer.seedDefaultMappings()
    }

    // ── Helper to load seed data ──

    private suspend fun loadSeedData() {
        val importer = CveImporter(fakeDao)
        importer.importSeedData()
    }

    // ── Tests ──

    @Test
    fun `iOS 26_0 device should show maximum exposure`() = runBlockingTest {
        loadSeedData()

        val observation = DeviceObservation(
            deviceId = "TEST-001",
            iosVersion = "26.0"
        )

        val report = analyzer.analyze(observation)

        assertEquals("26.0", report.iosVersion)
        assertTrue("Should have exposed CVEs", report.exposedCount > 0)
        assertTrue("Risk score should be elevated", report.overallRiskScore > 3.0)
        assertTrue("Total analyzed > 0", report.totalCvesAnalyzed > 0)
    }

    @Test
    fun `iOS 26_3 device should show minimal exposure`() = runBlockingTest {
        loadSeedData()

        val observation = DeviceObservation(
            deviceId = "TEST-002",
            iosVersion = "26.3"
        )

        val report = analyzer.analyze(observation)

        assertEquals("26.3", report.iosVersion)
        // 26.3 is the latest — most CVEs should be patched
        // Only CVE-2026-10006 (IOKit, unpatched across all versions) should remain
        assertTrue("Should have fewer exposed CVEs than 26.0",
            report.exposedCount <= 2)
    }

    @Test
    fun `component build observation resolves uncertainty`() = runBlockingTest {
        loadSeedData()

        val observation = DeviceObservation(
            deviceId = "TEST-003",
            iosVersion = "26.1",
            observedComponents = listOf(
                ObservedComponentVersion(
                    component = "WebKit",
                    observedBuild = "618.1.15", // newer than expected for 26.1
                    confidence = ConfidenceLevel.HIGH
                )
            )
        )

        val report = analyzer.analyze(observation)

        // CVE-2026-10001 (fixedComponentBuild = 618.1.15) should be PATCHED
        // because observed WebKit build >= fix build
        val webkitUaf = report.patchedCves.find { it.cveId == "CVE-2026-10001" }
        assertNotNull("WebKit UAF should be marked PATCHED due to component build", webkitUaf)
    }

    @Test
    fun `critical exposure count tracks active exploitation`() = runBlockingTest {
        loadSeedData()

        val critCount = analyzer.countCriticalExposures("26.1")

        // CVE-2026-10001 (WebKit UAF, active) and CVE-2026-10003 (Kernel LPE, active)
        // Both affect 26.1 and are NOT fixed in 26.1
        assertEquals(2, critCount)
    }

    @Test
    fun `empty CVE database returns clean report`() = runBlockingTest {
        // No seed data loaded
        val observation = DeviceObservation(
            deviceId = "TEST-004",
            iosVersion = "26.0"
        )

        val report = analyzer.analyze(observation)

        assertEquals(0, report.totalCvesAnalyzed)
        assertEquals(0, report.exposedCount)
        assertEquals(0.0, report.overallRiskScore, 0.001)
    }

    @Test
    fun `report includes component coverage info`() = runBlockingTest {
        loadSeedData()

        val observation = DeviceObservation(
            deviceId = "TEST-005",
            iosVersion = "26.1",
            observedComponents = listOf(
                ObservedComponentVersion("WebKit", "618.1.5"),
                ObservedComponentVersion("Kernel", "10000.10.1")
            )
        )

        val report = analyzer.analyze(observation)

        assertTrue("Should have component coverage info",
            report.componentCoverage.isNotEmpty())

        val webkitCoverage = report.componentCoverage["WebKit"]
        assertNotNull("WebKit coverage should exist", webkitCoverage)
        assertTrue("WebKit should have observed build", webkitCoverage!!.hasObservedBuild)
    }

    // ── Test runner ──

    /**
     * Mini coroutine test runner (avoids kotlinx-coroutines-test for simplicity).
     * In production tests, use runTest from kotlinx-coroutines-test.
     */
    private fun runBlockingTest(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking { block() }
    }
}

/**
 * Unit tests for [VersionMappingEngine].
 */
class VersionMappingEngineTest {

    private lateinit var engine: VersionMappingEngine

    @Before
    fun setup() {
        engine = VersionMappingEngine()
        engine.loadDefaults()
    }

    @Test
    fun `defaults load all iOS 26_x versions`() {
        val versions = engine.getTrackedVersions()
        assertTrue("26.0" in versions)
        assertTrue("26.1" in versions)
        assertTrue("26.2" in versions)
        assertTrue("26.3" in versions)
    }

    @Test
    fun `expected builds are correct for 26_0`() {
        assertEquals("10000.0.1", engine.getExpectedBuild("26.0", "Kernel"))
        assertEquals("618.1.1", engine.getExpectedBuild("26.0", "WebKit"))
        assertEquals("1200.0.1", engine.getExpectedBuild("26.0", "dyld"))
    }

    @Test
    fun `silent update detected when build newer than expected`() {
        val result = engine.checkForSilentUpdate("26.1", "WebKit", "618.1.12")
        assertNotNull("Should detect update", result)
        assertTrue("Should be newer", result!!.isNewer)
        assertFalse("Should not be older", result.isOlder)
    }

    @Test
    fun `no silent update when build matches expected`() {
        val result = engine.checkForSilentUpdate("26.1", "WebKit", "618.1.5")
        assertNull("No update expected when builds match", result)
    }

    @Test
    fun `semantically equivalent build strings do not trigger silent update`() {
        val result = engine.checkForSilentUpdate("26.1", "WebKit", "618.1.5.0")
        assertNull("Equivalent dotted versions should not be treated as a silent update", result)
    }

    @Test
    fun `downgrade detected when build older than expected`() {
        val result = engine.checkForSilentUpdate("26.2", "Kernel", "10000.10.1")
        assertNotNull("Should detect downgrade", result)
        assertTrue("Should be older", result!!.isOlder)
    }

    @Test
    fun `unknown version returns null`() {
        assertNull(engine.getExpectedBuild("99.9", "Kernel"))
    }

    @Test
    fun `unknown component returns null`() {
        assertNull(engine.getExpectedBuild("26.0", "NonExistentComponent"))
    }
}

/**
 * Unit tests for [CveImporter].
 */
class CveImporterTest {

    private lateinit var fakeDao: FakeCveDao
    private lateinit var importer: CveImporter

    @Before
    fun setup() {
        fakeDao = FakeCveDao()
        importer = CveImporter(fakeDao)
    }

    @Test
    fun `seed data imports successfully`() = runBlockingTest {
        val result = importer.importSeedData()

        assertTrue("Should import entries", result.inserted > 0)
        assertEquals(0, result.errors)
        assertTrue("Should have at least 8 seed entries", result.totalParsed >= 8)
    }

    @Test
    fun `duplicate import updates existing entries`() = runBlockingTest {
        val first = importer.importSeedData()
        val second = importer.importSeedData()

        assertTrue("First import should insert", first.inserted > 0)
        assertTrue("Second import should update", second.updated > 0)
        assertEquals(0, second.errors)
    }

    @Test
    fun `invalid CVE ID is rejected`() = runBlockingTest {
        val badEntry = CveEntry(
            cveId = "NOT-A-CVE",
            component = "WebKit"
        )
        val result = importer.importEntries(listOf(badEntry))

        assertEquals(1, result.errors)
        assertEquals(0, result.inserted)
    }

    @Test
    fun `invalid CVSS score is rejected`() = runBlockingTest {
        val badEntry = CveEntry(
            cveId = "CVE-2026-99999",
            component = "Kernel",
            cvssScore = 15.0 // invalid — max is 10.0
        )
        val result = importer.importEntries(listOf(badEntry))

        assertEquals(1, result.errors)
        assertEquals(0, result.inserted)
    }

    @Test
    fun `blank component is rejected`() = runBlockingTest {
        val badEntry = CveEntry(
            cveId = "CVE-2026-99998",
            component = ""
        )
        val result = importer.importEntries(listOf(badEntry))

        assertEquals(1, result.errors)
    }

    private fun runBlockingTest(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking { block() }
    }
}

/**
 * Unit tests for [CveTypeConverters].
 */
class CveTypeConvertersTest {

    private val converters = CveTypeConverters()

    @Test
    fun `string lists round trip without partial-match ambiguity`() {
        val encoded = converters.fromStringList(listOf("26.1", "26.10"))

        assertEquals("|||26.1|||26.10|||", encoded)
        assertEquals(listOf("26.1", "26.10"), converters.toStringList(encoded))
    }

    @Test
    fun `provenance round trip preserves all fields`() {
        val provenance = SourceProvenance(
            sourceUrl = "https://example.test/advisory",
            sourceName = "Unit Test Advisory",
            retrievedAt = 123456789L,
            analystVerified = true,
            analystNotes = "verified"
        )

        val encoded = converters.fromProvenance(provenance)
        val decoded = converters.toProvenance(encoded)

        assertEquals(provenance, decoded)
    }

    @Test
    fun `provenance round trip preserves empty analyst notes`() {
        val provenance = SourceProvenance(
            sourceUrl = "https://example.test/advisory",
            sourceName = "Unit Test Advisory",
            retrievedAt = 123456789L,
            analystVerified = false,
            analystNotes = ""
        )

        val encoded = converters.fromProvenance(provenance)
        val decoded = converters.toProvenance(encoded)

        assertEquals(provenance, decoded)
    }
}
