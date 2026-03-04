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
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Single-source USB connection controller.
 *
 * Owns: permission flow, attach/detach, re-enumeration detection (MTK mode-switch),
 * protocol detection, FD lifecycle, error throttling, and state emission.
 *
 * Re-enumeration fix (MTK hw_code:0x1209):
 *   When an MTK device switches preloader→BROM during protocol detection, Android
 *   creates a new UsbDevice (new deviceId). The old FD is invalid and the old
 *   permission is revoked. We detect this via [PhysicalDeviceKey] (VID:PID:serial)
 *   and enter [ConnState.REENUMERATION_WAIT] with a 2-second timeout. If the same
 *   physical device re-attaches within that window we auto-request permission on
 *   the new UsbDevice instead of falling through to DISCONNECTED.
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
        private const val TAG = "DeepEye-USB"
        private const val ERROR_WINDOW_MS = 5_000L
        private const val ERROR_THRESHOLD = 3
        /** Max time (ms) to wait for the same physical device to re-attach. */
        private const val REENUM_TIMEOUT_MS = 2_000L
    }

    // ── Core state ──────────────────────────────────────────────
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val _state = MutableStateFlow(UsbSessionState())
    val state: StateFlow<UsbSessionState> = _state

    private var connection: UsbDeviceConnection? = null
    private var currentDevice: UsbDevice? = null
    private var pendingPermissionKey: String? = null
    private var lastPermissionRequestMs: Long = 0L
    private var lastErrorLog: MutableMap<String, MutableList<Long>> = mutableMapOf()

    // ── Re-enumeration tracking ─────────────────────────────────
    /** Cached identity of the physical device (survives re-enumeration). */
    private var cachedPhysicalKey: PhysicalDeviceKey? = null
    /** True while we are inside CONNECTED_PROTOCOL_DETECT — the window where re-enum can happen. */
    private var wasInProtocolDetect: Boolean = false
    private val handler = Handler(Looper.getMainLooper())
    private val reEnumTimeoutRunnable = Runnable { onReEnumTimeout() }

    // ── BroadcastReceiver ───────────────────────────────────────
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

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC API
    // ══════════════════════════════════════════════════════════════

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
        // Bootstrap: scan devices already plugged in
        usbManager.deviceList.values.forEach { device -> onDeviceAttached(device) }
    }

    fun unregister() {
        handler.removeCallbacks(reEnumTimeoutRunnable)
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        closeConnection()
    }

    fun retry() {
        handler.removeCallbacks(reEnumTimeoutRunnable)
        val device = currentDevice
        closeConnection()
        updateState { UsbSessionState(state = ConnState.DISCONNECTED) }
        if (device != null) onDeviceAttached(device)
    }

    // ══════════════════════════════════════════════════════════════
    //  ATTACH / DETACH
    // ══════════════════════════════════════════════════════════════

    private fun onDeviceAttached(device: UsbDevice) {
        val vid = device.vendorId
        val pid = device.productId
        val deviceId = device.deviceId
        val deviceKey = deviceKeyOf(device)
        val prev = _state.value

        // ── Re-enumeration: same physical device came back during REENUMERATION_WAIT ──
        if (prev.state == ConnState.REENUMERATION_WAIT) {
            val incomingPhysical = buildPhysicalKey(device)
            if (incomingPhysical != null && incomingPhysical == cachedPhysicalKey) {
                handler.removeCallbacks(reEnumTimeoutRunnable)
                val reEnumN = prev.reEnumCount + 1
                logInfo("[USB] Re-enumeration confirmed (same physical device): " +
                        "oldId=${prev.deviceId} newId=$deviceId reEnumCount=$reEnumN")
                currentDevice = device
                updateState {
                    copy(
                        vid = vid, pid = pid, deviceId = deviceId, deviceKey = deviceKey,
                        hasPermission = false, connectionFd = null,
                        protocol = ProtocolClass.UNKNOWN, lastError = null,
                        state = ConnState.DEVICE_FOUND,
                        physicalDeviceKey = incomingPhysical,
                        reEnumCount = reEnumN
                    )
                }
                // Auto-request permission for the new UsbDevice object
                requestPermission(device)
                return
            } else {
                // Different physical device — treat as brand-new attach
                handler.removeCallbacks(reEnumTimeoutRunnable)
                logInfo("[USB] Attach during REENUMERATION_WAIT but different device; resetting")
                cachedPhysicalKey = null
            }
        }

        // ── Normal attach (or re-enum for non-MTK that doesn't need the wait) ──
        if (prev.vid == vid && prev.pid == pid && prev.deviceId != null && prev.deviceId != deviceId) {
            logInfo("[USB] Re-enumeration detected (fast path) oldId=${prev.deviceId} newId=$deviceId")
            closeConnection()
        }

        currentDevice = device
        val physical = buildPhysicalKey(device)
        cachedPhysicalKey = physical
        wasInProtocolDetect = false

        updateState {
            copy(
                vid = vid, pid = pid, deviceId = deviceId, deviceKey = deviceKey,
                protocol = ProtocolClass.UNKNOWN, lastError = null,
                state = ConnState.DEVICE_FOUND,
                physicalDeviceKey = physical
            )
        }
        logInfo("[USB] Device found VID=0x${"%04X".format(vid)} PID=0x${"%04X".format(pid)} deviceId=$deviceId sn=${physical?.serialNumber ?: "?"}")

        if (usbManager.hasPermission(device)) {
            updateState { copy(hasPermission = true, state = ConnState.CONNECTED_PROTOCOL_DETECT) }
            wasInProtocolDetect = true
            logInfo("[PERM] Permission already granted; opening")
            openDevice(device)
        } else {
            updateState { copy(hasPermission = false, state = ConnState.PERMISSION_PENDING) }
            requestPermission(device)
        }
    }

    /**
     * Detach handler. If we are in CONNECTED_PROTOCOL_DETECT and have a cached physical key,
     * enter REENUMERATION_WAIT instead of DISCONNECTED — the MTK preloader→BROM switch
     * causes a detach+attach pair within ~500 ms.
     */
    private fun onDeviceDetached(device: UsbDevice) {
        val prev = _state.value
        if (prev.deviceId != device.deviceId) {
            // Not our device
            return
        }

        logInfo("[USB] Device detached VID=0x${"%04X".format(device.vendorId)} PID=0x${"%04X".format(device.productId)} deviceId=${device.deviceId}")

        // FD lifecycle: close immediately on detach — never reuse
        closeConnectionButKeepPhysicalKey()

        val shouldWait = cachedPhysicalKey != null &&
                (wasInProtocolDetect || prev.state == ConnState.CONNECTED_PROTOCOL_DETECT ||
                 prev.state == ConnState.PERMISSION_PENDING)

        if (shouldWait) {
            logInfo("[USB] Entering REENUMERATION_WAIT (${REENUM_TIMEOUT_MS}ms) — expecting same device to re-attach")
            updateState { copy(state = ConnState.REENUMERATION_WAIT, connectionFd = null, hasPermission = false) }
            handler.removeCallbacks(reEnumTimeoutRunnable)
            handler.postDelayed(reEnumTimeoutRunnable, REENUM_TIMEOUT_MS)
        } else {
            logInfo("[USB] Detach → DISCONNECTED (no re-enumeration expected)")
            cachedPhysicalKey = null
            wasInProtocolDetect = false
            updateState { UsbSessionState(state = ConnState.DISCONNECTED) }
        }

        if (pendingPermissionKey == deviceKeyOf(device)) {
            pendingPermissionKey = null
        }
    }

    /** Timeout: the device did not re-attach within the window. */
    private fun onReEnumTimeout() {
        val prev = _state.value
        if (prev.state == ConnState.REENUMERATION_WAIT) {
            logInfo("[USB] Re-enumeration timeout — device did not re-attach within ${REENUM_TIMEOUT_MS}ms")
            cachedPhysicalKey = null
            wasInProtocolDetect = false
            updateState { UsbSessionState(state = ConnState.DISCONNECTED) }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PERMISSION
    // ══════════════════════════════════════════════════════════════

    /**
     * Request USB permission with correct PendingIntent flags.
     *
     * Android 12+ (API 31): FLAG_MUTABLE is required for the system to populate
     * EXTRA_DEVICE and EXTRA_PERMISSION_GRANTED on the result Intent.
     * FLAG_IMMUTABLE silently drops those extras → permission appears denied.
     * We also call setPackage() so no other app can intercept the broadcast.
     */
    private fun requestPermission(device: UsbDevice) {
        val now = System.currentTimeMillis()
        val key = deviceKeyOf(device)
        val tooSoon = now - lastPermissionRequestMs < 1_000
        if (pendingPermissionKey == key && tooSoon) {
            logInfo("[PERM] Skip duplicate request for $key (cooldown)")
            return
        }

        // FIX 5 — PendingIntent: FLAG_MUTABLE + setPackage
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val intent = Intent(ACTION_USB_PERMISSION).apply {
            setPackage(context.packageName)
        }
        val permissionPi = PendingIntent.getBroadcast(context, 0, intent, flags)

        pendingPermissionKey = key
        lastPermissionRequestMs = now
        logInfo("[PERM] Requesting permission for $key")

        try {
            usbManager.requestPermission(device, permissionPi)
        } catch (e: Exception) {
            logInfo("[PERM] requestPermission() threw: ${e.message}")
            emitError("Failed to request USB permission: ${e.message}")
            updateState { copy(state = ConnState.PERMISSION_DENIED, lastError = "Permission request failed") }
            pendingPermissionKey = null
        }
    }

    private fun handlePermissionBroadcast(intent: Intent) {
        val device = intent.getUsbDevice() ?: return
        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
        val incomingKey = deviceKeyOf(device)
        val sameVidPid = _state.value.vid == device.vendorId && _state.value.pid == device.productId

        logInfo("[PERM] Permission result: granted=$granted key=$incomingKey")

        val currentKey = _state.value.deviceKey
        if (currentKey != null && incomingKey != currentKey && !sameVidPid) {
            logInfo("[PERM] Broadcast for unrelated device; ignoring")
            return
        }

        // Handle re-enumerated deviceId within permission flow
        if (sameVidPid && _state.value.deviceId != device.deviceId) {
            logInfo("[PERM] Permission for re-enumerated deviceId=${device.deviceId}; updating")
            currentDevice = device
            updateState { copy(deviceId = device.deviceId, deviceKey = incomingKey) }
        }

        if (!isSameDevice(device) && !sameVidPid) return

        if (granted) {
            logInfo("[PERM] GRANTED — opening device")
            updateState { copy(hasPermission = true, state = ConnState.CONNECTED_PROTOCOL_DETECT, lastError = null) }
            wasInProtocolDetect = true
            openDevice(device)
        } else {
            logInfo("[PERM] DENIED by user")
            updateState {
                copy(
                    hasPermission = false,
                    state = ConnState.PERMISSION_DENIED,
                    lastError = "USB permission denied by user",
                    lastErrorAtMs = System.currentTimeMillis()
                )
            }
        }
        pendingPermissionKey = null
    }

    // ══════════════════════════════════════════════════════════════
    //  OPEN / DETECT / PROTOCOL
    // ══════════════════════════════════════════════════════════════

    private fun openDevice(device: UsbDevice) {
        if (!usbManager.hasPermission(device)) {
            logInfo("[USB] Permission lost before openDevice()")
            updateState {
                copy(
                    hasPermission = false,
                    state = ConnState.PERMISSION_DENIED,
                    lastError = "Permission lost before open",
                    lastErrorAtMs = System.currentTimeMillis()
                )
            }
            return
        }
        closeConnectionButKeepPhysicalKey()
        logInfo("[USB] Opening USB connection...")
        val conn = usbManager.openDevice(device)
        if (conn == null) {
            emitError("openDevice() returned null")
            updateState {
                copy(state = ConnState.ERROR, lastError = "openDevice() failed", lastErrorAtMs = System.currentTimeMillis())
            }
            return
        }
        connection = conn
        val fd = conn.fileDescriptor
        logInfo("[USB] USB link secured (FD=$fd)")
        updateState { copy(connectionFd = fd) }

        // Cache physical key from the live connection (serial may only be available here)
        val serial = try { conn.serial } catch (_: SecurityException) { null }
        if (serial != null && cachedPhysicalKey == null) {
            cachedPhysicalKey = PhysicalDeviceKey(device.vendorId, device.productId, serial)
            updateState { copy(physicalDeviceKey = cachedPhysicalKey) }
        }

        detectProtocol(device, conn)
    }

    private fun detectProtocol(device: UsbDevice, conn: UsbDeviceConnection) {
        logInfo("[PROTO] Inspecting interfaces/endpoints...")
        scope.launch(Dispatchers.IO) {
            try {
                val probe = ProtocolProbe(conn, device)
                val result = probe.detect()
                val protocolClass = mapProtocol(result.protocol)

                scope.launch(Dispatchers.Main) {
                    wasInProtocolDetect = false
                    when (protocolClass) {
                        ProtocolClass.MTP_ONLY -> {
                            updateState { copy(protocol = protocolClass, state = ConnState.CONNECTED_MTP_ONLY, lastError = null) }
                            logInfo("[PROTO] Detected: MTP_ONLY")
                        }
                        ProtocolClass.UNKNOWN -> {
                            emitError("Unknown protocol (unsupported / wrong USB mode)")
                            updateState {
                                copy(
                                    protocol = protocolClass, state = ConnState.ERROR,
                                    lastError = "Unknown protocol", lastErrorAtMs = System.currentTimeMillis()
                                )
                            }
                        }
                        else -> {
                            updateState { copy(protocol = protocolClass, state = ConnState.CONNECTED_READY, lastError = null) }
                            logInfo("[PROTO] Detected: $protocolClass")
                            listener?.onReady(_state.value, conn)
                        }
                    }
                }
            } catch (e: Exception) {
                // Protocol probe can throw if the device re-enumerates mid-read.
                // The detach handler will fire and manage the state transition.
                scope.launch(Dispatchers.Main) {
                    logInfo("[PROTO] Exception during probe (device may have re-enumerated): ${e.message}")
                    // Only emit error if we haven't already transitioned to REENUMERATION_WAIT
                    if (_state.value.state == ConnState.CONNECTED_PROTOCOL_DETECT) {
                        emitError("Protocol detection failed: ${e.message}")
                    }
                }
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

    // ══════════════════════════════════════════════════════════════
    //  PHYSICAL DEVICE KEY
    // ══════════════════════════════════════════════════════════════

    /**
     * Build a [PhysicalDeviceKey] from a [UsbDevice].
     * Serial may be null before Android 10 or without permission —
     * we fall back to an empty string, which means VID:PID matching only.
     */
    private fun buildPhysicalKey(device: UsbDevice): PhysicalDeviceKey? {
        val serial = try {
            device.serialNumber
        } catch (_: SecurityException) {
            null
        }
        return PhysicalDeviceKey(
            vid = device.vendorId,
            pid = device.productId,
            serialNumber = serial ?: ""
        )
    }

    // ══════════════════════════════════════════════════════════════
    //  CONNECTION LIFECYCLE
    // ══════════════════════════════════════════════════════════════

    private fun isSameDevice(device: UsbDevice): Boolean {
        val s = _state.value
        return s.vid == device.vendorId && s.pid == device.productId && s.deviceId == device.deviceId
    }

    /** Close FD + connection, clear currentDevice. Physical key is preserved for re-enum matching. */
    private fun closeConnectionButKeepPhysicalKey() {
        try { connection?.close() } catch (_: Exception) {}
        connection = null
        currentDevice = null
    }

    /** Full close — also wipes physical key. */
    private fun closeConnection() {
        closeConnectionButKeepPhysicalKey()
        cachedPhysicalKey = null
        wasInProtocolDetect = false
    }

    // ══════════════════════════════════════════════════════════════
    //  ERROR THROTTLING
    // ══════════════════════════════════════════════════════════════

    private fun emitError(message: String) {
        val now = System.currentTimeMillis()
        val list = lastErrorLog.getOrPut(message) { mutableListOf() }
        list.add(now)
        lastErrorLog[message] = list.filter { now - it < ERROR_WINDOW_MS }.toMutableList()
        val count = lastErrorLog[message]!!.size
        if (count > ERROR_THRESHOLD) {
            logInfo("[UX] Error throttled ($count in ${ERROR_WINDOW_MS}ms): $message")
            if (_state.value.state != ConnState.ERROR) {
                updateState { copy(state = ConnState.ERROR, lastError = message, lastErrorAtMs = now) }
            }
            return
        }
        updateState { copy(state = ConnState.ERROR, lastError = message, lastErrorAtMs = now) }
        listener?.onError(_state.value)
    }

    // ══════════════════════════════════════════════════════════════
    //  UTIL
    // ══════════════════════════════════════════════════════════════

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
        val old = _state.value
        val newState = block(old)
        if (old.state != newState.state) {
            logInfo("[STATE] ${old.state} → ${newState.state}")
        }
        scope.launch(Dispatchers.Main) {
            _state.value = newState
        }
    }

    private fun deviceKeyOf(device: UsbDevice): String =
        "${device.vendorId}:${device.productId}:${device.deviceId}"
}
