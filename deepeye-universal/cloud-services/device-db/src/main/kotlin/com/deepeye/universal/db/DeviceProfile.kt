package com.deepeye.universal.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant

// ============================================================================
// Feature-ID reference (1–24):
//  1  FRP_BYPASS           14  PATTERN_UNLOCK
//  2  FRP_RESET            15  PIN_UNLOCK
//  3  BOOTLOADER_UNLOCK    16  SCREEN_LOCK_REMOVE
//  4  FLASH_STOCK_ROM      17  IMEI_REPAIR
//  5  FLASH_CUSTOM_ROM     18  IMEI_READ
//  6  FLASH_RECOVERY       19  BASEBAND_REPAIR
//  7  READ_FLASH           20  NV_DATA_BACKUP
//  8  READ_INFO            21  NV_DATA_RESTORE
//  9  READ_GPT             22  CARRIER_UNLOCK
// 10  FORMAT_USERDATA      23  DRM_FIX
// 11  ERASE_PARTITION      24  ROOT_INSTALL
// 12  WRITE_PARTITION
// 13  BACKUP_FULL
// ============================================================================

/**
 * Enum mapping for frp_state_enum PostgreSQL type.
 */
enum class FrpState(val pgValue: String) {
    NO_FRP("NO_FRP"),
    FRP_STANDARD("FRP_STANDARD"),
    FRP_HARDENED("FRP_HARDENED"),
    FRP_UNKNOWN("FRP_UNKNOWN");

    companion object {
        fun fromPg(value: String): FrpState =
            entries.first { it.pgValue == value }
    }
}

/**
 * Enum mapping for chipset_family_enum PostgreSQL type.
 */
enum class ChipsetFamily(val pgValue: String) {
    QUALCOMM("QUALCOMM"),
    MEDIATEK("MEDIATEK"),
    EXYNOS("EXYNOS"),
    UNISOC("UNISOC"),
    KIRIN("KIRIN"),
    TENSOR("TENSOR"),
    SNAPDRAGON("SNAPDRAGON"),
    OTHER("OTHER");

    companion object {
        fun fromPg(value: String): ChipsetFamily =
            entries.first { it.pgValue == value }
    }
}

/**
 * Enum of all 24 supported device functions.
 * The [id] corresponds to the integer stored in device_profiles.supported_functions.
 */
enum class DeviceFunction(val id: Int, val label: String) {
    FRP_BYPASS(1, "FRP Bypass"),
    FRP_RESET(2, "FRP Reset"),
    BOOTLOADER_UNLOCK(3, "Bootloader Unlock"),
    FLASH_STOCK_ROM(4, "Flash Stock ROM"),
    FLASH_CUSTOM_ROM(5, "Flash Custom ROM"),
    FLASH_RECOVERY(6, "Flash Recovery"),
    READ_FLASH(7, "Read Flash"),
    READ_INFO(8, "Read Info"),
    READ_GPT(9, "Read GPT"),
    FORMAT_USERDATA(10, "Format Userdata"),
    ERASE_PARTITION(11, "Erase Partition"),
    WRITE_PARTITION(12, "Write Partition"),
    BACKUP_FULL(13, "Full Backup"),
    PATTERN_UNLOCK(14, "Pattern Unlock"),
    PIN_UNLOCK(15, "PIN Unlock"),
    SCREEN_LOCK_REMOVE(16, "Screen Lock Remove"),
    IMEI_REPAIR(17, "IMEI Repair"),
    IMEI_READ(18, "IMEI Read"),
    BASEBAND_REPAIR(19, "Baseband Repair"),
    NV_DATA_BACKUP(20, "NV Data Backup"),
    NV_DATA_RESTORE(21, "NV Data Restore"),
    CARRIER_UNLOCK(22, "Carrier Unlock"),
    DRM_FIX(23, "DRM Fix"),
    ROOT_INSTALL(24, "Root Install");

    companion object {
        private val byId = entries.associateBy { it.id }
        fun fromId(id: Int): DeviceFunction =
            byId[id] ?: throw IllegalArgumentException("Unknown function ID: $id")
    }
}

// ============================================================================
// Exposed Table Object — maps to PostgreSQL device_profiles table
// ============================================================================

private val jsonFormat = Json { ignoreUnknownKeys = true }

object DeviceProfiles : LongIdTable("device_profiles") {

    // Identity
    val brand: Column<String>          = varchar("brand", 64)
    val model: Column<String>          = varchar("model", 128)
    val marketingName: Column<String?> = varchar("marketing_name", 128).nullable()
    val codename: Column<String?>      = varchar("codename", 64).nullable()

    // Hardware
    val chipset: Column<String>        = varchar("chipset", 128)
    val chipsetFamily: Column<String>  = varchar("chipset_family", 32).default("OTHER")
    val cpuArch: Column<String>        = varchar("cpu_arch", 16).default("ARM64")

