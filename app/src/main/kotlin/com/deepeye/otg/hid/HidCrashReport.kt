package com.deepeye.otg.hid

// ──────────────────────────────────────────────────────────────
// HID Crash Report — Result Schema
// DeepEye OTG — HID Research Module (Part 5)
// ──────────────────────────────────────────────────────────────

/**
 * Structured crash report for a HID-related incident.
 */
data class HidCrashReport(
    /** Unique report ID */
    val reportId: String,

    /** Variant ID that triggered the crash */
    val variantId: String,

    /** Raw descriptor bytes that triggered the crash */
    val triggerDescriptor: ByteArray,

    /** iOS version where crash occurred */
    val iosVersion: String,

    /** Build ID */
    val iosBuildId: String? = null,

    /** Device model */
    val deviceModel: String? = null,

    /** Driver family affected */
    val driverFamily: String,

    /** Crash type classification */
    val crashType: CrashType,

    /** Panic string or crash signature */
    val crashSignature: String,

    /** Stack trace if available */
    val stackTrace: String? = null,

    /** Sysdiagnose reference (filename or path) */
    val sysdiagnoseRef: String? = null,

    /** Related CVE IDs */
    val relatedCves: List<String> = emptyList(),

    /** Parse result of the trigger descriptor */
    val parseResult: HidParseResult? = null,

    /** Specific malformations that likely caused the crash */
    val suspectedMalformations: List<HidMalformation> = emptyList(),

    /** Whether repro has been confirmed */
    val reproConfirmed: Boolean = false,

    /** How many times crash was reproduced */
    val reproCount: Int = 0,

    /** Whether a minimal reproduction input has been found */
    val isMinimized: Boolean = false,

    /** Analyst triage notes */
    val triageNotes: String = "",

    /** Analyst severity assessment */
    val severity: CrashSeverity = CrashSeverity.MEDIUM,

    /** Timestamp of first occurrence */
    val firstSeen: Long = System.currentTimeMillis(),

    /** Timestamp of last occurrence */
    val lastSeen: Long = System.currentTimeMillis(),

    /** Additional metadata */
    val metadata: Map<String, String> = emptyMap()
) {
    enum class CrashType {
        KERNEL_PANIC,
        DRIVER_CRASH,
        USB_ENUMERATION_FAILURE,
        RESOURCE_EXHAUSTION,
        HANG,
        ASSERTION_FAILURE,
        OTHER,
        UNKNOWN
    }

    enum class CrashSeverity {
        CRITICAL,   // Kernel panic, potential code execution
        HIGH,       // Driver crash, potential info leak
        MEDIUM,     // Denial of service
        LOW,        // Handled error, assertion
        INFO        // Interesting but not exploitable
    }

    /**
     * Triage checklist items.
     */
    data class TriageChecklist(
        val reproduced: Boolean = false,
        val minimized: Boolean = false,
        val rootCauseIdentified: Boolean = false,
        val driverVersionConfirmed: Boolean = false,
        val affectedVersionsChecked: Boolean = false,
        val cveSearchDone: Boolean = false,
        val regressTested: Boolean = false,
        val reportWritten: Boolean = false
    ) {
        val completionPercent: Int
            get() {
                val items = listOf(
                    reproduced, minimized, rootCauseIdentified,
                    driverVersionConfirmed, affectedVersionsChecked,
                    cveSearchDone, regressTested, reportWritten
                )
                return (items.count { it } * 100) / items.size
            }
    }

    override fun equals(other: Any?) = other is HidCrashReport && reportId == other.reportId
    override fun hashCode() = reportId.hashCode()
}
