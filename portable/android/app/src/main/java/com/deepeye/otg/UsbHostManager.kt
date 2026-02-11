package com.deepeye.otg

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
    private val permissionManager = UsbPermissionManager(context, usbManager)
    
    private var currentDevice: UsbDevice? = null

    interface HotplugListener {
        fun onDeviceAttached(device: UsbDevice)
        fun onDeviceReady(fd: Int, vid: Int, pid: Int, protocol: DetectedProtocol)
        fun onDeviceError(message: String)
        fun onStatusUpdate(message: String)
        fun onPermissionStateChanged(state: UsbPermissionManager.PermissionState, message: String)
    }

    private val hotplugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
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
        // Register hotplug listener
        val hotplugFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(hotplugReceiver, hotplugFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(hotplugReceiver, hotplugFilter)
        }
        
        // Register permission manager
        permissionManager.register(object : UsbPermissionManager.PermissionListener {
            override fun onPermissionGranted(device: UsbDevice) {
                Log.i("DeepEye-OTG", "[PERM-GRANTED] Opening device: ${device.vendorId}:${device.productId}")
                listener?.onStatusUpdate("[PERM] Permission GRANTED - Opening device...")
                openAndPassFd(device)
            }
            
            override fun onPermissionDenied(device: UsbDevice) {
                Log.w("DeepEye-OTG", "[PERM-DENIED] User denied permission: ${device.vendorId}:${device.productId}")
                listener?.onDeviceError("USB Permission DENIED by user. Please re-plug and try again.")
            }
            
            override fun onPermissionStateChanged(state: UsbPermissionManager.PermissionState, message: String) {
                listener?.onPermissionStateChanged(state, message)
            }
        })
        
        // Scan for already connected devices
        scanExistingDevices()
    }

    private fun scanExistingDevices() {
        Log.i("DeepEye-OTG", "Scanning existing USB devices...")
        usbManager.deviceList.values.forEach { device ->
            Log.d("DeepEye-OTG", "Found: ${device.vendorId}:${device.productId}")
            listener?.onDeviceAttached(device)
            handleDevice(device)
        }
    }

    private fun handleDevice(device: UsbDevice) {
        currentDevice = device
        
        if (usbManager.hasPermission(device)) {
            Log.d("DeepEye-OTG", "Permission already granted for ${device.vendorId}:${device.productId}")
            listener?.onStatusUpdate("USB Permission OK - Opening device...")
            openAndPassFd(device)
        } else {
            Log.d("DeepEye-OTG", "Requesting permissions for ${device.vendorId}:${device.productId}")
            listener?.onStatusUpdate("Requesting USB permission...")
            permissionManager.requestPermission(device)
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
                val fd = connection.fileDescriptor
                Log.i("DeepEye-OTG", "Direct Link Established. FD=$fd")
                listener?.onStatusUpdate("USB Link Secured (FD=$fd)")
                
                // Probe Protocol
                val iface = device.getInterface(0)
                val probe = ProtocolProbe(connection, iface)
                val protocol = probe.detect()
                
                connection.claimInterface(iface, true)
                
                Log.i("DeepEye-OTG", "Protocol Detected: $protocol")
                if (protocol == DetectedProtocol.UNKNOWN) {
                    listener?.onStatusUpdate("Warning: Unknown Protocol. Device might be in MTP/Charge-Only mode.")
                }
                
                if (!wakeLock.isHeld) wakeLock.acquire(10 * 60 * 1000L)
                listener?.onDeviceReady(fd, device.vendorId, device.productId, protocol)
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
            permissionManager.unregister()
            context.unregisterReceiver(hotplugReceiver)
        } catch (e: Exception) {
            Log.w("DeepEye-OTG", "Cleanup error: ${e.message}")
        }
    }
}
