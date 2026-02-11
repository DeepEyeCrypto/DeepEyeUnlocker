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
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
    private val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "DeepEye:OTG")
    private val ACTION_USB_PERMISSION = "com.deepeye.otg.USB_PERMISSION"

    interface HotplugListener {
        fun onDeviceAttached(device: UsbDevice)
        fun onDeviceReady(fd: Int, vid: Int, pid: Int)
        fun onDeviceError(message: String)
        fun onStatusUpdate(message: String)
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
                    Log.i("DeepEye-OTG", "Hotplug: Device Attached (${vendorId}:${productId})")
                    listener?.onDeviceAttached(this)
                    handleDevice(this)
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
        
        // Scan for already connected devices
        scanExistingDevices()
    }

    private fun scanExistingDevices() {
        Log.i("DeepEye-OTG", "Scanning existing USB devices...")
        usbManager.deviceList.values.forEach { device ->
            Log.d("DeepEye-OTG", "Found: ${device.vendorId}:${device.productId}")
            // We don't automatically connect to EVERYTHING, 
            // but we alert the UI that something is there.
            listener?.onDeviceAttached(device)
            handleDevice(device)
        }
    }

    private fun handleDevice(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            Log.d("DeepEye-OTG", "Permission already granted for ${device.vendorId}:${device.productId}")
            listener?.onStatusUpdate("USB Permission OK - Opening device...")
            openAndPassFd(device)
        } else {
            Log.d("DeepEye-OTG", "Requesting permissions for ${device.vendorId}:${device.productId}")
            listener?.onStatusUpdate("Requesting USB permission...")
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    fun findAndConnect(vid: Int, pid: Int) {
        val targetDevice = usbManager.deviceList.values.find { it.vendorId == vid && it.productId == pid }
        targetDevice?.apply { handleDevice(this) }
    }

    private fun openAndPassFd(device: UsbDevice) {
        try {
            listener?.onStatusUpdate("Opening USB connection...")
            val connection = usbManager.openDevice(device)
            if (connection != null) {
                Log.i("DeepEye-OTG", "Direct Link Established. Handing FD=${connection.fileDescriptor}")
                listener?.onStatusUpdate("USB Link Secured (FD=${connection.fileDescriptor})")
                if (!wakeLock.isHeld) wakeLock.acquire(10 * 60 * 1000L)
                listener?.onDeviceReady(connection.fileDescriptor, device.vendorId, device.productId)
            } else {
                Log.e("DeepEye-OTG", "openDevice() returned null for ${device.vendorId}:${device.productId}")
                listener?.onDeviceError("USB Connection Failed (openDevice returned null). Try re-plugging the cable.")
            }
        } catch (e: SecurityException) {
            Log.e("DeepEye-OTG", "SecurityException: ${e.message}")
            listener?.onDeviceError("USB Permission Denied by System")
        } catch (e: Exception) {
            Log.e("DeepEye-OTG", "Exception in openAndPassFd: ${e.message}")
            listener?.onDeviceError("USB Error: ${e.message}")
        }
    }

    fun unregister() {
        try {
            if (wakeLock.isHeld) wakeLock.release()
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.w("DeepEye-OTG", "Receiver already unregistered: ${e.message}")
        }
    }
}
