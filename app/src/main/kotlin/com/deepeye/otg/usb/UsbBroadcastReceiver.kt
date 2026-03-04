package com.deepeye.otg.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log

/**
 * Standalone BroadcastReceiver that delegates all USB events to [UsbSessionManager].
 *
 * Register from Activity/Service with the three actions:
 *   ACTION_USB_DEVICE_ATTACHED, ACTION_USB_DEVICE_DETACHED, ACTION_USB_PERMISSION
 */
class UsbBroadcastReceiver(
    private val manager: UsbSessionManager
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "DeepEye-UsbRx"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val device = extractDevice(intent)

        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                device?.let {
                    Log.i(TAG, "[USB] Broadcast: ATTACHED ${it.deviceName}")
                    manager.onDeviceAttached(it)
                }
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                device?.let {
                    Log.i(TAG, "[USB] Broadcast: DETACHED ${it.deviceName}")
                    manager.onDeviceDetached(it)
                }
            }

            UsbSessionManager.ACTION_USB_PERMISSION -> {
                val granted = intent.getBooleanExtra(
                    UsbManager.EXTRA_PERMISSION_GRANTED, false
                )
                device?.let {
                    Log.i(TAG, "[PERM] Broadcast: granted=$granted ${it.deviceName}")
                    manager.onPermissionResult(it, granted)
                }
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