    // Security / FRP
    val frpState: Column<String>       = varchar("frp_state", 24).default("FRP_UNKNOWN")
    val bootloaderUnlockable: Column<Boolean> = bool("bootloader_unlockable").default(false)

    // Capabilities — JSONB array of integer feature IDs 1–24
    val supportedFunctions: Column<List<Int>> = jsonb(
        "supported_functions",
        { jsonFormat.encodeToString(ListSerializer(Int.serializer()), it) },
        { jsonFormat.decodeFromString(ListSerializer(Int.serializer()), it) }
    )

    // Protocol metadata
    val supportedProtocols: Column<String?> = text("supported_protocols").nullable() // stored as PG array literal
    val usbVid: Column<String?>        = varchar("usb_vid", 8).nullable()
    val usbPid: Column<String?>        = varchar("usb_pid", 8).nullable()

    // Audit
    val region: Column<String>         = varchar("region", 32).default("Global")
    val validationStatus: Column<String> = varchar("validation_status", 24).default("untested")
    val notes: Column<String?>         = text("notes").nullable()
    val createdAt: Column<Instant>     = timestamp("created_at").defaultExpression(org.jetbrains.exposed.sql.CurrentTimestamp)
    val updatedAt: Column<Instant>     = timestamp("updated_at").defaultExpression(org.jetbrains.exposed.sql.CurrentTimestamp)

    init {
        uniqueIndex("uq_brand_model", brand, model)
        index("idx_dp_brand", false, brand)
        index("idx_dp_chipset_family", false, chipsetFamily)
        index("idx_dp_frp_state", false, frpState)
    }
}

// ============================================================================
// Exposed DAO Entity
// ============================================================================

class DeviceProfile(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<DeviceProfile>(DeviceProfiles)

    var brand              by DeviceProfiles.brand
    var model              by DeviceProfiles.model
    var marketingName      by DeviceProfiles.marketingName
    var codename           by DeviceProfiles.codename
    var chipset            by DeviceProfiles.chipset
    var chipsetFamily      by DeviceProfiles.chipsetFamily
    var cpuArch            by DeviceProfiles.cpuArch
    var frpState           by DeviceProfiles.frpState
    var bootloaderUnlockable by DeviceProfiles.bootloaderUnlockable
    var supportedFunctions by DeviceProfiles.supportedFunctions
    var supportedProtocols by DeviceProfiles.supportedProtocols
    var usbVid             by DeviceProfiles.usbVid
    var usbPid             by DeviceProfiles.usbPid
    var region             by DeviceProfiles.region
    var validationStatus   by DeviceProfiles.validationStatus
    var notes              by DeviceProfiles.notes
    var createdAt          by DeviceProfiles.createdAt
    var updatedAt          by DeviceProfiles.updatedAt

    // ── Convenience helpers ──────────────────────────────────────────

    /** Typed FRP state enum. */
    val frpStateEnum: FrpState get() = FrpState.fromPg(frpState)

    /** Typed chipset family enum. */
    val chipsetFamilyEnum: ChipsetFamily get() = ChipsetFamily.fromPg(chipsetFamily)

    /** Resolved list of [DeviceFunction] enums from the stored integer IDs. */
    val functions: List<DeviceFunction>
        get() = supportedFunctions.map { DeviceFunction.fromId(it) }

    /** Check if a specific function is supported. */
    fun supports(fn: DeviceFunction): Boolean =
        fn.id in supportedFunctions

    /** Compact display string for logging/UI. */
    override fun toString(): String =
        "DeviceProfile(id=$id, brand=$brand, model=$model, chipset=$chipset, frp=$frpState, functions=${supportedFunctions.size})"
}

// ============================================================================
// Data Transfer Object — for API serialization
// ============================================================================

@Serializable
data class DeviceProfileDto(
    val id: Long,
    val brand: String,
    val model: String,
    val marketingName: String? = null,
    val codename: String? = null,
    val chipset: String,
    val chipsetFamily: String,
    val cpuArch: String = "ARM64",
    val frpState: String,
    val bootloaderUnlockable: Boolean,
    val supportedFunctions: List<Int>,
    val supportedProtocols: String?,
    val usbVid: String? = null,
    val usbPid: String? = null,
    val region: String = "Global",
    val validationStatus: String = "untested",
    val notes: String? = null,
) {
    companion object {
        fun from(entity: DeviceProfile) = DeviceProfileDto(
            id = entity.id.value,
            brand = entity.brand,
            model = entity.model,
            marketingName = entity.marketingName,
            codename = entity.codename,
            chipset = entity.chipset,
            chipsetFamily = entity.chipsetFamily,
            cpuArch = entity.cpuArch,
            frpState = entity.frpState,
            bootloaderUnlockable = entity.bootloaderUnlockable,
            supportedFunctions = entity.supportedFunctions,
            supportedProtocols = entity.supportedProtocols,
            usbVid = entity.usbVid,
            usbPid = entity.usbPid,
            region = entity.region,
            validationStatus = entity.validationStatus,
            notes = entity.notes,
        )
    }
}
