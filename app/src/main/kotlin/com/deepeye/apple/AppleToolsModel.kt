package com.deepeye.apple

/**
 * Apple Pro Tools — Complete iOS/iPadOS Tool Registry
 * 
 * All Apple/iOS related features organized by category for the Apple Pro Tools tab.
 * This serves as the single source of truth for all Apple tool capabilities.
 */

/**
 * Represents a single Apple tool/feature with metadata
 */
data class AppleTool(
    val id: String,
    val name: String,
    val description: String,
    val category: AppleCategory,
    val isSupported: Boolean = true,
    val requiresJailbreak: Boolean = false,
    val supportedVersions: String = "iOS 12–18",
    val iconRes: String = "default", // Resource name for icon
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val estimatedTime: String = "~2 min"
)

/**
 * Risk levels for Apple operations
 */
enum class RiskLevel {
    LOW,      // Safe, non-destructive
    MEDIUM,   // May require reboot
    HIGH,     // Irreversible changes
    CRITICAL  // Potential brick risk
}

/**
 * Categories for Apple Pro Tools
 */
enum class AppleCategory(val displayName: String, val icon: String) {
    ACTIVATION_BYPASS("Activation Lock", "lock_open"),
    MDM_BYPASS("MDM/DEP", "shield_off"),
    PASSCODE_BYPASS("Passcode", "pin"),
    FIRMWARE_TOOLS("Firmware", "download"),
    CHECKM8_EXPLOIT("checkm8", "bug"),
    ICLOUD_TOOLS("iCloud", "cloud"),
    DIAGNOSTICS("Diagnostics", "info"),
    NETWORK_UNLOCK("Network Unlock", "signal")
}

/**
 * Complete registry of all Apple tools
 */
object AppleToolsRegistry {
    
