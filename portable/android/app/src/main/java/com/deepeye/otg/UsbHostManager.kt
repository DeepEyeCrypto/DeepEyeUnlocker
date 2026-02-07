package com.deepeye.otg

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log

class UsbHostManager(private val context: Context, private val listener: HotplugListener? = null) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val ACTION_USB_PERMISSION = "com.deepeye.otg.USB_PERMISSION"

    interface HotplugListener {
        fun onDeviceAttached(device: UsbDevice)
        fun onDeviceReady(fd: Int)
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply { openAndPassFd(this) }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
                val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }
                device?.apply { 
                    Log.i("DeepEye-OTG", "Proactive Hotplug: Device Attached. Requesting link...")
                    listener?.onDeviceAttached(this)
                    findAndConnect(vendorId, productId) // Auto-connect
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        }
        // Android 14+ requires RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
    }

    fun findAndConnect(vid: Int, pid: Int) {
        val targetDevice = usbManager.deviceList.values.find { it.vendorId == vid && it.productId == pid }

        if (targetDevice != null) {
            if (usbManager.hasPermission(targetDevice)) {
                openAndPassFd(targetDevice)
            } else {
                Log.d("DeepEye-OTG", "Proactive: Requesting permissions for $vid:$pid")
                val permissionIntent = PendingIntent.getBroadcast(
                    context, 
                    0, 
                    Intent(ACTION_USB_PERMISSION), 
                    PendingIntent.FLAG_IMMUTABLE
                )
                usbManager.requestPermission(targetDevice, permissionIntent)
            }
        }
    }

    private fun openAndPassFd(device: UsbDevice) {
        val connection = usbManager.openDevice(device)
        if (connection != null) {
            Log.i("DeepEye-OTG", "Direct Link Established. Handing FD to Core.")
            listener?.onDeviceReady(connection.fileDescriptor)
        }
    }

    fun unregister() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.w("DeepEye-OTG", "Receiver already unregistered: ${e.message}")
        }
    }
}
