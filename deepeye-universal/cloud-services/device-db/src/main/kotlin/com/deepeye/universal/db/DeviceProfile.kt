package com.deepeye.universal.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

// ============================================================================
// Feature-ID reference (1–24) — matches device_profiles.supported_functions
//
//  1  FRP_BYPASS             13  SAMSUNG_ACCOUNT_REMOVE
//  2  SCREEN_LOCK_REMOVE     14  PATTERN_UNLOCK
//  3  CARRIER_UNLOCK         15  EMERGENCY_DOWNLOAD
//  4  IMEI_REPAIR            16  QUALCOMM_EDL
//  5  BOOTLOADER_UNLOCK      17  MEDIATEK_AUTH
//  6  FACTORY_RESET          18  ADB_ENABLE
//  7  FIRMWARE_FLASH         19  FASTBOOT_FLASH
//  8  ROOT_ACCESS            20  ODIN_FLASH
//  9  KNOX_REMOVE            21  BASEBAND_REPAIR
// 10  GOOGLE_ACCOUNT_REMOVE  22  NVM_REPAIR
// 11  MI_ACCOUNT_REMOVE      23  EFS_BACKUP
// 12  HUAWEI_ID_REMOVE       24  ROOT_INSTALL
// ============================================================================

/**
 * Maps to PostgreSQL `chipset_type` ENUM.
 *
 * Values: qualcomm, mediatek, samsung_exynos, kirin, tensor, unisoc, unknown
 */
enum class ChipsetType(val pgValue: String) {
    QUALCOMM("qualcomm"),
    MEDIATEK("mediatek"),
    SAMSUNG_EXYNOS("samsung_exynos"),
    KIRIN("kirin"),
    TENSOR("tensor"),
    UNISOC("unisoc"),
    UNKNOWN("unknown");

    companion object {
        private val byPg = entries.associateBy { it.pgValue }
        fun fromPg(value: String): ChipsetType =
            byPg[value] ?: throw IllegalArgumentException("Unknown chipset_type: $value")
    }
}

/**
 * Maps to PostgreSQL `engine_type` ENUM.
 *
 * Values: qualcomm, mediatek, samsung, unisoc
 */
enum class EngineType(val pgValue: String) {
    QUALCOMM("qualcomm"),
    MEDIATEK("mediatek"),
    SAMSUNG("samsung"),
    UNISOC("unisoc");

    companion object {
        private val byPg = entries.associateBy { it.pgValue }
        fun fromPg(value: String): EngineType =
            byPg[value] ?: throw IllegalArgumentException("Unknown engine_type: $value")
    }
}

/**
 * All 24 supported device functions.
 * [id] corresponds to the integer stored in `device_profiles.supported_functions`.
 */
enum class DeviceFunction(val id: Int, val label: String) {
    FRP_BYPASS(1, "FRP Bypass"),
    SCREEN_LOCK_REMOVE(2, "Screen Lock Remove"),
    CARRIER_UNLOCK(3, "Carrier Unlock"),
    IMEI_REPAIR(4, "IMEI Repair"),
    BOOTLOADER_UNLOCK(5, "Bootloader Unlock"),
    FACTORY_RESET(6, "Factory Reset"),
    FIRMWARE_FLASH(7, "Firmware Flash"),
    ROOT_ACCESS(8, "Root Access"),
    KNOX_REMOVE(9, "Knox Remove"),
    GOOGLE_ACCOUNT_REMOVE(10, "Google Account Remove"),
    MI_ACCOUNT_REMOVE(11, "Mi Account Remove"),
    HUAWEI_ID_REMOVE(12, "Huawei ID Remove"),
    SAMSUNG_ACCOUNT_REMOVE(13, "Samsung Account Remove"),
    PATTERN_UNLOCK(14, "Pattern Unlock"),
    EMERGENCY_DOWNLOAD(15, "Emergency Download"),
    QUALCOMM_EDL(16, "Qualcomm EDL"),
    MEDIATEK_AUTH(17, "MediaTek Auth"),
    ADB_ENABLE(18, "ADB Enable"),
    FASTBOOT_FLASH(19, "Fastboot Flash"),
    ODIN_FLASH(20, "Odin Flash"),
    BASEBAND_REPAIR(21, "Baseband Repair"),
    NVM_REPAIR(22, "NVM Repair"),
    EFS_BACKUP(23, "EFS Backup"),
    ROOT_INSTALL(24, "Root Install");