    val ALL_TOOLS = listOf(
        
        // ═══════════════════════════════════════════════════════════
        // ACTIVATION BYPASS CATEGORY
        // ═══════════════════════════════════════════════════════════
        
        AppleTool(
            id = "activation_lock_bypass",
            name = "Activation Lock Bypass",
            description = "Bypass iCloud Activation Lock on iPhone/iPad",
            category = AppleCategory.ACTIVATION_BYPASS,
            supportedVersions = "iOS 12–16.7",
            requiresJailbreak = true,
            riskLevel = RiskLevel.HIGH,
            estimatedTime = "~5 min"
        ),
        AppleTool(
            id = "fmi_off_check",
            name = "FMI Status Check",
            description = "Check Find My iPhone ON/OFF status via IMEI",
            category = AppleCategory.ACTIVATION_BYPASS,
            supportedVersions = "All iOS",
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~30 sec"
        ),
        AppleTool(
            id = "gsm_bypass",
            name = "GSM Activation Bypass",
            description = "Bypass activation for GSM-only iPhones",
            category = AppleCategory.ACTIVATION_BYPASS,
            supportedVersions = "iOS 12–17",
            requiresJailbreak = true,
            riskLevel = RiskLevel.HIGH,
            estimatedTime = "~4 min"
        ),
        AppleTool(
            id = "signal_bypass",
            name = "Signal Activation Bypass",
            description = "Restore phone signal after activation bypass",
            category = AppleCategory.ACTIVATION_BYPASS,
            supportedVersions = "iOS 12–16.7",
            requiresJailbreak = true,
            riskLevel = RiskLevel.HIGH,
            estimatedTime = "~3 min"
        ),
        AppleTool(
            id = "hello_bypass_signal",
            name = "Hello Screen Bypass (Signal)",
            description = "Bypass Hello activation with cellular signal",
            category = AppleCategory.ACTIVATION_BYPASS,
            supportedVersions = "iOS 12–17",
            requiresJailbreak = true,
            riskLevel = RiskLevel.HIGH,
            estimatedTime = "~5 min"
        ),
        AppleTool(
            id = "hello_bypass_no_signal",
            name = "Hello Screen Bypass (No Signal)",
            description = "Bypass Hello activation without cellular signal",
            category = AppleCategory.ACTIVATION_BYPASS,
            supportedVersions = "iOS 12–17",
            requiresJailbreak = true,
            riskLevel = RiskLevel.HIGH,
            estimatedTime = "~4 min"
        ),
        
        // ═══════════════════════════════════════════════════════════
        // MDM BYPASS CATEGORY
        // ═══════════════════════════════════════════════════════════
        
        AppleTool(
            id = "mdm_bypass",
            name = "MDM Profile Bypass",
            description = "Remove MDM/DEP enrollment profile",
            category = AppleCategory.MDM_BYPASS,
            supportedVersions = "iOS 12–18",
            requiresJailbreak = true,
            riskLevel = RiskLevel.HIGH,
            estimatedTime = "~6 min"
        ),
        AppleTool(
            id = "dep_bypass",
            name = "DEP Bypass",
            description = "Device Enrollment Program profile removal",
            category = AppleCategory.MDM_BYPASS,
            supportedVersions = "iOS 12–18",
            requiresJailbreak = true,
            riskLevel = RiskLevel.HIGH,
            estimatedTime = "~6 min"
        ),
        AppleTool(
            id = "supervised_bypass",
            name = "Supervised Mode Bypass",
            description = "Remove supervised device restrictions",
            category = AppleCategory.MDM_BYPASS,
            supportedVersions = "iOS 12–18",
            requiresJailbreak = true,
            riskLevel = RiskLevel.MEDIUM,
            estimatedTime = "~4 min"
        ),
        AppleTool(
            id = "mdm_profile_parser",
            name = "MDM Profile Parser",
            description = "Parse and analyze MDM configuration PLIST",
            category = AppleCategory.MDM_BYPASS,
            supportedVersions = "All iOS",
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~1 min"
        ),
        
        // ═══════════════════════════════════════════════════════════
        // PASSCODE BYPASS CATEGORY
        // ═══════════════════════════════════════════════════════════
        
        AppleTool(
            id = "passcode_remove",
            name = "Screen Passcode Remove",
            description = "Remove iPhone/iPad screen lock passcode",
            category = AppleCategory.PASSCODE_BYPASS,
            supportedVersions = "iOS 12–15.8 (A7–A11)",
            requiresJailbreak = true,
            riskLevel = RiskLevel.CRITICAL,
            estimatedTime = "~10 min"
        ),
        AppleTool(
            id = "screen_time_bypass",
            name = "Screen Time Bypass",
            description = "Remove Screen Time restrictions without passcode",
            category = AppleCategory.PASSCODE_BYPASS,
            supportedVersions = "iOS 12–17",
            requiresJailbreak = true,
            riskLevel = RiskLevel.MEDIUM,
            estimatedTime = "~3 min"
        ),
        AppleTool(
            id = "token_backup",
            name = "Activation Token Backup",
            description = "Backup activation tokens before passcode removal",
            category = AppleCategory.PASSCODE_BYPASS,
            supportedVersions = "iOS 12–17",
            requiresJailbreak = true,
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~2 min"
        ),
        
        // ═══════════════════════════════════════════════════════════
        // CHECKM8 EXPLOIT CATEGORY
        // ═══════════════════════════════════════════════════════════
        
        AppleTool(
            id = "checkm8_dfu",
            name = "checkm8 DFU Exploit",
            description = "Hardware bootrom exploit (A5–A11 only)",
            category = AppleCategory.CHECKM8_EXPLOIT,
            supportedVersions = "A5–A11 (iPhone 4S–X)",
            riskLevel = RiskLevel.MEDIUM,
            estimatedTime = "~2 min"
        ),
        AppleTool(
            id = "dfu_mode",
            name = "Force DFU Mode",
            description = "Force device into DFU mode via USB timing",
            category = AppleCategory.CHECKM8_EXPLOIT,
            supportedVersions = "All Apple devices",
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~1 min"
        ),
        AppleTool(
            id = "recovery_mode",
            name = "Recovery Mode Toggle",
            description = "Enter/exit Recovery Mode via libimobiledevice",
            category = AppleCategory.CHECKM8_EXPLOIT,
            supportedVersions = "All Apple devices",
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~30 sec"
        ),
        AppleTool(
            id = "pwned_dfu",
            name = "Pwned DFU Entry",
            description = "Enter pwned DFU mode after checkm8 exploit",
            category = AppleCategory.CHECKM8_EXPLOIT,
            supportedVersions = "A5–A11 (iPhone 4S–X)",
            riskLevel = RiskLevel.MEDIUM,
            estimatedTime = "~3 min"
        ),
        
        // ═══════════════════════════════════════════════════════════
        // FIRMWARE TOOLS CATEGORY
        // ═══════════════════════════════════════════════════════════
        
        AppleTool(
            id = "ipsw_flash",
            name = "IPSW Firmware Flash",
            description = "Flash iOS firmware .ipsw file via DFU mode",
            category = AppleCategory.FIRMWARE_TOOLS,
            supportedVersions = "All iOS",
            riskLevel = RiskLevel.HIGH,
            estimatedTime = "~15 min"
        ),
        AppleTool(
            id = "ipsw_downgrade",
            name = "iOS Downgrade",
            description = "Downgrade iOS using signed IPSW + blobs",
            category = AppleCategory.FIRMWARE_TOOLS,
            supportedVersions = "Signed versions only",
            riskLevel = RiskLevel.HIGH,
            estimatedTime = "~20 min"
        ),
        AppleTool(
            id = "shsh_save",
            name = "SHSH Blob Saver",
            description = "Save SHSH2 blobs for future downgrades",
            category = AppleCategory.FIRMWARE_TOOLS,
            supportedVersions = "All iOS",
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~1 min"
        ),
        AppleTool(
            id = "ota_blocker",
            name = "OTA Update Blocker",
            description = "Block automatic iOS OTA updates",
            category = AppleCategory.FIRMWARE_TOOLS,
            supportedVersions = "iOS 12–18",
            requiresJailbreak = true,
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~1 min"
        ),
        AppleTool(
            id = "reset_lock",
            name = "Reset & Settings Lock",
            description = "Lock reset and settings to prevent unauthorized changes",
            category = AppleCategory.FIRMWARE_TOOLS,
            supportedVersions = "iOS 12–18",
            requiresJailbreak = true,
            riskLevel = RiskLevel.MEDIUM,
            estimatedTime = "~2 min"
        ),
        
        // ═══════════════════════════════════════════════════════════
        // ICLOUD TOOLS CATEGORY
        // ═══════════════════════════════════════════════════════════
        
        AppleTool(
            id = "icloud_remove",
            name = "iCloud Account Remove",
            description = "Remove iCloud account from device",
            category = AppleCategory.ICLOUD_TOOLS,
            supportedVersions = "iOS 12–16.7",
            requiresJailbreak = true,
            riskLevel = RiskLevel.HIGH,
            estimatedTime = "~8 min"
        ),
        AppleTool(
            id = "apple_id_unlock",
            name = "Apple ID Disabled Fix",
            description = "Fix disabled Apple ID / unlock Apple account",
            category = AppleCategory.ICLOUD_TOOLS,
            supportedVersions = "All iOS",
            riskLevel = RiskLevel.MEDIUM,
            estimatedTime = "~5 min"
        ),
        AppleTool(
            id = "fmi_off_api",
            name = "FMI-OFF API Submit",
            description = "Submit tokens to FMI-OFF API service",
            category = AppleCategory.ICLOUD_TOOLS,
            supportedVersions = "iOS 12–16.7",
            requiresJailbreak = true,
            riskLevel = RiskLevel.HIGH,
            estimatedTime = "~10 min"
        ),
        AppleTool(
            id = "activation_check",
            name = "Activation Status Check",
            description = "Query activation / iCloud status from current USB mode",
            category = AppleCategory.ICLOUD_TOOLS,
            supportedVersions = "All iOS",
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~30 sec"
        ),
        AppleTool(
            id = "getenv_snapshot",
            name = "GetEnv Snapshot",
            description = "Collect boot variables using iRecovery console access",
            category = AppleCategory.ICLOUD_TOOLS,
            supportedVersions = "Recovery/DFU mode",
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~1 min"
        ),
        
        // ═══════════════════════════════════════════════════════════
        // DIAGNOSTICS CATEGORY
        // ═══════════════════════════════════════════════════════════
        
        AppleTool(
            id = "device_info",
            name = "Device Info Extractor",
            description = "Extract device info: ECID, UDID, iOS version, model",
            category = AppleCategory.DIAGNOSTICS,
            supportedVersions = "All iOS",
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~30 sec"
        ),
        AppleTool(
            id = "cve_scan",
            name = "CVE Intelligence Scan",
            description = "Scan device for known vulnerabilities by iOS version",
            category = AppleCategory.DIAGNOSTICS,
            supportedVersions = "All iOS",
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~2 min"
        ),
        AppleTool(
            id = "mode_probe",
            name = "Mode Probe",
            description = "Run non-destructive environment probe for device state",
            category = AppleCategory.DIAGNOSTICS,
            supportedVersions = "All modes",
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~1 min"
        ),
        AppleTool(
            id = "refresh_mode",
            name = "Refresh Device Mode",
            description = "Re-query device mode and update live session metadata",
            category = AppleCategory.DIAGNOSTICS,
            supportedVersions = "All modes",
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~30 sec"
        ),
        AppleTool(
            id = "imei_check",
            name = "IMEI/Serial Check",
            description = "Validate IMEI/Serial and check device warranty status",
            category = AppleCategory.DIAGNOSTICS,
            supportedVersions = "All iOS",
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~1 min"
        ),
        
        // ═══════════════════════════════════════════════════════════
        // NETWORK UNLOCK CATEGORY
        // ═══════════════════════════════════════════════════════════
        
        AppleTool(
            id = "carrier_unlock",
            name = "Carrier/SIM Unlock",
            description = "Remove carrier lock for SIM-free operation",
            category = AppleCategory.NETWORK_UNLOCK,
            supportedVersions = "iOS 12–17",
            requiresJailbreak = true,
            riskLevel = RiskLevel.HIGH,
            estimatedTime = "~8 min"
        ),
        AppleTool(
            id = "baseband_backup",
            name = "Baseband Backup",
            description = "Backup baseband firmware before unlock operations",
            category = AppleCategory.NETWORK_UNLOCK,
            supportedVersions = "All iOS",
            requiresJailbreak = true,
            riskLevel = RiskLevel.LOW,
            estimatedTime = "~2 min"
        )
    )
    
    /**
     * Get tools by category
     */
    fun getToolsByCategory(category: AppleCategory): List<AppleTool> {
        return ALL_TOOLS.filter { it.category == category }
    }
    
    /**
     * Get tools that don't require jailbreak
     */
    fun getNoJailbreakTools(): List<AppleTool> {
        return ALL_TOOLS.filter { !it.requiresJailbreak }
    }
    
    /**
     * Get tools compatible with specific iOS version
     */
    fun getToolsForIosVersion(iosVersion: String): List<AppleTool> {
        return ALL_TOOLS.filter { tool ->
            tool.supportedVersions.contains(iosVersion) || 
            tool.supportedVersions.contains("All iOS") ||
            tool.supportedVersions.contains("All modes") ||
            tool.supportedVersions.contains("Recovery/DFU")
        }
    }
    
    /**
     * Get tool by ID
     */
    fun getToolById(id: String): AppleTool? {
        return ALL_TOOLS.find { it.id == id }
    }
}
