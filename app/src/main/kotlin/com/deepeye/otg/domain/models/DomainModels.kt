package com.deepeye.otg.domain.models

enum class ProtocolFamily {
    UNKNOWN, ADB, FASTBOOT, EDL, BROM, PRELOADER, DIAG, MTP, TESTPOINT, ODIN, RECOVERY, GENERIC,
    QC, SAMSUNG, UNISOC, MTP_ONLY, MTK,
    APPLE_DFU, APPLE_RECOVERY, APPLE_NORMAL, CDC_SERIAL
}

enum class DeviceMode {
    DISCONNECTED, MTP_ONLY, ADB, FASTBOOT, FASTBOOTD, RECOVERY,
    MTK_BROM, MTK_PRELOADER, MTK_META, QC_EDL, QC_DIAG,
    SAMSUNG_ODIN, UNISOC_FDL, TESTPOINT, UNKNOWN,
    APPLE_DFU, APPLE_RECOVERY, APPLE_NORMAL, CDC_SERIAL
}

enum class PolicyTier {
    SAFE, POLICY, RESTRICTED, NEVER
}

data class OperationAvailability(
    val enabled: Boolean,
    val reason: String? = null,
    val currentModeMismatch: Boolean = false,
    val missingConnection: Boolean = false,
    val missingModel: Boolean = false,
    val policyBlocked: Boolean = false
)

