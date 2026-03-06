package com.deepeye.otg.data

import androidx.compose.ui.graphics.Color

enum class ConnectionMode(
    val label: String,
    val shortLabel: String,
    val description: String,
    val chipset: String,          // QC / MTK / ALL / HW
    val requiresHardware: Boolean // testpoint/ISP = true
) {
    ADB(
        label = "ADB",
        shortLabel = "ADB",
        description = "Android Debug Bridge — USB Debugging mode",
        chipset = "ALL",
        requiresHardware = false
    ),
    FASTBOOT(
        label = "Fastboot",
        shortLabel = "FBOOT",
        description = "Bootloader mode — Volume Down + Power",
        chipset = "ALL",
        requiresHardware = false
    ),
    EDL(
        label = "EDL",
        shortLabel = "EDL",
        description = "Emergency Download — Qualcomm 9008 port",
        chipset = "QC",
        requiresHardware = false
    ),
    TESTPOINT(
        label = "Testpoint",
        shortLabel = "T.POINT",
        description = "Hardware short to force EDL/BROM mode",
        chipset = "ALL",
        requiresHardware = true
    ),
    BROM(
        label = "BROM",
        shortLabel = "BROM",
        description = "Boot ROM mode — MediaTek emergency port",
        chipset = "MTK",
        requiresHardware = false
    ),
    PRELOADER(
        label = "Preloader",
        shortLabel = "PRELDR",
        description = "MTK Preloader mode — partial boot",
        chipset = "MTK",
        requiresHardware = false
    ),
    DIAG(
        label = "DIAG",
        shortLabel = "DIAG",
        description = "Qualcomm diagnostic AT commands port",
        chipset = "QC",
        requiresHardware = false
    ),
    MTP(
        label = "MTP",
        shortLabel = "MTP",
        description = "Normal USB — detect and ADB sideload",
        chipset = "ALL",
        requiresHardware = false
    ),
    ISP(
        label = "ISP",
        shortLabel = "ISP",
        description = "In-System Programming — hardware eMMC read",
        chipset = "ALL",
        requiresHardware = true
    ),
    META(
        label = "META",
        shortLabel = "META",
        description = "MTK/Huawei factory META test mode",
        chipset = "MTK",
        requiresHardware = false
    );

    // Color per chipset for chip indicator dot
    fun chipsetColor(): Color = when (chipset) {
        "QC"  -> Color(0xFF6750A4)   // purple = Qualcomm
        "MTK" -> Color(0xFF0891B2)   // cyan = MediaTek
        "HW"  -> Color(0xFFD97706)   // amber = Huawei
        else  -> Color(0xFF059669)   // green = Universal
    }

    fun hardwareWarning(): String? = if (requiresHardware)
        "⚠️ Requires hardware testpoint or ISP wiring"
    else null
}

// Extension: which features are available per mode
fun ConnectionMode.availableFeatureIds(): List<String> = when(this) {
    ConnectionMode.ADB -> listOf(
        "device_info", "adb_enable", "root", "app_manager",
        "erase_frp", "factory_reset", "demo_unlock"
    )
    ConnectionMode.FASTBOOT -> listOf(
        "unlock_bl", "factory_reset", "write_firmware",
        "read_firmware", "restore_efs"
    )
    ConnectionMode.EDL -> listOf(
        "erase_frp", "factory_reset", "write_firmware", "read_firmware",
        "partition_manager", "backup_efs", "restore_efs"
    )
    ConnectionMode.TESTPOINT -> listOf(
        "erase_frp", "factory_reset", "write_firmware", "read_firmware",
        "unlock_bl", "restore_efs"
    )
    ConnectionMode.BROM -> listOf(
        "erase_frp", "factory_reset", "write_firmware", "read_firmware",
        "backup_efs", "restore_efs"
    )
    ConnectionMode.PRELOADER -> listOf(
        "erase_frp", "factory_reset", "device_info"
    )
    ConnectionMode.DIAG -> listOf(
        "device_info", "imei_check"
    )
    ConnectionMode.MTP -> listOf(
        "device_info", "erase_frp"
    )
    ConnectionMode.ISP -> listOf(
        "write_firmware", "read_firmware", "partition_manager", "restore_efs"
    )
    ConnectionMode.META -> listOf(
        "erase_frp", "factory_reset", "device_info"
    )
}
