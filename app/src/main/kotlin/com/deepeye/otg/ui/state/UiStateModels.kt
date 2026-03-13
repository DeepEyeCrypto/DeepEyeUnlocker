package com.deepeye.otg.ui.state

import com.deepeye.otg.intelligence.vulndb.*
import com.deepeye.otg.protocol.apple.model.AppleDeviceProfile
import com.deepeye.otg.security.Finding
import com.deepeye.otg.security.SecurityScore

// ──────────────────────────────────────────────────────────────
// CVE Dashboard UI State
// DeepEye OTG — UI Module (Part 8)
// ──────────────────────────────────────────────────────────────

/**
 * Top-level UI state for the CVE Intelligence dashboard.
 */
data class CveDashboardState(
    /** Loading state */
    val isLoading: Boolean = false,

    /** Error message if any */
    val error: String? = null,

    /** All CVE entries */
    val allEntries: List<CveEntry> = emptyList(),

    /** Currently selected/filtered entries */
    val filteredEntries: List<CveEntry> = emptyList(),

    /** Active filter configuration */
    val filter: CveFilter = CveFilter(),

    /** Currently selected entry for detail view */
    val selectedEntry: CveEntry? = null,

    /** Component statistics */
    val componentStats: List<ComponentStat> = emptyList(),

    /** Exploitation statistics */
    val exploitationStats: List<ExploitationStat> = emptyList(),

    /** Total CVE count */
    val totalCount: Int = 0,

    /** Last database sync time */
    val lastSyncAt: Long = 0,

    /** Import progress (0.0 – 1.0, null if not importing) */
    val importProgress: Float? = null,

    /** Search query */
    val searchQuery: String = "",

    /** Sort configuration */
    val sortBy: CveSortField = CveSortField.CVSS_SCORE,
    val sortAscending: Boolean = false
)

/**
 * Filter configuration for CVE list.
 */
data class CveFilter(
    val components: Set<String> = emptySet(),
    val severityMin: Double? = null,
    val exploitationStatuses: Set<ExploitationStatus> = emptySet(),
    val confidenceLevels: Set<ConfidenceLevel> = emptySet(),
    val vulnerabilityTypes: Set<VulnerabilityType> = emptySet(),
    val affectedVersion: String? = null,
    val unpatchedOnly: Boolean = false,
    val activeExploitationOnly: Boolean = false
) {
    val hasActiveFilters: Boolean
        get() = components.isNotEmpty() || severityMin != null ||
                exploitationStatuses.isNotEmpty() || confidenceLevels.isNotEmpty() ||
                vulnerabilityTypes.isNotEmpty() || affectedVersion != null ||
                unpatchedOnly || activeExploitationOnly
}

enum class CveSortField {
    CVE_ID,
    COMPONENT,
    CVSS_SCORE,
    EXPLOITATION_STATUS,
    CONFIDENCE,
    UPDATED_AT
}

// ──────────────────────────────────────────────────────────────
// Device Analysis UI State
// ──────────────────────────────────────────────────────────────

/**
 * Top-level UI state for device analysis.
 */
data class DeviceAnalysisState(
    /** Loading state */
    val isLoading: Boolean = false,

    /** Error message */
    val error: String? = null,

    /** Connected device profile */
    val deviceProfile: AppleDeviceProfile? = null,

    /** Exposure report from PatchStateAnalyzer */
    val exposureReport: ExposureReport? = null,

    /** Security score */
    val securityScore: SecurityScore? = null,

    /** Security findings */
    val findings: List<Finding> = emptyList(),

    /** Connection status */
    val connectionStatus: DeviceConnectionStatus = DeviceConnectionStatus.DISCONNECTED,

    /** Analysis phase */
    val analysisPhase: AnalysisPhase = AnalysisPhase.IDLE,

    /** Analysis progress (0.0 – 1.0) */
    val analysisProgress: Float = 0f,

    /** Tab selection */
    val selectedTab: DeviceTab = DeviceTab.OVERVIEW,

    /** Version mapping data */
    val versionMappings: List<VersionComponentMapping> = emptyList(),

    /** Silent updates detected */
    val silentUpdates: List<SilentUpdateInfo> = emptyList()
)

enum class DeviceConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ANALYZING,
    ERROR
}

enum class AnalysisPhase {
    IDLE,
    ENUMERATING,        // Finding USB devices
    PROFILING,          // Building device profile
    CVE_ANALYSIS,       // Running PatchStateAnalyzer
    SECURITY_SCAN,      // Running RuleEngine
    COMPLETE,
    ERROR
}

enum class DeviceTab {
    OVERVIEW,
    CVE_EXPOSURE,
    COMPONENTS,
    SECURITY_FINDINGS,
    USB_DETAILS,
    TIMELINE
}

data class SilentUpdateInfo(
    val component: String,
    val expectedBuild: String,
    val observedBuild: String,
    val isNewer: Boolean,
    val isOlder: Boolean
)

// ──────────────────────────────────────────────────────────────
// Fuzzing UI State
// ──────────────────────────────────────────────────────────────

/**
 * Top-level UI state for the fuzzing dashboard.
 */
