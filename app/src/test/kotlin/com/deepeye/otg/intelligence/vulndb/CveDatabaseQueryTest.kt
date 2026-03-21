package com.deepeye.otg.intelligence.vulndb

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Integration tests for the current Room DAO surface.
 */
@RunWith(RobolectricTestRunner::class)
class CveDatabaseQueryTest {

    private lateinit var database: CveDatabase
    private lateinit var dao: CveDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CveDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.cveDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `search matches cve id and respects limit`() = runBlocking {
        dao.upsertAll(
            listOf(
                entry(
                    cveId = "CVE-2026-21001",
                    affectedVersions = listOf("26.1"),
                    title = "Kernel token collision regression"
                ),
                entry(
                    cveId = "CVE-2026-21002",
                    affectedVersions = listOf("26.10"),
                    title = "Unrelated entry"
                )
            )
        )

        val affecting = dao.search(query = "21001", limit = 1)

        assertEquals(1, affecting.size)
        assertEquals(listOf("CVE-2026-21001"), affecting.map { it.cveId })
    }

    @Test
    fun `getByComponent returns exact component matches`() = runBlocking {
        dao.upsertAll(
            listOf(
                entry(
                    cveId = "CVE-2026-21003",
                    affectedVersions = listOf("26.0", "26.1"),
                    component = "Kernel"
                ),
                entry(
                    cveId = "CVE-2026-21004",
                    affectedVersions = listOf("26.0", "26.1"),
                    component = "WebKit"
                )
            )
        )

        val fixed = dao.getByComponent("Kernel")

        assertEquals(listOf("CVE-2026-21003"), fixed.map { it.cveId })
        assertTrue(fixed.all { it.component == "Kernel" })
    }

    @Test
    fun `observeUnpatchedForVersion returns matching versions from current DAO surface`() = runBlocking {
        dao.upsertAll(
            listOf(
                entry(
                    cveId = "CVE-2026-21005",
                    affectedVersions = listOf("26.1")
                ),
                entry(
                    cveId = "CVE-2026-21006",
                    affectedVersions = listOf("27.0")
                )
            )
        )

        val unpatched = dao.observeUnpatchedForVersion("26.1").first()

        assertEquals(listOf("CVE-2026-21005"), unpatched.map { it.cveId })
        assertTrue(unpatched.none { it.cveId == "CVE-2026-21006" })
    }

    private fun entry(
        cveId: String,
        affectedVersions: List<String>,
        component: String = "Kernel",
        title: String = "Regression test fixture"
    ): CveEntry = CveEntry(
        cveId = cveId,
        title = title,
        bugClass = BugClass.LogicFlaw,
        component = component,
        affectedVersions = affectedVersions,
        patchedInSpl = "2026-01-01",
        cvssScore = 7.0,
        cwe = "CWE-000",
        exploitedInWild = false,
        confidence = ConfidenceLevel.HIGH,
        notes = "Regression test fixture"
    )
}