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
    ),
    ODIN(
        label = "ODIN",
        shortLabel = "ODIN",
        description = "Samsung Download Mode (Odin/Heimdall)",
        chipset = "EXYNOS/QC",
        requiresHardware = false
    ),
    FDL(
        label = "FDL",
        shortLabel = "FDL",
        description = "UniSoc Setup — Firmware Downloader",
        chipset = "UNISOC",
        requiresHardware = false
    ),
    UNKNOWN(
        label = "Unknown",
        shortLabel = "UNK",
        description = "Unidentified device — Check cable or device state",
        chipset = "UNK",
        requiresHardware = false
    );

    // Color per chipset for chip indicator dot
    fun chipsetColor(): Color = when (chipset) {
        "QC"  -> Color(0xFF6750A4)   // purple = Qualcomm
        "MTK" -> Color(0xFF0891B2)   // cyan = MediaTek
        "HW"  -> Color(0xFFD97706)   // amber = Huawei
        "UNK" -> Color(0xFF64748B)   // slate = Unknown
        else  -> Color(0xFF059669)   // green = Universal
    }

    fun hardwareWarning(): String? = if (requiresHardware)
        "⚠️ Requires hardware testpoint or ISP wiring"
    else null
}
