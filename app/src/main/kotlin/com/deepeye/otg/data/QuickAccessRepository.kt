package com.deepeye.otg.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import com.deepeye.otg.ui.model.QuickAccessCategory
import com.deepeye.otg.ui.model.QuickAccessItem
import com.deepeye.otg.ui.theme.DeepEyeColors

/**
 * QuickAccessRepository - Central registry of ALL features in DeepEyeUnlocker
 * This indexes every screen/feature available in the app for Quick Access navigation
 */
object QuickAccessRepository {

    fun getAllItems(): List<QuickAccessItem> = listOf(

        // ── BYPASS FEATURES ─────────────────────────────────
        QuickAccessItem(
            id = "unlock_screen",
            label = "Screen Lock",
            icon = Icons.Default.LockOpen,
            iconTint = Color(0xFFFF9800),
            navTarget = "UNLOCK_SCREEN",
            category = QuickAccessCategory.BYPASS,
            description = "Bypass screen lock patterns/PIN"
        ),
        QuickAccessItem(
            id = "frp_bypass",
            label = "FRP Bypass",
            icon = Icons.Default.Security,
            iconTint = Color(0xFF4CAF50),
            navTarget = "MISSION_HUB",
            category = QuickAccessCategory.BYPASS,
            description = "Factory Reset Protection bypass"
        ),
        QuickAccessItem(
            id = "mi_account",
            label = "MI Account",
            icon = Icons.Default.AccountCircle,
            iconTint = Color(0xFFFF5722),
            navTarget = "MISSION_HUB",
            category = QuickAccessCategory.BYPASS,
            description = "Xiaomi MI Account removal"
        ),

        // ── FLASH & PROTOCOL FEATURES ───────────────────────
        QuickAccessItem(
            id = "mtk_unlock",
            label = "MTK Unlock",
            icon = Icons.Default.Memory,
            iconTint = DeepEyeColors.TealSecondary,
            navTarget = "MTK_UNLOCK",
            category = QuickAccessCategory.FLASH,
            description = "MediaTek bootloader unlock"
        ),
        QuickAccessItem(
            id = "mtk_exploit",
            label = "MTK Exploit",
            icon = Icons.Default.DeveloperBoard,
            iconTint = Color(0xFF64B5F6),
            navTarget = "MTK_EXPLOIT",
            category = QuickAccessCategory.FLASH,
            description = "MediaTek BROM exploit"
        ),
        QuickAccessItem(
            id = "xiaomi_flash",
            label = "Xiaomi Flash",
            icon = Icons.Default.FlashOn,
            iconTint = Color(0xFFFF7043),
            navTarget = "XIAOMI_FLASH",
            category = QuickAccessCategory.FLASH,
            description = "Xiaomi device firmware flashing"
        ),
        QuickAccessItem(
            id = "xiaomi_exploit",
            label = "Xiaomi Exploit",
            icon = Icons.Default.BugReport,
            iconTint = Color(0xFFE57373),
            navTarget = "XIAOMI_EXPLOIT",
            category = QuickAccessCategory.FLASH,
            description = "Xiaomi-specific exploits"
        ),
        QuickAccessItem(
            id = "edl_console",
            label = "Qualcomm EDL",
            icon = Icons.Default.Memory,
            iconTint = DeepEyeColors.PurpleDim,
            navTarget = "EDL_CONSOLE",
            category = QuickAccessCategory.FLASH,
            description = "Qualcomm Emergency Download Mode"
        ),

        // ── DEVICE & TOOLS ──────────────────────────────────
        QuickAccessItem(
            id = "devices",
            label = "Devices",
            icon = Icons.Default.Devices,
            iconTint = DeepEyeColors.BlueAccent,
            navTarget = "DEVICES",
            category = QuickAccessCategory.TOOLS,
            description = "Connected device management"
        ),
        QuickAccessItem(
            id = "device_support",
            label = "Device Support",
            icon = Icons.Default.List,
            iconTint = Color(0xFF40C4FF),
            navTarget = "DEVICE_SUPPORT",
            category = QuickAccessCategory.TOOLS,
            description = "Check device compatibility"
        ),
        QuickAccessItem(
            id = "imei_repair",
            label = "IMEI Repair",
            icon = Icons.Default.SimCard,
            iconTint = DeepEyeColors.TealSecondary,
            navTarget = "IMEI_REPAIR",
            category = QuickAccessCategory.TOOLS,
            description = "Repair/restore IMEI numbers"
        ),
        QuickAccessItem(
            id = "storage",
            label = "Storage",
            icon = Icons.Default.Storage,
            iconTint = Color(0xFFAB47BC),
            navTarget = "STORAGE",
            category = QuickAccessCategory.TOOLS,
            description = "Device storage management"
        ),
        QuickAccessItem(
            id = "file_explorer",
            label = "File Explorer",
            icon = Icons.Default.Folder,
            iconTint = Color(0xFFFFD740),
            navTarget = "FILE_EXPLORER",
            category = QuickAccessCategory.TOOLS,
            description = "Browse device file system"
        ),
        QuickAccessItem(
            id = "partition_explorer",
            label = "Partitions",
            icon = Icons.Default.Dashboard,
            iconTint = Color(0xFF69F0AE),
            navTarget = "PARTITION_EXPLORER",
            category = QuickAccessCategory.TOOLS,
            description = "View/manage device partitions"
        ),
        QuickAccessItem(
            id = "terminal",
            label = "Terminal",
            icon = Icons.Default.Terminal,
            iconTint = Color(0xFF76FF03),
            navTarget = "TERMINAL",
            category = QuickAccessCategory.TOOLS,
            description = "Command line interface"
        ),

        // ── FORENSICS & LAB ─────────────────────────────────
        QuickAccessItem(
            id = "forensics_lab",
            label = "Forensics Lab",
            icon = Icons.Default.Science,
            iconTint = Color(0xFF29B6F6),
            navTarget = "LAB_HOME",
            category = QuickAccessCategory.TOOLS,
            description = "Digital forensics tools"
        ),
        QuickAccessItem(
            id = "vault",
            label = "Vault",
            icon = Icons.Default.Lock,
            iconTint = DeepEyeColors.GoldAccent,
            navTarget = "VAULT",
            category = QuickAccessCategory.TOOLS,
            description = "Secure data storage"
        ),
        QuickAccessItem(
            id = "remote_share",
            label = "Remote Share",
            icon = Icons.Default.Share,
            iconTint = Color(0xFFFFCA28),
            navTarget = "REMOTE_SHARE",
            category = QuickAccessCategory.TOOLS,
            description = "Share device remotely"
        ),

        // ── RESEARCH & INTELLIGENCE ─────────────────────────
        QuickAccessItem(
            id = "cve_intelligence",
            label = "CVE Database",
            icon = Icons.Default.Warning,
            iconTint = Color(0xFFFF5252),
            navTarget = "CVE_INTELLIGENCE",
            category = QuickAccessCategory.RESEARCH,
            description = "Vulnerability database"
        ),
        QuickAccessItem(
            id = "fuzz_dashboard",
            label = "Fuzz Testing",
            icon = Icons.Default.BugReport,
            iconTint = Color(0xFFE040FB),
            navTarget = "FUZZ_DASHBOARD",
            category = QuickAccessCategory.RESEARCH,
            description = "Fuzzing test results"
        ),
        QuickAccessItem(
            id = "hid_research",
            label = "HID Research",
            icon = Icons.Default.Usb,
            iconTint = Color(0xFF40C4FF),
            navTarget = "HID_RESEARCH",
            category = QuickAccessCategory.RESEARCH,
            description = "Human Interface Device research"
        ),
        QuickAccessItem(
            id = "iphone_15_research",
            label = "iPhone 15 Research",
            icon = Icons.Default.PhoneIphone,
            iconTint = Color(0xFFE0E0E0),
            navTarget = "IPHONE_15_RESEARCH",
            category = QuickAccessCategory.RESEARCH,
            description = "iPhone 15 security research"
        ),

        // ── APPLE / iOS FEATURES ────────────────────────────
        QuickAccessItem(
            id = "iphone_firmware",
            label = "iPhone Firmware",
            icon = Icons.Default.PhoneIphone,
            iconTint = DeepEyeColors.GoldAccent,
            navTarget = "DASHBOARD",
            category = QuickAccessCategory.APPLE,
            description = "iOS firmware management"
        ),

        // ── SYSTEM & SETTINGS ───────────────────────────────
        QuickAccessItem(
            id = "settings",
            label = "Settings",
            icon = Icons.Default.Settings,
            iconTint = Color(0xFFB0BEC5),
            navTarget = "SETTINGS",
            category = QuickAccessCategory.TOOLS,
            description = "App configuration"
        ),
        QuickAccessItem(
            id = "logs",
            label = "Logs",
            icon = Icons.Default.Article,
            iconTint = Color(0xFF90A4AE),
            navTarget = "LOG_SCREEN",
            category = QuickAccessCategory.TOOLS,
            description = "View operation logs"
        ),
        QuickAccessItem(
            id = "bypass_history",
            label = "History",
            icon = Icons.Default.History,
            iconTint = Color(0xFF80CBC4),
            navTarget = "BYPASS_HISTORY",
            category = QuickAccessCategory.TOOLS,
            description = "Past operations history"
        )
    )

    fun getByCategory(cat: QuickAccessCategory): List<QuickAccessItem> =
        getAllItems().filter { it.category == cat }

    fun getById(id: String): QuickAccessItem? =
        getAllItems().find { it.id == id }
}
