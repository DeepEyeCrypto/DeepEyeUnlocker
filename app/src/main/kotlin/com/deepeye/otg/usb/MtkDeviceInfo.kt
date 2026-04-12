package com.deepeye.otg.usb

enum class MtkChip {
    MT6580, MT6735, MT6737, MT6739,
    MT6750, MT6753, MT6755, MT6757,
    MT6761, MT6762, MT6763, MT6765,
    MT6768, MT6771, MT6779, MT6785,
    MT6789, MT6833, MT6853, MT6873,
    MT6877, MT6879, MT6883, MT6885,
    MT6889, MT6891, MT6893, MT6983,
    MT6985, UNKNOWN
}

data class MtkDeviceInfo(
    val chip: MtkChip = MtkChip.UNKNOWN,
    val chipId: String = "0x0000",
    val hwCode: Int = 0x0000,
    val hwVersion: String = "0x0000",
    val swVersion: String = "0x0000",
    val isInBromMode: Boolean = false,
    val isInPreloaderMode: Boolean = false,
    val isInFastbootMode: Boolean = false,
    val supportsSla: Boolean = false,
    val supportsGlitch: Boolean = false,
    val daRequired: Boolean = true
) {
    val displayName: String get() = chip.name
        .replace("MT", "MT-")

    val isSupportedChip: Boolean get() =
        chip != MtkChip.UNKNOWN

    val recommendedMethod: String get() = when {
        isInBromMode && supportsGlitch ->
            "BROM Voltage Glitch"
        isInBromMode && supportsSla ->
            "SLA Auth Bypass"
        isInBromMode ->
            "DA Auth Bypass"
        isInFastbootMode ->
            "Force BL Unlock"
        else ->
            "Connect device in BROM/Fastboot mode"
    }
}
