package com.deepeye.otg.usb

/**
 * Central lookup table for USB device VID/PID mappings.
 * Used by ProtocolDetector and other components to identify device modes.
 */
object DeviceMatrix {

    // MTK VID
    const val MTK_VID = 0x0E8D

    enum class MtkMode {
        BROM,
        PRELOADER,
        META,
        FASTBOOTD,
        AUTH_BYPASS,
        UNKNOWN
    }

    enum class MtkChipFamily {
        MT6572,
        MT6580,
        MT6735,
        MT6737,
        MT6750,
        MT6755,
        MT6757,
        MT6761,
        MT6763,
        MT6765,
        MT6768,
        MT6771,
        MT6779,
        MT6785,
        MT6833,
        MT6877,
        MT6893,
        DIMENSITY_700,
        DIMENSITY_900,
        DIMENSITY_1000,
        DIMENSITY_8000,
        UNKNOWN
    }

    val MTK_DEVICE_MODES = mapOf(
        0x0003 to MtkMode.BROM,
        0x2000 to MtkMode.PRELOADER,
        0x0023 to MtkMode.PRELOADER,
        0x200A to MtkMode.PRELOADER,
        0x200C to MtkMode.PRELOADER,
        0x200E to MtkMode.PRELOADER,
        0x0001 to MtkMode.META,
        0x2001 to MtkMode.META,
    )

    enum class OemBrand {
        SAMSUNG, XIAOMI, OPPO, VIVO, REALME, ONEPLUS, GOOGLE, MOTOROLA, LG, HUAWEI, APPLE, GENERIC
    }

    enum class FrpMethod {
        EDL_ERASE,
        BROM_ERASE,
        FASTBOOT_ERASE,
        ADB_BYPASS,
        CVE_EXPLOIT,
        ICLOUD_BYPASS,
        MTP_BROWSER_LAUNCH
    }

    enum class Chipset {
        QUALCOMM, MEDIATEK, EXYNOS, APPLE, UNISOC, HISILICON, UNKNOWN
    }

    data class FrpProfile(
        val brand: OemBrand,
        val chipset: Chipset,
        val method: FrpMethod,
        val vid: Int? = null,
        val pid: Int? = null,
        val partitionName: String? = null,
        val cveId: String? = null,
        val description: String
    )

    val FRP_PROFILES = listOf(
        FrpProfile(
            brand = OemBrand.SAMSUNG,
            chipset = Chipset.QUALCOMM,
            method = FrpMethod.EDL_ERASE,
            vid = 0x05C6,
            pid = 0x9008,
            partitionName = "frp",
            description = "Samsung Qualcomm EDL FRP Erase"
        ),
        FrpProfile(
            brand = OemBrand.SAMSUNG,
            chipset = Chipset.MEDIATEK,
            method = FrpMethod.BROM_ERASE,
            vid = 0x0E8D,
            pid = 0x0003,
            partitionName = "frp",
            description = "Samsung MTK BROM FRP Erase"
        ),
        FrpProfile(
            brand = OemBrand.XIAOMI,
            chipset = Chipset.QUALCOMM,
            method = FrpMethod.EDL_ERASE,
            vid = 0x05C6,
            pid = 0x9008,
            partitionName = "config",
            description = "Xiaomi Qualcomm EDL FRP Erase (config)"
        ),
        FrpProfile(
            brand = OemBrand.GOOGLE,
            chipset = Chipset.QUALCOMM,
            method = FrpMethod.FASTBOOT_ERASE,
            vid = 0x18D1,
            pid = 0xD00D,
            partitionName = "frp",
            description = "Google Pixel Fastboot FRP Erase"
        ),
        FrpProfile(
            brand = OemBrand.GENERIC,
            chipset = Chipset.MEDIATEK,
            method = FrpMethod.CVE_EXPLOIT,
            cveId = "CVE-2020-0069",
            description = "Generic MTK CVE-2020-0069 (MTK-SU) Bypass"
        )
    )

    enum class HydraProtocol {
        EDL_SAHARA_FIREHOSE,
        EDL_SAHARA_NAND,
        SAMSUNG_ODIN,
        SAMSUNG_EDL,
        MTK_BROM,
        MTK_PRELOADER,
        MTK_META,
        MTK_DOWNLOAD_AGENT,
        SPD_FDL1,
        SPD_FDL2,
        SPD_RESEARCH_DOWNLOAD,
        HUAWEI_HISI_USB_SERIAL,
        HUAWEI_FRP_BYPASS,
        LG_DOWNLOAD_MODE,
        LG_LAF,
        MOTO_FASTBOOT,
        MOTO_EDL,
        FASTBOOT,
        ADB,
    }

