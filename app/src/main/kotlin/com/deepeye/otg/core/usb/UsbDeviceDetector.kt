package com.deepeye.otg.core.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.deepeye.otg.usb.UsbPermissionGuard
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class UsbDeviceInfo(
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val manufacturer: String,
    val product: String,
    val serial: String,
    val deviceClass: Int,
    val isQualcomm: Boolean,
    val isMtk: Boolean,
    val isSamsung: Boolean,
    val isApple: Boolean,
    val isEdlMode: Boolean,
    val hasPermission: Boolean
) {
    val vidPid: String get() = "%04X:%04X".format(vendorId, productId)

    val chipFamily: String get() = when {
        isEdlMode -> "Qualcomm EDL 9008"
        isQualcomm -> "Qualcomm"
        isMtk -> "MediaTek"
        isSamsung -> "Samsung"
        isApple -> "Apple"
        else -> "Unknown"
    }
}

@Singleton
class UsbDeviceDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    fun getConnectedDevices(): List<UsbDeviceInfo> {
        return usbManager.deviceList.values.map { device ->
            UsbDeviceInfo(
                name = device.deviceName,
                vendorId = device.vendorId,
                productId = device.productId,
                manufacturer = device.manufacturerName ?: "Unknown",
                product = device.productName ?: "Unknown",
                serial = device.serialNumber ?: "",
                deviceClass = device.deviceClass,
                isQualcomm = device.vendorId == 0x05C6,
                isMtk = device.vendorId == 0x0E8D,
                isSamsung = device.vendorId == 0x04E8,
                isApple = device.vendorId == 0x05AC,
                isEdlMode = device.vendorId == 0x05C6 && device.productId == 0x9008,
                hasPermission = usbManager.hasPermission(device)
            )
        }
    }

    fun observeDevices(): Flow<List<UsbDeviceInfo>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                trySend(getConnectedDevices())
            }
        }
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbPermissionGuard.ACTION_USB_PERMISSION) // Listen for permission changes too
        }
        context.registerReceiver(receiver, filter, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_NOT_EXPORTED
        } else {
            0
        })
        trySend(getConnectedDevices())
        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    fun requestPermission(device: UsbDevice) {
        UsbPermissionGuard.requestPermission(
            context = context,
            usbManager = usbManager,
            device = device,
            actionPermission = UsbPermissionGuard.ACTION_USB_PERMISSION
        )
    }

    fun hasPermission(device: UsbDevice): Boolean {
        return usbManager.hasPermission(device)
    }

    fun findDeviceByPath(deviceName: String): UsbDevice? {
        return usbManager.deviceList[deviceName]
    }
}
