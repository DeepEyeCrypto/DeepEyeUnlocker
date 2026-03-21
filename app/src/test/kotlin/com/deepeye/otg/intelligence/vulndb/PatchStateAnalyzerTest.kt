package com.deepeye.otg.intelligence.vulndb

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeCveDao : CveDao {
    private val entries = linkedMapOf<String, CveEntry>()

    override suspend fun upsert(entry: CveEntry) {
        entries[entry.cveId] = entry
    }

    override suspend fun upsertAll(entryList: List<CveEntry>) {
        entryList.forEach { upsert(it) }
    }

    override suspend fun deleteAll() {
        entries.clear()
    }

    override suspend fun getById(cveId: String): CveEntry? =
        entries[cveId]

    override fun observeAll(): Flow<List<CveEntry>> = flowOf(getAllSync())

    override suspend fun getAll(): List<CveEntry> = getAllSync()

    override fun getAllSync(): List<CveEntry> = entries.values.sortedByDescending { it.updatedAt }

    override suspend fun count(): Int = entries.size

    override suspend fun getByComponent(component: String): List<CveEntry> =
        getAllSync().filter { it.component == component }

    override suspend fun getDistinctComponents(): List<String> =
        getAllSync().map { it.component }.distinct().sorted()

    override suspend fun getActivelyExploited(): List<CveEntry> =
        getAllSync().filter { it.exploitedInWild == true }

    override suspend fun search(query: String, limit: Int): List<CveEntry> =
        getAllSync().filter {
            query.contains(it.cveId) ||
                it.cveId.contains(query, ignoreCase = true) ||
                it.component.contains(query, ignoreCase = true) ||
                it.title.contains(query, ignoreCase = true) ||
                it.notes.contains(query, ignoreCase = true)
        }.take(limit)

    override suspend fun getComponentStats(): List<ComponentStat> =
        getAllSync().groupBy { it.component }
            .map { (comp, list) -> ComponentStat(comp, list.size) }
            .sortedByDescending { it.cnt }

    override fun observeUnpatchedForVersion(iosVersion: String): Flow<List<CveEntry>> =
        flowOf(
            getAllSync().filter { entry ->
                entry.affectedVersions.joinToString("|||").contains(iosVersion)
            }
        )

    override suspend fun getExploitationStats(): List<ExploitationStat> =
        getAllSync()
            .groupBy { it.exploitedInWild?.toString() ?: "null" }
            .map { (status, list) -> ExploitationStat(status, list.size) }
}

class PatchStateAnalyzerTest {
    private lateinit var fakeDao: FakeCveDao
    private lateinit var analyzer: PatchStateAnalyzer

    @Before
    fun setup() {
        fakeDao = FakeCveDao()
        analyzer = PatchStateAnalyzer(fakeDao)
    }

    @Test
    fun `qualcomm components use qti spl when available`() = runBlockingTest {
        fakeDao.upsert(
            testCve(
                cveId = "CVE-QTI-0001",
                component = "Qualcomm DSP",
                patchedInSpl = "2024-11-01"
            )
        )

        val observation = DeviceObservation(
            brand = "Google",
            model = "Pixel 8",
            androidSpl = "2024-12-01",
            qtiSpl = "2024-10-01"
        )

        val report = analyzer.analyze(observation)

        assertEquals(listOf("CVE-QTI-0001"), report.exposedCves.map { it.cveId })
        assertEquals(RiskLevel.LOW, report.overallRiskLevel)
    }

    @Test
    fun `mediatek components use mtk spl when available`() = runBlockingTest {
        fakeDao.upsert(
            testCve(
                cveId = "CVE-MTK-0001",
                component = "MediaTek Preloader",
                patchedInSpl = "2025-02-01"
            )
        )

        val observation = DeviceObservation(
            brand = "Realme",
            model = "14x",
            androidSpl = "2025-03-01",
            mtkSpl = "2025-01-01"
        )

        val report = analyzer.analyze(observation)

        assertEquals(listOf("CVE-MTK-0001"), report.exposedCves.map { it.cveId })
        assertTrue(report.patchedCves.isEmpty())
    }

