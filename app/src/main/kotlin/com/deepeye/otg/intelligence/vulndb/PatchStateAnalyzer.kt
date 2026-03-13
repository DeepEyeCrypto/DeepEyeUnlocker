package com.deepeye.otg.intelligence.vulndb

// Note: avoid android.util.Log for JVM tests; use LogSafe instead.
// import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ──────────────────────────────────────────────────────────────
// Patch State Analyzer
// DeepEye OTG — CVE Intelligence Module (Part 1)
// ──────────────────────────────────────────────────────────────

private const val TAG = "PatchStateAnalyzer"

/**
 * Observed component-level version metadata from a device.
 *
 * iOS ships multiple independently-versioned components;
 * the OS version alone does not determine the full patch state.
 * For example, WebKit may receive silent updates via App Store
 * without an iOS version bump.
 */
data class ObservedComponentVersion(
    val component: String,           // e.g. "WebKit", "Safari", "Kernel"
    val observedBuild: String?,      // e.g. "618.1.15.10.5" or null if unknown
    val observedAt: Long = System.currentTimeMillis(),
    val source: String = "device_inspection", // how we got this data
    val confidence: ConfidenceLevel = ConfidenceLevel.MEDIUM
)

/**
 * Complete observed device state for patch-state analysis.
 */
data class DeviceObservation(
    val deviceId: String,
    val iosVersion: String,                           // e.g. "26.1"
    val iosBuildId: String? = null,                    // e.g. "24A345"
    val observedComponents: List<ObservedComponentVersion> = emptyList(),
    val observedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

/**
 * Exposure classification for a single CVE on a specific device.
 */
enum class ExposureStatus {
    /** CVE is confirmed patched on this device */
    PATCHED,
    /** CVE is confirmed unpatched / device is vulnerable */
    EXPOSED,
    /** Cannot determine — missing component version or low confidence */
    UNCERTAIN,
    /** Component not present on this device */
    NOT_APPLICABLE
}

/**
 * Single CVE exposure assessment result.
 */
data class CveExposureResult(
    val cveId: String,
    val component: String,
    val vulnerabilityType: VulnerabilityType,
    val cvssScore: Double?,
    val exploitationStatus: ExploitationStatus,
    val exposureStatus: ExposureStatus,
    val confidence: ConfidenceLevel,
    val reasoning: String,
    val summary: String = ""
)

/**
 * Complete effective-exposure report for a device.
 */
data class ExposureReport(
    val deviceId: String,
    val iosVersion: String,
    val iosBuildId: String?,
    val generatedAt: Long = System.currentTimeMillis(),
    val totalCvesAnalyzed: Int,
    val exposedCves: List<CveExposureResult>,
    val patchedCves: List<CveExposureResult>,
    val uncertainCves: List<CveExposureResult>,
    val notApplicableCves: List<CveExposureResult>,
    val componentCoverage: Map<String, ComponentCoverageInfo>,
    val overallRiskScore: Double,
    val analystNotes: String = ""
) {
    val exposedCount: Int get() = exposedCves.size
    val patchedCount: Int get() = patchedCves.size
    val uncertainCount: Int get() = uncertainCves.size
    val activeExploitExposed: List<CveExposureResult>
        get() = exposedCves.filter {
            it.exploitationStatus == ExploitationStatus.ACTIVE_EXPLOITATION
        }
}

/**
 * How well we can assess a specific component on this device.
 */
data class ComponentCoverageInfo(
    val component: String,
    val hasObservedBuild: Boolean,
    val observedBuild: String?,
    val totalCves: Int,
    val exposedCount: Int,
    val patchedCount: Int,
    val uncertainCount: Int
)

/**
 * Mapping rule: iOS version range → component expected builds.
 * Used to determine what builds normally ship with each iOS version.
 */
data class VersionComponentMapping(
    val iosVersion: String,
    val component: String,
    val expectedBuild: String,
    val confidence: ConfidenceLevel = ConfidenceLevel.HIGH,
    val notes: String = ""
)

/**
 * Analyzes the effective security/patch state of a device by correlating:
 * - observed iOS version
 * - observed component builds (where available)
 * - local CVE database entries
 * - version ↔ component mapping rules
 *
 * Key design decisions:
 * 1. Separates OS-level from component-level analysis
 * 2. Handles partial/uncertain observations
 * 3. Tracks confidence for each assessment
 * 4. Produces actionable exposure reports
 */
class PatchStateAnalyzer(private val cveDao: CveDao) {

    // ── Known version → component build mappings ────────────────
    // In production, load from JSON or a separate Room table.
    // These are seed values for iOS 26.x research.

    private val versionMappings = mutableListOf<VersionComponentMapping>()

    /**
     * Register known version → component mappings.
     * Call during initialization or when importing new intelligence.
     */
    fun registerMappings(mappings: List<VersionComponentMapping>) {
        versionMappings.addAll(mappings)
        LogSafe.i(TAG, "Registered ${mappings.size} version-component mappings (total: ${versionMappings.size})")
    }

    /**
     * Seed default iOS 26.x mappings for initial research.
     */
    fun seedDefaultMappings() {
        val defaults = listOf(
            // iOS 26.0
            VersionComponentMapping("26.0", "Kernel", "10000.0.1"),
            VersionComponentMapping("26.0", "WebKit", "618.1.1"),
            VersionComponentMapping("26.0", "dyld", "1200.0.1"),
            VersionComponentMapping("26.0", "IOKit", "1.0.0"),
            // iOS 26.1
            VersionComponentMapping("26.1", "Kernel", "10000.10.1"),
            VersionComponentMapping("26.1", "WebKit", "618.1.5"),
            VersionComponentMapping("26.1", "dyld", "1200.0.5"),
            // iOS 26.2
            VersionComponentMapping("26.2", "Kernel", "10000.20.1"),
            VersionComponentMapping("26.2", "WebKit", "618.1.10"),
            VersionComponentMapping("26.2", "dyld", "1200.1.0"),
            // iOS 26.3
            VersionComponentMapping("26.3", "Kernel", "10000.30.1"),
            VersionComponentMapping("26.3", "WebKit", "618.1.15"),
            VersionComponentMapping("26.3", "dyld", "1200.2.0"),
        )
        registerMappings(defaults)
    }

    // ── Core Analysis ───────────────────────────────────────────

    /**
     * Analyze a device's effective security state.
     *
     * @param observation observed device metadata
     * @return full exposure report
     */
    suspend fun analyze(observation: DeviceObservation): ExposureReport {
        LogSafe.i(TAG, "Analyzing device=${observation.deviceId} iOS=${observation.iosVersion}")

        // 1. Fetch all CVEs that affect this iOS version
        val affectingCves = cveDao.getAffectingVersion(observation.iosVersion)
        LogSafe.i(TAG, "Found ${affectingCves.size} CVEs potentially affecting iOS ${observation.iosVersion}")

        // 2. Also check CVEs where the version is explicitly in fixedVersions
        val fixedInThisVersion = cveDao.getFixedInVersion(observation.iosVersion)
        val fixedCveIds = fixedInThisVersion.map { it.cveId }.toSet()

        // 3. Build observed component build map for fast lookup
        val observedBuilds = observation.observedComponents
            .associate { it.component to it }

        // 4. Classify each CVE
        val results = mutableListOf<CveExposureResult>()
        val componentStats = mutableMapOf<String, MutableList<CveExposureResult>>()

        for (cve in affectingCves) {
            val result = classifyCve(cve, observation, observedBuilds, fixedCveIds)
            results.add(result)
            componentStats.getOrPut(cve.component) { mutableListOf() }.add(result)
        }

        // 5. Build component coverage info
        val coverage = componentStats.map { (comp, cveResults) ->
            val observed = observedBuilds[comp]
            comp to ComponentCoverageInfo(
                component = comp,
                hasObservedBuild = observed != null,
                observedBuild = observed?.observedBuild,
                totalCves = cveResults.size,
                exposedCount = cveResults.count { it.exposureStatus == ExposureStatus.EXPOSED },
                patchedCount = cveResults.count { it.exposureStatus == ExposureStatus.PATCHED },
                uncertainCount = cveResults.count { it.exposureStatus == ExposureStatus.UNCERTAIN }
            )
        }.toMap()

        // 6. Calculate overall risk score
        val riskScore = calculateRiskScore(results)

        // 7. Partition results
        val exposed = results.filter { it.exposureStatus == ExposureStatus.EXPOSED }
        val patched = results.filter { it.exposureStatus == ExposureStatus.PATCHED }
        val uncertain = results.filter { it.exposureStatus == ExposureStatus.UNCERTAIN }
        val notApplicable = results.filter { it.exposureStatus == ExposureStatus.NOT_APPLICABLE }

        val report = ExposureReport(
            deviceId = observation.deviceId,
            iosVersion = observation.iosVersion,
            iosBuildId = observation.iosBuildId,
            totalCvesAnalyzed = affectingCves.size,
            exposedCves = exposed,
            patchedCves = patched,
            uncertainCves = uncertain,
            notApplicableCves = notApplicable,
            componentCoverage = coverage,
            overallRiskScore = riskScore
        )

        LogSafe.i(TAG, "Analysis complete: ${exposed.size} exposed, ${patched.size} patched, ${uncertain.size} uncertain")

        return report
    }

    /**
     * Observe real-time exposure status for a given iOS version.
     * Emits new report whenever the CVE database changes.
     */
    fun observeExposure(version: String): Flow<List<CveEntry>> {
        return cveDao.observeUnpatchedForVersion(version)
    }

    /**
     * Quick check: how many actively-exploited CVEs affect this version
     * and are NOT yet patched?
     */
    suspend fun countCriticalExposures(iosVersion: String): Int {
        val unpatched = cveDao.getUnpatchedForVersion(iosVersion)
        return unpatched.count {
            it.exploitationStatus == ExploitationStatus.ACTIVE_EXPLOITATION
        }
    }

    // ── Classification Logic ────────────────────────────────────

    /**
     * Classify a single CVE's exposure status on a device.
     *
     * Decision tree:
     * 1. If CVE's fixedVersions includes this iOS version → PATCHED
     * 2. If we have observed component build AND fixedComponentBuild:
     *    - compare builds → PATCHED or EXPOSED
     * 3. If we have version mapping for this component + version:
     *    - use expected build to infer → PATCHED / EXPOSED / UNCERTAIN
     * 4. Otherwise → UNCERTAIN
     */
    private fun classifyCve(
        cve: CveEntry,
        observation: DeviceObservation,
        observedBuilds: Map<String, ObservedComponentVersion>,
        fixedCveIds: Set<String>
    ): CveExposureResult {

        // Case 1: CVE is explicitly fixed in this iOS version
        if (cve.cveId in fixedCveIds || cve.fixedVersions.contains(observation.iosVersion)) {
            return CveExposureResult(
                cveId = cve.cveId,
                component = cve.component,
                vulnerabilityType = cve.vulnerabilityType,
                cvssScore = cve.cvssScore,
                exploitationStatus = cve.exploitationStatus,
                exposureStatus = ExposureStatus.PATCHED,
                confidence = cve.confidence,
                reasoning = "CVE fixed in iOS ${observation.iosVersion} per advisory",
                summary = cve.summary
            )
        }

        // Case 2: We have both observed build and known fix build
        val observedComponent = observedBuilds[cve.component]
        if (observedComponent?.observedBuild != null && cve.fixedComponentBuild != null) {
            val comparison = compareBuildStrings(
                observedComponent.observedBuild,
                cve.fixedComponentBuild
            )
            val status = when {
                comparison >= 0 -> ExposureStatus.PATCHED
                else -> ExposureStatus.EXPOSED
            }
            return CveExposureResult(
                cveId = cve.cveId,
                component = cve.component,
                vulnerabilityType = cve.vulnerabilityType,
                cvssScore = cve.cvssScore,
                exploitationStatus = cve.exploitationStatus,
                exposureStatus = status,
                confidence = minOf(cve.confidence, observedComponent.confidence),
                reasoning = "Component build ${observedComponent.observedBuild} " +
                        "${if (status == ExposureStatus.PATCHED) ">=" else "<"} " +
                        "fix build ${cve.fixedComponentBuild}",
                summary = cve.summary
            )
        }

        // Case 3: Use version → component mapping to infer
        val mapping = versionMappings.find {
            it.iosVersion == observation.iosVersion && it.component == cve.component
        }
        if (mapping != null && cve.fixedComponentBuild != null) {
            val comparison = compareBuildStrings(mapping.expectedBuild, cve.fixedComponentBuild)
            val status = when {
                comparison >= 0 -> ExposureStatus.PATCHED
                else -> ExposureStatus.EXPOSED
            }
            return CveExposureResult(
                cveId = cve.cveId,
                component = cve.component,
                vulnerabilityType = cve.vulnerabilityType,
                cvssScore = cve.cvssScore,
                exploitationStatus = cve.exploitationStatus,
                exposureStatus = status,
                confidence = ConfidenceLevel.MEDIUM, // inferred, not directly observed
                reasoning = "Inferred from version mapping: iOS ${observation.iosVersion} " +
                        "ships ${cve.component} build ${mapping.expectedBuild} " +
                        "${if (status == ExposureStatus.PATCHED) ">=" else "<"} " +
                        "fix build ${cve.fixedComponentBuild}",
                summary = cve.summary
            )
        }

        // Case 4: Can't determine — insufficient data
        return CveExposureResult(
            cveId = cve.cveId,
            component = cve.component,
            vulnerabilityType = cve.vulnerabilityType,
            cvssScore = cve.cvssScore,
            exploitationStatus = cve.exploitationStatus,
            exposureStatus = ExposureStatus.UNCERTAIN,
            confidence = ConfidenceLevel.LOW,
            reasoning = "Insufficient data: no observed build for ${cve.component}, " +
                    "no fix build specified, or no version mapping available",
            summary = cve.summary
        )
    }

    // ── Build String Comparison ─────────────────────────────────

    /**
     * Compare dotted build strings numerically segment by segment.
     * e.g. "618.1.15.10.5" vs "618.1.15"
     *
     * Returns:
     * - positive if a > b
     * - negative if a < b
     * - 0 if equal
     *
     * Missing segments are treated as 0.
     */
    private fun compareBuildStrings(a: String, b: String): Int {
        val partsA = a.split(".").map { it.trim().toIntOrNull() ?: 0 }
        val partsB = b.split(".").map { it.trim().toIntOrNull() ?: 0 }
        val maxLen = maxOf(partsA.size, partsB.size)

        for (i in 0 until maxLen) {
            val segA = partsA.getOrElse(i) { 0 }
            val segB = partsB.getOrElse(i) { 0 }
            if (segA != segB) return segA - segB
        }
        return 0
    }

    // ── Risk Scoring ────────────────────────────────────────────

    /**
     * Calculate overall risk score (0.0–10.0) from exposure results.
     *
     * Scoring factors:
     * - CVSS scores of exposed CVEs (weighted)
     * - Actively exploited CVEs get 2x weight
     * - POC-available CVEs get 1.5x weight
     * - Uncertainty penalty (many uncertain = higher risk assumption)
     */
    private fun calculateRiskScore(results: List<CveExposureResult>): Double {
        if (results.isEmpty()) return 0.0

        val exposed = results.filter { it.exposureStatus == ExposureStatus.EXPOSED }
        val uncertain = results.filter { it.exposureStatus == ExposureStatus.UNCERTAIN }

        if (exposed.isEmpty() && uncertain.isEmpty()) return 0.0

        var weightedSum = 0.0

        for (result in exposed) {
            val baseCvss = result.cvssScore ?: 5.0 // default to medium if unknown
            val multiplier = when (result.exploitationStatus) {
                ExploitationStatus.ACTIVE_EXPLOITATION -> 2.0
                ExploitationStatus.POC_AVAILABLE -> 1.5
                ExploitationStatus.ATTEMPTED -> 1.3
                else -> 1.0
            }
            weightedSum += baseCvss * multiplier
        }

        // Uncertainty penalty: each uncertain CVE adds a small amount
        val uncertaintyPenalty = uncertain.size * 0.3

        // Normalize to 0–10 scale
        val rawScore = (weightedSum / maxOf(exposed.size, 1)) + uncertaintyPenalty
        return rawScore.coerceIn(0.0, 10.0)
    }

    // ── Helpers ─────────────────────────────────────────────────

    /**
     * Get the minimum confidence from two levels.
     * Used when combining device observation confidence with CVE confidence.
     */
    private fun minOf(a: ConfidenceLevel, b: ConfidenceLevel): ConfidenceLevel {
        val order = listOf(
            ConfidenceLevel.UNVERIFIED,
            ConfidenceLevel.LOW,
            ConfidenceLevel.MEDIUM,
            ConfidenceLevel.HIGH,
            ConfidenceLevel.CONFIRMED
        )
        return if (order.indexOf(a) <= order.indexOf(b)) a else b
    }
}
