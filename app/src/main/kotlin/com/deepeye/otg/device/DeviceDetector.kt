package com.deepeye.otg.device

import android.content.Context
import android.hardware.usb.*
import kotlinx.serialization.Serializable

@Serializable
enum class DeviceMode {
    BROM, PRELOADER, EDL, FASTBOOT, ADB, MTP, RECOVERY, UNKNOWN
}

@Serializable
data class DetectedDevice(
    val mode:         DeviceMode,
    val vid:          Int,
    val pid:          Int,
    val serial:       String?   = null,
    val manufacturer: String?   = null,
    val productName:  String?   = null,
    val chipset:      String?   = null,
    val deviceName:   String    = "",
    val detectedAt:   Long      = System.currentTimeMillis(),
)

object DeviceDetector {

    internal fun classifyDevice(vid: Int, pid: Int): DeviceMode = when {
        vid == 0x0E8D && pid == 0x0003 -> DeviceMode.BROM
        vid == 0x0E8D && pid == 0x2000 -> DeviceMode.PRELOADER
        vid == 0x0E8D && pid == 0x0006 -> DeviceMode.PRELOADER
        vid == 0x0E8D && pid == 0x0C01 -> DeviceMode.FASTBOOT
        vid == 0x05C6 && pid == 0x9008 -> DeviceMode.EDL
        vid == 0x05C6 && pid == 0x900E -> DeviceMode.EDL
        vid == 0x18D1 && pid == 0xD00D -> DeviceMode.FASTBOOT
        vid == 0x18D1 && pid == 0x4EE7 -> DeviceMode.RECOVERY
        vid == 0x18D1 && pid == 0x4EE2 -> DeviceMode.ADB
        vid == 0x18D1 && pid == 0x4EE1 -> DeviceMode.ADB
        vid == 0x0E8D && pid == 0x201D -> DeviceMode.MTP
        vid == 0x04E8                  -> DeviceMode.ADB   // Samsung
        vid == 0x2717                  -> DeviceMode.ADB   // Xiaomi
        vid == 0x12D1                  -> DeviceMode.ADB   // Huawei
        else                           -> DeviceMode.UNKNOWN
    }

    fun scanDevices(context: Context): List<DetectedDevice> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return usbManager.deviceList.values.mapNotNull { device ->
            val mode = classifyDevice(device.vendorId, device.productId)
            if (mode == DeviceMode.UNKNOWN) return@mapNotNull null
            DetectedDevice(
                mode         = mode,
                vid          = device.vendorId,
                pid          = device.productId,
                manufacturer = device.manufacturerName,
                productName  = device.productName,
                serial       = device.serialNumber,
                deviceName   = device.deviceName,
                detectedAt   = System.currentTimeMillis(),
            )
        }
    }

    fun findUsbDevice(context: Context, vid: Int, pid: Int): UsbDevice? {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return usbManager.deviceList.values.find {
            it.vendorId == vid && it.productId == pid
        }
    }

    fun fromUsbDevice(dev: UsbDevice): DetectedDevice? {
        val vid  = dev.vendorId
        val pid  = dev.productId
        val mode = classifyDevice(vid, pid)
        if (mode == DeviceMode.UNKNOWN) return null
        return DetectedDevice(
            mode         = mode,
            vid          = vid,
            pid          = pid,
            serial       = dev.serialNumber,
            manufacturer = dev.manufacturerName,
            productName  = dev.productName,
            deviceName   = dev.deviceName,
            detectedAt   = System.currentTimeMillis()
        )
    }
}