    companion object {
        private val byId = entries.associateBy { it.id }
        fun fromId(id: Int): DeviceFunction =
            byId[id] ?: throw IllegalArgumentException("Unknown function ID: $id")
    }
}

// ============================================================================
// Exposed Table — device_profiles  (UUID PK)
// ============================================================================

private val jsonFormat = Json { ignoreUnknownKeys = true }

object DeviceProfiles : UUIDTable("device_profiles") {

    // Identity
    val brand: Column<String>          = varchar("brand", 64)
    val model: Column<String>          = varchar("model", 128)
    val series: Column<String?>        = varchar("series", 64).nullable()
    val releaseYear: Column<Short?>    = short("release_year").nullable()
    val deviceType: Column<String?>    = varchar("device_type", 32).nullable()

    // Hardware
    val chipset: Column<String>        = varchar("chipset", 32).default("unknown")
    val engine: Column<String>         = varchar("engine", 32)

    // Security / FRP
    val bootloaderUnlockable: Column<Boolean> = bool("bootloader_unlockable").default(false)
    val frpState: Column<String>       = varchar("frp_state", 32).default("UNKNOWN")

    // Capabilities — JSONB array of integer feature IDs 1–24
    val supportedFunctions: Column<List<Int>> = jsonb(
        "supported_functions",
        { jsonFormat.encodeToString(ListSerializer(Int.serializer()), it) },
        { jsonFormat.decodeFromString(ListSerializer(Int.serializer()), it) },
    )

    // Metadata
    val notes: Column<String?>         = text("notes").nullable()
    val createdAt: Column<Instant>     = timestamp("created_at")
        .defaultExpression(org.jetbrains.exposed.sql.CurrentTimestamp)
    val updatedAt: Column<Instant>     = timestamp("updated_at")
        .defaultExpression(org.jetbrains.exposed.sql.CurrentTimestamp)

    init {
        index("idx_dp_brand", false, brand)
        index("idx_dp_chipset", false, chipset)
        index("idx_dp_engine", false, engine)
        index("idx_dp_year", false, releaseYear)
        index("idx_dp_brand_model", false, brand, model)
    }
}

// ============================================================================
// Exposed DAO Entity — DeviceProfile
// ============================================================================

class DeviceProfile(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<DeviceProfile>(DeviceProfiles)

    var brand                by DeviceProfiles.brand
    var model                by DeviceProfiles.model
    var series               by DeviceProfiles.series
    var releaseYear          by DeviceProfiles.releaseYear
    var deviceType           by DeviceProfiles.deviceType
    var chipset              by DeviceProfiles.chipset
    var engine               by DeviceProfiles.engine
    var bootloaderUnlockable by DeviceProfiles.bootloaderUnlockable
    var frpState             by DeviceProfiles.frpState
    var supportedFunctions   by DeviceProfiles.supportedFunctions
    var notes                by DeviceProfiles.notes
    var createdAt            by DeviceProfiles.createdAt
    var updatedAt            by DeviceProfiles.updatedAt

    // ── Convenience helpers ──────────────────────────────────────────

    /** Typed chipset enum. */
    val chipsetType: ChipsetType get() = ChipsetType.fromPg(chipset)

    /** Typed engine enum. */
    val engineType: EngineType get() = EngineType.fromPg(engine)

    /** Resolved list of [DeviceFunction] enums. */
    val functions: List<DeviceFunction>
        get() = supportedFunctions.map { DeviceFunction.fromId(it) }

    /** Check whether a specific function is supported. */
    fun supports(fn: DeviceFunction): Boolean = fn.id in supportedFunctions

    override fun toString(): String =
        "DeviceProfile(id=$id, brand=$brand, model=$model, chipset=$chipset, engine=$engine, frp=$frpState, fns=${supportedFunctions.size})"
}