    val HYDRA_DEVICE_MATRIX = mapOf(
        0x05C6 to mapOf(
            0x9008 to HydraProtocol.EDL_SAHARA_FIREHOSE,
            0x9006 to HydraProtocol.EDL_SAHARA_NAND,
        ),
        0x04E8 to mapOf(
            0x685D to HydraProtocol.SAMSUNG_ODIN,
            0x6860 to HydraProtocol.SAMSUNG_ODIN,
            0x685E to HydraProtocol.SAMSUNG_ODIN,
        ),
        0x0E8D to mapOf(
            0x0003 to HydraProtocol.MTK_BROM,
            0x2000 to HydraProtocol.MTK_PRELOADER,
            0x0001 to HydraProtocol.MTK_META,
        ),
        0x1782 to mapOf(
            0x4D00 to HydraProtocol.SPD_FDL1,
            0x4D01 to HydraProtocol.SPD_FDL2,
            0x5400 to HydraProtocol.SPD_RESEARCH_DOWNLOAD,
        ),
        0x12D1 to mapOf(
            0x1037 to HydraProtocol.HUAWEI_HISI_USB_SERIAL,
            0x3609 to HydraProtocol.HUAWEI_FRP_BYPASS,
        ),
        0x1004 to mapOf(
            0x6300 to HydraProtocol.LG_LAF,
            0x633E to HydraProtocol.LG_DOWNLOAD_MODE,
        ),
        0x22B8 to mapOf(
            0x003A to HydraProtocol.MOTO_FASTBOOT,
            0x2E76 to HydraProtocol.MOTO_EDL,
        ),
    )

    // Apple VID
    const val APPLE_VID = 0x05AC

    // Apple PID to mode mapping
    // [INFERRED] Mappings are based on libirecovery behavior + existing ProtocolDetector routing.
    // PHYSICAL_DEVICE_REQUIRED: PID verification on iPhone 4/4S/iPad2-era devices and modern variants.
    val APPLE_DEVICE_MODES = mapOf(
        // Normal mode
        0x12A8 to AppleMode.NORMAL,      // [INFERRED] iPhone normal-mode USBMux
        0x12AB to AppleMode.NORMAL,      // [INFERRED] iPad normal-mode USBMux

        // Recovery mode
        0x1280 to AppleMode.RECOVERY,    // [INFERRED] Recovery legacy variant
        0x1281 to AppleMode.RECOVERY,    // [CONFIRMED] Recovery mode canonical PID
        0x1282 to AppleMode.RECOVERY,    // [INFERRED] Recovery legacy variant
        0x12A0 to AppleMode.RECOVERY,    // [INFERRED] Recovery variant seen on older/newer transitions
        0x1338 to AppleMode.RECOVERY,    // [INFERRED] Recovery (newer chips)

        // DFU / WTF
        0x1227 to AppleMode.DFU,         // [CONFIRMED] DFU mode canonical PID
        0x1222 to AppleMode.WTF,         // [INFERRED] WTF mode (A4 and below)
    )

    enum class AppleMode {
        NORMAL, RECOVERY, DFU, WTF, PWNED_DFU, UNKNOWN
    }

    /**
     * Detect Apple mode from USB device.
     * Returns AppleMode.UNKNOWN if not an Apple device or unknown PID.
     */
    fun detectAppleMode(vendorId: Int, productId: Int): AppleMode {
        if (vendorId != APPLE_VID) return AppleMode.UNKNOWN
        return APPLE_DEVICE_MODES[productId] ?: run {
            // Normal mode — PID varies by model, trust VID match
            if (productId in 0x1200..0x12FF) AppleMode.NORMAL
            else AppleMode.UNKNOWN
        }
    }

    fun detectHydraProtocol(vendorId: Int, productId: Int): HydraProtocol? {
        return HYDRA_DEVICE_MATRIX[vendorId]?.get(productId)
    }

    fun detectMtkMode(vendorId: Int, productId: Int): MtkMode {
        if (vendorId != MTK_VID) return MtkMode.UNKNOWN
        return MTK_DEVICE_MODES[productId] ?: MtkMode.UNKNOWN
    }

    fun detectMtkChipFamily(productName: String?): MtkChipFamily {
        val name = productName?.uppercase() ?: return MtkChipFamily.UNKNOWN
        return when {
            name.contains("MT6572") -> MtkChipFamily.MT6572
            name.contains("MT6580") -> MtkChipFamily.MT6580
            name.contains("MT6735") -> MtkChipFamily.MT6735
            name.contains("MT6737") -> MtkChipFamily.MT6737
            name.contains("MT6750") -> MtkChipFamily.MT6750
            name.contains("MT6755") -> MtkChipFamily.MT6755
            name.contains("MT6757") -> MtkChipFamily.MT6757
            name.contains("MT6761") -> MtkChipFamily.MT6761
            name.contains("MT6763") -> MtkChipFamily.MT6763
            name.contains("MT6765") -> MtkChipFamily.MT6765
            name.contains("MT6768") -> MtkChipFamily.MT6768
            name.contains("MT6771") -> MtkChipFamily.MT6771
            name.contains("MT6779") -> MtkChipFamily.MT6779
            name.contains("MT6785") -> MtkChipFamily.MT6785
            name.contains("MT6833") -> MtkChipFamily.MT6833
            name.contains("MT6877") -> MtkChipFamily.MT6877
            name.contains("MT6893") -> MtkChipFamily.MT6893
            name.contains("6300") || name.contains("DIMENSITY 700") -> MtkChipFamily.DIMENSITY_700
            name.contains("6900") || name.contains("DIMENSITY 900") -> MtkChipFamily.DIMENSITY_900
            name.contains("DIMENSITY 1000") -> MtkChipFamily.DIMENSITY_1000
            name.contains("DIMENSITY 8") -> MtkChipFamily.DIMENSITY_8000
            else -> MtkChipFamily.UNKNOWN
        }
    }

    fun lookupFrpProfile(vid: Int, pid: Int): FrpProfile? =
        FRP_PROFILES.firstOrNull { it.vid == vid && it.pid == pid }
}
