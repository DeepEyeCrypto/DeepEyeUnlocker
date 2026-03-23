package com.deepeye.otg.data

// =============================================================================
// UniversalDeviceDatabase.kt
// Maps VID+PID+Brand → exact protocol + DA binary + entry method
// Covers all 1879 devices in DeepEye DB
// =============================================================================

// ── Protocol families ─────────────────────────────────────────────────────────

enum class ProtocolFamily {
    MTK_V6,           // Dimensity (MT6833+) — VID 0x22D9 or 0x0E8D PID 0x6
    MTK_BROM_CLASSIC, // Helio/older MTK — VID 0x0E8D PID 0x0003
    MTK_META,         // META mode — VID 0x0E8D PID 0x200x
    QC_EDL,           // Qualcomm 9008 — VID 0x05C6 PID 0x9008
    QC_DIAG,          // Qualcomm DIAG — VID 0x05C6 various PID
    SAMSUNG_ODIN,     // Samsung DL mode — VID 0x04E8 PID 0x685D
    SAMSUNG_MTK,      // Samsung MTK (A06/A14 etc) — BROM via UT
    SPD_UNISOC,       // Spreadtrum/UniSoc — VID 0x1782 PID 0x4d00
    HUAWEI_HISI,      // HiSilicon — VID 0x12D1 various
    ADB_GENERIC,      // ADB — any VID, IF cls=0xFF sub=0x42 proto=0x01
    FASTBOOT,         // Fastboot — any VID, IF cls=0xFF sub=0x42 proto=0x03
    IOS_DFU,          // Apple DFU — VID 0x05AC PID 0x1227
    IOS_RECOVERY,     // Apple Recovery — VID 0x05AC PID 0x1281
    IOS_NORMAL,       // Apple Normal — VID 0x05AC PID 0x12A8+
}

// ── DA chip map ────────────────────────────────────────────────────────────────

/**
 * Every MTK chip → DA asset name + protocol family.
 * DA binaries go in: app/src/main/assets/da/<filename>
 */
object MtkChipDatabase {

    data class ChipEntry(
        val hwCode:       Int,
        val chipName:     String,
        val daAsset:      String,      // path in assets/da/
        val protocol:     ProtocolFamily,
        val requiresSla:  Boolean = false,  // V5/V6 key exchange
        val requiresDaa:  Boolean = false,  // Download Agent Auth
        val brands:       List<String>,
    )

