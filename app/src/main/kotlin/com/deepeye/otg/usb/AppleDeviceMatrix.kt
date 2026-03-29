package com.deepeye.otg.usb

object AppleDeviceMatrix {

    val F3ARRAIN_SUPPORTED_CHIPS = mapOf(
        "A7" to listOf("iPhone 5s"),
        "A8" to listOf("iPhone 6", "iPhone 6 Plus", "iPod Touch 6"),
        "A9" to listOf("iPhone 6s", "iPhone 6s Plus", "iPhone SE (1st)"),
        "A10" to listOf("iPhone 7", "iPhone 7 Plus", "iPod Touch 7"),
        "A11" to listOf("iPhone 8", "iPhone 8 Plus", "iPhone X")
    )

    enum class AppleChip {
        A5, A6, A7, A8, A9, A10, A11,
        A12, A13, A14, A15, A16, A17,
        A18, // [HYPOTHESIS] Future proofing
        UNKNOWN
    }

    val CHECKM8_SUPPORTED = setOf(
        AppleChip.A5,
        AppleChip.A6,
        AppleChip.A7,
        AppleChip.A8,
        AppleChip.A9,
        AppleChip.A10,
        AppleChip.A11
    )

    // Device identifier -> chip map
    // NOTE: This table can be extended over time.
    val DEVICE_CHIP_MAP: Map<String, AppleChip> = mapOf(
        "iPhone5,1" to AppleChip.A5,
        "iPhone5,2" to AppleChip.A5,
        "iPhone5,3" to AppleChip.A6,
        "iPhone5,4" to AppleChip.A6,
        "iPhone6,1" to AppleChip.A7,
        "iPhone6,2" to AppleChip.A7,
        "iPhone7,1" to AppleChip.A8,
        "iPhone7,2" to AppleChip.A8,
        "iPhone8,1" to AppleChip.A9,
        "iPhone8,2" to AppleChip.A9,
        "iPhone8,4" to AppleChip.A9,
        "iPhone9,1" to AppleChip.A10,
        "iPhone9,2" to AppleChip.A10,
        "iPhone9,3" to AppleChip.A10,
        "iPhone9,4" to AppleChip.A10,
        "iPhone10,1" to AppleChip.A11,
        "iPhone10,2" to AppleChip.A11,
        "iPhone10,3" to AppleChip.A11,
        "iPhone10,4" to AppleChip.A11,
        "iPhone10,5" to AppleChip.A11,
        "iPhone10,6" to AppleChip.A11,
        "iPhone11,2" to AppleChip.A12,
        "iPhone11,4" to AppleChip.A12,
        "iPhone11,6" to AppleChip.A12,
        "iPhone11,8" to AppleChip.A12,
        "iPhone12,1" to AppleChip.A13,
        "iPhone12,3" to AppleChip.A13,
        "iPhone12,5" to AppleChip.A13,
        "iPhone13,1" to AppleChip.A14,
        "iPhone13,2" to AppleChip.A14,
        "iPhone13,3" to AppleChip.A14,
        "iPhone13,4" to AppleChip.A14,
        "iPhone14,2" to AppleChip.A15,
        "iPhone14,3" to AppleChip.A15,
        "iPhone14,4" to AppleChip.A15,
        "iPhone14,5" to AppleChip.A15,
        "iPhone14,6" to AppleChip.A15,
        "iPhone14,7" to AppleChip.A15,
        "iPhone14,8" to AppleChip.A15,
        "iPhone15,2" to AppleChip.A16,
        "iPhone15,3" to AppleChip.A16,
        "iPhone15,4" to AppleChip.A16,
        "iPhone15,4" to AppleChip.A16,
        "iPhone15,5" to AppleChip.A16,
        "iPhone16,1" to AppleChip.A17,
        "iPhone16,2" to AppleChip.A17,
        "iPhone17,1" to AppleChip.A18, // [HYPOTHESIS] iPhone 16 series
        "iPhone17,2" to AppleChip.A18,
        "iPhone17,3" to AppleChip.A18,
        "iPhone17,4" to AppleChip.A18
    )

    fun getChip(identifier: String): AppleChip =
        DEVICE_CHIP_MAP[identifier] ?: AppleChip.UNKNOWN

    fun isCheckm8Supported(identifier: String): Boolean =
        DEVICE_CHIP_MAP[identifier]?.let { it in CHECKM8_SUPPORTED } ?: false
}
