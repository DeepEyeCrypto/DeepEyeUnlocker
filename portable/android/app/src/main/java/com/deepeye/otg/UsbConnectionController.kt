package com.deepeye.otg

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Single-source USB connection controller that owns permission flow, attach/detach handling,
 * re-enumeration detection, protocol detection, error throttling, and state emission.
 */
class UsbConnectionController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val listener: Listener? = null
) {
    interface Listener {
        fun onReady(session: UsbSessionState, connection: UsbDeviceConnection?) {}
        fun onError(session: UsbSessionState) {}
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.deepeye.USB_PERMISSION"
        private const val TAG = "DeepEye-UsbController"
        private const val ERROR_WINDOW_MS = 5_000L
        private const val ERROR_THRESHOLD = 3
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val session = MutableStateFlow(UsbSessionState())
    val state: StateFlow<UsbSessionState> = session

    private var connection: UsbDeviceConnection? = null
    private var currentDevice: UsbDevice? = null
    private var pendingPermissionKey: String? = null
    private var lastPermissionRequestMs: Long = 0L
    private var lastErrorLog: MutableMap<String, MutableList<Long>> = mutableMapOf()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                ACTION_USB_PERMISSION -> handlePermissionBroadcast(intent)
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> intent.getUsbDevice()?.let { onDeviceAttached(it) }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> intent.getUsbDevice()?.let { onDeviceDetached(it) }
            }
        }
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        // Boot strap: scan existing devices
        usbManager.deviceList.values.forEach { device ->
            onDeviceAttached(device)
        }
    }

    fun unregister() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        closeConnection()
    }

    fun retry() {
        val device = currentDevice ?: return
        closeConnection()
        updateState { UsbSessionState(state = ConnState.DISCONNECTED) }
        onDeviceAttached(device)
    }

    private fun onDeviceAttached(device: UsbDevice) {
        val vid = device.vendorId
        val pid = device.productId
        val deviceId = device.deviceId
        val deviceKey = deviceKeyOf(device)

        val prev = session.value
        if (prev.vid == vid && prev.pid == pid && prev.deviceId != null && prev.deviceId != deviceId) {
            logInfo("[INFO] Re-enumeration detected oldId=${prev.deviceId} newId=$deviceId")
            closeConnection()
            updateState { UsbSessionState() }
        }

        currentDevice = device
        updateState {
            copy(
                vid = vid,
                pid = pid,
                deviceId = deviceId,
                deviceKey = deviceKey,
                protocol = ProtocolClass.UNKNOWN,
                lastError = null,
                state = ConnState.DEVICE_FOUND
            )
        }
        logInfo("[INFO] USB device found vid=$vid pid=$pid deviceId=$deviceId")

        if (usbManager.hasPermission(device)) {
            updateState { copy(hasPermission = true, state = ConnState.CONNECTED_PROTOCOL_DETECT) }
            logInfo("[INFO] Permission already granted; proceeding to open")
            openDevice(device)
        } else {
            updateState { copy(hasPermission = false, state = ConnState.PERMISSION_PENDING) }
            requestPermission(device)
        }
    }

    private fun onDeviceDetached(device: UsbDevice) {
        val prev = session.value
        if (prev.deviceId == device.deviceId) {
            logInfo("[INFO] Device detached vid=${device.vendorId} pid=${device.productId} deviceId=${device.deviceId}")
            closeConnection()
            updateState { UsbSessionState() }
        }
        if (pendingPermissionKey == deviceKeyOf(device)) {
            pendingPermissionKey = null
        }
    }

    private fun requestPermission(device: UsbDevice) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val intent = Intent(ACTION_USB_PERMISSION)
        val permissionIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
        logInfo("[INFO] Requesting permission deviceKey=${session.value.deviceKey}")
        val now = System.currentTimeMillis()
        val deviceKey = deviceKeyOf(device)
        val tooSoon = now - lastPermissionRequestMs < 1_000
        if (pendingPermissionKey == deviceKey && tooSoon) {
            logInfo("[INFO] Skip duplicate permission request for $deviceKey (cooldown)")
            return
        }
        pendingPermissionKey = deviceKey
        lastPermissionRequestMs = now
        try {
            usbManager.requestPermission(device, permissionIntent)
        } catch (e: Exception) {
            emitError("Failed to request permission: ${e.message}")
            updateState { copy(state = ConnState.PERMISSION_DENIED, lastError = "USB permission denied") }
            pendingPermissionKey = null
        }
    }

    private fun handlePermissionBroadcast(intent: Intent) {
        val device = intent.getUsbDevice() ?: return
        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
        logInfo("[INFO] Permission result granted=$granted deviceKey=${session.value.deviceKey}")

        val currentKey = session.value.deviceKey
        val incomingKey = deviceKeyOf(device)
        val sameVidPid = session.value.vid == device.vendorId && session.value.pid == device.productId

        // Accept grant if device matches by key or vid/pid (to handle re-enumeration changing deviceId)
        if (currentKey != null && incomingKey != currentKey && !sameVidPid) {
            logInfo("[INFO] Permission broadcast for unrelated device; ignoring")
            return
        }

        if (sameVidPid && session.value.deviceId != device.deviceId) {
            logInfo("[INFO] Permission for re-enumerated deviceId=${device.deviceId}; updating state")
            currentDevice = device
            updateState {
                copy(
                    deviceId = device.deviceId,
                    deviceKey = incomingKey
                )
            }
        }

        if (!isSameDevice(device) && !sameVidPid) return

        if (granted) {
            updateState { copy(hasPermission = true, state = ConnState.CONNECTED_PROTOCOL_DETECT, lastError = null) }
            openDevice(device)
        } else {
            updateState { copy(hasPermission = false, state = ConnState.PERMISSION_DENIED, lastError = "USB permission denied", lastErrorAtMs = System.currentTimeMillis()) }
        }
        pendingPermissionKey = null
    }

    private fun openDevice(device: UsbDevice) {
        if (!usbManager.hasPermission(device)) {
            updateState { copy(hasPermission = false, state = ConnState.PERMISSION_DENIED, lastError = "Permission lost before open", lastErrorAtMs = System.currentTimeMillis()) }
            return
        }
        closeConnection()
        logInfo("[INFO] Opening device connection...")
        val conn = usbManager.openDevice(device)
        if (conn == null) {
            emitError("openDevice() failed")
            updateState { copy(state = ConnState.ERROR, lastError = "openDevice() failed", lastErrorAtMs = System.currentTimeMillis()) }
            return
        }
        connection = conn
        val fd = conn.fileDescriptor
        updateState { copy(connectionFd = fd) }
        detectProtocol(device, conn)
    }

    private fun detectProtocol(device: UsbDevice, conn: UsbDeviceConnection) {
        val probe = ProtocolProbe(conn, device)
        val result = probe.detect()
        val protocolClass = mapProtocol(result.protocol)

        when (protocolClass) {
            ProtocolClass.MTP_ONLY -> {
                updateState { copy(protocol = protocolClass, state = ConnState.CONNECTED_MTP_ONLY, lastError = null) }
                logInfo("[INFO] Protocol detected protocol=MTP_ONLY")
            }
            ProtocolClass.UNKNOWN -> {
                emitError("Unknown protocol (not supported / wrong USB mode)")
                updateState { copy(protocol = protocolClass, state = ConnState.ERROR, lastError = "Unknown protocol (not supported / wrong USB mode)", lastErrorAtMs = System.currentTimeMillis()) }
            }
            else -> {
                updateState { copy(protocol = protocolClass, state = ConnState.CONNECTED_READY, lastError = null) }
                logInfo("[INFO] Protocol detected protocol=$protocolClass")
                listener?.onReady(session.value, conn)
            }
        }
    }

    private fun mapProtocol(proto: DetectedProtocol): ProtocolClass = when (proto) {
        DetectedProtocol.QUALCOMM_EDL, DetectedProtocol.FASTBOOT, DetectedProtocol.ADB -> ProtocolClass.QC
        DetectedProtocol.MTK_BROM, DetectedProtocol.MTK_PRELOADER -> ProtocolClass.MTK
        DetectedProtocol.SAMSUNG_ODIN -> ProtocolClass.SAMSUNG
        DetectedProtocol.MTP_ONLY -> ProtocolClass.MTP_ONLY
        else -> ProtocolClass.UNKNOWN
    }

    private fun isSameDevice(device: UsbDevice): Boolean {
        val s = session.value
        return s.vid == device.vendorId && s.pid == device.productId && s.deviceId == device.deviceId
    }

    private fun closeConnection() {
        try { connection?.close() } catch (_: Exception) {}
        connection = null
        currentDevice = null
    }

    private fun emitError(message: String) {
        val now = System.currentTimeMillis()
        val list = lastErrorLog.getOrPut(message) { mutableListOf() }
        list.add(now)
        lastErrorLog[message] = list.filter { now - it < ERROR_WINDOW_MS }.toMutableList()
        val count = lastErrorLog[message]!!.size
        if (count > ERROR_THRESHOLD) {
            logInfo("[UX] Error throttled")
            // Ensure state remains ERROR without spamming logs
            if (session.value.state != ConnState.ERROR) {
                updateState { copy(state = ConnState.ERROR, lastError = message, lastErrorAtMs = now) }
            }
            return
        }
        logInfo("[INFO] Transition old=${session.value.state} new=${ConnState.ERROR}")
        updateState { copy(state = ConnState.ERROR, lastError = message, lastErrorAtMs = now) }
        listener?.onError(session.value)
    }

    private fun logInfo(msg: String) {
        Log.i(TAG, msg)
    }

    private fun Intent.getUsbDevice(): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }

    private fun updateState(block: UsbSessionState.() -> UsbSessionState) {
        val old = session.value
        val newState = block(old)
        if (old.state != newState.state) {
            logInfo("[INFO] Transition old=${old.state} new=${newState.state}")
        }
        scope.launch(Dispatchers.Main) {
            session.value = newState
        }
    }

    private fun deviceKeyOf(device: UsbDevice): String = "${device.vendorId}:${device.productId}:${device.deviceId}"
}
