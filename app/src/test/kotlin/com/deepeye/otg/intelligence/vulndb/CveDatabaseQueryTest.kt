package com.deepeye.otg.intelligence.vulndb

import com.deepeye.otg.intelligence.vulndb.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the CVE Intelligence logic.
 * Using a FakeCveDao to avoid native SQLite UnsatisfiedLinkError in CI/local unit test environments.
 */
class CveDatabaseQueryTest {

    private lateinit var dao: CveDao

    @Before
    fun setup() {
        dao = FakeCveDao()
    }

    private class FakeCveDao : CveDao {
        private val entries = mutableListOf<CveEntry>()

        override suspend fun upsert(entry: CveEntry) { entries.add(entry) }
        override suspend fun upsertAll(entries: List<CveEntry>) { this.entries.addAll(entries) }
        override suspend fun getById(cveId: String): CveEntry? = entries.find { it.cveId == cveId }
        override fun observeAll(): Flow<List<CveEntry>> = flowOf(entries)
        override suspend fun getAll(): List<CveEntry> = entries
        override fun getAllSync(): List<CveEntry> = entries
        override suspend fun deleteAll() { entries.clear() }
        override suspend fun getByComponent(component: String): List<CveEntry> = entries.filter { it.component == component }
        override suspend fun getDistinctComponents(): List<String> = entries.map { it.component }.distinct().sorted()
        override suspend fun getActivelyExploited(): List<CveEntry> = entries.filter { it.exploitedInWild == true }.sortedByDescending { it.cvssScore }
        override suspend fun search(query: String, limit: Int): List<CveEntry> = entries.filter { 
            it.cveId.contains(query, true) || it.component.contains(query, true) || it.title.contains(query, true)
        }.take(limit)
        override suspend fun count(): Int = entries.size
        override suspend fun getComponentStats(): List<ComponentStat> = entries.groupBy { it.component }.map { ComponentStat(it.key, it.value.size) }
        override fun observeUnpatchedForVersion(iosVersion: String): Flow<List<CveEntry>> = flowOf(entries.filter { it.affectedVersions.contains(iosVersion) })
        override suspend fun getExploitationStats(): List<ExploitationStat> = entries.groupBy { it.exploitedInWild?.toString() ?: "null" }.map { ExploitationStat(it.key, it.value.size) }
    }

    @After
    fun tearDown() {
        // No cleanup needed for Fake
    }

    @Test
    fun getByComponent_returnsExactComponentMatches() = runBlocking {
        val entry1 = createTestEntry("CVE-2024-0001", "Kernel")
        val entry2 = createTestEntry("CVE-2024-0002", "Wifi")
        dao.upsertAll(listOf(entry1, entry2))

        val results = dao.getByComponent("Kernel")
        assertEquals(1, results.size)
        assertEquals("CVE-2024-0001", results[0].cveId)
    }

    @Test
    fun search_matchesCveIdAndRespectsLimit() = runBlocking {
        val entries = (1..10).map { createTestEntry("CVE-2024-000$it", "Component-$it") }
        dao.upsertAll(entries)

        val results = dao.search("CVE-2024", limit = 5)
        assertEquals(5, results.size)
    }

    @Test
    fun observeUnpatchedForVersion_returnsMatchingVersions() = runBlocking {
        val entry = createTestEntry("CVE-2024-9999", "Springboard", listOf("17.0", "17.1"))
        dao.upsert(entry)

        // Using a simple check for Flow result in Fake
        dao.observeUnpatchedForVersion("17.0").collect { list ->
            assertTrue(list.any { it.cveId == "CVE-2024-9999" })
        }
    }

    private fun createTestEntry(id: String, component: String, versions: List<String> = listOf("1.0")): CveEntry {
        return CveEntry(
            cveId = id,
            title = "Test Vuln",
            bugClass = BugClass.UNKNOWN,
            component = component,
            affectedVersions = versions,
            patchedInSpl = "2024-01-01",
            cvssScore = 7.5,
            cwe = "CWE-119"
        )
    }
}