package com.deepeye.otg.data.gsmg

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

// =============================================================================
// BypassModels.kt v4.0
// Full type system for all 7 tools:
// GSMG + iRemoval + F3arRa1n + DC-Unlocker + Chimera +
// Hydra Tool + Miracle Thunder
// =============================================================================

// ── Mechanism ─────────────────────────────────────────────────────────────────

enum class BypassMechanism(
    val displayName:      String,
    val requiresInternet: Boolean,
    val requiresDfu:      Boolean,
    val minMinutes:       Int,
    val maxMinutes:       Int,
) {
    // iOS mechanisms
    RAMDISK(               "Ramdisk Boot",            false, true,   3,  8),
    CHECKM8(               "checkm8 Exploit",         false, true,   2,  5),
    CHECKM8_IRAIN(         "checkm8 + iRa1n",         false, true,   5, 12),
    CHECKM8_RAMDISK(       "checkm8 + Ramdisk",       false, true,   4, 10),
    IRAIN(                 "iRa1n Jailbreak",         false, true,   5, 10),
    SERVER_EXPLOIT(        "Server Activation",       true,  false,  1,  3),
    SERVER_REGISTRATION(   "IMEI Registration",       true,  false,  1,  5),
    NVRAM_INJECTION(       "NVRAM Injection",         false, true,   2,  6),
    DFU_RESTORE(           "DFU Restore",             true,  true,  15, 40),
    RAMDISK_WRITE(         "Ramdisk Write",           false, true,   3,  8),
    RAMDISK_READ(          "Ramdisk Read",            false, true,   2,  5),
    RAMDISK_DELETE(        "Ramdisk Delete",          false, true,   3,  7),
    CUSTOM_IPSW(           "Custom IPSW Flash",       true,  true,  20, 50),
    USB_READ(              "USB Info Read",           false, false,  0,  1),
    USB_SEQUENCE(          "DFU Guide",               false, false,  1,  3),
    ACTIVATION_PATCH(      "Activation Patch",        false, false,  1,  3),
    ACTIVATION_RECORD_PATCH("iServices Patch",        true,  false,  2,  5),
    INTEGRITY_PATCH(       "Integrity Patch",         false, false,  1,  3),
    ADB_EXPLOIT(           "ADB FRP Exploit",         false, false,  2,  8),

    // Android mechanisms — DC-Unlocker / Chimera / Hydra / Miracle
    DIRECT_UNLOCK(         "Direct SIM Unlock",       true,  false,  1,  3),
    CODE_GENERATE(         "Unlock Code Generator",   true,  false,  1,  2),
    AT_COMMAND(            "AT Command Unlock",       false, false,  1,  3),
    DIAG_UNLOCK(           "Diagnostic Unlock",       false, false,  2,  5),
    FIRMWARE_FLASH_MTK(    "MTK Firmware Flash",      false, false,  5, 20),
    FIRMWARE_FLASH_QC(     "QC Firehose Flash",       false, false,  5, 20),
    FIRMWARE_FLASH_SPD(    "SPD/UniSoc Flash",        false, false,  5, 20),
    FIRMWARE_FLASH_ODIN(   "Samsung ODIN Flash",      false, false,  5, 30),
    FIRMWARE_FLASH_HUAWEI( "Huawei Mode Flash",       false, false,  5, 20),
    IMEI_REPAIR_MTK(       "MTK IMEI Repair",         false, false,  2,  8),
    IMEI_REPAIR_QC(        "QC DIAG IMEI Repair",     false, false,  2,  8),
    IMEI_REPAIR_SPD(       "SPD IMEI Repair",         false, false,  2,  8),
    IMEI_REPAIR_SAMSUNG(   "Samsung IMEI Repair",     false, false,  2,  8),
    MAC_REPAIR(            "WiFi/BT MAC Repair",      false, false,  1,  3),
    FRP_MTK_BROM(          "MTK BROM FRP Remove",     false, true,   3, 10),
    FRP_MTK_META(          "MTK META FRP Remove",     false, false,  2,  8),
    FRP_QC_EDL(            "QC EDL FRP Remove",       false, false,  3, 10),
    FRP_SPD(               "SPD FRP Remove",          false, false,  2,  8),
    FRP_SAMSUNG_MODEM(     "Samsung Modem FRP",       false, false,  2,  8),
    FRP_SAMSUNG_MTP(       "Samsung MTP FRP",         false, false,  2,  8),
    FRP_ADB(               "ADB FRP Remove",          false, false,  2,  8),
    FRP_DOWNLOAD_MODE(     "Download Mode FRP",       false, false,  3, 10),
    MDM_PAYJOY_REMOVE(     "PayJoy/MDM Removal",      false, false,  2,  8),
    BOOTLOADER_UNLOCK(     "Bootloader Unlock",       false, false,  2,  8),
    BOOTLOADER_RELOCK(     "Bootloader Relock",       false, false,  2,  5),
    EMMC_HEALTH_CHECK(     "eMMC Health Check",       false, false,  1,  3),
    RPMB_BACKUP(           "RPMB Backup",             false, false,  2,  8),
    RPMB_RESTORE(          "RPMB Restore",            false, false,  2,  8),
    NV_READ_WRITE(         "NV Read/Write",           false, false,  2,  8),
    PARTITION_MANAGER(     "Partition Manager",       false, false,  2,  8),
    SCREEN_LOCK_REMOVE(    "Screen Lock Remove",      false, false,  3, 10),
    HUAWEI_ID_REMOVE(      "Huawei ID Removal",       true,  false,  2,  8),
    MI_ACCOUNT_REMOVE(     "Mi Account Remove",       true,  false,  2,  8),
    OPPO_ID_REMOVE(        "Oppo ID Removal",         true,  false,  2,  8),
    MODEM_UNLOCK_HUAWEI(   "Huawei Modem Unlock",     true,  false,  1,  3),
    MODEM_UNLOCK_ZTE(      "ZTE Modem Unlock",        true,  false,  1,  3),
    MODEM_UNLOCK_SIERRA(   "Sierra Wireless Unlock",  true,  false,  1,  3),
    VOICE_ENABLE(          "Voice Feature Enable",    false, false,  2,  5),
    CSC_CHANGE(            "Samsung CSC Change",      false, false,  2,  5),
    EFS_BACKUP_RESTORE(    "EFS Backup/Restore",      false, false,  3, 10),
    DRK_REPAIR(            "Samsung DRK Repair",      false, false,  2,  5),
    CALIBRATION_RESTORE(   "Calibration Restore",     false, false,  2,  5),
    SLA_AUTH(              "SLA Authentication",      false, true,   2,  8),
    SPD_DOWNLOAD_MODE(     "SPD Download Mode",       false, false,  3, 10),
    UNISOC_META(           "UniSoc META Mode",        false, false,  3, 10),
}