data class FuzzDashboardState(
    /** Harness state */
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val hasCompleted: Boolean = false,

    /** Error */
    val error: String? = null,

    /** Session ID */
    val sessionId: String = "",

    /** Live stats */
    val totalExecutions: Long = 0,
    val totalCrashes: Long = 0,
    val uniqueCrashes: Long = 0,
    val executionsPerSecond: Double = 0.0,
    val corpusSize: Int = 0,
    val elapsedMs: Long = 0,

    /** Crash list */
    val crashes: List<CrashListItem> = emptyList(),

    /** Configuration */
    val config: FuzzConfigState = FuzzConfigState(),

    /** Previous sessions */
    val previousSessions: List<SessionListItem> = emptyList()
)

data class CrashListItem(
    val testCaseId: String,
    val bucket: String,
    val crashType: String,
    val severity: String,
    val component: String,
    val inputSize: Int,
    val timestamp: Long
)

data class SessionListItem(
    val sessionId: String,
    val targetSurface: String,
    val startedAt: Long,
    val totalExecutions: Long,
    val uniqueCrashes: Long
)

data class FuzzConfigState(
    val targetSurface: String = "USB_HID",
    val maxIterations: Long = 100_000,
    val maxDurationMinutes: Int = 60,
    val maxInputSize: Int = 4096,
    val parallelWorkers: Int = 1,
    val saveAllInputs: Boolean = false
)

// ──────────────────────────────────────────────────────────────
// HID Research UI State
// ──────────────────────────────────────────────────────────────

/**
 * Top-level UI state for HID research.
 */
data class HidResearchState(
    /** Loading */
    val isLoading: Boolean = false,

    /** Error */
    val error: String? = null,

    /** Currently loaded descriptor */
    val currentDescriptor: HidDescriptorSummary? = null,

    /** Parsed items view */
    val parsedItems: List<HidItemDisplay> = emptyList(),

    /** Malformation list */
    val malformations: List<HidMalformationDisplay> = emptyList(),

    /** Collections view */
    val collections: List<HidCollectionDisplay> = emptyList(),

    /** Variant tracker state */
    val trackedVariants: List<VariantListItem> = emptyList(),

    /** Corpus generation state */
    val corpusGenerated: Boolean = false,
    val corpusFileCount: Int = 0,

    /** Comparison state */
    val comparisonResult: ComparisonDisplay? = null,

    /** Selected tab */
    val selectedTab: HidTab = HidTab.PARSER
)

enum class HidTab {
    PARSER,
    VARIANTS,
    CORPUS,
    CRASH_REPORTS
}

data class HidDescriptorSummary(
    val totalItems: Int,
    val totalCollections: Int,
    val reportIds: Set<Int>,
    val usagePages: List<String>,
    val malformationCount: Int,
    val isWellFormed: Boolean,
    val hasCriticalIssues: Boolean,
    val rawSize: Int
)

data class HidItemDisplay(
    val offset: Int,
    val tagName: String,
    val type: String,
    val dataValue: Long,
    val rawHex: String
)

data class HidMalformationDisplay(
    val offset: Int,
    val severity: String,
    val type: String,
    val description: String
)

data class HidCollectionDisplay(
    val typeName: String,
    val depth: Int,
    val usagePage: String,
    val usage: Int,
    val itemCount: Int
)

data class VariantListItem(
    val id: String,
    val name: String,
    val driverFamily: String,
    val category: String,
    val descriptorSize: Int,
    val effectCount: Int,
    val hasCrash: Boolean
)

data class ComparisonDisplay(
    val variantA: String,
    val variantB: String,
    val diffCount: Int,
    val summary: String
)

// ──────────────────────────────────────────────────────────────
// Forensics UI State
// ──────────────────────────────────────────────────────────────

/**
 * Top-level UI state for forensics module.
 */
data class ForensicsState(
    /** Loading */
    val isLoading: Boolean = false,

    /** Error */
    val error: String? = null,

    /** Index results */
    val indexResult: IndexResultDisplay? = null,

    /** Timeline events */
    val timelineEvents: List<TimelineEventDisplay> = emptyList(),

    /** Hash verification */
    val verificationStatus: VerificationDisplay? = null,

    /** Chain of custody */
    val chainOfCustody: ChainOfCustodyDisplay? = null,

    /** Export state */
    val lastExportPath: String? = null,
    val isExporting: Boolean = false,

    /** Selected tab */
    val selectedTab: ForensicsTab = ForensicsTab.INDEX,

    /** Filter */
    val timelineFilter: TimelineFilterState = TimelineFilterState()
)

enum class ForensicsTab {
    INDEX,
    TIMELINE,
    VERIFICATION,
    REPORTS
}

data class IndexResultDisplay(
    val totalFiles: Int,
    val totalSizeFormatted: String,
    val byType: Map<String, Int>,
    val errorCount: Int,
    val durationMs: Long
)

data class TimelineEventDisplay(
    val timestampFormatted: String,
    val source: String,
    val category: String,
    val action: String,
    val description: String,
    val confidence: String
)

data class VerificationDisplay(
    val totalFiles: Int,
    val verified: Int,
    val mismatched: Int,
    val errors: Int,
    val allPassed: Boolean
)

data class ChainOfCustodyDisplay(
    val caseId: String,
    val examinerName: String,
    val acquisitionTimeFormatted: String,
    val totalArtifacts: Int,
    val totalSizeFormatted: String
)

data class TimelineFilterState(
    val startTime: Long? = null,
    val endTime: Long? = null,
    val selectedCategories: Set<String> = emptySet(),
    val searchQuery: String = ""
)
