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
    private var currentDeviceId: Int? = null
    private var currentConnection: android.hardware.usb.UsbDeviceConnection? = null
    private var lastErrorTs: Long = 0L
    private var lastErrorMsg: String = ""
    private var lastErrorCount: Int = 0
    private val ERROR_THROTTLE_WINDOW_MS = 5000L
    private val ERROR_THROTTLE_MAX = 3

    interface HotplugListener {
        fun onDeviceAttached(device: UsbDevice)
        fun onDeviceReady(fd: Int, vid: Int, pid: Int, protocol: DetectedProtocol, ifaceDump: String)
        fun onDeviceError(message: String)
        fun onStatusUpdate(message: String)
        fun onPermissionStateChanged(state: UsbPermissionManager.PermissionState, message: String)
    }

    // State Transition (high-level events → next state) comment:
    // DISCONNECTED --(ATTACH)--> DEVICE_FOUND
    // DEVICE_FOUND --(hasPermission=false/requestPermission)--> PERMISSION_PENDING
    // PERMISSION_PENDING --(BROADCAST GRANTED)--> USB_OPEN
    // USB_OPEN --(probe)--> CONNECTED_PROTOCOL_DETECT
    // CONNECTED_PROTOCOL_DETECT --(known proto)--> NATIVE_INITIALIZING --> CONNECTED
    // CONNECTED_PROTOCOL_DETECT --(MTP only)--> CONNECTED_MTP_ONLY
    // ANY --(DETACH)--> DISCONNECTED
    // ANY --(permission denied)--> PERMISSION_DENIED
    // ANY --(error)--> ERROR

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
                    Log.i("DeepEye-OTG", "[USB] Device Attached (${vendorId}:${productId})")
                    listener?.onDeviceAttached(this)
                    handleDevice(this)
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
                val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }
                device?.apply {
                    Log.i("DeepEye-OTG", "[USB] Device Detached (${vendorId}:${productId})")
                    handleDetach(this)
                }
            }
        }
    }

    init {
        // Register hotplug listener
        val hotplugFilter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(hotplugReceiver, hotplugFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(hotplugReceiver, hotplugFilter)
        }
        
        // Register permission manager
        permissionManager.register(object : UsbPermissionManager.PermissionListener {
            override fun onPermissionGranted(device: UsbDevice) {
                Log.i("DeepEye-OTG", "[PERM] GRANTED for ${device.vendorId}:${device.productId}")
                listener?.onStatusUpdate("[PERM] Permission GRANTED - Opening device...")
                openAndPassFd(device)
            }
            
            override fun onPermissionDenied(device: UsbDevice) {
                Log.w("DeepEye-OTG", "[PERM] DENIED by user: ${device.vendorId}:${device.productId}")
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
        Log.i("DeepEye-OTG", "[USB] Scanning existing USB devices...")
        usbManager.deviceList.values.forEach { device ->
            Log.d("DeepEye-OTG", "[USB] Found: ${device.vendorId}:${device.productId}")
            listener?.onDeviceAttached(device)
            handleDevice(device)
        }
    }

    private fun handleDevice(device: UsbDevice) {
        // Detect re-enumeration (same vid/pid but new deviceId)
        if (currentDevice != null && currentDevice?.vendorId == device.vendorId && currentDevice?.productId == device.productId && currentDeviceId != device.deviceId) {
            Log.w("DeepEye-OTG", "[USB] Re-enumeration detected: oldId=$currentDeviceId newId=${device.deviceId}")
            listener?.onStatusUpdate("[USB] Device re-enumerated (likely mode switch). Re-requesting permission.")
            handleDetach(currentDevice!!)
        }

        currentDevice = device
        currentDeviceId = device.deviceId

        if (usbManager.hasPermission(device)) {
            Log.d("DeepEye-OTG", "[PERM] Already granted for ${device.vendorId}:${device.productId}")
            listener?.onStatusUpdate("USB Permission OK - Opening device...")
            openAndPassFd(device)
        } else {
            Log.d("DeepEye-OTG", "[PERM] Requesting permission for ${device.vendorId}:${device.productId}")
            listener?.onStatusUpdate("Requesting USB permission...")
            permissionManager.requestPermission(device)
        }
    }

    private fun handleDetach(device: UsbDevice) {
        if (currentDevice?.deviceId == device.deviceId) {
            // Close active connection before clearing state
            try {
                currentConnection?.close()
                Log.i("DeepEye-OTG", "[USB] Closed previous connection for ${device.vendorId}:${device.productId}")
            } catch (_: Exception) {}
            currentConnection = null
            currentDevice = null
            currentDeviceId = null
        }
        // Reset error throttle on detach so next attach starts fresh
        lastErrorMsg = ""
        lastErrorCount = 0
        try {
            if (wakeLock.isHeld) wakeLock.release()
        } catch (_: Exception) {}
        listener?.onDeviceError("Device detached")
    }

    fun findAndConnect(vid: Int, pid: Int) {
        val targetDevice = usbManager.deviceList.values.find { it.vendorId == vid && it.productId == pid }
        targetDevice?.apply { handleDevice(this) }
    }

    private fun openAndPassFd(device: UsbDevice) {
        // Guard: verify permission is still valid right before opening
        if (!usbManager.hasPermission(device)) {
            Log.w("DeepEye-OTG", "[USB] Permission lost before openDevice (likely re-enumeration). Re-requesting.")
            listener?.onStatusUpdate("[USB] System revoked permission (device re-enumerated). Re-requesting...")
            permissionManager.requestPermission(device)
            return
        }

        try {
            listener?.onStatusUpdate("Opening USB connection...")
            val connection = usbManager.openDevice(device)
            if (connection != null) {
                // Store active connection for cleanup on re-enum/detach
                currentConnection = connection
                val fd = connection.fileDescriptor
                Log.i("DeepEye-OTG", "[USB] openDevice OK. FD=$fd")
                listener?.onStatusUpdate("USB Link Secured (FD=$fd)")

                // ┌──────────────────────────────────────────────────────────────┐
                // │ USB Connection Sequence Diagram                             │
                // │                                                            │
                // │ ATTACH ─► DEVICE_FOUND ─► PERMISSION_PENDING               │
                // │   ─►(BROADCAST GRANTED)─► USB_OPEN                         │
                // │   ─► CONNECTED_PROTOCOL_DETECT (probe interfaces)           │
                // │   ─► CONNECTED_READY / CONNECTED_MTP_ONLY / ERROR          │
                // │                                                            │
                // │ Re-enumeration (VID/PID same, deviceId changed):           │
                // │   Close old connection ─► DISCONNECTED ─► DEVICE_FOUND     │
                // │   ─► re-request permission for new UsbDevice               │
                // │                                                            │
                // │ SecurityException after open:                              │
                // │   ─► re-request permission (not "Permission Denied" loop)  │
                // └──────────────────────────────────────────────────────────────┘

                val probe = ProtocolProbe(connection, device)
                listener?.onStatusUpdate("[PROTO] Inspecting interfaces/endpoints...")
                val protocolResult = probe.detect()

                // Claim the appropriate interface for non-MTP protocols
                if (protocolResult.protocol != DetectedProtocol.UNKNOWN && protocolResult.protocol != DetectedProtocol.MTP_ONLY) {
                    // Find the interface that ProtocolProbe identified as having bulk endpoints
                    val claimIface = protocolResult.claimInterfaceIndex?.let { idx ->
                        if (idx < device.interfaceCount) device.getInterface(idx) else null
                    } ?: if (device.interfaceCount > 0) device.getInterface(0) else null
                    claimIface?.let {
                        try {
                            connection.claimInterface(it, true)
                            Log.d("DeepEye-OTG", "[USB] Claimed interface ${it.id}")
                        } catch (e: Exception) {
                            Log.w("DeepEye-OTG", "[USB] claimInterface failed: ${e.message}")
                        }
                    }
                }

                if (!wakeLock.isHeld) wakeLock.acquire(10 * 60 * 1000L)
                listener?.onDeviceReady(fd, device.vendorId, device.productId, protocolResult.protocol, protocolResult.ifaceDump)
            } else {
                Log.e("DeepEye-OTG", "[USB] openDevice() returned null for ${device.vendorId}:${device.productId}")
                listener?.onDeviceError("USB Connection Failed (openDevice returned null). Try re-plugging the cable.")
            }
        } catch (e: SecurityException) {
            // KEY FIX: SecurityException means system revoked permission (device re-enumerated,
            // or another app claimed it). Do NOT report as "Permission Denied" loop.
            // Instead, attempt clean re-request.
            Log.e("DeepEye-OTG", "[USB] SecurityException in openDevice: ${e.message}")
            currentConnection = null
            listener?.onStatusUpdate("[USB] System revoked USB access (likely device re-enumeration). Please re-plug or change USB mode.")
            listener?.onDeviceError("System revoked USB permission. Re-plug device or switch USB mode.")
        } catch (e: Exception) {
            Log.e("DeepEye-OTG", "[USB] Exception in openAndPassFd: ${e.message}")
            emitThrottledError("USB Error: ${e.message}")
        }
    }

    private fun emitThrottledError(message: String) {
        val now = System.currentTimeMillis()
        if (message == lastErrorMsg && (now - lastErrorTs) < ERROR_THROTTLE_WINDOW_MS) {
            lastErrorCount++
            if (lastErrorCount > ERROR_THROTTLE_MAX) {
                Log.w("DeepEye-OTG", "[USB] Error throttled ($lastErrorCount repeats in window): $message")
                return
            }
        } else {
            // New error or outside window - reset counter
            lastErrorCount = 1
        }
        lastErrorMsg = message
        lastErrorTs = now
        listener?.onDeviceError(message)
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
