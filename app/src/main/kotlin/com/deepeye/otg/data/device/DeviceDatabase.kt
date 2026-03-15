package com.deepeye.otg.data.device

import timber.log.Timber

enum class DeviceProtocol {
    MTK_V6,        // MediaTek Dimensity 2022+ (V6 handshake)
    MTK_BROM,      // MediaTek classic BROM (pre-2022 budget)
    MTK_OR_QC,     // Xiaomi — runtime detect needed
    QC_EDL,        // Qualcomm EDL + Firehose
    SAMSUNG_ODIN,  // Samsung ODIN protocol
    UNKNOWN
}

enum class DeviceGroup {
    OPLUS, HUAWEI, XIAOMI, SAMSUNG, GOOGLE, SONY,
    MOTOROLA, HMD, ASUS, ZTE, NOTHING, NUBIA,
    TRANSSION, MICROMAX, LAVA, KARBONN,
    TCL, BULLITT, HTC, LG, RAZER, FAIRPHONE,
    ESSENTIAL, NEXTBIT, UNKNOWN
}

data class DeviceEntry(
    val brand: String,
    val model: String,
    val series: String,
    val year: Int,
    val type: String,
    val protocol: DeviceProtocol,
    val group: DeviceGroup,
    val vid: Int? = null,     // USB VID if known
    val hwCode: Int? = null,  // MTK hw_code if known
)

object DeviceDatabase {

    private val entries: List<DeviceEntry> = 
        DeviceData01.entries +
        DeviceData02.entries +
        DeviceData03.entries +
        DeviceData04.entries +
        DeviceData05.entries +
        DeviceData06.entries +
        DeviceData07.entries

    // ── Query API ────────────────────────────────────────

    fun findByBrandModel(brand: String, model: String): DeviceEntry? =
        entries.firstOrNull {
            it.brand.equals(brand, ignoreCase = true) &&
            it.model.equals(model, ignoreCase = true)
        }

    fun findByBrand(brand: String): List<DeviceEntry> =
        entries.filter { it.brand.equals(brand, ignoreCase = true) }

    fun allBrands(): List<String> = entries.map { it.brand }.distinct().sorted()

    fun modelsForBrand(brand: String): List<String> =
        findByBrand(brand).map { it.model }.distinct().sorted()

    fun total(): Int = entries.size

    fun countByProtocol(): Map<DeviceProtocol, Int> =
        entries.groupBy { it.protocol }.mapValues { it.value.size }

    fun protocolForBrand(brand: String): DeviceProtocol =
        findByBrand(brand).firstOrNull()?.protocol ?: DeviceProtocol.UNKNOWN

    fun getAllEntries(): List<DeviceEntry> = entries

    fun getStats(): Map<DeviceProtocol, Int> =
        entries.groupBy { it.protocol }.mapValues { it.value.size }

    /** Auto-detect protocol from USB VID only (no model needed) */
    fun protocolFromVid(vid: Int): DeviceProtocol =
        when (vid) {
            0x22D9 -> DeviceProtocol.MTK_V6    // OPLUS (Realme/OPPO/Vivo/OnePlus)
            0x2D95 -> DeviceProtocol.MTK_V6    // Vivo alternate VID
            0x2717 -> DeviceProtocol.MTK_OR_QC // Xiaomi
            0x04E8 -> DeviceProtocol.SAMSUNG_ODIN // Samsung
            0x18D1 -> DeviceProtocol.QC_EDL    // Google
            0x0FCE -> DeviceProtocol.QC_EDL    // Sony
            0x22B8 -> DeviceProtocol.QC_EDL    // Motorola
            0x0B05 -> DeviceProtocol.QC_EDL    // ASUS
            0x19D2 -> DeviceProtocol.QC_EDL    // ZTE
            0x1004 -> DeviceProtocol.QC_EDL    // LG
            0x0BB4 -> DeviceProtocol.QC_EDL    // HTC
            0x1BBB -> DeviceProtocol.MTK_BROM  // Transsion (Infinix/Tecno)
            0x0E8D -> DeviceProtocol.MTK_BROM  // MTK raw BROM
            0x05C6 -> DeviceProtocol.QC_EDL    // Qualcomm EDL
            else   -> DeviceProtocol.UNKNOWN
        }
}