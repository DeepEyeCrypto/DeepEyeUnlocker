package com.deepeye.otg.intelligence.vulndb

import javax.inject.Inject

// Avoid android.util.Log in JVM tests; use LogSafe.

// ──────────────────────────────────────────────────────────────
// Version Mapping Engine
// DeepEye OTG — CVE Intelligence Module (Part 1)
// ──────────────────────────────────────────────────────────────

private const val TAG = "VersionMapping"

/**
 * Maps iOS versions to expected component builds.
 *
 * Apple ships multiple independently-versioned subsystems.
 * A single iOS version (e.g. 26.2) includes specific builds of:
 * - Kernel (XNU)
 * - WebKit / JavaScriptCore
 * - dyld
 * - IOKit drivers
 * - Safari
 * - AMFI, Sandbox, SEP firmware, etc.
 *
 * Additionally, some components (notably WebKit/Safari) can receive
 * "silent updates" via the App Store, changing the effective
 * component build without an iOS version change.
 *
 * This engine:
 * 1. Stores known version → component mappings
 * 2. Detects when observed builds diverge from expected (silent updates)
 * 3. Provides lookup for patch-state analysis
 */
class VersionMappingEngine @Inject constructor() {

    /**
     * iOS version → map of component → expected build
     */
    private val mappings = mutableMapOf<String, MutableMap<String, VersionComponentMapping>>()

    /**
     * Known components across all tracked iOS versions.
     */
    private val knownComponents = mutableSetOf<String>()

    /**
     * All tracked iOS versions.
     */
    private val trackedVersions = mutableSetOf<String>()

    // ── Registration ────────────────────────────────────────────

    /**
     * Register a version → component mapping.
     */
    fun register(mapping: VersionComponentMapping) {
        mappings.getOrPut(mapping.iosVersion) { mutableMapOf() }[mapping.component] = mapping
        knownComponents.add(mapping.component)
        trackedVersions.add(mapping.iosVersion)
    }

    /**
     * Register multiple mappings.
     */
    fun registerAll(mappingList: List<VersionComponentMapping>) {
        mappingList.forEach { register(it) }
        LogSafe.i(TAG, "Registered ${mappingList.size} mappings across ${trackedVersions.size} versions")
    }

    /**
     * Load the default iOS 26.x mapping set.
     *
     * In production, load from a JSON asset or remote sync.
     */
    fun loadDefaults() {
        val defaults = listOf(
            // ── iOS 26.0 (initial release) ──
            VersionComponentMapping("26.0", "Kernel", "10000.0.1", ConfidenceLevel.CONFIRMED),
            VersionComponentMapping("26.0", "WebKit", "618.1.1", ConfidenceLevel.CONFIRMED),
            VersionComponentMapping("26.0", "dyld", "1200.0.1", ConfidenceLevel.CONFIRMED),
            VersionComponentMapping("26.0", "IOKit", "1.0.0", ConfidenceLevel.MEDIUM),
            VersionComponentMapping("26.0", "Safari", "26.0.0", ConfidenceLevel.HIGH),
            VersionComponentMapping("26.0", "AMFI", "1.0.0", ConfidenceLevel.MEDIUM),

            // ── iOS 26.1 ──
            VersionComponentMapping("26.1", "Kernel", "10000.10.1", ConfidenceLevel.CONFIRMED),
            VersionComponentMapping("26.1", "WebKit", "618.1.5", ConfidenceLevel.CONFIRMED),
            VersionComponentMapping("26.1", "dyld", "1200.0.5", ConfidenceLevel.CONFIRMED),
            VersionComponentMapping("26.1", "IOKit", "1.0.0", ConfidenceLevel.MEDIUM),
            VersionComponentMapping("26.1", "Safari", "26.1.0", ConfidenceLevel.HIGH),
            VersionComponentMapping("26.1", "AMFI", "1.1.0", ConfidenceLevel.MEDIUM),

            // ── iOS 26.2 ──
            VersionComponentMapping("26.2", "Kernel", "10000.20.1", ConfidenceLevel.CONFIRMED),
            VersionComponentMapping("26.2", "WebKit", "618.1.10", ConfidenceLevel.CONFIRMED),
            VersionComponentMapping("26.2", "dyld", "1200.1.0", ConfidenceLevel.CONFIRMED),
            VersionComponentMapping("26.2", "IOKit", "1.0.0", ConfidenceLevel.MEDIUM),
            VersionComponentMapping("26.2", "Safari", "26.2.0", ConfidenceLevel.HIGH),
            VersionComponentMapping("26.2", "AMFI", "1.1.0", ConfidenceLevel.MEDIUM),

            // ── iOS 26.3 ──
            VersionComponentMapping("26.3", "Kernel", "10000.30.1", ConfidenceLevel.CONFIRMED),
            VersionComponentMapping("26.3", "WebKit", "618.1.15", ConfidenceLevel.CONFIRMED),
            VersionComponentMapping("26.3", "dyld", "1200.2.0", ConfidenceLevel.CONFIRMED),
            VersionComponentMapping("26.3", "IOKit", "1.0.1", ConfidenceLevel.MEDIUM),
            VersionComponentMapping("26.3", "Safari", "26.3.0", ConfidenceLevel.HIGH),
            VersionComponentMapping("26.3", "AMFI", "1.2.0", ConfidenceLevel.MEDIUM),
        )
        registerAll(defaults)
    }

