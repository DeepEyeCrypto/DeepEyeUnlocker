package com.deepeye.otg.domain.models

enum class ProtocolFamily {
    UNKNOWN, ADB, FASTBOOT, EDL, BROM, PRELOADER, DIAG, MTP, TESTPOINT, ODIN, RECOVERY, GENERIC,
    QC, SAMSUNG, UNISOC, MTP_ONLY, MTK
}

enum class DeviceMode {
    DISCONNECTED, MTP_ONLY, ADB, FASTBOOT, FASTBOOTD, RECOVERY,
    MTK_BROM, MTK_PRELOADER, MTK_META, QC_EDL, QC_DIAG,
    SAMSUNG_ODIN, UNISOC_FDL, UNKNOWN
}

enum class PolicyTier {
    SAFE, POLICY, RESTRICTED, NEVER
}

data class OperationAvailability(
    val enabled: Boolean,
    val reason: String? = null,
    val currentModeMismatch: Boolean = false,
    val policyBlocked: Boolean = false
)

data class DeepEyeOperation(
    val id: String,
    val name: String,
    val label: String,
    val description: String,
    val tags: List<String> = emptyList(),
    val tier: PolicyTier = PolicyTier.SAFE,
    val protocolFamily: ProtocolFamily = ProtocolFamily.GENERIC,
    val requiredModes: List<DeviceMode> = emptyList(),
    val requiresConnection: Boolean = true,
    val requiresModel: Boolean = false,
    val dangerous: Boolean = false,
    val enabledByDefaultInUi: Boolean = true
) {
    companion object {
        // Legacy shims for EngineDispatcher and PolicyEngine functionality
        val WRITE_FIRMWARE = DeepEyeOperation("op_write_fw", "Write Firmware", "Write Firmware", "", tier = PolicyTier.POLICY, dangerous = true)
        val READ_FIRMWARE = DeepEyeOperation("op_read_fw", "Read Firmware", "Read Firmware", "", tier = PolicyTier.SAFE)
        val BACKUP_EFS = DeepEyeOperation("op_backup_sec", "Backup EFS", "Backup EFS", "", tier = PolicyTier.SAFE)
        val RESTORE_EFS = DeepEyeOperation("op_restore_sec", "Restore EFS", "Restore EFS", "", tier = PolicyTier.POLICY)
        val PARTITION_MANAGER = DeepEyeOperation("op_part_mgr", "Partition Manager", "Partition Manager", "", tier = PolicyTier.RESTRICTED, dangerous = true)
        val FACTORY_RESET = DeepEyeOperation("op_factory_reset", "Factory Reset", "Factory Reset", "", tier = PolicyTier.SAFE, dangerous = true)
        val DEMO_UNLOCK = DeepEyeOperation("op_demo_retail", "Demo Unlock", "Demo Unlock", "", tier = PolicyTier.POLICY)
        val SAFE_WIPE = DeepEyeOperation("op_safe_wipe", "Safe Wipe", "Safe Wipe", "", tier = PolicyTier.SAFE)
        val ERASE_FRP = DeepEyeOperation("op_frp_google", "Erase FRP", "Erase FRP", "", tier = PolicyTier.SAFE)
        val MTK_METAMODE_FRP = DeepEyeOperation("op_meta_frp", "MTK MetaMode FRP", "MTK MetaMode FRP", "", tier = PolicyTier.POLICY)
        val REMOVE_MI_CLOUD = DeepEyeOperation("op_mi_cloud", "Remove Mi Cloud", "Remove Mi Cloud", "", tier = PolicyTier.POLICY)
        val EFRP_MDM_HOOK = DeepEyeOperation("op_efrp_enterprise", "EFRP / MDM Hook", "EFRP / MDM Hook", "", tier = PolicyTier.POLICY)
        val REMOVE_SCREEN_LOCK = DeepEyeOperation("op_screen_lock", "Remove Screen Lock", "Remove Screen Lock", "", tier = PolicyTier.POLICY)
        val LOCK_STATE_ANALYSIS = DeepEyeOperation("op_lock_state", "Lock Analysis", "Lock Analysis", "", tier = PolicyTier.SAFE)
        val UNLOCK_BOOTLOADER = DeepEyeOperation("op_bl_unlock", "Unlock Bootloader", "Unlock Bootloader", "", tier = PolicyTier.POLICY, dangerous = true)
        val MDM_REMOVE = DeepEyeOperation("op_mdm_check", "MDM Remove", "MDM Remove", "", tier = PolicyTier.POLICY)
        val IMEI_CHECK = DeepEyeOperation("op_imei_check", "IMEI Check", "IMEI Check", "", tier = PolicyTier.SAFE)
        val IMEI_RESTORE = DeepEyeOperation("op_imei_restore", "IMEI Restore", "IMEI Restore", "", tier = PolicyTier.POLICY)
        val MODEM_REPAIR = DeepEyeOperation("op_modem_diag", "Modem Repair", "Modem Repair", "", tier = PolicyTier.POLICY)
        val NETWORK_UNLOCK = DeepEyeOperation("op_net_unlock", "Network Unlock", "Network Unlock", "", tier = PolicyTier.POLICY)
        val DEEP_DEVICE_INFO = DeepEyeOperation("op_device_info", "Deep Device Info", "Deep Device Info", "", tier = PolicyTier.SAFE)
        val ADB_ENABLE = DeepEyeOperation("op_diag_enable", "Enable ADB", "Enable ADB", "", tier = PolicyTier.RESTRICTED)
        val ONE_CLICK_ROOT = DeepEyeOperation("op_root_flow", "One-Click Root", "One-Click Root", "", tier = PolicyTier.RESTRICTED, dangerous = true)
        val APP_MANAGER = DeepEyeOperation("op_app_mgr", "App Manager", "App Manager", "", tier = PolicyTier.SAFE)

        fun values(): Array<DeepEyeOperation> = arrayOf(
            WRITE_FIRMWARE, READ_FIRMWARE, BACKUP_EFS, RESTORE_EFS, PARTITION_MANAGER,
            FACTORY_RESET, DEMO_UNLOCK, SAFE_WIPE, ERASE_FRP, MTK_METAMODE_FRP,
            REMOVE_MI_CLOUD, EFRP_MDM_HOOK, REMOVE_SCREEN_LOCK, LOCK_STATE_ANALYSIS,
            UNLOCK_BOOTLOADER, MDM_REMOVE, IMEI_CHECK, IMEI_RESTORE, MODEM_REPAIR,
            NETWORK_UNLOCK, DEEP_DEVICE_INFO, ADB_ENABLE, ONE_CLICK_ROOT, APP_MANAGER
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
