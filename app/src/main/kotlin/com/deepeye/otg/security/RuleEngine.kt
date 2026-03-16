package com.deepeye.otg.security

import android.util.Log
import com.deepeye.otg.intelligence.vulndb.*
import com.deepeye.otg.protocol.apple.model.AppleDeviceProfile
import com.deepeye.otg.protocol.apple.model.AppleDeviceMode
import com.deepeye.otg.protocol.apple.model.PairingState

// ──────────────────────────────────────────────────────────────
// Rule Engine — Security Detection Rules
// DeepEye OTG — Detection + Hardening Module (Part 7)
// ──────────────────────────────────────────────────────────────

private const val TAG = "RuleEngine"

/**
 * Input context for rule evaluation.
 * Aggregates all available data about the target device.
 */
data class RuleContext(
    val deviceProfile: AppleDeviceProfile? = null,
    val exposureReport: ExposureReport? = null,
    val observedServices: List<ObservedService> = emptyList(),
    val trustRelationships: List<TrustRelationship> = emptyList(),
    val usbEvents: List<UsbEvent> = emptyList(),
    val deviceId: String = "unknown"
)

/** An observed service (network or local). */
data class ObservedService(
    val name: String,
    val port: Int? = null,
    val protocol: String = "",
    val isExposed: Boolean = false,
    val description: String = ""
)

/** A trust relationship between devices. */
data class TrustRelationship(
    val trustedEntity: String,
    val trustType: String,    // e.g. "USB_PAIRING", "WIFI_SYNC", "MDM"
    val isAuthorized: Boolean,
    val establishedAt: Long? = null
)

