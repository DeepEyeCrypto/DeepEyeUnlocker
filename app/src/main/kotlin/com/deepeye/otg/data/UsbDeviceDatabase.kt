package com.deepeye.otg.data

import com.deepeye.otg.data.ConnectionMode

data class UsbDeviceSignature(
    val vendorId: Int,
    val productId: Int,
    val brand: String,
    val mode: ConnectionMode,
    val description: String,
    val chipset: String = "UNKNOWN"
)

object UsbDeviceDatabase {

    // ── Qualcomm EDL (9008) ──────────────────────────────────
    // All Qualcomm phones in EDL = VID 0x05C6 PID 0x9008
    // Used by: Xiaomi, Oppo, Vivo, Realme, OnePlus, Samsung SD
    val EDL_SIGNATURES = listOf(
        UsbDeviceSignature(0x05C6, 0x9008, "Qualcomm", ConnectionMode.EDL,
            "Qualcomm HS-USB QDLoader 9008 — EDL mode", "QC"),
        UsbDeviceSignature(0x05C6, 0x900E, "Qualcomm", ConnectionMode.EDL,
            "Qualcomm HS-USB 900E — EDL variant", "QC"),
        UsbDeviceSignature(0x05C6, 0x9025, "Qualcomm", ConnectionMode.DIAG,
            "Qualcomm HS-USB Diagnostics 9025", "QC"),
        UsbDeviceSignature(0x05C6, 0x9091, "Qualcomm", ConnectionMode.DIAG,
            "Qualcomm HS-USB QDSS COM", "QC"),
        UsbDeviceSignature(0x05C6, 0x6000, "Qualcomm", ConnectionMode.DIAG,
            "Qualcomm HS-USB Serial — DIAG port", "QC"),
    )

    // ── MediaTek BROM / Preloader ────────────────────────────
    // MTK phones: VID 0x0E8D various PIDs
    val MTK_SIGNATURES = listOf(
        UsbDeviceSignature(0x0E8D, 0x0003, "MediaTek", ConnectionMode.BROM,
            "MTK Boot ROM (BROM) mode", "MTK"),
        UsbDeviceSignature(0x0E8D, 0x2000, "MediaTek", ConnectionMode.PRELOADER,
            "MTK Preloader mode (primary)", "MTK"),
        UsbDeviceSignature(0x0E8D, 0x2001, "MediaTek", ConnectionMode.META,
            "MTK Meta mode (secured boot variant)", "MTK"),
        UsbDeviceSignature(0x0E8D, 0x0023, "MediaTek", ConnectionMode.PRELOADER,
            "MTK Preloader mode", "MTK"),
        UsbDeviceSignature(0x0E8D, 0x0017, "MediaTek", ConnectionMode.META,
            "MTK Meta mode (factory test)", "MTK"),
        UsbDeviceSignature(0x0E8D, 0x763E, "MediaTek", ConnectionMode.META,
            "MTK USB Modem (META variant)", "MTK"),
    )

    // ── ADB — all major brands ────────────────────────────────
    // Standard Google ADB VID is 0x18D1
    // Each brand has own VID for ADB mode
    val ADB_SIGNATURES = listOf(
        // Google / AOSP ADB
        UsbDeviceSignature(0x18D1, 0x4EE7, "Google", ConnectionMode.ADB,
            "Google Android ADB Interface", "ALL"),
        UsbDeviceSignature(0x18D1, 0xD002, "Google", ConnectionMode.ADB,
            "Google Android ADB (rooted)", "ALL"),
        // Xiaomi ADB
        UsbDeviceSignature(0x2717, 0xFF48, "Xiaomi", ConnectionMode.ADB,
            "Xiaomi ADB Interface", "QC/MTK"),
        UsbDeviceSignature(0x2717, 0x5765, "Xiaomi", ConnectionMode.ADB,
            "Xiaomi ADB + MTP composite", "QC/MTK"),
        // Samsung ADB
        UsbDeviceSignature(0x04E8, 0x6860, "Samsung", ConnectionMode.ADB,
            "Samsung Android ADB", "EXYNOS/QC"),
        // Oppo / Realme ADB
        UsbDeviceSignature(0x22D9, 0x2773, "Oppo", ConnectionMode.ADB,
            "Oppo/Realme ADB Interface", "QC/MTK"),
        UsbDeviceSignature(0x22D9, 0x2769, "Oppo", ConnectionMode.ADB,
            "Oppo ADB + MTP", "QC/MTK"),
        // Vivo ADB
        UsbDeviceSignature(0x2D95, 0x6002, "Vivo", ConnectionMode.ADB,
            "Vivo ADB Interface", "QC/MTK"),
        UsbDeviceSignature(0x2D95, 0x6003, "Vivo", ConnectionMode.ADB,
            "Vivo ADB + MTP", "QC/MTK"),
        // OnePlus ADB
        UsbDeviceSignature(0x2A70, 0x9011, "OnePlus", ConnectionMode.ADB,
            "OnePlus ADB Interface", "QC"),
        UsbDeviceSignature(0x2A70, 0xF003, "OnePlus", ConnectionMode.ADB,
            "OnePlus ADB + MTP", "QC"),
        // HTC ADB
        UsbDeviceSignature(0x0BB4, 0x0C02, "HTC", ConnectionMode.ADB,
            "HTC Android ADB", "QC"),
        // Motorola ADB
        UsbDeviceSignature(0x22B8, 0x2E76, "Motorola", ConnectionMode.ADB,
            "Motorola ADB Interface", "QC"),
        // Sony ADB
        UsbDeviceSignature(0x0FCE, 0x6156, "Sony", ConnectionMode.ADB,
            "Sony Xperia ADB", "QC"),
    )

