package com.deepeye.otg.protocol.apple.iphone15

enum class IphoneChip {
    A16_BIONIC,
    A17_PRO,
    UNKNOWN
}

enum class SecureBootState {
    FULL,
    PARTIAL,
    UNKNOWN
}

enum class VulnType {
    TYPE_CONFUSION,
    USE_AFTER_FREE,
    OOB_READ,
    BUFFER_OVERFLOW,
    INTEGER_OVERFLOW,
    MEMORY_CORRUPTION,
    LOGIC_BUG,
    OTHER
}

enum class DeliveryMethod {
    WEB_CONTENT,
    ZERO_CLICK,
    LOCAL_APP,
    USB_PHYSICAL,
    UNKNOWN
}

enum class ChainPosition {
    INITIAL_VECTOR,
    SANDBOX_ESCAPE,
    KERNEL,
    PPL,
    SEP,
    USB,
    OTHER
}

enum class ResearchChainStatus {
    APPLICABLE_PUBLIC_RESEARCH,
    PATCHED_ON_DEVICE,
    LIMITED_PUBLIC_RESEARCH,
    NO_PUBLIC_CHAIN
}

data class CveEntry(
    val cve: String,
    val component: String,
    val type: VulnType,
    val affectedIos: String,
    val patched: String,
    val exploitPublic: Boolean,
    val deliveryMethod: DeliveryMethod,
    val chainPosition: ChainPosition,
    val pacRequired: Boolean = false,
    val pplRequired: Boolean = false,
    val notes: String,
    val references: List<String> = emptyList()
)

data class ExploitChain(
    val chainId: String,
    val name: String,
    val applicableIos: String,
    val status: ResearchChainStatus,
    val reliability: Int,
    val notes: String
)

data class UsbCSurfaceReport(
    val usb3Capable: Boolean,
    val thunderboltCapable: Boolean,
    val superSpeedEndpointCount: Int,
    val usbPdVersion: String,
    val altMode: String,
    val usbAuthPresent: Boolean,
    val fuzzingCoverageHint: Int,
    val notes: List<String>
)

data class Iphone15Profile(
    val isIphone15Family: Boolean,
    val modelName: String,
    val chip: IphoneChip,
    val ecid: String?,
    val boardConfig: String,
    val storageGb: Int?,
    val iosVersion: String,
    val buildNumber: String,
    val basebandVersion: String,
    val bootchainVersion: String,
    val usbRestrictedMode: Boolean?,
    val activationLocked: Boolean?,
    val secureBootState: SecureBootState,
    val pacEnabled: Boolean,
    val mteEnabled: Boolean,
    val pplEnabled: Boolean,
    val stolenDeviceProtection: Boolean?,
    val usbSurface: UsbCSurfaceReport,
    val applicableCves: List<CveEntry>,
    val exploitChains: List<ExploitChain>,
    val safeCapabilities: List<String>,
    val knownLimitations: List<String>
)
