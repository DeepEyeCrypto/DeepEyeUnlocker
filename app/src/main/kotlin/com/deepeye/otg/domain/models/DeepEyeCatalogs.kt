package com.deepeye.otg.domain.models

object DeepEyeCatalogs {

    val MODE_CATALOG = listOf(
        ModeCardSpec("adb", "ADB", true, "USB Debugging Enabled", "General info, sideload apps", null, DeviceMode.ADB),
        ModeCardSpec("adb_recovery", "ADB Recovery / Sideload", true, "Boot to Recovery", "FRP, sideload zips", null, DeviceMode.RECOVERY),
        ModeCardSpec("fastboot", "Fastboot", true, "Vol Down + Power", "Flash boot/recovery", null, DeviceMode.FASTBOOT),
        ModeCardSpec("fastbootd", "FastbootD", false, "Reboot to user-space fastboot", "Flash dynamic partitions", null, DeviceMode.FASTBOOTD),
        ModeCardSpec("mtk_brom", "MTK BROM", false, "Vol Up + Down on plugin", "Emergency MTK flashing", "Risk of hard brick if wrong DA", DeviceMode.MTK_BROM),
        ModeCardSpec("mtk_preldr", "MTK Preloader", true, "Plugin USB", "Normal MTK flashing", null, DeviceMode.MTK_PRELOADER),
        ModeCardSpec("mtk_meta", "MTK Meta", false, "From ADB/Preloader", "IMEI repair, factory tests", null, DeviceMode.MTK_META),
        ModeCardSpec("qc_edl", "Qualcomm EDL", false, "Testpoint or EDL cable", "Low-level unbrick (9008)", "Requires proper firehose", DeviceMode.QC_EDL),
        ModeCardSpec("qc_diag", "Qualcomm DIAG", false, "Enable via root/code", "QCN backup, network tests", null, DeviceMode.QC_DIAG),
        ModeCardSpec("unisoc_fdl", "UniSoc FDL / Download", false, "Vol Down + Plugin", "Spreadtrum/Unisoc flashing", null, DeviceMode.UNISOC_FDL),
        ModeCardSpec("samsung_odin", "Samsung Odin", true, "Vol Down+Bixby+Power", "Flash Samsung AP/BL/CP/CSC", null, DeviceMode.SAMSUNG_ODIN),
        ModeCardSpec("mtp", "MTP / Charge-only", true, "Screen Unlocked", "File transfer, detection", null, DeviceMode.MTP_ONLY),
        ModeCardSpec("unknown", "Unknown / Unclassified", true, "Any attached device", "No known protocol matched", null, DeviceMode.UNKNOWN)
    )