// ── Chip Range ────────────────────────────────────────────────────────────────

enum class ChipRange(
    val displayName:        String,
    val isCheckm8Vulnerable:Boolean,
) {
    // iOS
    A7_TO_A11(   "A7–A11  (5S–X)",          true),
    A8_TO_A11(   "A8–A11  (6–X)",           true),
    A12_TO_A18(  "A12–A18 (XR–16 Pro Max)", false),
    A7_TO_A18(   "All Apple Silicon",        false),

    // Android chipsets
    MTK_ALL(     "MediaTek All",            false),
    MTK_V6(      "MediaTek V6 (Dimensity)", false),
    MTK_CLASSIC( "MediaTek Classic",        false),
    QC_ALL(      "Qualcomm All",            false),
    SAMSUNG_ALL( "Samsung All",             false),
    SAMSUNG_EXYNOS("Samsung Exynos",        false),
    SAMSUNG_QC(  "Samsung Snapdragon",      false),
    SPD_ALL(     "Spreadtrum/UniSoc All",   false),
    HUAWEI_ALL(  "Huawei HiSilicon/MTK",    false),
    MODEM_ALL(   "Modems & Routers",        false),
    ALL_ANDROID( "All Android",             false),
    ALL_DEVICES( "All Devices",             false),
}

