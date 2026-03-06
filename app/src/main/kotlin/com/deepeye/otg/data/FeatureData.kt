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
    val brands = listOf("Xiaomi", "Samsung", "Oppo", "Vivo", "Realme", "OnePlus")

    val groups = listOf(
        FeatureGroup(
            "GROUP A: UNLOCK OPERATIONS",
            listOf(
                FeatureItem("unlock_bl", "🔓", "Unlock Bootloader", 2),
                FeatureItem("demo_unlock", "🔓", "Demo Unlock", 1),
                FeatureItem("factory_reset", "🧹", "Factory Reset", 1),
                FeatureItem("erase_frp", "🔑", "Erase FRP", 2)
            )
        ),
        FeatureGroup(
            "GROUP B: SECURITY REPAIR",
            listOf(
                FeatureItem("backup_efs", "🛡️", "Backup EFS", 1),
                FeatureItem("restore_efs", "🔄", "Restore EFS", 1),
                FeatureItem("imei_check", "📡", "IMEI Check", 1),
                FeatureItem("modem_repair", "📶", "Modem Repair", 3)
            )
        ),
        FeatureGroup(
            "GROUP C: SYSTEM UTILS",
            listOf(
                FeatureItem("write_firmware", "⚡", "Write Firmware", 2),
                FeatureItem("read_firmware", "📚", "Read Firmware", 2),
                FeatureItem("partition_manager", "🗄️", "Partition Manager", 3),
                FeatureItem("device_info", "ℹ️", "Deep Device Info", 1)
            )
        )
    )
}
