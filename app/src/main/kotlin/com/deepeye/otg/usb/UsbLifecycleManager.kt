package com.deepeye.otg.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.deepeye.otg.data.ConnectionMode
import com.deepeye.otg.domain.models.ProtocolFamily
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for USB connection lifecycle.
 * Lives in ViewModel — survives Activity recreation.
 */
class UsbLifecycleManager(
    private val context: Context,
    private val usbManager: UsbManager,
    private val scope: CoroutineScope
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
                updateSessionState(newKey, newState)

                if (usbManager.hasPermission(device)) {
                    openConnection(device, mode, detection, snapshot, newKey)
                } else {
                    pendingPermissionDeviceKey = newKey
                    val permState = UsbLifecycleState.PermissionPending(device)
                    _state.value = permState
                    updateSessionState(newKey, permState)
                    
                    UsbPermissionGuard.requestPermission(context, usbManager, device, UsbPermissionGuard.ACTION_USB_PERMISSION)
                    
                    permissionTimeoutJob?.cancel()
                    permissionTimeoutJob = scope.launch {
                        delay(10_000L)
                        lifecycleMutex.withLock {
                            if (pendingPermissionDeviceKey == newKey) {
                                Log.w(TAG, "[MODE] permission-timeout key=$newKey")
                                val err = UsbLifecycleState.Error("Permission timeout", true)
                                _state.value = err
                                updateSessionState(newKey, err)
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
                } else {
                    _state.value = _sessions.value.values.lastOrNull() ?: UsbLifecycleState.Idle
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
            _state.value = err
            updateSessionState(key, err)
            return@withContext
        }

        val endpoints = UsbEndpointResolver.resolve(device, mode) ?: run {
            conn.close()
            val err = UsbLifecycleState.Error("Endpoints not resolved", false)
            _state.value = err
            updateSessionState(key, err)
            return@withContext
        }

        val claimed = try { conn.claimInterface(endpoints.usbInterface, true) } catch (e: Exception) { false }
        if (!claimed) {
            conn.close()
            val err = UsbLifecycleState.Error("Interface claim failed", true)
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
                            onDeviceDetached(session.device)
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

    fun isConnected() = activeSessions.isNotEmpty()

    fun destroy() {
        activeSessions.values.forEach { it.close() }
        activeSessions.clear()
        _sessions.value = emptyMap()
        _state.value = UsbLifecycleState.Idle
    }
}