// ============================================================================
// Exposed Table — device_check_audit  (UUID PK)
// ============================================================================

object DeviceCheckAudits : UUIDTable("device_check_audit") {
    val userHash: Column<String>       = varchar("user_hash", 64)
    val brand: Column<String?>         = varchar("brand", 64).nullable()
    val model: Column<String?>         = varchar("model", 128).nullable()
    val serialHash: Column<String?>    = varchar("serial_hash", 64).nullable()
    val operation: Column<String>      = varchar("operation", 64)
    val tier: Column<Short>            = short("tier")
    val result: Column<String>         = varchar("result", 32)
    val checkedAt: Column<Instant>     = timestamp("checked_at")
        .defaultExpression(org.jetbrains.exposed.sql.CurrentTimestamp)

    init {
        index("idx_audit_user_hash", false, userHash, checkedAt)
        index("idx_audit_serial", false, serialHash)
    }
}

// ============================================================================
// Exposed DAO Entity — DeviceCheckAudit
// ============================================================================

class DeviceCheckAudit(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<DeviceCheckAudit>(DeviceCheckAudits)

    var userHash   by DeviceCheckAudits.userHash
    var brand      by DeviceCheckAudits.brand
    var model      by DeviceCheckAudits.model
    var serialHash by DeviceCheckAudits.serialHash
    var operation  by DeviceCheckAudits.operation
    var tier       by DeviceCheckAudits.tier
    var result     by DeviceCheckAudits.result
    var checkedAt  by DeviceCheckAudits.checkedAt

    override fun toString(): String =
        "DeviceCheckAudit(id=$id, user=$userHash, op=$operation, result=$result)"
}

// ============================================================================
// Data Transfer Object — DeviceProfileDto
// ============================================================================

@Serializable
data class DeviceProfileDto(
    val id: String,          // UUID as string
    val brand: String,
    val model: String,
    val series: String? = null,
    val releaseYear: Int? = null,
    val deviceType: String? = null,
    val chipset: String,
    val engine: String,
    val bootloaderUnlockable: Boolean,
    val supportedFunctions: List<Int>,
    val frpState: String,
    val notes: String? = null,
) {
    companion object {
        fun from(entity: DeviceProfile) = DeviceProfileDto(
            id = entity.id.value.toString(),
            brand = entity.brand,
            model = entity.model,
            series = entity.series,
            releaseYear = entity.releaseYear?.toInt(),
            deviceType = entity.deviceType,
            chipset = entity.chipset,
            engine = entity.engine,
            bootloaderUnlockable = entity.bootloaderUnlockable,
            supportedFunctions = entity.supportedFunctions,
            frpState = entity.frpState,
            notes = entity.notes,
        )
    }
}

// ============================================================================
// Data Transfer Object — DeviceCheckAuditDto
// ============================================================================

@Serializable
data class DeviceCheckAuditDto(
    val id: String,
    val userHash: String,
    val brand: String? = null,
    val model: String? = null,
    val serialHash: String? = null,
    val operation: String,
    val tier: Int,
    val result: String,
    val checkedAt: String,  // ISO-8601
) {
    companion object {
        fun from(entity: DeviceCheckAudit) = DeviceCheckAuditDto(
            id = entity.id.value.toString(),
            userHash = entity.userHash,
            brand = entity.brand,
            model = entity.model,
            serialHash = entity.serialHash,
            operation = entity.operation,
            tier = entity.tier.toInt(),
            result = entity.result,
            checkedAt = entity.checkedAt.toString(),
        )
    }
}
