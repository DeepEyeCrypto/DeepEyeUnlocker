package com.deepeye.otg.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.deepeye.otg.data.ConnectionMode
import com.deepeye.otg.data.UsbDeviceDatabase
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

    private var activeConnection: android.hardware.usb.UsbDeviceConnection? = null
    private var activeInterface: android.hardware.usb.UsbInterface? = null
    private var activeEndpoints: ResolvedEndpoints? = null
    private var transferQueue: UsbTransferQueue? = null
    private var watchdogJob: Job? = null
    private var missedPings = 0

    fun onDeviceAttached(device: UsbDevice) {
        scope.launch {
            lifecycleMutex.withLock {
                closeInternal()

                val sig = UsbDeviceDatabase.detect(device.vendorId, device.productId)
                val mode = sig?.mode ?: UsbDeviceDatabase.detectByVendor(device.vendorId)

                Log.i(TAG, "Device attached: ${device.productName} VID=0x${device.vendorId.toString(16)} mode=$mode")

                _state.value = UsbLifecycleState.DeviceDetected(
                    device = device,
                    detectedMode = mode,
                    brand = sig?.brand ?: "Unknown",
                    chipset = sig?.chipset ?: "Unknown"
                )

                if (usbManager.hasPermission(device)) {
                    openConnection(device, mode)
                } else {
                    _state.value = UsbLifecycleState.PermissionPending(device)
                    UsbPermissionGuard.requestPermission(
                        context, usbManager, device,
                        UsbSessionManager.ACTION_USB_PERMISSION
                    )
                }
            }
        }
    }

    fun onPermissionResult(device: UsbDevice, granted: Boolean) {
        scope.launch {
            lifecycleMutex.withLock {
                if (granted) {
                    val sig = UsbDeviceDatabase.detect(device.vendorId, device.productId)
                    val mode = sig?.mode ?: UsbDeviceDatabase.detectByVendor(device.vendorId)
                    openConnection(device, mode)
                } else {
                    _state.value = UsbLifecycleState.PermissionDenied(device, device.productName ?: "Unknown")
                }
            }
        }
    }

    fun onDeviceDetached(device: UsbDevice) {
        scope.launch {
            lifecycleMutex.withLock {
                closeInternal()
                _state.value = UsbLifecycleState.Idle
            }
        }
    }

    private suspend fun openConnection(device: UsbDevice, mode: ConnectionMode) = withContext(Dispatchers.IO) {
        _state.value = UsbLifecycleState.Connecting(device, mode)

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
        conn.claimInterface(usbInterface, true)
        OemCompatibilityLayer.postClaimInterfaceDelay()

        activeConnection = conn
        activeInterface = usbInterface
        activeEndpoints = endpoints
        transferQueue = UsbTransferQueue(conn, endpoints)

        val sig = UsbDeviceDatabase.detect(device.vendorId, device.productId)

        _state.value = UsbLifecycleState.Connected(
            deviceName = device.productName ?: "Unknown",
            mode = mode,
            brand = sig?.brand ?: "Unknown",
            endpoints = endpoints
        )

        startWatchdog(device.productName ?: "Unknown", mode)
    }

    private fun startWatchdog(deviceName: String, mode: ConnectionMode) {
        watchdogJob?.cancel()
        missedPings = 0
        watchdogJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                val conn = activeConnection ?: break
                if (pingDevice(conn)) {
                    missedPings = 0
                    if (_state.value is UsbLifecycleState.Degraded) {
                        _state.value = UsbLifecycleState.Connected(
                            deviceName = deviceName,
                            mode = mode,
                            brand = "Unknown",
                            endpoints = activeEndpoints!!
                        )
                    }
                } else {
                    missedPings++
                    if (missedPings >= MAX_MISSED_PINGS) {
                        closeInternal()
                        _state.value = UsbLifecycleState.Dead(deviceName, "No response")
                        break
                    } else {
                        _state.value = UsbLifecycleState.Degraded(deviceName, mode, missedPings, MAX_MISSED_PINGS)
                    }
                }
            }
        }
    }

    private fun pingDevice(conn: android.hardware.usb.UsbDeviceConnection): Boolean = try {
        val buf = ByteArray(2)
        val result = conn.controlTransfer(0x80, 0x00, 0, 0, buf, 2, 1000)
        result >= 0
    } catch (e: Exception) { false }

    private fun closeInternal() {
        watchdogJob?.cancel()
        watchdogJob = null
        missedPings = 0
        transferQueue = null
        try {
            activeInterface?.let { activeConnection?.releaseInterface(it) }
            activeConnection?.close()
        } catch (e: Exception) { } finally {
            activeConnection = null
            activeInterface = null
            activeEndpoints = null
        }
    }

    fun getTransferQueue() = transferQueue
    fun isConnected() = activeConnection != null
    fun getCurrentMode() = (state.value as? UsbLifecycleState.Connected)?.mode
    fun pauseWatchdog() { watchdogJob?.cancel() }
    fun resumeWatchdog() {
        val s = state.value
        if (s is UsbLifecycleState.Connected) startWatchdog(s.deviceName, s.mode)
    }

    fun destroy() {
        closeInternal()
        _state.value = UsbLifecycleState.Idle
    }
}
