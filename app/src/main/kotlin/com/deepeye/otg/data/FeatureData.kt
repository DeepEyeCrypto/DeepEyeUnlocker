package com.deepeye.otg.data

import com.deepeye.otg.usb.DeepEyeOperation

data class FeatureItem(
    val id: String,
    val icon: String,
    val label: String,
    val tier: Int
)

data class FeatureGroup(
    val title: String,
    val features: List<FeatureItem>
)

object FeatureData {
    val groups = listOf(
        FeatureGroup(
            "Flashing",
            listOf(
                FeatureItem("write_firmware", "⚡", DeepEyeOperation.WRITE_FIRMWARE.label, DeepEyeOperation.WRITE_FIRMWARE.tier),
                FeatureItem("read_firmware", "📚", DeepEyeOperation.READ_FIRMWARE.label, DeepEyeOperation.READ_FIRMWARE.tier),
                FeatureItem("backup_efs", "🛡️", DeepEyeOperation.BACKUP_EFS.label, DeepEyeOperation.BACKUP_EFS.tier),
                FeatureItem("restore_efs", "🔄",  DeepEyeOperation.RESTORE_EFS.label, DeepEyeOperation.RESTORE_EFS.tier),
                FeatureItem("partition_manager", "🗄️", DeepEyeOperation.PARTITION_MANAGER.label, DeepEyeOperation.PARTITION_MANAGER.tier)
            )
        ),
        FeatureGroup(
            "Reset",
            listOf(
                FeatureItem("factory_reset", "🧹", DeepEyeOperation.FACTORY_RESET.label, DeepEyeOperation.FACTORY_RESET.tier),
                FeatureItem("demo_unlock", "🔓", DeepEyeOperation.DEMO_UNLOCK.label, DeepEyeOperation.DEMO_UNLOCK.tier),
                FeatureItem("safe_wipe", "🗑️", DeepEyeOperation.SAFE_WIPE.label, DeepEyeOperation.SAFE_WIPE.tier)
            )
        ),
        FeatureGroup(
            "FRP",
            listOf(
                FeatureItem("erase_frp", "🔑", DeepEyeOperation.ERASE_FRP.label, DeepEyeOperation.ERASE_FRP.tier),
                FeatureItem("remove_mi_cloud", "☁️", DeepEyeOperation.REMOVE_MI_CLOUD.label, DeepEyeOperation.REMOVE_MI_CLOUD.tier),
                FeatureItem("efrp", "🏢", DeepEyeOperation.EFRP_MDM_HOOK.label, DeepEyeOperation.EFRP_MDM_HOOK.tier),
                FeatureItem("mtk_meta_frp", "🤖", DeepEyeOperation.MTK_METAMODE_FRP.label, DeepEyeOperation.MTK_METAMODE_FRP.tier)
            )
        ),
        FeatureGroup(
            "Locks",
            listOf(
                FeatureItem("remove_screen_lock", "📱", DeepEyeOperation.REMOVE_SCREEN_LOCK.label, DeepEyeOperation.REMOVE_SCREEN_LOCK.tier),
                FeatureItem("lock_analysis", "🔍", DeepEyeOperation.LOCK_STATE_ANALYSIS.label, DeepEyeOperation.LOCK_STATE_ANALYSIS.tier),
                FeatureItem("unlock_bl", "🔓", DeepEyeOperation.UNLOCK_BOOTLOADER.label, DeepEyeOperation.UNLOCK_BOOTLOADER.tier),
                FeatureItem("mdm_remove", "💸", DeepEyeOperation.MDM_REMOVE.label, DeepEyeOperation.MDM_REMOVE.tier)
            )
        ),
        FeatureGroup(
            "Network",
            listOf(
                FeatureItem("imei_check", "📡", DeepEyeOperation.IMEI_CHECK.label, DeepEyeOperation.IMEI_CHECK.tier),
                FeatureItem("imei_restore", "🔧", DeepEyeOperation.IMEI_RESTORE.label, DeepEyeOperation.IMEI_RESTORE.tier),
                FeatureItem("modem_repair", "📶", DeepEyeOperation.MODEM_REPAIR.label, DeepEyeOperation.MODEM_REPAIR.tier),
                FeatureItem("network_unlock", "🌍", DeepEyeOperation.NETWORK_UNLOCK.label, DeepEyeOperation.NETWORK_UNLOCK.tier)
            )
        ),
        FeatureGroup(
            "Advanced",
            listOf(
                FeatureItem("device_info", "ℹ️", DeepEyeOperation.DEEP_DEVICE_INFO.label, DeepEyeOperation.DEEP_DEVICE_INFO.tier),
                FeatureItem("adb_enable", "🔌", DeepEyeOperation.ADB_ENABLE.label, DeepEyeOperation.ADB_ENABLE.tier),
                FeatureItem("root", "🔥", DeepEyeOperation.ONE_CLICK_ROOT.label, DeepEyeOperation.ONE_CLICK_ROOT.tier),
                FeatureItem("app_manager", "📦", DeepEyeOperation.APP_MANAGER.label, DeepEyeOperation.APP_MANAGER.tier)
            )
        )
    )
}
