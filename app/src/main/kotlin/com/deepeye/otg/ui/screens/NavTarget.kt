package com.deepeye.otg.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * High-level Mission Hubs for V3.0 Command Architecture.
 */
enum class MissionHub(val label: String, val icon: ImageVector) {
    COMMAND("Command", Icons.Default.Terminal),
    LAB("Lab", Icons.Default.Science),
    BYPASS("Bypass", Icons.Default.FlashOn),
    INTEL("Intel", Icons.Default.Public),
    ARCHIVE("Archive", Icons.Default.Inventory2)
}

enum class NavTarget(val hub: MissionHub) {
    // COMMAND Hub
    DASHBOARD(MissionHub.COMMAND),
    DEVICES(MissionHub.COMMAND),
    DEVICE_SUPPORT(MissionHub.COMMAND),
    EDL_CONSOLE(MissionHub.COMMAND),
    
    // LAB Hub
    LAB_HOME(MissionHub.LAB),
    IMEI_REPAIR(MissionHub.LAB),
    STORAGE(MissionHub.LAB),
    PARTITION_EXPLORER(MissionHub.LAB),
    FILE_EXPLORER(MissionHub.LAB),
    FORENSICS_LAB(MissionHub.LAB),
    REMOTE_SHARE(MissionHub.LAB),
    
    // BYPASS Hub
    MISSION_HUB(MissionHub.BYPASS),
    UNLOCK_SCREEN(MissionHub.BYPASS),
    
    // INTEL Hub
    CVE_INTELLIGENCE(MissionHub.INTEL),
    FUZZ_DASHBOARD(MissionHub.INTEL),
    HID_RESEARCH(MissionHub.INTEL),
    IPHONE_15_RESEARCH(MissionHub.INTEL),
    
    // ARCHIVE Hub
    SETTINGS(MissionHub.ARCHIVE),
    TERMINAL(MissionHub.ARCHIVE),
    VAULT(MissionHub.ARCHIVE),
    LOG_SCREEN(MissionHub.ARCHIVE)
}