/** A USB event for anomaly detection. */
data class UsbEvent(
    val timestamp: Long,
    val eventType: String,    // CONNECT, DISCONNECT, ROLE_CHANGE, MODE_SWITCH
    val deviceInfo: String,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * A single detection rule.
 */
data class DetectionRule(
    val id: String,
    val name: String,
    val description: String,
    val category: FindingCategory,
    val defaultSeverity: FindingSeverity,
    val enabled: Boolean = true,
    val evaluate: (RuleContext) -> List<Finding>
)

/**
 * Rule engine that evaluates security detection rules
 * against observed device state.
 *
 * Built-in rules cover:
 * - Outdated patch levels
 * - Active CVE exposure
 * - Exposed services
 * - USB anomalies
 * - Unsafe trust relationships
 * - Missing security configurations
 */
class RuleEngine {

    private val rules = mutableListOf<DetectionRule>()

    init {
        registerBuiltInRules()
    }

    // ── Rule Registration ───────────────────────────────────────

    /**
     * Register a custom detection rule.
     */
    fun registerRule(rule: DetectionRule) {
        rules.add(rule)
        Log.i(TAG, "Registered rule: ${rule.id} (${rule.name})")
    }

    /**
     * Get all registered rules.
     */
    fun getRules(): List<DetectionRule> = rules.toList()

    /**
     * Enable/disable a specific rule.
     */
    fun setRuleEnabled(ruleId: String, enabled: Boolean) {
        val idx = rules.indexOfFirst { it.id == ruleId }
        if (idx >= 0) {
            rules[idx] = rules[idx].copy(enabled = enabled)
        }
    }

    // ── Evaluation ──────────────────────────────────────────────

    /**
     * Evaluate all enabled rules against the context.
     *
     * @param context aggregated device/environment data
     * @return list of detected findings
     */
    fun evaluate(context: RuleContext): List<Finding> {
        val findings = mutableListOf<Finding>()

        for (rule in rules.filter { it.enabled }) {
            try {
                val ruleFindings = rule.evaluate(context)
                findings.addAll(ruleFindings.map { it.copy(ruleId = rule.id) })
            } catch (e: Exception) {
                Log.e(TAG, "Rule ${rule.id} failed: ${e.message}")
            }
        }

        Log.i(TAG, "Evaluated ${rules.count { it.enabled }} rules, " +
                "produced ${findings.size} findings")

        return findings.sortedWith(
            compareBy<Finding> { it.severity.ordinal }
                .thenByDescending { it.cvssScore ?: 0.0 }
        )
    }

    // ── Built-in Rules ──────────────────────────────────────────

    private fun registerBuiltInRules() {

        // RULE 1: Actively exploited CVEs
        registerRule(DetectionRule(
            id = "SEC-001",
            name = "Active Exploitation Detected",
            description = "Device is exposed to CVEs with known active exploitation",
            category = FindingCategory.KNOWN_VULNERABILITY,
            defaultSeverity = FindingSeverity.CRITICAL,
            evaluate = { ctx ->
                val report = ctx.exposureReport ?: return@DetectionRule emptyList()
                report.exposedCves.filter { it.exploitedInWild == true }.map { cve ->
                    Finding(
                        id = "SEC-001_${cve.cveId}",
                        title = "Active Exploitation: ${cve.cveId}",
                        description = "${cve.cveId} (${cve.component}) is actively exploited in the wild " +
                                "and this device appears exposed. ${cve.title}",
                        severity = FindingSeverity.CRITICAL,
                        category = FindingCategory.KNOWN_VULNERABILITY,
                        affectedComponent = cve.component,
                        deviceId = ctx.deviceId,
                        relatedCves = listOf(cve.cveId),
                        cvssScore = cve.cvssScore,
                        evidence = cve.primitive,
                        remediation = "Update to iOS version that includes the fix for ${cve.cveId}",
                        remediationEffort = RemediationEffort.EASY
                    )
                }
            }
        ))

        // RULE 2: Outdated OS version
        registerRule(DetectionRule(
            id = "SEC-002",
            name = "Outdated iOS Version",
            description = "Device is running an outdated iOS version with known unpatched vulnerabilities",
            category = FindingCategory.OUTDATED_PATCH,
            defaultSeverity = FindingSeverity.HIGH,
            evaluate = { ctx ->
                val report = ctx.exposureReport ?: return@DetectionRule emptyList()
                val activeExploited = report.exposedCves.filter { it.exploitedInWild == true }
                if (report.exposedCves.size > 3) {
                    listOf(Finding(
                        id = "SEC-002_${report.androidSpl}",
                        title = "Outdated SPL ${report.androidSpl} — ${report.exposedCves.size} unpatched CVEs",
                        description = "The device SPL ${report.androidSpl} has ${report.exposedCves.size} unpatched vulnerabilities, " +
                                "including ${activeExploited.size} actively exploited.",
                        severity = if (activeExploited.isNotEmpty())
                            FindingSeverity.CRITICAL else FindingSeverity.HIGH,
                        category = FindingCategory.OUTDATED_PATCH,
                        affectedComponent = "Android",
                        deviceId = ctx.deviceId,
                        evidence = "Risk level: ${report.overallRiskLevel}",
                        remediation = "Update to the latest security patch to address known vulnerabilities",
                        remediationEffort = RemediationEffort.EASY
                    ))
                } else emptyList()
            }
        ))

        // RULE 3: DFU mode detected
        registerRule(DetectionRule(
            id = "SEC-003",
            name = "Device in DFU Mode",
            description = "Device is in DFU mode — lowest-level USB interface exposed",
            category = FindingCategory.USB_ANOMALY,
            defaultSeverity = FindingSeverity.MEDIUM,
            evaluate = { ctx ->
                val profile = ctx.deviceProfile ?: return@DetectionRule emptyList()
                if (profile.deviceMode == AppleDeviceMode.DFU) {
                    listOf(Finding(
                        id = "SEC-003_dfu",
                        title = "Device in DFU Mode",
                        description = "Device ${profile.displayName} is in DFU mode. This exposes low-level " +
                                "USB interfaces and may indicate maintenance, restore, or research activity.",
                        severity = FindingSeverity.MEDIUM,
                        category = FindingCategory.USB_ANOMALY,
                        affectedComponent = "USB/DFU",
                        deviceId = ctx.deviceId,
                        evidence = "USB PID: 0x${profile.usbProductId.toString(16).uppercase()}",
                        remediation = "If unintended, boot the device normally by holding the appropriate button combination",
                        remediationEffort = RemediationEffort.TRIVIAL
                    ))
                } else emptyList()
            }
        ))

        // RULE 4: Unknown pairing state
        registerRule(DetectionRule(
            id = "SEC-004",
            name = "Untrusted Device Connection",
            description = "Device is connected but pairing/trust state is not established",
            category = FindingCategory.UNSAFE_TRUST,
            defaultSeverity = FindingSeverity.LOW,
            evaluate = { ctx ->
                val profile = ctx.deviceProfile ?: return@DetectionRule emptyList()
                if (profile.pairingState == PairingState.UNTRUSTED) {
                    listOf(Finding(
                        id = "SEC-004_untrusted",
                        title = "Untrusted Connection to ${profile.displayName}",
                        description = "The device has not established a trust relationship with this host. " +
                                "Limited information is available without pairing.",
                        severity = FindingSeverity.LOW,
                        category = FindingCategory.UNSAFE_TRUST,
                        affectedComponent = "USB Pairing",
                        deviceId = ctx.deviceId,
                        remediation = "Accept the trust dialog on the device if authorized pairing is intended"
                    ))
                } else emptyList()
            }
        ))

        // RULE 5: Suspicious USB role changes
        registerRule(DetectionRule(
            id = "SEC-005",
            name = "Suspicious USB Role Change",
            description = "Detects unexpected USB mode or role transitions",
            category = FindingCategory.USB_ANOMALY,
            defaultSeverity = FindingSeverity.MEDIUM,
            evaluate = { ctx ->
                val roleChanges = ctx.usbEvents.filter { it.eventType == "ROLE_CHANGE" }
                if (roleChanges.size > 3) {
                    listOf(Finding(
                        id = "SEC-005_role_changes",
                        title = "Multiple USB Role Changes Detected (${roleChanges.size})",
                        description = "${roleChanges.size} USB role changes detected in session. " +
                                "Rapid role switching may indicate USB-based attack attempts or device instability.",
                        severity = FindingSeverity.MEDIUM,
                        category = FindingCategory.USB_ANOMALY,
                        affectedComponent = "USB",
                        deviceId = ctx.deviceId,
                        evidence = "Role changes: ${roleChanges.size}",
                        remediation = "Investigate the cause of rapid USB role changes; consider using a known-good cable"
                    ))
                } else emptyList()
            }
        ))

        // RULE 6: Missing Activation Lock check
        registerRule(DetectionRule(
            id = "SEC-006",
            name = "Activation Lock Status Unknown",
            description = "Cannot determine if Activation Lock is enabled",
            category = FindingCategory.MISSING_PROTECTION,
            defaultSeverity = FindingSeverity.INFO,
            evaluate = { ctx ->
                val profile = ctx.deviceProfile ?: return@DetectionRule emptyList()
                if (profile.isActivationLocked == null &&
                    profile.deviceMode == AppleDeviceMode.NORMAL
                ) {
                    listOf(Finding(
                        id = "SEC-006_activation_lock",
                        title = "Activation Lock Status Unknown",
                        description = "Cannot determine Activation Lock status for ${profile.displayName}. " +
                                "This status typically requires pairing or iCloud verification.",
                        severity = FindingSeverity.INFO,
                        category = FindingCategory.MISSING_PROTECTION,
                        affectedComponent = "Activation Lock",
                        deviceId = ctx.deviceId,
                        remediation = "Verify Activation Lock status in device Settings or via Apple's activation status checker"
                    ))
                } else emptyList()
            }
        ))

        // RULE 7: Uncertain CVE exposure
        registerRule(DetectionRule(
            id = "SEC-007",
            name = "High Uncertainty in Security Assessment",
            description = "Many CVE exposures cannot be confidently determined",
            category = FindingCategory.GENERAL,
            defaultSeverity = FindingSeverity.LOW,
            evaluate = { ctx ->
                val report = ctx.exposureReport ?: return@DetectionRule emptyList()
                if (report.unknownCves.size > 5) {
                    listOf(Finding(
                        id = "SEC-007_uncertainty",
                        title = "${report.unknownCves.size} CVEs with Unknown Exposure Status",
                        description = "${report.unknownCves.size} CVEs could not be confidently classified. " +
                                "More component version data is needed for accurate assessment.",
                        severity = FindingSeverity.LOW,
                        category = FindingCategory.GENERAL,
                        affectedComponent = "Assessment",
                        deviceId = ctx.deviceId,
                        evidence = "Coverage gaps in various subsystems",
                        remediation = "Obtain additional component version data through device inspection or pairing"
                    ))
                } else emptyList()
            }
        ))

        Log.i(TAG, "Registered ${rules.size} built-in detection rules")
    }
}