    val chips: List<ChipEntry> = listOf(

        // ── MediaTek V6 (Dimensity) — Preloader mode, VID:0x22D9 ──────────
        ChipEntry(0x1209, "MT6835T (Dimensity 6300)",
            "da/mt6835t_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("Realme", "OPPO", "Vivo", "Samsung")),

        ChipEntry(0x0321, "MT6789 (Dimensity 9000)",
            "da/mt6789_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("Realme", "OPPO")),

        ChipEntry(0x0355, "MT6893 (Dimensity 1200)",
            "da/mt6893_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("Xiaomi", "Realme", "OPPO")),

        ChipEntry(0x0321, "MT6895 (Dimensity 8100)",
            "da/mt6895_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("Realme", "OPPO", "Vivo")),

        ChipEntry(0x6983, "MT6983 (Dimensity 9200)",
            "da/mt6983_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("OPPO", "Vivo")),

        ChipEntry(0x6985, "MT6985 (Dimensity 9300)",
            "da/mt6985_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("OPPO", "Vivo", "Xiaomi")),

        ChipEntry(0x6989, "MT6989 (Dimensity 9400)",
            "da/mt6989_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("Vivo", "OPPO")),

        ChipEntry(0x6991, "MT6991 (Dimensity 9500)",
            "da/mt6991_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("OPPO", "Vivo")),

        ChipEntry(0x6855, "MT6855 (Dimensity 7050)",
            "da/mt6855_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("Realme", "OPPO", "Vivo")),

        ChipEntry(0x6877, "MT6877 (Dimensity 900)",
            "da/mt6877_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("Samsung", "Realme")),

        ChipEntry(0x6883, "MT6883 (Dimensity 1000L)",
            "da/mt6883_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("Vivo", "Realme")),

        ChipEntry(0x6885, "MT6885 (Dimensity 1000+)",
            "da/mt6885_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("Xiaomi", "OPPO")),

        ChipEntry(0x6833, "MT6833 (Dimensity 700)",
            "da/mt6833_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("Samsung", "Realme", "Xiaomi", "Vivo")),

        ChipEntry(0x6853, "MT6853 (Dimensity 720)",
            "da/mt6853_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("Xiaomi", "Realme", "OPPO")),

        ChipEntry(0x6873, "MT6873 (Dimensity 800)",
            "da/mt6873_da.bin", ProtocolFamily.MTK_V6,
            requiresSla = true,
            brands = listOf("Xiaomi", "Realme")),

        // ── MediaTek Classic BROM — VID:0x0E8D PID:0x0003 ─────────────────
        ChipEntry(0x6765, "MT6765 (Helio G35/P35)",
            "da/mt6765_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            requiresSla = false,
            brands = listOf("Realme", "Samsung", "Infinix", "Tecno",
                            "OPPO", "Vivo", "Xiaomi")),

        ChipEntry(0x6768, "MT6768 (Helio G85/P65)",
            "da/mt6768_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            requiresSla = false,
            brands = listOf("Samsung", "Realme", "OPPO", "Motorola")),

        ChipEntry(0x6769, "MT6769 (Helio G85)",
            "da/mt6769_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            requiresSla = false,
            brands = listOf("Xiaomi", "Samsung", "Realme", "OPPO")),

        ChipEntry(0x6771, "MT6771 (Helio P60/P70)",
            "da/mt6771_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            brands = listOf("Xiaomi", "OPPO", "Vivo")),

        ChipEntry(0x6779, "MT6779 (Helio G90T)",
            "da/mt6779_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            brands = listOf("Xiaomi", "Realme")),

        ChipEntry(0x6785, "MT6785 (Helio G90/G95)",
            "da/mt6785_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            requiresSla = false,
            brands = listOf("Xiaomi", "Realme", "Samsung", "OPPO")),

        ChipEntry(0x6799, "MT6799 (Helio X30)",
            "da/mt6799_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            brands = listOf("Meizu", "Sharp")),

        ChipEntry(0x6757, "MT6757 (Helio P25)",
            "da/mt6757_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            brands = listOf("OPPO", "Vivo", "Meizu")),

        ChipEntry(0x6739, "MT6739 (Helio A22)",
            "da/mt6739_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            brands = listOf("Nokia", "Samsung", "Infinix")),

        ChipEntry(0x6737, "MT6737 (Helio A22)",
            "da/mt6737_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            brands = listOf("Nokia", "Samsung")),

        // ── Transsion group (Infinix/Tecno/Itel) ──────────────────────────
        ChipEntry(0x6762, "MT6762 (Helio P22)",
            "da/mt6762_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            requiresSla = false,
            brands = listOf("Infinix", "Tecno", "Itel", "Samsung")),

        ChipEntry(0x6761, "MT6761 (Helio A20)",
            "da/mt6761_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            brands = listOf("Infinix", "Tecno", "Itel")),

        ChipEntry(0x6580, "MT6580",
            "da/mt6580_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            brands = listOf("Infinix", "Tecno", "Itel", "Micromax")),

        ChipEntry(0x6572, "MT6572",
            "da/mt6572_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            brands = listOf("Infinix", "Itel")),

        // ── Huawei MTK ────────────────────────────────────────────────────
        ChipEntry(0x6833, "MT6833 (Huawei Honor MTK)",
            "da/mt6833_huawei_da.bin", ProtocolFamily.MTK_BROM_CLASSIC,
            requiresSla = false,
            brands = listOf("Huawei", "Honor")),
    )

    private val byHwCode: Map<Int, ChipEntry> = chips.associateBy { it.hwCode }

    fun find(hwCode: Int): ChipEntry? = byHwCode[hwCode]

    fun daAsset(hwCode: Int): String? = byHwCode[hwCode]?.daAsset

    fun protocol(hwCode: Int): ProtocolFamily =
        byHwCode[hwCode]?.protocol ?: ProtocolFamily.MTK_BROM_CLASSIC
}

// ── Qualcomm device DB ────────────────────────────────────────────────────────

object QualcommDeviceDatabase {

    data class QcDevice(
        val brand:       String,
        val models:      List<String>,
        val chipset:     String,
        val programmer:  String,      // assets/prog/<filename>
        val edlMethod:   EdlEntryMethod,
        val is64Bit:     Boolean = true,
    )

    enum class EdlEntryMethod {
        ADB_REBOOT,        // adb reboot edl
        FASTBOOT_OEM,      // fastboot oem edl
        VOLUME_UP_USB,     // hardware: VolUp + USB
        NINE_PIN_CABLE,    // 9-pin EDL cable
        TEST_POINT,        // PCB test point
    }

    val devices: List<QcDevice> = listOf(

        // ── Xiaomi Qualcomm ───────────────────────────────────────────────
        QcDevice("Xiaomi",
            models   = listOf("Redmi Note 13 Pro+","Redmi Note 12 Pro",
                               "POCO X6 Pro","POCO F5","Mi 11","Mi 12","Mi 13"),
            chipset  = "SM8475/SM8550 (Snapdragon 8+/8 Gen2)",
            programmer = "prog/sm8550_ufs_firehose.elf",
            edlMethod  = EdlEntryMethod.ADB_REBOOT),

        QcDevice("Xiaomi",
            models   = listOf("Redmi 12 5G","Redmi 13C 5G",
                               "POCO M5","POCO X5"),
            chipset  = "SM6375/SM6450 (Snapdragon 4/6s Gen1)",
            programmer = "prog/sm6375_firehose.elf",
            edlMethod  = EdlEntryMethod.ADB_REBOOT),

        QcDevice("Xiaomi",
            models   = listOf("Redmi Note 11 4G","Redmi Note 10",
                               "POCO M3 Pro"),
            chipset  = "SM6115 (Snapdragon 662)",
            programmer = "prog/sm6115_firehose.elf",
            edlMethod  = EdlEntryMethod.ADB_REBOOT),

        // ── Samsung Qualcomm ──────────────────────────────────────────────
        QcDevice("Samsung",
            models   = listOf("Galaxy S21","Galaxy S22","Galaxy S23",
                               "Galaxy S24","Galaxy Z Fold4","Galaxy Z Flip4",
                               "Galaxy Note 20 Ultra"),
            chipset  = "SM8350/SM8450/SM8550 (Snapdragon 888/8 Gen1/8 Gen2)",
            programmer = "prog/sm8550_samsung_firehose.elf",
            edlMethod  = EdlEntryMethod.NINE_PIN_CABLE),

        QcDevice("Samsung",
            models   = listOf("Galaxy A42 5G","Galaxy A52 5G",
                               "Galaxy A72"),
            chipset  = "SM7225 (Snapdragon 750G)",
            programmer = "prog/sm7225_firehose.elf",
            edlMethod  = EdlEntryMethod.NINE_PIN_CABLE),

        // ── OPPO/OnePlus Qualcomm ─────────────────────────────────────────
        QcDevice("OPPO",
            models   = listOf("Find X5","Find X5 Pro","Find X6",
                               "Reno 10 Pro+","Reno 11 Pro"),
            chipset  = "SM8450/SM8550 (Snapdragon 8 Gen1/Gen2)",
            programmer = "prog/sm8550_firehose.elf",
            edlMethod  = EdlEntryMethod.VOLUME_UP_USB),

        QcDevice("OnePlus",
            models   = listOf("OnePlus 10 Pro","OnePlus 11",
                               "OnePlus 12","OnePlus Nord 3"),
            chipset  = "SM8475/SM8550",
            programmer = "prog/sm8550_firehose.elf",
            edlMethod  = EdlEntryMethod.ADB_REBOOT),

        // ── Motorola ──────────────────────────────────────────────────────
        QcDevice("Motorola",
            models   = listOf("Moto G84","Moto G85","Moto G Power 5G",
                               "Edge 40","Edge 50"),
            chipset  = "SM6375/SM7450",
            programmer = "prog/sm7450_firehose.elf",
            edlMethod  = EdlEntryMethod.VOLUME_UP_USB),

        // ── Google Pixel ──────────────────────────────────────────────────
        QcDevice("Google",
            models   = listOf("Pixel 6","Pixel 7","Pixel 8","Pixel 9"),
            chipset  = "GS101/GS201/Tensor G2/G3",
            programmer = "prog/tensor_fastboot.elf",
            edlMethod  = EdlEntryMethod.FASTBOOT_OEM),
    )

    private val brandIndex: Map<String, List<QcDevice>> =
        devices.groupBy { it.brand }

    fun find(brand: String, model: String): QcDevice? =
        brandIndex[brand]?.firstOrNull { d ->
            d.models.any { m -> m.equals(model, ignoreCase = true) ||
                                model.contains(m, ignoreCase = true) }
        }

    fun programmerForChipset(chipset: String): String? =
        devices.firstOrNull { chipset.lowercase() in it.chipset.lowercase() }
            ?.programmer
}

// ── Samsung MTK DB ────────────────────────────────────────────────────────────

object SamsungMtkDatabase {
    // Samsung devices using MTK chipsets (A-series budget)
    val devices = mapOf(
        // model → hwCode
        "Galaxy A06"  to 0x6765,
        "Galaxy A14"  to 0x6769,
        "Galaxy A15"  to 0x6769,
        "Galaxy A16"  to 0x6769,
        "Galaxy A23"  to 0x6768,
        "Galaxy A24"  to 0x6833,
        "Galaxy A25"  to 0x6833,
        "Galaxy A32"  to 0x6769,
        "Tab A7 Lite" to 0x6765,
        "Tab S10 FE"  to 0x6833,
        "Tab A9"      to 0x6765,
    )

    fun hwCode(model: String): Int? =
        devices.entries.firstOrNull { (m, _) ->
            model.contains(m, ignoreCase = true)
        }?.value
}

// ── UniSoc/SPD DB ─────────────────────────────────────────────────────────────

object UnisocDatabase {
    data class UnisocChip(
        val chipId:   String,
        val chipName: String,
        val brands:   List<String>,
    )

    val chips = listOf(
        UnisocChip("T606",  "UniSoc T606",
            listOf("Samsung", "Motorola", "Nokia", "Tecno")),
        UnisocChip("T610",  "UniSoc T610",
            listOf("Motorola", "Nokia")),
        UnisocChip("T616",  "UniSoc T616",
            listOf("Samsung", "Nokia", "Tecno")),
        UnisocChip("T618",  "UniSoc T618",
            listOf("Motorola", "Nokia")),
        UnisocChip("T700",  "UniSoc T700",
            listOf("Motorola", "Samsung", "Tecno", "Itel")),
        UnisocChip("SC9832","UniSoc SC9832",
            listOf("Samsung", "Nokia", "Lava")),
        UnisocChip("SC9863","UniSoc SC9863A",
            listOf("Samsung", "Tecno", "Itel", "Micromax")),
    )
}

// ── Universal VID/PID → Protocol router ──────────────────────────────────────

object UniversalProtocolDetector {

    data class DetectionResult(
        val protocol:    ProtocolFamily,
        val hwCode:      Int,
        val chipEntry:   MtkChipDatabase.ChipEntry?,
        val confidence:  String,  // HIGH / MEDIUM / LOW
        val brand:       String?,
    )

    fun detect(
        vid:         Int,
        pid:         Int,
        featureStr:  String? = null,  // from V6 hello packet
        ifaceClass:  Int = -1,
        ifaceSubclass:Int = -1,
        ifaceProto:  Int = -1,
    ): DetectionResult {
        return when {

            // Apple iOS
            vid == 0x05AC && pid == 0x1227 ->
                DetectionResult(ProtocolFamily.IOS_DFU, 0, null, "HIGH", "Apple")
            vid == 0x05AC && pid == 0x1281 ->
                DetectionResult(ProtocolFamily.IOS_RECOVERY, 0, null, "HIGH", "Apple")
            vid == 0x05AC ->
                DetectionResult(ProtocolFamily.IOS_NORMAL, 0, null, "HIGH", "Apple")

            // MTK V6 — OPLUS group (Realme/OPPO/Vivo/Honor/Samsung MTK V6)
            vid == 0x22D9 && pid == 0x6 -> {
                val hwCode = parseHwCodeFromFeature(featureStr) ?: 0x1209
                val chip   = MtkChipDatabase.find(hwCode)
                DetectionResult(ProtocolFamily.MTK_V6, hwCode, chip, "HIGH", "OPLUS")
            }

            // MTK V6 via standard VID
            vid == 0x0E8D && pid in listOf(0x6, 0x0006) -> {
                val hwCode = parseHwCodeFromFeature(featureStr) ?: 0x6833
                val chip   = MtkChipDatabase.find(hwCode)
                DetectionResult(ProtocolFamily.MTK_V6, hwCode, chip, "HIGH", "MTK")
            }

            // MTK BROM Classic
            vid == 0x0E8D && pid == 0x0003 -> {
                val hwCode = parseHwCodeFromFeature(featureStr) ?: 0x6765
                val chip   = MtkChipDatabase.find(hwCode)
                DetectionResult(ProtocolFamily.MTK_BROM_CLASSIC, hwCode, chip,
                    "HIGH", "MTK")
            }

            // MTK Preloader META mode
            vid == 0x0E8D && pid in listOf(0x2000, 0x2001, 0x201D, 0x200A) ->
                DetectionResult(ProtocolFamily.MTK_META, 0, null, "HIGH", "MTK")

            // Qualcomm EDL 9008
            vid == 0x05C6 && pid == 0x9008 ->
                DetectionResult(ProtocolFamily.QC_EDL, 0, null, "HIGH", "Qualcomm")

            // Qualcomm DIAG
            vid == 0x05C6 && pid in listOf(0x9025, 0x9091, 0x9001) ->
                DetectionResult(ProtocolFamily.QC_DIAG, 0, null, "HIGH", "Qualcomm")

            // Samsung Download Mode
            vid == 0x04E8 && pid in listOf(0x685D, 0x6860, 0x6861) ->
                DetectionResult(ProtocolFamily.SAMSUNG_ODIN, 0, null, "HIGH", "Samsung")

            // Transsion — Infinix/Tecno/Itel (use MTK BROM, VID 0x1BBB)
            vid == 0x1BBB ->
                DetectionResult(ProtocolFamily.MTK_BROM_CLASSIC, 0, null,
                    "MEDIUM", "Transsion")

            // UniSoc/SPD
            vid == 0x1782 && pid in listOf(0x4D00, 0x4E00) ->
                DetectionResult(ProtocolFamily.SPD_UNISOC, 0, null, "HIGH", "UniSoc")

            vid == 0x05C6 && pid == 0x9207 ->
                DetectionResult(ProtocolFamily.SPD_UNISOC, 0, null, "MEDIUM", "UniSoc")

            // Huawei/HiSilicon
            vid == 0x12D1 && pid in listOf(0x1446, 0x107E, 0x1057, 0x3609) ->
                DetectionResult(ProtocolFamily.HUAWEI_HISI, 0, null, "HIGH", "Huawei")

            // ADB (interface-class based — most reliable)
            ifaceClass == 0xFF && ifaceSubclass == 0x42 && ifaceProto == 0x01 ->
                DetectionResult(ProtocolFamily.ADB_GENERIC, 0, null, "HIGH", null)

            // Fastboot (interface-class based)
            ifaceClass == 0xFF && ifaceSubclass == 0x42 && ifaceProto == 0x03 ->
                DetectionResult(ProtocolFamily.FASTBOOT, 0, null, "HIGH", null)

            // Google Pixel / Nexus via ADB
            vid == 0x18D1 ->
                DetectionResult(ProtocolFamily.ADB_GENERIC, 0, null, "MEDIUM", "Google")

            else ->
                DetectionResult(ProtocolFamily.ADB_GENERIC, 0, null, "LOW", null)
        }
    }

    private fun parseHwCodeFromFeature(featureStr: String?): Int? {
        if (featureStr == null) return null
        // Parse "hw_code:0x1209;feature:V6;key:02;sn:XXXXXX"
        return featureStr
            .split(";")
            .firstOrNull { it.startsWith("hw_code:") }
            ?.removePrefix("hw_code:")
            ?.removePrefix("0x")
            ?.toIntOrNull(16)
    }
}