// ── Feature Category ─────────────────────────────────────────────────────────

enum class FeatureCategory(
    val displayName: String,
    val icon:        String,
) {
    // iOS categories
    ICLOUD_BYPASS(    "iCloud Bypass",       "🔓"),
    PASSCODE(         "Passcode",            "🔐"),
    DEVICE_MANAGEMENT("Device Mgmt",         "📱"),
    FIRMWARE(         "Firmware",            "💾"),
    CARRIER(          "Carrier",             "📶"),
    MDM(              "MDM",                 "🏢"),
    SERVICES(         "iServices",           "⚙️"),
    DEVICE_INFO(      "Device Info",         "ℹ️"),
    EXPLOIT_ENGINE(   "Exploit Engine",      "⚡"),

    // Android categories
    FRP_BYPASS(       "FRP Bypass",          "🔑"),
    IMEI_REPAIR(      "IMEI Repair",         "📡"),
    NETWORK_UNLOCK(   "Network Unlock",      "🌐"),
    FIRMWARE_FLASH(   "Firmware Flash",      "🔥"),
    BOOTLOADER(       "Bootloader",          "🔧"),
    ACCOUNT_REMOVE(   "Account Remove",      "👤"),
    SCREEN_UNLOCK(    "Screen Unlock",       "🖥️"),
    HARDWARE_INFO(    "Hardware Info",       "🔬"),
    EFS_NV(          "EFS / NV",            "💽"),
    MODEM_ROUTER(     "Modem/Router",        "📡"),
    SAMSUNG_SPECIAL(  "Samsung Special",     "🔵"),
    HUAWEI_SPECIAL(   "Huawei Special",      "🔴"),
    TRANSSION_SPECIAL("Infinix/Tecno",       "🟢"),
    ANDROID_MISC(     "Android Misc",        "🤖"),
}

// ── Feature Source ────────────────────────────────────────────────────────────

enum class FeatureSource(val displayName: String) {
    GSMG_IREMOVAL( "GSMG + iRemoval"),
    F3ARRAIN(      "F3arRa1n"),
    DC_UNLOCKER(   "DC-Unlocker"),
    CHIMERA(       "Chimera Tool"),
    HYDRA(         "Hydra Tool"),
    MIRACLE(       "Miracle Thunder"),
    UNLOCKTOOL(    "UnlockTool"),
    REIBOOT(       "Tenorshare ReiBoot"),
    MULTI_TOOL(    "Multi-Tool"),   // confirmed across 3+ tools
}

// ── Risk + Confidence ─────────────────────────────────────────────────────────

enum class RiskLevel { LOW, MEDIUM, HIGH, EXTREME }
enum class ConfidenceLevel { CONFIRMED, INFERRED, HYPOTHESIS }

// ── Core Feature Model ────────────────────────────────────────────────────────

@Immutable
data class ExecutionStep(
    val stepNum:     Int,
    val title:       String,
    val instruction: String,
    val isAutomatic: Boolean,
    val timeoutSecs: Int = 30,
)

@Stable
data class BypassFeature(
    val id:                  String,
    val displayName:         String,
    val description:         String,
    val detailedDescription: String,
    val category:            FeatureCategory,
    val mechanism:           BypassMechanism,
    val chipRange:           ChipRange,
    val iosRange:            String,
    val iosMinVersion:       String,
    val iosMaxVersion:       String,
    val source:              FeatureSource,
    val confidence:          ConfidenceLevel,
    val requiresJailbreak:   Boolean,
    val dataLoss:            Boolean,
    val signalAfter:         Boolean,
    val iServicesAfter:      Boolean,
    val untethered:          Boolean,
    val isFree:              Boolean,
    val costCredits:         Int,
    val requiresInternet:    Boolean,
    val requiresDfu:         Boolean,
    val requiresImei:        Boolean,
    val riskLevel:           RiskLevel,
    val riskNotes:           List<String>,
    val executionSteps:      List<ExecutionStep>,
    val tags:                List<String>,
    // Android-specific
    val supportedBrands:     List<String>    = emptyList(),
    val supportedChipsets:   List<String>    = emptyList(),
    val connectionMode:      String          = "USB",  // USB | ADB | META | EDL | BROM | DIAG
) {
    val estimatedMinutes: IntRange
        get() = mechanism.minMinutes..mechanism.maxMinutes
}