    // ── Fastboot ──────────────────────────────────────────────
    val FASTBOOT_SIGNATURES = listOf(
        // Google Fastboot
        UsbDeviceSignature(0x18D1, 0x4EE0, "Google", ConnectionMode.FASTBOOT,
            "Google Fastboot Interface", "ALL"),
        UsbDeviceSignature(0x18D1, 0x0D02, "Google", ConnectionMode.FASTBOOT,
            "Google Fastboot (bootloader)", "ALL"),
        // Xiaomi Fastboot
        UsbDeviceSignature(0x2717, 0xFF40, "Xiaomi", ConnectionMode.FASTBOOT,
            "Xiaomi Fastboot / Bootloader", "QC/MTK"),
        // Samsung Download Mode (Odin/Fastboot variant)
        UsbDeviceSignature(0x04E8, 0x685D, "Samsung", ConnectionMode.ODIN,
            "Samsung Download Mode (Odin)", "EXYNOS/QC"),
        UsbDeviceSignature(0x04E8, 0x685E, "Samsung", ConnectionMode.ODIN,
            "Samsung Download Mode (Odin)", "EXYNOS/QC"),
        UsbDeviceSignature(0x04E8, 0x6601, "Samsung", ConnectionMode.ODIN,
            "Samsung Odin Download Mode 2", "EXYNOS/QC"),
        // Oppo Fastboot
        UsbDeviceSignature(0x22D9, 0x276A, "Oppo", ConnectionMode.FASTBOOT,
            "Oppo Fastboot / Deep Test", "QC/MTK"),
        // OnePlus Fastboot
        UsbDeviceSignature(0x2A70, 0x9008, "OnePlus", ConnectionMode.FASTBOOT,
            "OnePlus Fastboot Mode", "QC"),
        // Vivo Fastboot
        UsbDeviceSignature(0x2D95, 0x6000, "Vivo", ConnectionMode.FASTBOOT,
            "Vivo Fastboot Mode", "QC/MTK"),
    )

    // ── UniSoc FDL ────────────────────────────────────────────
    val FDL_SIGNATURES = listOf(
        // Spreadtrum/UniSoc Bootloader / FDL mode (usually 0x1782)
        UsbDeviceSignature(0x1782, 0x4D00, "Spreadtrum", ConnectionMode.FDL,
            "UniSoc FDL Download Mode", "UNISOC"),
        UsbDeviceSignature(0x1782, 0x3D00, "Spreadtrum", ConnectionMode.FDL,
            "UniSoc FDL / Diag", "UNISOC")
    )

    // ── MTP (normal USB) ──────────────────────────────────────
    val MTP_SIGNATURES = listOf(
        UsbDeviceSignature(0x2717, 0xFF60, "Xiaomi", ConnectionMode.MTP,
            "Xiaomi MTP File Transfer", "QC/MTK"),
        UsbDeviceSignature(0x04E8, 0x685C, "Samsung", ConnectionMode.MTP,
            "Samsung MTP File Transfer", "EXYNOS/QC"),
        UsbDeviceSignature(0x22D9, 0x2764, "Oppo", ConnectionMode.MTP,
            "Oppo MTP File Transfer", "QC/MTK"),
        UsbDeviceSignature(0x2D95, 0x6004, "Vivo", ConnectionMode.MTP,
            "Vivo MTP File Transfer", "QC/MTK"),
    )

    // ── Master lookup ─────────────────────────────────────────
    private val ALL_SIGNATURES =
        EDL_SIGNATURES + MTK_SIGNATURES + ADB_SIGNATURES +
        FASTBOOT_SIGNATURES + MTP_SIGNATURES + FDL_SIGNATURES

    fun detect(vendorId: Int, productId: Int): UsbDeviceSignature? =
        ALL_SIGNATURES.firstOrNull {
            it.vendorId == vendorId && it.productId == productId
        }

    // Conservative fallback — VID-only detection when PID unknown.
    // MUST NEVER coerce unknown Android-like devices into ADB.
    // If explicit ADB VID:PID is not matched above, fallback stays non-ADB.
    fun detectByVendor(vendorId: Int): ConnectionMode = when (vendorId) {
        0x05C6 -> ConnectionMode.EDL       // Qualcomm
        0x0E8D -> ConnectionMode.BROM      // MediaTek
        0x1782 -> ConnectionMode.FDL       // UniSoc / Spreadtrum
        else   -> ConnectionMode.MTP       // Unknown → assume MTP
    }
}