    // ── Lookups ─────────────────────────────────────────────────

    /**
     * Get expected build for a component in a specific iOS version.
     */
    fun getExpectedBuild(iosVersion: String, component: String): String? =
        mappings[iosVersion]?.get(component)?.expectedBuild

    /**
     * Get the full mapping for a component in a specific version.
     */
    fun getMapping(iosVersion: String, component: String): VersionComponentMapping? =
        mappings[iosVersion]?.get(component)

    /**
     * Get all known mappings for a specific iOS version.
     */
    fun getMappingsForVersion(iosVersion: String): Map<String, VersionComponentMapping> =
        mappings[iosVersion]?.toMap() ?: emptyMap()

    /**
     * Get all known mappings for a specific component across versions.
     */
    fun getMappingsForComponent(component: String): Map<String, VersionComponentMapping> =
        mappings.mapNotNull { (version, compMap) ->
            compMap[component]?.let { version to it }
        }.toMap()

    /**
     * Get all tracked iOS versions.
     */
    fun getTrackedVersions(): Set<String> = trackedVersions.toSet()

    /**
     * Get all known components.
     */
    fun getKnownComponents(): Set<String> = knownComponents.toSet()

    // ── Silent Update Detection ─────────────────────────────────

    /**
     * Result of checking for silent component updates.
     */
    data class SilentUpdateResult(
        val component: String,
        val iosVersion: String,
        val expectedBuild: String,
        val observedBuild: String,
        val isNewer: Boolean,
        val isOlder: Boolean,
        val notes: String
    )

    /**
     * Check if an observed component build differs from the expected
     * build for the device's iOS version.
     *
     * This detects "silent updates" — e.g. WebKit updated via App Store
     * without an iOS version bump.
     *
     * @param iosVersion the device's reported iOS version
     * @param component the component name
     * @param observedBuild the actual build observed on-device
     * @return analysis result, or null if no mapping exists
     */
    fun checkForSilentUpdate(
        iosVersion: String,
        component: String,
        observedBuild: String
    ): SilentUpdateResult? {
        val expected = getExpectedBuild(iosVersion, component) ?: return null

        val comparison = compareBuildStrings(observedBuild, expected)
        if (comparison == 0) return null // Numerically equivalent build strings are not discrepancies

        return SilentUpdateResult(
            component = component,
            iosVersion = iosVersion,
            expectedBuild = expected,
            observedBuild = observedBuild,
            isNewer = comparison > 0,
            isOlder = comparison < 0,
            notes = when {
                comparison > 0 -> "Observed build is NEWER than expected for iOS $iosVersion — likely a silent component update"
                comparison < 0 -> "Observed build is OLDER than expected for iOS $iosVersion — unusual, may indicate downgrade or partial update"
                else -> ""
            }
        ).also {
            LogSafe.i(TAG, "Silent update detected: ${it.component} on iOS $iosVersion: " +
                    "expected=$expected observed=$observedBuild (${if (it.isNewer) "NEWER" else "OLDER"})")
        }
    }

    /**
     * Check all observed components for silent updates.
     */
    fun checkAllForSilentUpdates(
        iosVersion: String,
        observedComponents: Map<String, String>
    ): List<SilentUpdateResult> {
        return observedComponents.mapNotNull { (component, build) ->
            checkForSilentUpdate(iosVersion, component, build)
        }
    }

    // ── Build Comparison ────────────────────────────────────────

    /**
     * Compare dotted build strings segment by segment.
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
}

/**
 * Convenience wrapper for iOS version logic.
 */
object VersionMapping {
    fun toVersionCode(version: String): Int {
        return try {
            val parts = version.split('.')
            val major = parts.getOrNull(0)?.toInt() ?: 0
            val minor = parts.getOrNull(1)?.toInt() ?: 0
            val patch = parts.getOrNull(2)?.toInt() ?: 0
            major * 10000 + minor * 100 + patch
        } catch (e: Exception) { 0 }
    }

    fun isVulnerable(version: String, fixedIn: String): Boolean {
        val current = toVersionCode(version)
        val fixed = toVersionCode(fixedIn)
        return current < fixed
    }

    fun getWebkitBuildForIos(version: String): String {
        // Fallback mapping if engine not initialized
        return when {
            version.startsWith("26.1") -> "8618.1.15.10.15"
            version.startsWith("26.2") -> "8618.2.12.11.7"
            version.startsWith("26.3") -> "8618.3.11.10.5"
            else -> "UNKNOWN"
        }
    }
}
