package com.deepeye.otg.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UsbBroadcastReceiver(
    private val lifecycleManager: UsbLifecycleManager,
    private val scope: CoroutineScope
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val device: android.hardware.usb.UsbDevice? = intent.getParcelableExtra(android.hardware.usb.UsbManager.EXTRA_DEVICE)

        when (intent.action) {

            android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                device ?: return
                lifecycleManager.onDeviceAttached(device)
            }

            android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                device ?: return
                lifecycleManager.onDeviceDetached(device)
            }

            UsbSessionManager.ACTION_USB_PERMISSION -> {
                device ?: return
                val granted = intent.getBooleanExtra(
                    android.hardware.usb.UsbManager.EXTRA_PERMISSION_GRANTED, false
                )
                lifecycleManager.onPermissionResult(device, granted)
            }
        }
    }
}
