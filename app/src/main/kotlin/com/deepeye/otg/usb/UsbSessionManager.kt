package com.deepeye.otg.usb

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.deepeye.otg.ProtocolProbe
import com.deepeye.otg.DetectedProtocol
import com.deepeye.otg.engine.EngineDispatcher
import com.deepeye.otg.auth.LicenseManager
import com.deepeye.otg.policy.PolicyDeniedException
import com.deepeye.otg.policy.UserRole
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ═══════════════════════════════════════════════════════════════════
//  1. Operations enum — all 24 features with policy tier
// ═══════════════════════════════════════════════════════════════════

enum class DeepEyeOperation(val label: String, val tier: Int) {
    // Category A — Flashing & Firmware
    WRITE_FIRMWARE      ("Write Firmware",        1),
    READ_FIRMWARE       ("Read Firmware",         1),
    BACKUP_EFS          ("Backup EFS / NV",       2),
    RESTORE_EFS         ("Restore EFS / NV",      2),
    PARTITION_MANAGER   ("Partition Manager",     1),

    // Category B — Reset & Cleanup
    FACTORY_RESET       ("Factory Reset",         1),
    DEMO_UNLOCK         ("Demo to Retail",        1),
    SAFE_WIPE           ("Safe Wipe + Backup",    1),

    // Category C — FRP & Account
    ERASE_FRP           ("Erase FRP Lock",        3),
    REMOVE_MI_CLOUD     ("Remove Mi Cloud",       3),
    EFRP_MDM_HOOK       ("Enterprise EFRP Hook",  3),
    MTK_METAMODE_FRP    ("MTK MetaMode FRP",      3),

    // Category D — Locks & Security
    REMOVE_SCREEN_LOCK  ("Remove Screen Lock",    2),
    LOCK_STATE_ANALYSIS ("Lock State Analysis",   1),
    UNLOCK_BOOTLOADER   ("Unlock Bootloader",     1),
    MDM_REMOVE          ("MDM / Finance Unlock",  3),

    // Category E — IMEI & Network
    IMEI_CHECK          ("IMEI Integrity Check",  1),
    IMEI_RESTORE        ("IMEI Restore",          2),
    MODEM_REPAIR        ("5G Modem / CPID",       2),
    NETWORK_UNLOCK      ("Network Unlock",        3),

    // Category F — Advanced & Diagnostics
    DEEP_DEVICE_INFO    ("Deep Device Info",      1),
    ADB_ENABLE          ("ADB / Diag Enable",     1),
    ONE_CLICK_ROOT      ("One-Click Root",        1),
    APP_MANAGER         ("ADB App Manager",       1),
}

// ═══════════════════════════════════════════════════════════════════
//  2. Sealed-class state machine
// ═══════════════════════════════════════════════════════════════════

sealed class SessionState {
    /** No device, no queued operation. */
    object Idle : SessionState()

    /** User selected an operation — waiting for USB cable. */
    data class WaitingForDevice(
        val queuedOp: DeepEyeOperation
    ) : SessionState()

    /** USB attach seen; permission may or may not be granted yet. */
    data class DeviceFound(
        val device: UsbDevice,
        val queuedOp: DeepEyeOperation? = null
    ) : SessionState()

    /** Permission dialog shown, waiting for tap. */
    data class PermissionPending(
        val device: UsbDevice,
        val queuedOp: DeepEyeOperation? = null
    ) : SessionState()

    /** User tapped "Deny". */
    data class PermissionDenied(
        val device: UsbDevice,
        val queuedOp: DeepEyeOperation? = null
    ) : SessionState()

    /** MTK mode-switch: old device detached during probe, expecting re-attach. */
    data class ReenumerationWait(
        val physicalKey: PhysicalDeviceKey,
        val queuedOp: DeepEyeOperation? = null
    ) : SessionState()

    /** Probing USB interfaces to classify protocol. */
    data class ProtocolDetect(
        val device: UsbDevice,
        val queuedOp: DeepEyeOperation? = null
    ) : SessionState()

    /** Device open, protocol known. Ready to run (or auto-running). */
    data class ConnectedReady(
        val device: UsbDevice,
        val protocol: ProtocolFamily,
        val fd: Int,
        val queuedOp: DeepEyeOperation? = null
    ) : SessionState()

    /** Operation actively executing. */
    data class ExecutingOperation(
        val op: DeepEyeOperation,
        val protocol: ProtocolFamily,
        val progress: Int = 0,         // 0-100
        val statusMsg: String = ""
    ) : SessionState()

    /** MTP/charge-only — no service mode. */
    data class ConnectedMtpOnly(val device: UsbDevice) : SessionState()

    /** Operation finished. */
    data class OperationComplete(
        val op: DeepEyeOperation,
        val success: Boolean,
        val message: String
    ) : SessionState()