    @Test
    fun `critical exploited exposure raises risk to critical`() = runBlockingTest {
        fakeDao.upsert(
            testCve(
                cveId = "CVE-CRIT-0001",
                component = "Android Framework",
                patchedInSpl = "2025-04-01",
                cvssScore = 9.8,
                exploitedInWild = true
            )
        )

        val observation = DeviceObservation(
            brand = "Samsung",
            model = "Galaxy A55",
            androidSpl = "2025-03-01"
        )

        val report = analyzer.analyze(observation)

        assertEquals(RiskLevel.CRITICAL, report.overallRiskLevel)
        assertEquals(1, report.exposedCves.size)
    }

    @Test
    fun `android spl at patch level marks cve as patched`() = runBlockingTest {
        fakeDao.upsert(
            testCve(
                cveId = "CVE-PATCHED-0001",
                component = "Android Framework",
                patchedInSpl = "2025-01-01"
            )
        )

        val report = analyzer.analyze(
            DeviceObservation(
                brand = "Nothing",
                model = "Phone 2",
                androidSpl = "2025-01-01"
            )
        )

        assertEquals(listOf("CVE-PATCHED-0001"), report.patchedCves.map { it.cveId })
        assertTrue(report.exposedCves.isEmpty())
    }

    @Test
    fun `empty CVE database returns clean report`() = runBlockingTest {
        val observation = DeviceObservation(
            brand = "Unknown",
            model = "Unknown",
            deviceId = "TEST-004",
            androidSpl = "2025-01-01"
        )

        val report = analyzer.analyze(observation)

        assertTrue(report.exposedCves.isEmpty())
        assertTrue(report.patchedCves.isEmpty())
        assertEquals(RiskLevel.SAFE, report.overallRiskLevel)
    }

    private fun runBlockingTest(block: suspend () -> Unit) {
        runBlocking { block() }
    }
}

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
        importer.importSeedData()

        assertTrue("Should import entries", fakeDao.count() > 0)
        assertNotNull(fakeDao.getById("CVE-2024-43093"))
    }

    @Test
    fun `duplicate seed import replaces existing entries without growing count`() = runBlockingTest {
        importer.importSeedData()
        val firstCount = fakeDao.count()
        importer.importSeedData()
        val secondCount = fakeDao.count()

        assertEquals(firstCount, secondCount)
        assertTrue(secondCount > 0)
    }

    @Test
    fun `empty json array import is a successful no-op`() = runBlockingTest {
        val result = importer.importFromJson("[]")

        assertTrue(result.success)
        assertEquals(0, result.totalProcessed)
        assertEquals(0, fakeDao.count())
    }

    private fun runBlockingTest(block: suspend () -> Unit) {
        runBlocking { block() }
    }
}

class CveTypeConvertersTest {

    private val converters = CveTypeConverters()

    @Test
    fun `string lists round trip without partial-match ambiguity`() {
        val encoded = converters.fromStringList(listOf("26.1", "26.10"))

        assertEquals("26.1|||26.10", encoded)
        assertEquals(listOf("26.1", "26.10"), converters.toStringList(encoded))
    }

    @Test
    fun `bug class round trip preserves enum`() {
        val encoded = converters.fromBugClass(BugClass.LogicFlaw)

        assertEquals(BugClass.LogicFlaw, converters.toBugClass(encoded))
    }

    @Test
    fun `unknown bug class falls back to UNKNOWN`() {
        assertEquals(BugClass.UNKNOWN, converters.toBugClass("NOT_REAL"))
    }

    @Test
    fun `confidence round trip preserves enum`() {
        val encoded = converters.fromConfidence(ConfidenceLevel.HIGH)

        assertEquals(ConfidenceLevel.HIGH, converters.toConfidence(encoded))
    }
}

private fun testCve(
    cveId: String,
    component: String,
    patchedInSpl: String?,
    cvssScore: Double? = 5.0,
    exploitedInWild: Boolean? = false,
    affectedVersions: List<String> = listOf("Android 14")
): CveEntry = CveEntry(
    cveId = cveId,
    title = "$cveId regression fixture",
    bugClass = BugClass.LogicFlaw,
    component = component,
    affectedVersions = affectedVersions,
    patchedInSpl = patchedInSpl,
    cvssScore = cvssScore,
    cwe = "CWE-000",
    exploitedInWild = exploitedInWild,
    confidence = ConfidenceLevel.HIGH,
    notes = "Unit test fixture"
)
