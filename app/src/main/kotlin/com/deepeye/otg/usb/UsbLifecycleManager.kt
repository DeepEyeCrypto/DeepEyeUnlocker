package com.deepeye.otg.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.deepeye.otg.data.ConnectionMode
import com.deepeye.otg.domain.models.ConnectionState
import com.deepeye.otg.domain.models.ProtocolFamily
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlin.math.pow
import kotlinx.coroutines.sync.withLock
import com.deepeye.otg.data.device.ProtocolRouter
import com.deepeye.otg.data.device.DeviceProtocol
import javax.inject.Inject

/**
 * Single source of truth for USB connection lifecycle.
 * Lives in ViewModel — survives Activity recreation.
 */
class UsbLifecycleManager @Inject constructor(
    private val context: Context,
    private val usbManager: UsbManager,
    private val scope: CoroutineScope,
    private val coordinator: SessionCoordinator
) {
    companion object {
        private const val TAG = "UsbLifecycle"
        private const val WATCHDOG_INTERVAL_MS = 5_000L
        private const val MAX_MISSED_PINGS = 3
    }

    private val _state = MutableStateFlow<UsbLifecycleState>(UsbLifecycleState.Idle)
    val state: StateFlow<UsbLifecycleState> = _state.asStateFlow()

    private val _sessions = MutableStateFlow<Map<String, UsbLifecycleState>>(emptyMap())
    val sessions: StateFlow<Map<String, UsbLifecycleState>> = _sessions.asStateFlow()

    private val activeSessions = mutableMapOf<String, DeviceSession>()
    private val lifecycleMutex = Mutex()

    private var pendingPermissionDeviceKey: String? = null
    private var permissionTimeoutJob: Job? = null
    private val detector = ProtocolDetector()
    private var retryCount = 0
    private val MAX_RETRY_COUNT = 5
    private val BASE_BACKOFF_MS = 500L

    private fun deviceKey(device: UsbDevice): String =
        "${device.vendorId}:${device.productId}:${device.deviceId}"

    fun onDeviceAttached(device: UsbDevice) {
        scope.launch {
            lifecycleMutex.withLock {
                val newKey = deviceKey(device)

                if (activeSessions.containsKey(newKey)) {
                    Log.i(TAG, "[MODE] attach ignored; session already active key=$newKey")
                    return@withLock
                }

                if (_state.value is UsbLifecycleState.PermissionPending && pendingPermissionDeviceKey == newKey) {
                    Log.i(TAG, "[MODE] attach ignored; permission pending key=$newKey")
                    return@withLock
                }

                val snapshot = UsbSnapshotFactory.from(device)
                val detection = detector.detect(snapshot)
                val mode = detection.toConnectionMode()

                // Protocol Routing Logic (Stage 201.2)
                val routingResult = ProtocolRouter.route(
                    device.vendorId, 
                    device.productId, 
                    snapshot.manufacturerName, 
                    snapshot.productName
                )
                Log.d(TAG, "[ROUTER] vid=0x${device.vendorId.toString(16)} protocol=${routingResult.protocol} confidence=${routingResult.confidence}")

                // Dispatch to specific protocol session (Stage 201.3)
                when (routingResult.protocol) {
                    DeviceProtocol.MTK_V6       -> startMtkV6Session(device)
                    DeviceProtocol.MTK_BROM     -> startMtkBromSession(device)
                    DeviceProtocol.QC_EDL       -> startQcEdlSession(device)
                    DeviceProtocol.SAMSUNG_ODIN -> startSamsungSession(device)
                    DeviceProtocol.MTK_OR_QC    -> askUserToSelect(device)
                    DeviceProtocol.UNKNOWN      -> showUnknownDevice(device)
                }

                Log.i(TAG, "[PROTOCOL] Device attached: ${device.productName} (VID=0x${Integer.toHexString(device.vendorId)})")
                Log.i(TAG, "[PROTOCOL] Route Result: ${routingResult.protocol} | Confidence: ${routingResult.confidence} | Reason: ${routingResult.reason}")

                val newState = UsbLifecycleState.DeviceDetected(
                    device = device,
                    detectedMode = mode,
                    protocolFamily = detection.protocolFamily,
                    detectedDeviceMode = detection.deviceMode,
                    detectionReason = detection.reason,
                    confidence = detection.confidence,
                    vendorId = device.vendorId,
                    productId = device.productId,
                    deviceId = device.deviceId,
                    deviceKey = newKey,
                    descriptorSnapshot = snapshot,
                    brand = snapshot.manufacturerName ?: "Unknown",
                    chipset = snapshot.productName ?: "Generic"
                )

                _state.value = newState
                coordinator.transition(ConnectionState.DeviceDetected, "VID=${device.vendorId} PID=${device.productId}")
                updateSessionState(newKey, newState)

                if (usbManager.hasPermission(device)) {
                    openConnection(device, mode, detection, snapshot, newKey)
                } else {
                    pendingPermissionDeviceKey = newKey
                    val permState = UsbLifecycleState.PermissionPending(device)
                    _state.value = permState
                    coordinator.transition(ConnectionState.PermissionPending, "Requesting USB permission")
                    updateSessionState(newKey, permState)
                    
                    UsbPermissionGuard.requestPermission(context, usbManager, device, UsbPermissionGuard.ACTION_USB_PERMISSION)
                    
                    permissionTimeoutJob?.cancel()
                    permissionTimeoutJob = scope.launch {
                        delay(10_000L)
                        lifecycleMutex.withLock {
                            if (pendingPermissionDeviceKey == newKey) {
                                Log.w(TAG, "[MODE] permission-timeout key=$newKey")
                                val err = UsbLifecycleState.Error("Permission timeout", true)
                                coordinator.transition(ConnectionState.Failed("TIMEOUT", "Permission timeout"), "timeout for $newKey")
                                _state.value = err
                                updateSessionState(newKey, err)
                                // Permission timeout is terminal, no auto-reconnect here as it requires user interaction usually.
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateSessionState(key: String, state: UsbLifecycleState) {
        val current = _sessions.value.toMutableMap()
        current[key] = state
        _sessions.value = current
    }

    fun onPermissionResult(device: UsbDevice, granted: Boolean) {
        scope.launch {
            lifecycleMutex.withLock {
                val key = deviceKey(device)
                if (pendingPermissionDeviceKey != key) return@withLock
                pendingPermissionDeviceKey = null
                permissionTimeoutJob?.cancel()

                if (granted) {
                    val snapshot = UsbSnapshotFactory.from(device)
                    val detection = detector.detect(snapshot)
                    openConnection(device, detection.toConnectionMode(), detection, snapshot, key)
                } else {
                    val denied = UsbLifecycleState.PermissionDenied(device, device.productName ?: "Unknown")
                    _state.value = denied
                    updateSessionState(key, denied)
                }
            }
        }
    }

    fun onDeviceDetached(device: UsbDevice) {
        scope.launch {
            lifecycleMutex.withLock {
                val key = deviceKey(device)
                activeSessions[key]?.close()
                activeSessions.remove(key)
                
                val current = _sessions.value.toMutableMap()
                current.remove(key)
                _sessions.value = current

                if (activeSessions.isEmpty()) {
                    _state.value = UsbLifecycleState.Idle
                    coordinator.transition(ConnectionState.Disconnected, "Last device detached")
                } else {
                    _state.value = _sessions.value.values.lastOrNull() ?: UsbLifecycleState.Idle
                    coordinator.transition(ConnectionState.Disconnected, "One device detached")
                }
                Log.i(TAG, "[MODE] detached key=$key activeSessions=${activeSessions.size}")
            }
        }
    }

    private suspend fun openConnection(
        device: UsbDevice,
        mode: ConnectionMode,
        detection: DetectionResult,
        snapshot: UsbDescriptorSnapshot,
        key: String
    ) = withContext(Dispatchers.IO) {
        val conn = OemCompatibilityLayer.openDeviceWithRetry(usbManager, device) ?: run {
            val err = UsbLifecycleState.Error("Cannot open device", true)
            if (retryCount < MAX_RETRY_COUNT) {
                scheduleReconnect(device, key, "OPEN_FAIL")
            } else {
                coordinator.transition(ConnectionState.Failed("OPEN_FAIL", "Cannot open connection"), "Failed to open $key after $retryCount retries")
            }
            _state.value = err
            updateSessionState(key, err)
            return@withContext
        }

        val endpoints = UsbEndpointResolver.resolve(device, mode) ?: run {
            conn.close()
            val err = UsbLifecycleState.Error("Endpoints not resolved", false)
            coordinator.transition(ConnectionState.Failed("EP_FAIL", "Endpoints not resolved"), "No matching endpoints")
            _state.value = err
            updateSessionState(key, err)
            return@withContext
        }

        val claimed = try { conn.claimInterface(endpoints.usbInterface, true) } catch (e: Exception) { false }
        if (!claimed) {
            conn.close()
            val err = UsbLifecycleState.Error("Interface claim failed", true)
            coordinator.transition(ConnectionState.Failed("CLAIM_FAIL", "Interface claim failed"), "Failed to claim interface")
            _state.value = err
            updateSessionState(key, err)
            return@withContext
        }

        val session = DeviceSession(
            device = device,
            connection = conn,
            usbInterface = endpoints.usbInterface,
            endpoints = endpoints,
            transport = BulkTransport(conn, endpoints),
            deviceKey = key,
            detection = detection,
            snapshot = snapshot
        )

        activeSessions[key] = session
        
        val connectedState = UsbLifecycleState.Connected(
            deviceName = device.productName ?: "Unknown",
            mode = mode,
            protocolFamily = detection.protocolFamily,
            detectedDeviceMode = detection.deviceMode,
            detectionReason = detection.reason,
            confidence = detection.confidence,
            vendorId = device.vendorId,
            productId = device.productId,
            deviceId = device.deviceId,
            deviceKey = key,
            descriptorSnapshot = snapshot,
            brand = device.manufacturerName ?: "Unknown",
            chipset = device.productName ?: "Generic",
            endpoints = endpoints
        )

        _state.value = connectedState
        retryCount = 0 // Reset on success
        coordinator.transition(ConnectionState.Ready, "Session established")
        updateSessionState(key, connectedState)
        startWatchdog(session)
    }

    private fun startWatchdog(session: DeviceSession) {
        session.watchdogJob?.cancel()
        session.watchdogJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                if (pingDevice(session.connection)) {
                    session.missedPings = 0
                } else {
                    session.missedPings++
                    if (session.missedPings >= MAX_MISSED_PINGS) {
                        lifecycleMutex.withLock {
                            Log.e(TAG, "[LIFECYCLE] Watchdog failure: missed ${session.missedPings} pings. Attempting recovery.")
                            coordinator.transition(ConnectionState.Recovering, "Watchdog timeout")
                            onDeviceDetached(session.device)
                            if (retryCount < MAX_RETRY_COUNT) {
                                scheduleReconnect(session.device, session.deviceKey, "WATCHDOG_FAIL")
                            }
                        }
                        break
                    }
                }
            }
        }
    }

    private fun pingDevice(conn: android.hardware.usb.UsbDeviceConnection): Boolean = try {
        val buf = ByteArray(2)
        conn.controlTransfer(0x80, 0x00, 0, 0, buf, 2, 500) >= 0
    } catch (e: Exception) { false }

    fun getTransport(key: String? = null): BulkTransport? {
        val targetKey = key ?: activeSessions.keys.lastOrNull() ?: return null
        return activeSessions[targetKey]?.transport
    }

    fun getActiveConnection(key: String? = null): android.hardware.usb.UsbDeviceConnection? {
        val targetKey = key ?: activeSessions.keys.lastOrNull() ?: return null
        return activeSessions[targetKey]?.connection
    }

    fun getActiveDevice(key: String? = null): UsbDevice? {
        val targetKey = key ?: activeSessions.keys.lastOrNull() ?: return null
        return activeSessions[targetKey]?.device
    }

    fun getActiveSnapshot(key: String? = null): UsbDescriptorSnapshot? {
        val targetKey = key ?: activeSessions.keys.lastOrNull() ?: return null
        return activeSessions[targetKey]?.snapshot
    }

    /**
     * Gets a list of all currently active and discovered devices.
     */
    fun getDiscoveredDevices(): List<UsbDevice> {
        return activeSessions.values.map { it.device }
    }

    fun isConnected() = activeSessions.isNotEmpty()

    fun destroy() {
        activeSessions.values.forEach { it.close() }
        activeSessions.clear()
        _sessions.value = emptyMap()
        _state.value = UsbLifecycleState.Idle
    }

    private fun scheduleReconnect(device: UsbDevice, key: String, reason: String) {
        scope.launch {
            val backoff = (BASE_BACKOFF_MS * 2.0.pow(retryCount.toDouble())).toLong()
            coordinator.transition(ConnectionState.Recovering, "Retrying $reason ($retryCount/$MAX_RETRY_COUNT) in ${backoff}ms")
            delay(backoff)
            retryCount++
            onDeviceAttached(device)
        }
    }

    // ── Session Dispatching (Stage 201.3) ───────────────────────

    private fun startMtkV6Session(device: UsbDevice) {
        Log.i(TAG, "[SESSION] Starting MTK V6 (Dimensity) session for ${device.productName}")
        // Next: Implement V6 handshake trigger
    }

    private fun startMtkBromSession(device: UsbDevice) {
        Log.i(TAG, "[SESSION] Starting MTK Classic BROM session for ${device.productName}")
    }

    private fun startQcEdlSession(device: UsbDevice) {
        Log.i(TAG, "[SESSION] Starting Qualcomm EDL (Sahara/Firehose) session for ${device.productName}")
    }

    private fun startSamsungSession(device: UsbDevice) {
        Log.i(TAG, "[SESSION] Starting Samsung ODIN session for ${device.productName}")
    }

    private fun askUserToSelect(device: UsbDevice) {
        Log.w(TAG, "[SESSION] Ambiguous protocol (MTK/QC) for ${device.productName}. Awaiting User choice.")
    }

    private fun showUnknownDevice(device: UsbDevice) {
        Log.w(TAG, "[SESSION] Unknown device 0x${device.vendorId.toString(16)}. Showing generic support.")
    }
}