data class DeepEyeOperation(
    val id: String,
    val name: String,
    val label: String,
    val description: String,
    val tags: List<String> = emptyList(),
    val policyTier: PolicyTier = PolicyTier.SAFE,
    val protocolFamily: ProtocolFamily = ProtocolFamily.GENERIC,
    val requiredModes: Set<DeviceMode> = emptySet(),
    val requiresConnection: Boolean = true,
    val requiresModel: Boolean = false,
    val dangerous: Boolean = false,
    val enabledByDefaultInUi: Boolean = true
) {
    companion object {
        // Legacy shims for EngineDispatcher and PolicyEngine functionality
        val WRITE_FIRMWARE = DeepEyeOperation("op_write_fw", "Write Firmware", "Write Firmware", "", policyTier = PolicyTier.POLICY, dangerous = true)
        val READ_FIRMWARE = DeepEyeOperation("op_read_fw", "Read Firmware", "Read Firmware", "", policyTier = PolicyTier.SAFE)
        val BACKUP_EFS = DeepEyeOperation("op_backup_sec", "Backup EFS", "Backup EFS", "", policyTier = PolicyTier.SAFE)
        val RESTORE_EFS = DeepEyeOperation("op_restore_sec", "Restore EFS", "Restore EFS", "", policyTier = PolicyTier.POLICY)
        val PARTITION_MANAGER = DeepEyeOperation("op_part_mgr", "Partition Manager", "Partition Manager", "", policyTier = PolicyTier.RESTRICTED, dangerous = true)
        val FACTORY_RESET = DeepEyeOperation("op_factory_reset", "Factory Reset", "Factory Reset", "", policyTier = PolicyTier.SAFE, dangerous = true)
        val DEMO_UNLOCK = DeepEyeOperation("op_demo_retail", "Demo Unlock", "Demo Unlock", "", policyTier = PolicyTier.POLICY)
        val SAFE_WIPE = DeepEyeOperation("op_safe_wipe", "Safe Wipe", "Safe Wipe", "", policyTier = PolicyTier.SAFE)
        val ERASE_FRP = DeepEyeOperation("op_frp_google", "Erase FRP", "Erase FRP", "", policyTier = PolicyTier.SAFE)
        val MTK_METAMODE_FRP = DeepEyeOperation("op_meta_frp", "MTK MetaMode FRP", "MTK MetaMode FRP", "", policyTier = PolicyTier.POLICY)
        val REMOVE_MI_CLOUD = DeepEyeOperation("op_mi_cloud", "Remove Mi Cloud", "Remove Mi Cloud", "", policyTier = PolicyTier.POLICY)
        val EFRP_MDM_HOOK = DeepEyeOperation("op_efrp_enterprise", "EFRP / MDM Hook", "EFRP / MDM Hook", "", policyTier = PolicyTier.POLICY)
        val REMOVE_SCREEN_LOCK = DeepEyeOperation("op_screen_lock", "Remove Screen Lock", "Remove Screen Lock", "", policyTier = PolicyTier.POLICY)
        val LOCK_STATE_ANALYSIS = DeepEyeOperation("op_lock_state", "Lock Analysis", "Lock Analysis", "", policyTier = PolicyTier.SAFE)
        val UNLOCK_BOOTLOADER = DeepEyeOperation("op_bl_unlock", "Unlock Bootloader", "Unlock Bootloader", "", policyTier = PolicyTier.POLICY, dangerous = true)
        val MDM_REMOVE = DeepEyeOperation("op_mdm_check", "MDM Remove", "MDM Remove", "", policyTier = PolicyTier.POLICY)
        val IMEI_CHECK = DeepEyeOperation("op_imei_check", "IMEI Check", "IMEI Check", "", policyTier = PolicyTier.SAFE)
        val IMEI_RESTORE = DeepEyeOperation("op_imei_restore", "IMEI Restore", "IMEI Restore", "", policyTier = PolicyTier.POLICY)
        val MODEM_REPAIR = DeepEyeOperation("op_modem_diag", "Modem Repair", "Modem Repair", "", policyTier = PolicyTier.POLICY)
        val NETWORK_UNLOCK = DeepEyeOperation("op_net_unlock", "Network Unlock", "Network Unlock", "", policyTier = PolicyTier.POLICY)
        val DEEP_DEVICE_INFO = DeepEyeOperation("op_device_info", "Deep Device Info", "Deep Device Info", "", policyTier = PolicyTier.SAFE)
        val ADB_ENABLE = DeepEyeOperation("op_diag_enable", "Enable ADB", "Enable ADB", "", policyTier = PolicyTier.RESTRICTED)
        val ONE_CLICK_ROOT = DeepEyeOperation("op_root_flow", "One-Click Root", "One-Click Root", "", policyTier = PolicyTier.RESTRICTED, dangerous = true)
        val APP_MANAGER = DeepEyeOperation("op_app_mgr", "App Manager", "App Manager", "", policyTier = PolicyTier.SAFE)
        
        // Forensic Engine (added for EngineDispatcher compatibility)
        val SAFE_DUMP = DeepEyeOperation("op_safe_dump", "Safe Dump", "Safe Dump", "Bit-stream acquisition", policyTier = PolicyTier.SAFE)
        val DELETED_DATA_CARVING = DeepEyeOperation("op_carve", "Carve Deleted Data", "Carve", "Signature-based carving", policyTier = PolicyTier.POLICY)
        val FORENSIC_ACQUISITION = DeepEyeOperation("op_forensic", "Forensic Acquisition", "Acquire", "Full acquisition with hash", policyTier = PolicyTier.RESTRICTED)
        
        // Testing Harness
        val TEST_HARNESS = DeepEyeOperation("op_test_harness", "Test Harness", "Test", "Integration testing", policyTier = PolicyTier.SAFE)

        fun values(): Array<DeepEyeOperation> = arrayOf(            WRITE_FIRMWARE, READ_FIRMWARE, BACKUP_EFS, RESTORE_EFS, PARTITION_MANAGER,
            FACTORY_RESET, DEMO_UNLOCK, SAFE_WIPE, ERASE_FRP, MTK_METAMODE_FRP,
            REMOVE_MI_CLOUD, EFRP_MDM_HOOK, REMOVE_SCREEN_LOCK, LOCK_STATE_ANALYSIS,
            UNLOCK_BOOTLOADER, MDM_REMOVE, IMEI_CHECK, IMEI_RESTORE, MODEM_REPAIR,
            NETWORK_UNLOCK, DEEP_DEVICE_INFO, ADB_ENABLE, ONE_CLICK_ROOT, APP_MANAGER,
            SAFE_DUMP, DELETED_DATA_CARVING, FORENSIC_ACQUISITION, TEST_HARNESS
        )
    }
}

data class FeatureGroup(
    val id: String,
    val title: String,
    val operations: List<DeepEyeOperation>
)

data class ModeCardSpec(
    val id: String,
    val name: String,
    val isCommon: Boolean,
    val requirementsSummary: String,
    val typicalUseSummary: String,
    val cautionCopy: String? = null,
    val relatedDeviceMode: DeviceMode
)

data class SessionState(
    val connected: Boolean = false,
    val deviceName: String? = null,
    val selectedBrand: String? = null,
    val selectedModel: String? = null,
    val protocolFamily: ProtocolFamily = ProtocolFamily.UNKNOWN,
    val deviceMode: DeviceMode = DeviceMode.DISCONNECTED,
    val statusMessage: String = "Waiting for device...",
    val hasPermission: Boolean = false,
    val currentError: String? = null,
    val queuedOperation: DeepEyeOperation? = null,
    val progress: Float = 0f
)

data class PartitionItem(
    val id: String,
    val name: String,
    val sizeMb: String,
    val startAddress: String? = null,
    val type: String? = null
)
