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
    INTEL("Intel", Icons.Default.Public),
    ARCHIVE("Archive", Icons.Default.Inventory2)
}

enum class NavTarget(val hub: MissionHub) {
    DASHBOARD(MissionHub.COMMAND),
    DEVICES(MissionHub.COMMAND),
    DEVICE_SUPPORT(MissionHub.COMMAND),
    
    LAB_HOME(MissionHub.LAB),
    IMEI_REPAIR(MissionHub.LAB),
    STORAGE(MissionHub.LAB),
    PARTITION_EXPLORER(MissionHub.LAB),
    FILE_EXPLORER(MissionHub.LAB),
    FORENSICS_LAB(MissionHub.LAB),
    
    CVE_INTELLIGENCE(MissionHub.INTEL),
    FUZZ_DASHBOARD(MissionHub.INTEL),
    HID_RESEARCH(MissionHub.INTEL),
    IPHONE_15_RESEARCH(MissionHub.INTEL),
    
    SETTINGS(MissionHub.ARCHIVE),
    TERMINAL(MissionHub.ARCHIVE),
    VAULT(MissionHub.ARCHIVE)
}
