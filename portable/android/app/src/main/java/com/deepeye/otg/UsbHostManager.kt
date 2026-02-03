package com.deepeye.otg

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log

class UsbHostManager(private val context: Context) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val ACTION_USB_PERMISSION = "com.deepeye.otg.USB_PERMISSION"

    fun findAndConnect(vid: Int, pid: Int, onConnected: (Int) -> Unit) {
        val deviceList = usbManager.deviceList
        val targetDevice = deviceList.values.find { it.vendorId == vid && it.productId == pid }

        if (targetDevice != null) {
            if (usbManager.hasPermission(targetDevice)) {
                openAndPassFd(targetDevice, onConnected)
            } else {
                val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
                usbManager.requestPermission(targetDevice, permissionIntent)
            }
        } else {
            Log.e("DeepEye-OTG", "Target device not found on USB bus.")
        }
    }

    private fun openAndPassFd(device: UsbDevice, onConnected: (Int) -> Unit) {
        val connection = usbManager.openDevice(device)
        if (connection != null) {
            val fd = connection.fileDescriptor
            Log.i("DeepEye-OTG", "Device opened. FD: $fd. Transferring control to Native Core.")
            
            // Safety Check: Verify this isn't the host device itself (simplified)
            if (device.vendorId == 0x0E8D && device.productId == 0x2000) { // Example MTK Preloader
                onConnected(fd)
            } else {
                Log.w("DeepEye-OTG", "Host Safety Filter: Blocked connection to unknown VID/PID.")
            }
        }
    }
}
