package com.deepeye.otg.security

// ──────────────────────────────────────────────────────────────
// Security Finding — Data Model
// DeepEye OTG — Detection + Hardening Module (Part 7)
// ──────────────────────────────────────────────────────────────

/**
 * Severity level for security findings.
 * Aligned with CVSS qualitative ratings.
 */
enum class FindingSeverity {
    CRITICAL,   // 9.0–10.0  Immediate action required
    HIGH,       // 7.0–8.9   Urgent remediation
    MEDIUM,     // 4.0–6.9   Scheduled fix
    LOW,        // 0.1–3.9   Informational / best practice
    INFO        // 0.0       Observation only
}

/**
 * Category of security finding.
 */
enum class FindingCategory {
    OUTDATED_PATCH,         // OS or component is outdated
    EXPOSED_SERVICE,        // Risky service or port exposed
    UNSAFE_TRUST,           // Unauthorized/suspicious trust relationship
    USB_ANOMALY,            // USB role change, unexpected device behavior
    CONFIGURATION_WEAKNESS, // Weak security settings
    KNOWN_VULNERABILITY,    // Matched CVE
    MISSING_PROTECTION,     // Expected protection not present
    SUSPICIOUS_ARTIFACT,    // Unusual file or data found
    NETWORK_EXPOSURE,       // Network-level risk
    GENERAL                 // Other
}

/**
 * Remediation difficulty.
 */
enum class RemediationEffort {
    TRIVIAL,    // Single setting change
    EASY,       // Software update or simple config
    MODERATE,   // Requires planning / downtime
    DIFFICULT,  // Significant effort or risk
    UNKNOWN
}

/**
 * A single security finding.
 */
data class Finding(
    /** Unique finding ID */
    val id: String,

    /** Short title */
    val title: String,

    /** Detailed description of the finding */
    val description: String,

    /** Severity level */
    val severity: FindingSeverity,

    /** Category */
    val category: FindingCategory,

    /** Affected component or service */
    val affectedComponent: String,

    /** Device ID this finding applies to (null = general) */
    val deviceId: String? = null,

    /** Related CVE IDs */
    val relatedCves: List<String> = emptyList(),

    /** CVSS base score if applicable */
    val cvssScore: Double? = null,

    /** Evidence supporting this finding */
    val evidence: String = "",

    /** Remediation guidance */
    val remediation: String = "",

    /** Remediation effort estimate */
    val remediationEffort: RemediationEffort = RemediationEffort.UNKNOWN,

    /** Whether this finding has been acknowledged */
    val acknowledged: Boolean = false,

    /** Whether remediation has been applied */
    val remediated: Boolean = false,

    /** Detection rule ID that produced this finding */
    val ruleId: String? = null,

    /** When this finding was first detected */
    val detectedAt: Long = System.currentTimeMillis(),

    /** Analyst notes */
    val notes: String = "",

    /** Additional metadata */
    val metadata: Map<String, String> = emptyMap()
)