// ── Device State ──────────────────────────────────────────────────────────────

@Immutable
data class DeviceState(
    val sessionId:    String,
    val ecid:         String?,
    val imei:         String?,
    val serial:       String?,
    val chipName:     String,
    val chipRange:    ChipRange,
    val iosVersion:   String,
    val buildNumber:  String?,
    val isJailbroken: Boolean,
    val fmiEnabled:   Boolean,
    val imeiPresent:  Boolean,
    val imeiValid:    Boolean,
    val isCdmaMeid:   Boolean,
    val activated:    Boolean,
    val mdmEnrolled:  Boolean,
    val dfuMode:      Boolean,
    val androidBrand: String?    = null,
    val androidModel: String?    = null,
    val androidChip:  String?    = null,
    val adbAvailable: Boolean    = false,
    val edlAvailable: Boolean    = false,
    val metaAvailable:Boolean    = false,
) {
    val isCheckm8Device: Boolean get() = chipRange.isCheckm8Vulnerable
    val canUseSignal:    Boolean get() = imeiPresent && imeiValid
}

// ── Supporting Models ─────────────────────────────────────────────────────────

data class Prerequisite(
    val name:    String,
    val met:     Boolean,
    val fixHint: String,
)

@Stable
data class ExecutionPlan(
    val feature:       BypassFeature,
    val device:        DeviceState,
    val prerequisites: List<Prerequisite>,
    val canExecute:    Boolean,
    val blockers:      List<String>,
    val warnings:      List<String>,
    val sessionId:     String,
)

data class RecommendationResult(
    val best:          BypassFeature?,
    val alternatives:  List<BypassFeature>,
    val freeOptions:   List<BypassFeature>,
    val signalOptions: List<BypassFeature>,
    val reasoning:     String,
    val device:        DeviceState?,
)

// ── Event Stream ──────────────────────────────────────────────────────────────

sealed class BypassEvent {
    data class PlanReady(val plan: ExecutionPlan, val sessionId: String) : BypassEvent()
    data class Started(val featureId: String, val sessionId: String) : BypassEvent()
    data class StepBegin(val featureId: String, val step: ExecutionStep, val sessionId: String) : BypassEvent()
    data class StepDone(val featureId: String, val stepNum: Int, val sessionId: String) : BypassEvent()
    data class ProgressUpdate(val featureId: String, val pct: Int, val currentPhase: String, val sessionId: String) : BypassEvent()
    data class NeedUserAction(val featureId: String, val instruction: String, val timeoutSecs: Int, val sessionId: String) : BypassEvent()
    data class Completed(val featureId: String, val signalEnabled: Boolean, val iServices: Boolean, val untethered: Boolean, val notes: List<String>, val sessionId: String) : BypassEvent()
    data class Failed(val featureId: String, val reason: String, val layer: String, val retryable: Boolean, val sessionId: String) : BypassEvent()
    data class RetryingNow(val featureId: String, val attempt: Int, val maxAttempts: Int, val backoffMs: Long, val sessionId: String) : BypassEvent()
    data class WarningIssued(val featureId: String, val message: String, val sessionId: String) : BypassEvent()
}

enum class DevicePlatform {
    IOS,
    ANDROID,
    UNKNOWN,
}

data class FeatureFilters(
    val searchQuery: String = "",
    val freeOnly: Boolean = false,
    val signalOnly: Boolean = false,
)