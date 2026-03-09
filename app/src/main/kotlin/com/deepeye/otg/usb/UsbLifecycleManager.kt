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

    private val lifecycleMutex = Mutex()

    private var activeDevice: UsbDevice? = null
    private var activeConnection: android.hardware.usb.UsbDeviceConnection? = null
    private var activeInterface: android.hardware.usb.UsbInterface? = null
    private var activeEndpoints: ResolvedEndpoints? = null
    private var transferQueue: UsbTransferQueue? = null
    private var watchdogJob: Job? = null

    private val detector = ProtocolDetector()
    private var missedPings = 0
    private var activeDeviceKey: String? = null
    private var pendingPermissionDeviceKey: String? = null
    private var activeDetection: DetectionResult? = null
    private var activeSnapshot: UsbDescriptorSnapshot? = null
    private var permissionTimeoutJob: Job? = null

    private fun deviceKey(device: UsbDevice): String =
        "${device.vendorId}:${device.productId}:${device.deviceId}"

    fun onDeviceAttached(device: UsbDevice) {
        scope.launch {
            lifecycleMutex.withLock {
                val newKey = deviceKey(device)
                val previousKey = activeDeviceKey

                if (_state.value is UsbLifecycleState.PermissionPending && pendingPermissionDeviceKey == newKey) {
                    Log.i(TAG, "[MODE] attach ignored; permission already pending key=$newKey")
                    return@withLock
                }

                if (previousKey == newKey && activeConnection != null) {
                    Log.i(TAG, "[MODE] attach ignored; session already active key=$newKey")
                    return@withLock
                }

                if (previousKey != null && previousKey != newKey) {
                    Log.i(TAG, "[MODE] re-enumeration oldKey=$previousKey newKey=$newKey action=reset")
                } else if (previousKey == newKey) {
                    Log.i(TAG, "[MODE] attach same-key=$newKey action=reclassify")
                }

                // Stage 6 Rule 1: Reset session to DISCONNECTED / empty
                closeInternal()

                val snapshot = UsbSnapshotFactory.from(device)
                val detection = detector.detect(snapshot)
                val mode = detection.toConnectionMode()

                Log.i(
                    TAG,
                    "[MODE] attach-session key=$newKey mode=${detection.deviceMode} family=${detection.protocolFamily} reason=\"${detection.reason}\""
                )

                _state.value = UsbLifecycleState.DeviceDetected(
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

                if (usbManager.hasPermission(device)) {
                    pendingPermissionDeviceKey = null
                    permissionTimeoutJob?.cancel()
                    permissionTimeoutJob = null
                    openConnection(
                        device = device,
                        mode = mode,
                        detection = detection,
                        snapshot = snapshot,
                        deviceKey = newKey
                    )
                } else {
                    pendingPermissionDeviceKey = newKey
                    _state.value = UsbLifecycleState.PermissionPending(device)
                    UsbPermissionGuard.requestPermission(
                        context, usbManager, device,
                        UsbPermissionGuard.ACTION_USB_PERMISSION
                    )
                    // Start permission timeout watchdog (10s)
                    permissionTimeoutJob?.cancel()
                    permissionTimeoutJob = scope.launch {
                        delay(10_000L)
                        lifecycleMutex.withLock {
                            val currentState = _state.value
                            if (currentState is UsbLifecycleState.PermissionPending &&
                                pendingPermissionDeviceKey == newKey
                            ) {
                                Log.w(TAG, "[MODE] permission-timeout key=$newKey")
                                closeInternal()
                                _state.value = UsbLifecycleState.Error(
                                    message = "USB permission request timed out",
                                    recoverable = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun onPermissionResult(device: UsbDevice, granted: Boolean) {
        scope.launch {
            lifecycleMutex.withLock {
                val permissionKey = deviceKey(device)
                val expectedPendingKey = pendingPermissionDeviceKey

                val pending = _state.value as? UsbLifecycleState.PermissionPending
                if (pending != null && pending.device.deviceId != device.deviceId) {
                    Log.w(
                        TAG,
                        "[MODE] permission result ignored for stale deviceId=${device.deviceId} expected=${pending.device.deviceId}"
                    )
                    return@withLock
                }

                if (expectedPendingKey == null) {
                    Log.w(TAG, "[MODE] permission result ignored; no pending request key=$permissionKey granted=$granted")
                    return@withLock
                }

                if (expectedPendingKey != permissionKey) {
                    Log.w(
                        TAG,
                        "[MODE] permission result ignored for stale key=$permissionKey expected=$expectedPendingKey"
                    )
                    return@withLock
                }

                if (granted) {
                    pendingPermissionDeviceKey = null
                    permissionTimeoutJob?.cancel()
                    permissionTimeoutJob = null
                    val snapshot = UsbSnapshotFactory.from(device)
                    val detection = detector.detect(snapshot)
                    openConnection(
                        device = device,
                        mode = detection.toConnectionMode(),
                        detection = detection,
                        snapshot = snapshot,
                        deviceKey = permissionKey
                    )
                } else {
                    pendingPermissionDeviceKey = null
                    permissionTimeoutJob?.cancel()
                    permissionTimeoutJob = null
                    Log.w(TAG, "[MODE] permission denied key=$permissionKey")
                    _state.value = UsbLifecycleState.PermissionDenied(device, device.productName ?: "Unknown")
                }
            }
        }
    }

    fun onDeviceDetached(device: UsbDevice) {
        scope.launch {
            lifecycleMutex.withLock {
                val detachedKey = deviceKey(device)
                val activeKey = activeDeviceKey
                val pendingKey = pendingPermissionDeviceKey

                if (activeKey != null && activeKey != detachedKey && pendingKey != detachedKey) {
                    Log.i(TAG, "[MODE] detach ignored for non-active key=$detachedKey active=$activeKey pending=$pendingKey")
                    return@withLock
                }

                if (activeKey == null && pendingKey != null && pendingKey != detachedKey) {
                    Log.i(TAG, "[MODE] detach ignored for non-pending key=$detachedKey pending=$pendingKey")
                    return@withLock
                }

                Log.i(TAG, "[MODE] detach key=$detachedKey")
                // Stage 6 Rule 1 & 2: Clear cached mode, protocol, and error fields
                closeInternal()
                _state.value = UsbLifecycleState.Idle
            }
        }
    }

    private suspend fun openConnection(
        device: UsbDevice,
        mode: ConnectionMode,
        detection: DetectionResult,
        snapshot: UsbDescriptorSnapshot,
        deviceKey: String
    ) = withContext(Dispatchers.IO) {
        _state.value = UsbLifecycleState.Connecting(
            device = device,
            mode = mode,
            protocolFamily = detection.protocolFamily,
            deviceKey = deviceKey
        )

        val conn = OemCompatibilityLayer.openDeviceWithRetry(usbManager, device) ?: run {
            _state.value = UsbLifecycleState.Error("Cannot open device", true)
            return@withContext
        }

        val endpoints = UsbEndpointResolver.resolve(device, mode) ?: run {
            conn.close()
            _state.value = UsbLifecycleState.Error("No endpoints for $mode", false)
            return@withContext
        }

        val usbInterface = endpoints.usbInterface
        val claimed = try {
            conn.claimInterface(usbInterface, true)
        } catch (_: Exception) {
            false
        }

        if (!claimed) {
            conn.close()
            _state.value = UsbLifecycleState.Error("Cannot claim USB interface", true)
            return@withContext
        }

        OemCompatibilityLayer.postClaimInterfaceDelay()

        activeDevice = device
        activeConnection = conn
        activeInterface = usbInterface
        activeEndpoints = endpoints
        transferQueue = UsbTransferQueue(conn, endpoints)
        activeDeviceKey = deviceKey
        activeDetection = detection
        activeSnapshot = snapshot

        _state.value = UsbLifecycleState.Connected(
            deviceName = device.productName ?: device.deviceName ?: "Unknown",
            mode = mode,
            protocolFamily = detection.protocolFamily,
            detectedDeviceMode = detection.deviceMode,
            detectionReason = detection.reason,
            confidence = detection.confidence,
            vendorId = device.vendorId,
            productId = device.productId,
            deviceId = device.deviceId,
            deviceKey = deviceKey,
            descriptorSnapshot = snapshot,
            brand = device.manufacturerName ?: "Unknown",
            endpoints = endpoints
        )

        startWatchdog()
    }

    private fun startWatchdog() {
        watchdogJob?.cancel()
        missedPings = 0

        watchdogJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                val conn = activeConnection ?: break

                if (pingDevice(conn)) {
                    missedPings = 0
                    if (_state.value is UsbLifecycleState.Degraded) {
                        restoreConnectedState(overrideBrand = "Healthy")
                    }
                } else {
                    missedPings++

                    val activeName = activeDevice?.productName ?: activeDevice?.deviceName ?: "Unknown"
                    val activeMode = activeDetection?.toConnectionMode() ?: ConnectionMode.UNKNOWN

                    if (missedPings >= MAX_MISSED_PINGS) {
                        closeInternal()
                        _state.value = UsbLifecycleState.Dead(activeName, "No response")
                        break
                    } else {
                        _state.value = UsbLifecycleState.Degraded(activeName, activeMode, missedPings, MAX_MISSED_PINGS)
                    }
                }
            }
        }
    }

    private fun restoreConnectedState(overrideBrand: String? = null) {
        val device = activeDevice ?: return
        val detection = activeDetection ?: return
        val snapshot = activeSnapshot ?: return
        val endpoints = activeEndpoints ?: return

        _state.value = UsbLifecycleState.Connected(
            deviceName = device.productName ?: device.deviceName ?: "Unknown",
            mode = detection.toConnectionMode(),
            protocolFamily = detection.protocolFamily,
            detectedDeviceMode = detection.deviceMode,
            detectionReason = detection.reason,
            confidence = detection.confidence,
            vendorId = device.vendorId,
            productId = device.productId,
            deviceId = device.deviceId,
            deviceKey = activeDeviceKey ?: deviceKey(device),
            descriptorSnapshot = snapshot,
            brand = overrideBrand ?: device.manufacturerName ?: "Unknown",
            endpoints = endpoints
        )
    }

    private fun pingDevice(conn: android.hardware.usb.UsbDeviceConnection): Boolean = try {
        // GET_STATUS (Standard Request) to check if device is still alive
        val buf = ByteArray(2)
        val result = conn.controlTransfer(0x80, 0x00, 0, 0, buf, 2, 500)
        result >= 0
    } catch (e: Exception) {
        Log.d(TAG, "[MODE] ping-failure key=$activeDeviceKey reason=\"${e.message}\"")
        false
    }

    private fun closeInternal() {
        watchdogJob?.cancel()
        watchdogJob = null
        missedPings = 0
        transferQueue = null
        permissionTimeoutJob?.cancel()
        permissionTimeoutJob = null
        try {
            activeInterface?.let { activeConnection?.releaseInterface(it) }
            activeConnection?.close()
        } catch (e: Exception) { } finally {
            activeDevice = null
            activeConnection = null
            activeInterface = null
            activeDeviceKey = null
            pendingPermissionDeviceKey = null
            activeDetection = null
            activeSnapshot = null
            activeEndpoints = null
        }
    }

    fun getTransferQueue() = transferQueue
    fun isConnected() = activeConnection != null
    fun getActiveDevice() = activeDevice
    fun getActiveConnection() = activeConnection
    fun getCurrentMode() = (state.value as? UsbLifecycleState.Connected)?.mode
    fun pauseWatchdog() { watchdogJob?.cancel() }

    fun resumeWatchdog() {
        if (state.value is UsbLifecycleState.Connected) startWatchdog()
    }

    fun destroy() {
        closeInternal()
        _state.value = UsbLifecycleState.Idle
    }
}
