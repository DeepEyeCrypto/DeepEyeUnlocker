package com.deepeye.otg.usb

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log

/**
 * Safety wrapper for USB permissions to avoid SecurityExceptions and notification issues.
 */
object UsbPermissionGuard {
    private const val TAG = "DeepEye-PermGuard"

    fun requestPermission(
        context: Context,
        usbManager: UsbManager,
        device: UsbDevice,
        actionPermission: String
    ) {
        if (usbManager.hasPermission(device)) return

        val intent = Intent(actionPermission).apply {
            setPackage(context.packageName)
        }

        // FLAG_MUTABLE required for USB permission callbacks on Android 12+
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Samsung requirement: unique requestCode for PermissionPendingIntent
        val requestCode = OemCompatibilityLayer.permissionRequestCode(device)

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, flags
        )
        
        Log.i(TAG, "Requesting USB permission: ${device.productName} (requestCode=$requestCode)")
        usbManager.requestPermission(device, pendingIntent)
    }

    // Safe open with OEM-specific retries
    suspend fun safeOpenDevice(
        usbManager: UsbManager,
        device: UsbDevice
    ): android.hardware.usb.UsbDeviceConnection? {
        if (!usbManager.hasPermission(device)) return null
        
        return OemCompatibilityLayer.openDeviceWithRetry(usbManager, device)
    }
}
