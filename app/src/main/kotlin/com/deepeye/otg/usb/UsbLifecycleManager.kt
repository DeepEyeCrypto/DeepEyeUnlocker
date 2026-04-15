package com.deepeye.otg.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
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
import java.util.UUID
import timber.log.Timber

/**
 * Single source of truth for USB connection lifecycle.
 * Lives in ViewModel — survives Activity recreation.
 */
@javax.inject.Singleton
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
    private val MAX_RETRY_COUNT = 5
    private val BASE_BACKOFF_MS = 500L
    private val retryCounts = mutableMapOf<String, Int>()
    private val sessionIds = mutableMapOf<String, String>()

    private fun deviceKey(device: UsbDevice): String =
        "${device.vendorId}:${device.productId}:${device.deviceId}"

    private fun sessionIdFor(key: String): String =
        sessionIds.getOrPut(key) { UUID.randomUUID().toString() }

    private fun retryCountFor(key: String): Int = retryCounts[key] ?: 0

    private fun resetRetryCount(key: String) {
        retryCounts.remove(key)
    }

    private fun clearTracking(key: String) {
        retryCounts.remove(key)
        sessionIds.remove(key)
    }

    fun onDeviceAttached(device: UsbDevice) {
        scope.launch {
            lifecycleMutex.withLock {
                val newKey = deviceKey(device)
                val sessionId = sessionIdFor(newKey)

                if (activeSessions.containsKey(newKey)) {
                    UsbLogger.info(TAG, "[USB_LIFECYCLE] attach_ignored reason=already_active key=$newKey sessionId=$sessionId")
                    return@withLock
                }

                if (_state.value is UsbLifecycleState.PermissionPending && pendingPermissionDeviceKey == newKey) {
                    UsbLogger.info(TAG, "[USB_LIFECYCLE] attach_ignored reason=permission_pending key=$newKey sessionId=$sessionId")
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
                UsbLogger.info(
                    TAG,
                    "[ROUTER] detectedMode=$mode protocolHint=${routingResult.protocol} confidence=${routingResult.confidence} vid=0x${device.vendorId.toString(16)} pid=0x${device.productId.toString(16)} sessionId=$sessionId"
                )
                if (mode == ConnectionMode.UNKNOWN && routingResult.protocol != DeviceProtocol.UNKNOWN) {
                    UsbLogger.warn(
                        TAG,
                        "[ROUTER] descriptor_unknown protocolHint=${routingResult.protocol} reason=\"${routingResult.reason}\" sessionId=$sessionId"
                    )
                }

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
                    openConnection(device, mode, detection, snapshot, newKey, sessionId)
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
                                UsbLogger.warn(TAG, "[USB_LIFECYCLE] permission_timeout key=$newKey sessionId=$sessionId")
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
                    openConnection(device, detection.toConnectionMode(), detection, snapshot, key, sessionIdFor(key))
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
                val sessionId = sessionIds[key] ?: "nosession"
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
                UsbLogger.info(TAG, "[USB_LIFECYCLE] detached key=$key activeSessions=${activeSessions.size} sessionId=$sessionId")
            }
        }
    }

    private suspend fun openConnection(
        device: UsbDevice,
        mode: ConnectionMode,
        detection: DetectionResult,
        snapshot: UsbDescriptorSnapshot,
        key: String,
        sessionId: String
    ) = withContext(Dispatchers.IO) {
        // Retry logic for opening device (up to 3 attempts)
        val maxRetries = 3
        var conn: UsbDeviceConnection? = null
        var lastOpenError: String = "Unknown error"
        
        repeat(maxRetries) { attempt ->
            Timber.d("[USB] Open attempt ${attempt + 1}/$maxRetries for VID=0x${device.vendorId.toString(16)} PID=0x${device.productId.toString(16)}")
            
            conn = OemCompatibilityLayer.openDeviceWithRetry(usbManager, device)
            if (conn != null) {
                Timber.d("[USB] ✅ Device opened successfully")
                return@repeat
            }
            
            lastOpenError = when {
                !usbManager.hasPermission(device) -> "Permission denied - reconnect OTG cable"
                else -> "Cannot open device - device busy or not ready"
            }
            Timber.e("[USB] openDevice failed on attempt ${attempt + 1}: $lastOpenError")
            
            if (attempt < maxRetries - 1) {
                Timber.d("[USB] Retrying in 500ms...")
                delay(500)
            }
        }
        
        if (conn == null) {
            val err = UsbLifecycleState.Error(lastOpenError, true)
            if (retryCountFor(key) < MAX_RETRY_COUNT) {
                scheduleReconnect(device, key, "OPEN_FAIL")
            } else {
                coordinator.transition(
                    ConnectionState.Failed("OPEN_FAIL", "Cannot open connection: $lastOpenError"),
                    "Failed to open $key after $maxRetries retries"
                )
            }
            _state.value = err
            updateSessionState(key, err)
            return@withContext
        }

        val endpoints = UsbEndpointResolver.resolve(device, mode) ?: run {
            conn!!.close()
            val err = UsbLifecycleState.Error("Endpoints not resolved", false)
            coordinator.transition(ConnectionState.Failed("EP_FAIL", "Endpoints not resolved"), "No matching endpoints")
            _state.value = err
            updateSessionState(key, err)
            return@withContext
        }

        // Try to claim interface with retry
        val claimed = try {
            val claimResult = conn!!.claimInterface(endpoints.usbInterface, true)
            if (!claimResult) {
                Timber.e("[USB] ❌ claimInterface returned false for interface ${endpoints.usbInterface.id}")
                // Try forcing kernel driver detach
                try {
                    conn!!.releaseInterface(endpoints.usbInterface)
                    delay(100)
                    conn!!.claimInterface(endpoints.usbInterface, true)
                } catch (e: Exception) {
                    Timber.e("[USB] claimInterface retry failed: ${e.message}")
                    false
                }
            } else {
                Timber.d("[USB] ✅ Interface ${endpoints.usbInterface.id} claimed successfully")
                true
            }
        } catch (e: Exception) {
            Timber.e("[USB] ❌ claimInterface exception: ${e.message}")
            false
        }
        
        if (!claimed) {
            conn!!.close()
            val err = UsbLifecycleState.Error("Interface busy - close other apps and retry", true)
            coordinator.transition(
                ConnectionState.Failed("CLAIM_FAIL", "Cannot claim USB interface"),
                "Failed to claim interface - another app may be using it"
            )
            _state.value = err
            updateSessionState(key, err)
            return@withContext
        }

        val session = DeviceSession(
            device = device,
            connection = conn,
            usbInterface = endpoints.usbInterface,
            endpoints = endpoints,
            transport = BulkTransport(conn, endpoints, snapshot),
            sessionId = sessionId,
            deviceKey = key,
            detection = detection,
            snapshot = snapshot
        )

        activeSessions[key] = session

        // THE FIX: Extract feature string from productName if present
        val featureStr = device.productName?.takeIf { it.startsWith("hw_code:") }
        val isSecBoot = featureStr?.contains("SEC_BOOT_EN=1") ?: false

        val connectedState = UsbLifecycleState.Connected(
            device = device, // THE FIX: Hold real object
            deviceName = device.productName ?: "Generic",
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
            brand = snapshot.manufacturerName ?: "Unknown",
            chipset = snapshot.productName ?: "Generic",
            secureBootStatus = if (isSecBoot) "ON" else "OFF",
            endpoints = endpoints,
            sessionId = sessionId
        )

        _state.value = connectedState
        resetRetryCount(key)
        coordinator.transition(ConnectionState.Ready, "Session established")
        updateSessionState(key, connectedState)
        UsbLogger.info(TAG, "[USB_LIFECYCLE] connected mode=$mode key=$key sessionId=$sessionId")
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
                            UsbLogger.error(TAG, "[USB_LIFECYCLE] watchdog_failed missedPings=${session.missedPings} sessionId=${session.sessionId}")
                            coordinator.transition(ConnectionState.Recovering, "Watchdog timeout")
                            onDeviceDetached(session.device)
                            if (retryCountFor(session.deviceKey) < MAX_RETRY_COUNT) {
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
        retryCounts.clear()
        sessionIds.clear()
        _sessions.value = emptyMap()
        _state.value = UsbLifecycleState.Idle
    }

    private fun scheduleReconnect(device: UsbDevice, key: String, reason: String) {
        scope.launch {
            val attempt = retryCountFor(key)
            val backoff = (BASE_BACKOFF_MS * 2.0.pow(attempt.toDouble())).toLong()
            coordinator.transition(ConnectionState.Recovering, "Retrying $reason ($attempt/$MAX_RETRY_COUNT) in ${backoff}ms")
            UsbLogger.warn(TAG, "[USB_LIFECYCLE] reconnect_scheduled reason=$reason attempt=$attempt backoffMs=$backoff sessionId=${sessionIdFor(key)}")
            delay(backoff)
            retryCounts[key] = attempt + 1
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
