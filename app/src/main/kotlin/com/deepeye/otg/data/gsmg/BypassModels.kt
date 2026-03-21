package com.deepeye.otg.data.gsmg

// =============================================================================
// BypassModels.kt — Complete type system v3.0
// All enums, data classes, sealed classes for bypass feature system
// =============================================================================

// ─── Enums ────────────────────────────────────────────────────────────────────

enum class BypassMechanism(
    val displayName:      String,
    val requiresInternet: Boolean,
    val requiresDfu:      Boolean,
    val minMinutes:       Int,
    val maxMinutes:       Int,
) {
    RAMDISK(              "Ramdisk Boot",            false, true,  3,  8),
    CHECKM8(              "checkm8 Exploit",         false, true,  2,  5),
    CHECKM8_IRAIN(        "checkm8 + iRa1n",         false, true,  5, 12),
    CHECKM8_RAMDISK(      "checkm8 + Ramdisk",       false, true,  4, 10),
    IRAIN(                "iRa1n Jailbreak",         false, true,  5, 10),
    SERVER_EXPLOIT(       "Server Activation",       true,  false, 1,  3),
    SERVER_REGISTRATION(  "IMEI Registration",       true,  false, 1,  5),
    NVRAM_INJECTION(      "NVRAM Injection",         false, true,  2,  6),
    DFU_RESTORE(          "DFU Restore",             true,  true, 15, 40),
    RAMDISK_WRITE(        "Ramdisk Write",           false, true,  3,  8),
    RAMDISK_READ(         "Ramdisk Read",            false, true,  2,  5),
    RAMDISK_DELETE(       "Ramdisk Delete",          false, true,  3,  7),
    CUSTOM_IPSW(          "Custom IPSW Flash",       true,  true, 20, 50),
    USB_READ(             "USB Info Read",           false, false, 0,  1),
    USB_SEQUENCE(         "DFU Guide",               false, false, 1,  3),
    ACTIVATION_PATCH(     "Activation Patch",        false, false, 1,  3),
    ACTIVATION_RECORD_PATCH("iServices Patch",       true,  false, 2,  5),
    INTEGRITY_PATCH(      "Integrity Patch",         false, false, 1,  3),
    ADB_EXPLOIT(          "ADB FRP Exploit",         false, false, 2,  8),
}

enum class ChipRange(
    val displayName:        String,
    val isCheckm8Vulnerable:Boolean,
) {
    A7_TO_A11(  "A7–A11  (5S–X)",          true),
    A8_TO_A11(  "A8–A11  (6–X)",           true),
    A12_TO_A18( "A12–A18 (XR–16 Pro Max)", false),
    A7_TO_A18(  "All Apple Silicon",        false),
    ALL_ANDROID("All Android",             false),
}

enum class FeatureCategory(
    val displayName: String,
    val icon:        String,
) {
    ICLOUD_BYPASS(    "iCloud Bypass",   "🔓"),
    PASSCODE(         "Passcode",        "🔐"),
    DEVICE_MANAGEMENT("Device Mgmt",    "📱"),
    FIRMWARE(         "Firmware",        "💾"),
    CARRIER(          "Carrier",         "📶"),
    MDM(              "MDM",             "🏢"),
    SERVICES(         "iServices",       "⚙️"),
    DEVICE_INFO(      "Device Info",     "ℹ️"),
    EXPLOIT_ENGINE(   "Exploit Engine",  "⚡"),
    ANDROID(          "Android",         "🤖"),
}

enum class FeatureSource(val displayName: String) {
    GSMG(          "GSMG Tool"),
    IREMOVAL(      "iRemoval Pro"),
    F3ARRAIN(      "F3arRa1n"),
    GSMG_IREMOVAL( "GSMG + iRemoval"),
    ALL_TOOLS(     "All Tools"),
}

enum class RiskLevel {
    LOW, MEDIUM, HIGH, EXTREME;
    fun label(): String = when (this) {
        LOW     -> "Low Risk"
        MEDIUM  -> "Medium Risk"
        HIGH    -> "High Risk — review before running"
        EXTREME -> "EXTREME — data loss, irreversible"
    }
}

enum class ConfidenceLevel {
    CONFIRMED, INFERRED, HYPOTHESIS;
    fun label(): String = when (this) {
        CONFIRMED  -> "Confirmed from tool documentation"
        INFERRED   -> "Inferred from UI/reviews"
        HYPOTHESIS -> "Needs device test to confirm"
    }
}

// ─── Core Feature Model ───────────────────────────────────────────────────────

data class ExecutionStep(
    val stepNum:     Int,
    val title:       String,
    val instruction: String,
    val isAutomatic: Boolean,
    val timeoutSecs: Int = 30,
)

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
) {
    val estimatedMinutes: IntRange
        get() = mechanism.minMinutes..mechanism.maxMinutes
}

// ─── Device State ─────────────────────────────────────────────────────────────

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
) {
    val isCheckm8Device: Boolean get() = chipRange.isCheckm8Vulnerable
    val canUseSignal:    Boolean get() = imeiPresent && imeiValid
}

// ─── Plan + Prerequisite ─────────────────────────────────────────────────────

data class Prerequisite(
    val name:    String,
    val met:     Boolean,
    val fixHint: String,
)

data class ExecutionPlan(
    val feature:          BypassFeature,
    val device:           DeviceState,
    val prerequisites:    List<Prerequisite>,
    val canExecute:       Boolean,
    val blockers:         List<String>,
    val warnings:         List<String>,
    val sessionId:        String,
)

// ─── Recommendation ───────────────────────────────────────────────────────────

data class RecommendationResult(
    val best:          BypassFeature?,
    val alternatives:  List<BypassFeature>,
    val freeOptions:   List<BypassFeature>,
    val signalOptions: List<BypassFeature>,
    val reasoning:     String,
    val device:        DeviceState?,
)

// ─── Events ──────────────────────────────────────────────────────────────────

sealed class BypassEvent {
    data class PlanReady(
        val plan:      ExecutionPlan,
        val sessionId: String,
    ) : BypassEvent()

    data class Started(
        val featureId: String,
        val sessionId: String,
    ) : BypassEvent()

    data class StepBegin(
        val featureId: String,
        val step:      ExecutionStep,
        val sessionId: String,
    ) : BypassEvent()

    data class StepDone(
        val featureId: String,
        val stepNum:   Int,
        val sessionId: String,
    ) : BypassEvent()

    data class ProgressUpdate(
        val featureId:    String,
        val pct:          Int,
        val currentPhase: String,
        val sessionId:    String,
    ) : BypassEvent()

    data class NeedUserAction(
        val featureId:   String,
        val instruction: String,
        val timeoutSecs: Int,
        val sessionId:   String,
    ) : BypassEvent()

    data class Completed(
        val featureId:     String,
        val signalEnabled: Boolean,
        val iServices:     Boolean,
        val untethered:    Boolean,
        val notes:         List<String>,
        val sessionId:     String,
    ) : BypassEvent()

    data class Failed(
        val featureId: String,
        val reason:    String,
        val layer:     String,
        val retryable: Boolean,
        val sessionId: String,
    ) : BypassEvent()

    data class RetryingNow(
        val featureId:   String,
        val attempt:     Int,
        val maxAttempts: Int,
        val backoffMs:   Long,
        val sessionId:   String,
    ) : BypassEvent()

    data class WarningIssued(
        val featureId: String,
        val message:   String,
        val sessionId: String,
    ) : BypassEvent()
}
