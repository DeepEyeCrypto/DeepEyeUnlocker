package com.deepeye.otg.intelligence.vulndb

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository coordinating CVE data access and patch analysis.
 */
class CveRepository(
    private val cveDao: CveDao,
    private val importer: CveImporter,
    private val analyzer: PatchStateAnalyzer
) {
    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    /**
     * Observe all CVE entries.
     */
    fun getAllEntries(): Flow<List<CveEntry>> = cveDao.observeAll()

    /**
     * Search for CVEs.
     */
    suspend fun search(query: String): List<CveEntry> = cveDao.search(query)

    /**
     * Import seed data if database is empty.
     */
    suspend fun ensureSeedData() {
        if (cveDao.count() == 0) {
            _isImporting.value = true
            try {
                importer.importSeedData()
            } finally {
                _isImporting.value = false
            }
        }
    }

    /**
     * Import CVEs from a JSON source.
     */
    suspend fun importJson(json: String): ImportResult {
        _isImporting.value = true
        return try {
            importer.importFromJson(json)
        } finally {
            _isImporting.value = false
        }
    }

    /**
     * Perform a live patch analysis for a device.
     */
    suspend fun analyzeDevice(observation: DeviceObservation): ExposureReport {
        return analyzer.analyze(observation)
    }

    /**
     * Get unpatched CVEs for a specific version.
     */
    fun observeExposures(version: String): Flow<List<CveEntry>> =
        cveDao.observeUnpatchedForVersion(version)
}