    val FEATURE_GROUPS = listOf(
        FeatureGroup("g1", "Flashing & Firmware", listOf(
            DeepEyeOperation("op_write_fw", "Write Firmware", "Write Firmware", "Flash full or partial firmware image to device", policyTier = PolicyTier.POLICY, protocolFamily = ProtocolFamily.GENERIC, dangerous = true),
            DeepEyeOperation("op_read_fw", "Read / Backup Firmware", "Read / Backup", "Dump full ROM blocks from device to host", policyTier = PolicyTier.SAFE),
            DeepEyeOperation("op_backup_sec", "Backup / Restore Security", "Backup Security", "Backup sensitive partitions (EFS, NVRAM, persist)", policyTier = PolicyTier.POLICY),
            DeepEyeOperation("op_part_mgr", "Partition Manager", "Partition Manager", "View and modify raw partition tables dynamically", policyTier = PolicyTier.RESTRICTED, dangerous = true)
        )),
        FeatureGroup("g2", "Reset & Cleanup", listOf(
            DeepEyeOperation("op_factory_reset", "Factory Reset / Format Userdata", "Factory Reset", "Securely wipe all user content", policyTier = PolicyTier.SAFE, dangerous = true),
            DeepEyeOperation("op_demo_retail", "Demo Mode to Retail", "Demo-to-Retail", "Convert store demo units to standard retail logic", policyTier = PolicyTier.RESTRICTED),
            DeepEyeOperation("op_safe_wipe", "Safe Wipe with Backup", "Safe Wipe", "Wipe device but retain app data or specific media", policyTier = PolicyTier.SAFE),
            DeepEyeOperation("op_brand_config", "Brand Config Presets", "Brand Config", "Apply specific region/brand NV configurations", policyTier = PolicyTier.POLICY)
        )),
        FeatureGroup("g3", "Account / Recovery Guidance", listOf(
            DeepEyeOperation("op_frp_google", "Google Account Recovery Guidance", "FRP Bypass / Reset", "Remove Factory Reset Protection locks", policyTier = PolicyTier.POLICY),
            DeepEyeOperation("op_frp_samsung", "Samsung / Mi Account Guidance", "Brand Account Reset", "Bypass or remove vendor cloud locks", policyTier = PolicyTier.POLICY),
            DeepEyeOperation("op_efrp_enterprise", "Enterprise EFRP Hooks", "Enterprise EFRP", "Manage enterprise managed setups", policyTier = PolicyTier.RESTRICTED),
            DeepEyeOperation("op_recovery_flow", "Recovery Flow Assistant", "Recovery Assist", "Step-by-step help for bootlooped phones", policyTier = PolicyTier.SAFE)
        )),
        FeatureGroup("g4", "Locks & Security", listOf(
            DeepEyeOperation("op_screen_lock", "Screen Lock Repair Workflow", "Screen Lock Reset", "Remove pattern or pin without data loss (if possible)", policyTier = PolicyTier.POLICY),
            DeepEyeOperation("op_lock_state", "Lock State Analysis", "Lock Analysis", "Detect current BL and FRP lock statuses", policyTier = PolicyTier.SAFE),
            DeepEyeOperation("op_bl_unlock", "Bootloader Unlock", "Bootloader Unlock", "Automate BL unlock processes (Brand dependent)", policyTier = PolicyTier.POLICY, dangerous = true),
            DeepEyeOperation("op_mdm_check", "MDM / Finance Lock Status", "MDM Status", "Check for finance or MDM restrictions", policyTier = PolicyTier.SAFE)
        )),
        FeatureGroup("g5", "Identity & Network", listOf(
            DeepEyeOperation("op_imei_check", "IMEI Integrity Check", "IMEI Check", "Verify partition signatures for Baseband/IMEI", policyTier = PolicyTier.SAFE),
            DeepEyeOperation("op_imei_restore", "Original IMEI Restore Workflow", "Original IMEI Restore", "Restore original backed-up IMEI (Legal compliance mode)", policyTier = PolicyTier.POLICY),
            DeepEyeOperation("op_modem_diag", "5G Modem / CPID Diagnostics", "Modem Diag", "Interact with diagnostic port for RF tests", policyTier = PolicyTier.RESTRICTED),
            DeepEyeOperation("op_net_unlock", "Network / SIM Unlock Guidance", "Network Unlock", "Read carrier lock states and provide guidance", policyTier = PolicyTier.POLICY)
        )),
        FeatureGroup("g6", "Advanced & Diagnostics", listOf(
            DeepEyeOperation("op_device_info", "Deep Device Info", "Device Info", "Read extensive hardware/software telemetry", policyTier = PolicyTier.SAFE, requiresConnection = true),
            DeepEyeOperation("op_diag_enable", "Diag / ADB Enabler", "Enable Diag/ADB", "Attempt auto-enable of debugging ports via exploits", policyTier = PolicyTier.POLICY),
            DeepEyeOperation("op_root_flow", "Root / Patch Workflow", "Root Manager", "Patch boot images for Magisk/KernelSU", policyTier = PolicyTier.POLICY),
            DeepEyeOperation("op_app_mgr", "ADB App Manager", "App Manager", "Uninstall bloatware over ADB connections", policyTier = PolicyTier.SAFE, requiredModes = setOf(DeviceMode.ADB))
        )),
        FeatureGroup("g7", "Forensics & Data Extraction", listOf(
            DeepEyeOperation("op_browse_fs", "Browse Decrypted Files", "Browse FS", "Live exploration of userdata partitions", policyTier = PolicyTier.POLICY),
            DeepEyeOperation("op_safe_dump", "Safe Dump (Bit-stream)", "Safe Dump", "Physical acquisition with hash verification", policyTier = PolicyTier.SAFE),
            DeepEyeOperation("op_carve", "Carve Deleted Data", "Carve Data", "Recover deleted files via signature analysis", policyTier = PolicyTier.RESTRICTED),
            DeepEyeOperation("op_ram_imaging", "RAM Forensics (Volatile)", "RAM Image", "Capture raw device memory for malware analysis", policyTier = PolicyTier.RESTRICTED)
        ))
    )
}
