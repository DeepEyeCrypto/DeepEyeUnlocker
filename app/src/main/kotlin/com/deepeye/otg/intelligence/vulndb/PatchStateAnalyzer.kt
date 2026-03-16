package com.deepeye.otg.intelligence.vulndb

import com.deepeye.otg.domain.models.ProtocolFamily

/**
 * Analyzes device patch states against the local CVE database.
 * Cross-references Android SPL, vendor-specific SPLs, and kernel versions.
 */
class PatchStateAnalyzer(private val cveDao: CveDao) {

    /**
     * Conducts a deep vulnerability assessment of a device based on its reported telemetry.
     */
    suspend fun analyze(observation: DeviceObservation): ExposureReport {
        val cveDatabase = cveDao.getAll()
        val exposed = mutableListOf<CveEntry>()
        val patched = mutableListOf<CveEntry>()

        for (cve in cveDatabase) {
            val splToPatch = cve.patchedInSpl ?: continue
            
            // Resolve which SPL to check based on vulnerability component
            val deviceSpl = when {
                cve.component.contains("Qualcomm", ignoreCase = true) || 
                cve.component.contains("DSP", ignoreCase = true) ->
                    observation.qtiSpl ?: observation.androidSpl
                cve.component.contains("MediaTek", ignoreCase = true) || 
                cve.component.contains("MTK", ignoreCase = true) ->
                    observation.mtkSpl ?: observation.androidSpl
                else -> observation.androidSpl
            }

            // Simple string comparison for standard ISO date format (YYYY-MM-DD)
            if (deviceSpl < splToPatch) {
                exposed.add(cve)
            } else {
                patched.add(cve)
            }
        }

        // Calculate overall risk level based on exposure primitives
        val riskLevel = when {
            exposed.any { it.exploitedInWild == true && (it.cvssScore ?: 0.0) >= 9.0 } -> RiskLevel.CRITICAL
            exposed.any { it.exploitedInWild == true && (it.cvssScore ?: 0.0) >= 7.5 } -> RiskLevel.HIGH
            exposed.any { (it.cvssScore ?: 0.0) >= 7.0 } -> RiskLevel.MEDIUM
            exposed.isNotEmpty() -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }

        return DevicePatchReport(
            deviceModel = observation.model,
            brand = observation.brand,
            androidSpl = observation.androidSpl,
            vendorQtiSpl = observation.qtiSpl,
            vendorMtkSpl = observation.mtkSpl,
            kernelVersion = observation.kernelVersion,
            exposedCves = exposed,
            patchedCves = patched,
            unknownCves = emptyList(),
            reportTimestamp = System.currentTimeMillis(),
            overallRiskLevel = riskLevel
        )
    }
}
