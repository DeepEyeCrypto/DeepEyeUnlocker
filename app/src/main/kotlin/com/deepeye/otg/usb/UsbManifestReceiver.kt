package com.deepeye.otg.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.deepeye.otg.DeepEyeApplication

/**
 * Process entry-point for USB broadcasts.
 *
 * Declared in manifest to survive Activity lifecycle and background restrictions.
 * Work is delegated immediately to the process-scoped [UsbLifecycleManager].
 */
class UsbManifestReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? DeepEyeApplication ?: return
        val manager = app.usbLifecycleManager
        val device = extractDevice(intent) ?: return

        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                manager.onDeviceAttached(device)
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                manager.onDeviceDetached(device)
            }

            UsbPermissionGuard.ACTION_USB_PERMISSION -> {
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                manager.onPermissionResult(device, granted)
            }
        }
    }

    private fun extractDevice(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }
}
