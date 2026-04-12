package com.deepeye.otg.data.model

import java.util.UUID

enum class XiaomiFlashMode {
    FASTBOOT,       // Normal fastboot flash
    EDL,            // 9008 Emergency Download Mode
    MIFLASH,        // MiFlash protocol
    TWRP_SIDELOAD   // TWRP ADB sideload
}

enum class XiaomiPartition(val label: String, val fastbootCmd: String) {
    BOOT("boot", "boot"),
    RECOVERY("recovery", "recovery"),
    FASTBOOT_IMG("fastbootd", "fastboot"),
    SYSTEM("system", "system"),
    VENDOR("vendor", "vendor"),
    DTBO("dtbo", "dtbo"),
    VBMETA("vbmeta", "vbmeta"),
    VBMETA_SYSTEM("vbmeta_system", "vbmeta_system"),
    SUPER("super", "super"),
    CUST("cust", "cust"),
    MODEM("modem", "modem"),
    PERSIST("persist", "persist")
}

enum class FlashStatus {
    PENDING, FLASHING, SUCCESS, FAILED, SKIPPED
}

data class XiaomiFlashTask(
    val id: String = UUID.randomUUID().toString(),
    val partition: XiaomiPartition,
    val imagePath: String,
    val imageSize: Long = 0L,
    val status: FlashStatus = FlashStatus.PENDING,
    val progress: Float = 0f,
    val logOutput: String = ""
)

data class XiaomiDeviceInfo(
    val codename: String = "",
    val model: String = "",
    val androidVersion: String = "",
    val miuiVersion: String = "",
    val bootloaderStatus: String = "",  // locked/unlocked
    val antiRollback: String = "",
    val serialNo: String = "",
    val flashMode: XiaomiFlashMode = XiaomiFlashMode.FASTBOOT
)
