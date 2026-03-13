package com.deepeye.otg.intelligence.vulndb

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Integration tests for Room query semantics.
 *
 * These regression tests protect against substring matches when serialized
 * version lists contain values like 26.1 and 26.10.
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
    fun `getAffectingVersion matches exact versions only`() = runBlocking {
        dao.upsertAll(
            listOf(
                entry(
                    cveId = "CVE-2026-21001",
                    affectedVersions = listOf("26.1")
                ),
                entry(
                    cveId = "CVE-2026-21002",
                    affectedVersions = listOf("26.10")
                )
            )
        )

        val affecting = dao.getAffectingVersion("26.1")

        assertEquals(listOf("CVE-2026-21001"), affecting.map { it.cveId })
    }

    @Test
    fun `getFixedInVersion matches exact versions only`() = runBlocking {
        dao.upsertAll(
            listOf(
                entry(
                    cveId = "CVE-2026-21003",
                    affectedVersions = listOf("26.0", "26.1"),
                    fixedVersions = listOf("26.1")
                ),
                entry(
                    cveId = "CVE-2026-21004",
                    affectedVersions = listOf("26.0", "26.1"),
                    fixedVersions = listOf("26.10")
                )
            )
        )

        val fixed = dao.getFixedInVersion("26.1")

        assertEquals(listOf("CVE-2026-21003"), fixed.map { it.cveId })
    }

    @Test
    fun `getUnpatchedForVersion does not treat 26_10 as fixed for 26_1`() = runBlocking {
        dao.upsertAll(
            listOf(
                entry(
                    cveId = "CVE-2026-21005",
                    affectedVersions = listOf("26.1"),
                    fixedVersions = listOf("26.10")
                ),
                entry(
                    cveId = "CVE-2026-21006",
                    affectedVersions = listOf("26.1"),
                    fixedVersions = listOf("26.1")
                )
            )
        )

        val unpatched = dao.getUnpatchedForVersion("26.1")

        assertEquals(listOf("CVE-2026-21005"), unpatched.map { it.cveId })
    }

    private fun entry(
        cveId: String,
        affectedVersions: List<String>,
        fixedVersions: List<String> = emptyList()
    ): CveEntry = CveEntry(
        cveId = cveId,
        component = "Kernel",
        vulnerabilityType = VulnerabilityType.PRIVILEGE_ESCALATION,
        affectedVersions = affectedVersions,
        fixedVersions = fixedVersions,
        exploitationStatus = ExploitationStatus.UNKNOWN,
        cvssScore = 7.0,
        confidence = ConfidenceLevel.HIGH,
        summary = "Regression test fixture"
    )
}