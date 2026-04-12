package com.deepeye.otg.data.model

import java.util.UUID

enum class MtkConnectionMode {
    BROM,        // Boot ROM mode (preloader interrupt)
    PRELOADER,   // Preloader mode
    ADB,         // Normal ADB
    META,        // META mode (AT commands)
    FASTBOOT     // Fastboot mode
}

enum class MtkChip(val chipId: String, val chipName: String) {
    MT6580("0x6580", "Helio A20/A22"),
    MT6735("0x6735", "Helio P10"),
    MT6737("0x6737", "Helio P20"),
    MT6739("0x6739", "Helio A22"),
    MT6750("0x6750", "Helio P10"),
    MT6755("0x6755", "Helio P10"),
    MT6757("0x6757", "Helio P20"),
    MT6761("0x6761", "Helio A22"),
    MT6762("0x6762", "Helio P22"),
    MT6763("0x6763", "Helio P23"),
    MT6765("0x6765", "Helio G85"),
    MT6768("0x6768", "Helio G85"),
    MT6771("0x6771", "Helio P60"),
    MT6785("0x6785", "Helio G90T"),
    MT6789("0x6789", "Helio G99"),
    MT6833("0x6833", "Dimensity 700"),
    MT6853("0x6853", "Dimensity 720"),
    MT6873("0x6873", "Dimensity 800"),
    MT6877("0x6877", "Dimensity 900"),
    MT6879("0x6879", "Dimensity 1080"),
    MT6883("0x6883", "Dimensity 1000L"),
    MT6889("0x6889", "Dimensity 1000+"),
    MT6891("0x6891", "Dimensity 1200"),
    MT6893("0x6893", "Dimensity 1200 Ultra"),
    MT6983("0x6983", "Dimensity 9000"),
    UNKNOWN("0x0000", "Unknown MTK")
}

enum class MtkUnlockOperation {
    READ_INFO,           // Read device info
    READ_NVRAM,          // Read NVRAM partition
    WRITE_NVRAM,         // Write NVRAM partition
    REMOVE_FRP,          // Remove Google FRP lock
    REMOVE_MI_ACCOUNT,   // Remove Mi/Xiaomi account lock
    UNLOCK_BOOTLOADER,   // Unlock bootloader via BROM
    FORMAT_USERDATA,     // Format userdata (factory reset)
    READ_PRELOADER,      // Backup preloader
    WRITE_PRELOADER,     // Flash preloader
    DA_AUTH_BYPASS,      // Bypass Download Agent auth
    SLA_AUTH_BYPASS,     // Bypass SLA auth (newer chips)
    DISABLE_VERITY,      // Disable dm-verity
    PATCH_BOOT,          // Patch boot image (root)
    READ_PARTITIONS      // Read partition table
}

data class MtkDeviceInfo(
    val chipId: String = "",
    val chip: MtkChip = MtkChip.UNKNOWN,
    val hwCode: String = "",
    val hwSubCode: String = "",
    val hwVersion: String = "",
    val swVersion: String = "",
    val securityConfig: String = "",
    val connectMode: MtkConnectionMode = MtkConnectionMode.BROM,
    val brand: String = "",
    val model: String = "",
    val androidVer: String = "",
    val buildId: String = "",
    val daAuthRequired: Boolean = false,
    val slaAuthRequired: Boolean = false
)

data class MtkOperationResult(
    val operation: MtkUnlockOperation,
    val success: Boolean,
    val message: String,
    val outputPath: String? = null
)

data class MtkFlashTask(
    val id: String = UUID.randomUUID().toString(),
    val operation: MtkUnlockOperation,
    val partition: String = "",
    val imagePath: String = "",
    val fileSize: Long = 0L,
    val status: MtkTaskStatus = MtkTaskStatus.PENDING,
    val progress: Float = 0f,
    val logOutput: String = ""
)

enum class MtkTaskStatus {
    PENDING, RUNNING, SUCCESS, FAILED, SKIPPED
}

data class MtkPartitionInfo(
    val name: String,
    val startLba: Long,
    val endLba: Long,
    val sizeMb: Float,
    val index: Int,
    val type: String = ""
)