    /** Unrecoverable error. */
    data class Error(
        val message: String,
        val queuedOp: DeepEyeOperation? = null
    ) : SessionState()
}

// ═══════════════════════════════════════════════════════════════════
//  3. Physical device identity (survives re-enumeration)
// ═══════════════════════════════════════════════════════════════════

data class PhysicalDeviceKey(
    val vid: Int,
    val pid: Int,
    val serialNumber: String
)

enum class ProtocolFamily { MTK, QC, SAMSUNG, UNISOC, UNKNOWN, MTP_ONLY }

// ═══════════════════════════════════════════════════════════════════
//  4. UsbSessionManager — Queue & Wait + Auto-Execute
// ═══════════════════════════════════════════════════════════════════

class UsbSessionManager(private val context: Context) {

    companion object {
        private const val TAG = "DeepEye-Session"
        const val ACTION_USB_PERMISSION = "com.deepeye.otg.USB_PERMISSION"
        private const val REENUM_TIMEOUT_MS = 2_500L
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = _state

    private var connection: UsbDeviceConnection? = null
    private var cachedPhysicalKey: PhysicalDeviceKey? = null
    private var reenumTimeoutJob: Job? = null

    // Error throttle: tag → (count, firstSeen)
    private val errorThrottle = mutableMapOf<String, Pair<Int, Long>>()
    private companion object ErrorThrottleConfig {
        private const val ERROR_THROTTLE_MAX = 3
        private const val ERROR_THROTTLE_WINDOW_MS = 5_000L
    }

    // ── Public API ──────────────────────────────────────────────

    /**
     * Queue an operation. UI switches to "Plug in your device..." screen.
     * If a device is already attached, the flow auto-advances.
     */
    fun queueOperation(op: DeepEyeOperation) {
        log("[QUEUE] ${op.label} (Tier ${op.tier})")
        _state.value = SessionState.WaitingForDevice(op)

        // If a device is already plugged in, kick-start immediately
        usbManager.deviceList.values.firstOrNull()?.let { device ->
            log("[QUEUE] Device already attached — fast-path")
            onDeviceAttached(device)
        }
    }

    /** Cancel a queued operation and return to idle. */
    fun cancelQueue() {
        log("[QUEUE] Cancelled by user")
        closeConnection()
        _state.value = SessionState.Idle
    }

    /** Full reset — drops connection, physical key cache, state. */
    fun reset() {
        reenumTimeoutJob?.cancel()
        closeConnection()
        cachedPhysicalKey = null
        _state.value = SessionState.Idle
    }

    fun destroy() {
        reset()
        scope.cancel()
    }

    // ── Attach / Detach ─────────────────────────────────────────

    fun onDeviceAttached(device: UsbDevice) {
        val cur = _state.value

        // Re-enumeration resolve
        if (cur is SessionState.ReenumerationWait) {
            val incoming = buildPhysicalKey(device)
            if (incoming != null && incoming == cur.physicalKey) {
                reenumTimeoutJob?.cancel()
                log("[USB] Re-enum resolved — same physical device sn=${incoming.serialNumber}")
                advanceToDeviceFound(device, cur.queuedOp)
                return
            }
            // Different device during re-enum wait — reset and treat as new
            reenumTimeoutJob?.cancel()
            cachedPhysicalKey = null
        }

        val queuedOp = when (cur) {
            is SessionState.WaitingForDevice -> cur.queuedOp
            is SessionState.Error -> cur.queuedOp
            else -> null
        }

        log("[USB] Attached VID=0x${"%04X".format(device.vendorId)} PID=0x${"%04X".format(device.productId)} queued=${queuedOp?.label}")
        cachedPhysicalKey = buildPhysicalKey(device)
        advanceToDeviceFound(device, queuedOp)
    }

    fun onDeviceDetached(device: UsbDevice) {
        val cur = _state.value
        val queuedOp = cur.extractQueuedOp()

        val wasProbing = cur is SessionState.ProtocolDetect ||
                cur is SessionState.PermissionPending

        // Close FD immediately — never reuse after detach
        closeConnection()

        if (wasProbing && cachedPhysicalKey != null) {
            log("[USB] Detach during probe — entering REENUMERATION_WAIT (${REENUM_TIMEOUT_MS}ms)")
            _state.value = SessionState.ReenumerationWait(cachedPhysicalKey!!, queuedOp)
            startReenumTimeout(queuedOp)
        } else if (queuedOp != null) {
            log("[USB] Detach — returning to WaitingForDevice (queued: ${queuedOp.label})")
            _state.value = SessionState.WaitingForDevice(queuedOp)
        } else {
            log("[USB] Detach — idle")
            cachedPhysicalKey = null
            _state.value = SessionState.Idle
        }
    }

    // ── Permission ──────────────────────────────────────────────

    fun onPermissionResult(device: UsbDevice, granted: Boolean) {
        val queuedOp = _state.value.extractQueuedOp()
        if (granted) {
            log("[PERM] GRANTED — opening connection")
            _state.value = SessionState.ProtocolDetect(device, queuedOp)
            scope.launch { openAndDetect(device, queuedOp) }
        } else {
            log("[PERM] DENIED by user")
            _state.value = SessionState.PermissionDenied(device, queuedOp)
        }
    }

    // ── Execute (called externally for manual trigger, or auto) ─

    fun executeNow(op: DeepEyeOperation) {
        val cur = _state.value
        if (cur is SessionState.ConnectedReady) {
            scope.launch { executeOperation(op, cur.device, cur.protocol, cur.fd) }
        } else {
            log("[ENGINE] Cannot execute — state is not ConnectedReady")
        }
    }

    // ── Internals ───────────────────────────────────────────────

    private fun advanceToDeviceFound(device: UsbDevice, queuedOp: DeepEyeOperation?) {
        _state.value = SessionState.DeviceFound(device, queuedOp)

        if (usbManager.hasPermission(device)) {
            log("[PERM] Already granted — skipping dialog")
            _state.value = SessionState.ProtocolDetect(device, queuedOp)
            scope.launch { openAndDetect(device, queuedOp) }
        } else {
            requestPermission(device, queuedOp)
        }
    }

    private fun requestPermission(device: UsbDevice, queuedOp: DeepEyeOperation?) {
        // FIX 5: FLAG_MUTABLE + setPackage (Android 12+)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_MUTABLE else 0
        val intent = Intent(ACTION_USB_PERMISSION).apply {
            setPackage(context.packageName)
        }
        val pi = PendingIntent.getBroadcast(context, 0, intent, flags)
        _state.value = SessionState.PermissionPending(device, queuedOp)
        log("[PERM] Requesting permission...")
        try {
            usbManager.requestPermission(device, pi)
        } catch (e: Exception) {
            log("[PERM] requestPermission() threw: ${e.message}")
            _state.value = SessionState.Error("Permission request failed: ${e.message}", queuedOp)
        }
    }

    private suspend fun openAndDetect(device: UsbDevice, queuedOp: DeepEyeOperation?) {
        withContext(Dispatchers.IO) {
            try {
                if (!usbManager.hasPermission(device)) {
                    withContext(Dispatchers.Main) {
                        _state.value = SessionState.Error("Permission lost before open", queuedOp)
                    }
                    return@withContext
                }

                val conn = usbManager.openDevice(device)
                if (conn == null) {
                    withContext(Dispatchers.Main) {
                        _state.value = SessionState.Error("openDevice() returned null", queuedOp)
                    }
                    return@withContext
                }

                connection = conn
                val fd = conn.fileDescriptor
                log("[USB] Link secured (FD=$fd)")

                // Cache serial from live connection if we don't have it yet
                val serial = try { conn.serial } catch (_: SecurityException) { null }
                if (serial != null) {
                    cachedPhysicalKey = PhysicalDeviceKey(device.vendorId, device.productId, serial)
                }

                // Protocol probe (runs on IO — this is where MTK re-enum can trigger)
                log("[PROTO] Inspecting interfaces/endpoints...")
                val probe = ProtocolProbe(conn, device)
                val result = probe.detect()
                val family = mapProtocol(result.protocol)

                withContext(Dispatchers.Main) {
                    when (family) {
                        ProtocolFamily.MTP_ONLY -> {
                            log("[PROTO] MTP_ONLY — no service mode")
                            _state.value = SessionState.ConnectedMtpOnly(device)
                        }
                        ProtocolFamily.UNKNOWN -> {
                            _state.value = SessionState.Error(
                                "Unknown protocol (unsupported / wrong USB mode)", queuedOp
                            )
                        }
                        else -> {
                            log("[PROTO] Detected: $family")
                            _state.value = SessionState.ConnectedReady(device, family, fd, queuedOp)

                            // ── AUTO-EXECUTE if operation was queued ──
                            if (queuedOp != null) {
                                log("[QUEUE] Auto-executing: ${queuedOp.label}")
                                delay(300) // brief visual confirmation
                                executeOperation(queuedOp, device, family, fd)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Probe can throw if device re-enumerates mid-read.
                // The detach handler manages re-enum transitions.
                withContext(Dispatchers.Main) {
                    if (_state.value is SessionState.ProtocolDetect) {
                        log("[PROTO] Exception (device may have re-enumerated): ${e.message}")
                        _state.value = SessionState.Error(
                            "Protocol detection failed: ${e.message}", queuedOp
                        )
                    }
                }
            }
        }
    }

    private suspend fun executeOperation(
        op: DeepEyeOperation,
        device: UsbDevice,
        protocol: ProtocolFamily,
        fd: Int
    ) {
        _state.value = SessionState.ExecutingOperation(op, protocol, 0, "Starting ${op.label}...")
        log("[ENGINE] Executing: ${op.name} | protocol=$protocol | tier=${op.tier} | FD=$fd")

        withContext(Dispatchers.IO) {
            try {
                val result = EngineDispatcher.execute(
                    op = op,
                    device = device,
                    protocol = protocol,
                    fd = fd,
                    role = LicenseManager.currentRole
                ) { progress, msg ->
                    withContext(Dispatchers.Main) {
                        _state.value = SessionState.ExecutingOperation(op, protocol, progress, msg)
                    }
                }

                withContext(Dispatchers.Main) {
                    _state.value = SessionState.OperationComplete(
                        op, result.success, result.message
                    )
                    log("[ENGINE] ${op.name} ${if (result.success) "completed" else "failed"}: ${result.message}")
                }
            } catch (e: PolicyDeniedException) {
                withContext(Dispatchers.Main) {
                    log("[POLICY] ${op.name} denied: ${e.message}")
                    _state.value = SessionState.OperationComplete(
                        op, false, "Policy denied: ${e.message}"
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    logThrottled("ENGINE-${op.name}", "[ENGINE] ${op.name} failed: ${e.message}")
                    _state.value = SessionState.OperationComplete(
                        op, false, "${op.label} failed: ${e.message}"
                    )
                }
            }
        }
    }

    // ── Re-enumeration timeout ──────────────────────────────────

    private fun startReenumTimeout(queuedOp: DeepEyeOperation?) {
        reenumTimeoutJob?.cancel()
        reenumTimeoutJob = scope.launch {
            delay(REENUM_TIMEOUT_MS)
            if (_state.value is SessionState.ReenumerationWait) {
                log("[USB] Re-enum timeout — device did not re-attach")
                _state.value = if (queuedOp != null)
                    SessionState.WaitingForDevice(queuedOp)
                else
                    SessionState.Idle
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────

    private fun mapProtocol(proto: DetectedProtocol): ProtocolFamily = when (proto) {
        DetectedProtocol.QUALCOMM_EDL, DetectedProtocol.FASTBOOT, DetectedProtocol.ADB -> ProtocolFamily.QC
        DetectedProtocol.MTK_BROM, DetectedProtocol.MTK_PRELOADER -> ProtocolFamily.MTK
        DetectedProtocol.SAMSUNG_ODIN -> ProtocolFamily.SAMSUNG
        DetectedProtocol.MTP_ONLY -> ProtocolFamily.MTP_ONLY
        else -> ProtocolFamily.UNKNOWN
    }

    private fun buildPhysicalKey(device: UsbDevice): PhysicalDeviceKey? {
        val serial = try { device.serialNumber } catch (_: SecurityException) { null }
        // Empty serial → can't track across re-enum, return null to prevent false matches
        if (serial.isNullOrBlank()) return null
        return PhysicalDeviceKey(device.vendorId, device.productId, serial)
    }

    private fun closeConnection() {
        try { connection?.close() } catch (_: Exception) {}
        connection = null
    }

    private fun log(msg: String) = Log.i(TAG, msg)

    /**
     * Error throttle: suppress repeated identical errors (>3x in 5s).
     * Prevents infinite error log loops from flapping USB state.
     */
    private fun logThrottled(tag: String, msg: String) {
        val now = System.currentTimeMillis()
        val existing = errorThrottle[tag]
        if (existing != null) {
            val (count, firstSeen) = existing
            if (now - firstSeen < ERROR_THROTTLE_WINDOW_MS) {
                if (count >= ERROR_THROTTLE_MAX) {
                    // Suppressed — already logged 3x in this window
                    return
                }
                errorThrottle[tag] = (count + 1) to firstSeen
            } else {
                // Window expired, reset
                errorThrottle[tag] = 1 to now
            }
        } else {
            errorThrottle[tag] = 1 to now
        }
        Log.i(TAG, msg)
    }

    /** Extract the queued op from whatever state we're currently in. */
    private fun SessionState.extractQueuedOp(): DeepEyeOperation? = when (this) {
        is SessionState.WaitingForDevice -> queuedOp
        is SessionState.DeviceFound -> queuedOp
        is SessionState.PermissionPending -> queuedOp
        is SessionState.PermissionDenied -> queuedOp
        is SessionState.ProtocolDetect -> queuedOp
        is SessionState.ReenumerationWait -> queuedOp
        is SessionState.ConnectedReady -> queuedOp
        is SessionState.Error -> queuedOp
        else -> null
    }
}
