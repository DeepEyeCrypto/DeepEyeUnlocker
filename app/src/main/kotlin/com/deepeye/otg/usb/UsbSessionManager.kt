package com.deepeye.otg.usb

import android.util.Log
import android.hardware.usb.*
import android.content.Context
import com.deepeye.otg.data.ConnectionMode
import com.deepeye.otg.data.UsbDeviceDatabase
import com.deepeye.otg.data.UsbDeviceSignature
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class UsbConnectionEvent {
    data class DeviceDetected(
        val device: UsbDevice,
        val signature: UsbDeviceSignature?,
        val detectedMode: ConnectionMode
    ) : UsbConnectionEvent()
    data class DevicePermissionGranted(val device: UsbDevice) : UsbConnectionEvent()
    data class DevicePermissionDenied(val device: UsbDevice) : UsbConnectionEvent()
    data class DeviceDisconnected(val deviceName: String) : UsbConnectionEvent()
    data class ConnectionOpened(
        val device: UsbDevice,
        val connection: UsbDeviceConnection,
        val mode: ConnectionMode
    ) : UsbConnectionEvent()
    data class ConnectionFailed(val reason: String) : UsbConnectionEvent()
    object NoOtgSupport : UsbConnectionEvent()
}

/**
 * Production-stable USB connection layer with Mutex-serialization, 
 * active disconnect watchdog and queue-based transfers.
 */
class UsbSessionManager(
    private val context: Context,
    private val usbManager: UsbManager
) {
    companion object {
        const val ACTION_USB_PERMISSION = "com.deepeye.otg.USB_PERMISSION"
        private const val TAG = "DeepEye-UsbMgr"
    }

    // ── State ─────────────────────────────────────────────────
    private val _events = MutableSharedFlow<UsbConnectionEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<UsbConnectionEvent> = _events.asSharedFlow()

    private val connectMutex = Mutex()
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var activeDevice: UsbDevice? = null
    private var activeConnection: UsbDeviceConnection? = null
    private var activeEndpoints: ResolvedEndpoints? = null
    
    private var transferQueue: UsbTransferQueue? = null
    private var watchdog: UsbConnectionWatchdog? = null
    private val protocolDetector = ProtocolDetector()

    @Volatile private var isInitialized = false

    // ── Init ──────────────────────────────────────────────────
    suspend fun initAsync() = withContext(Dispatchers.IO) {
        if (!context.packageManager.hasSystemFeature("android.hardware.usb.host")) {
            UsbLogger.error(TAG, "USB host NOT supported on this device")
            _events.emit(UsbConnectionEvent.NoOtgSupport)
            return@withContext
        }

        usbManager.deviceList.values.forEach { device ->
            processDetectedDevice(device)
        }
        isInitialized = true
    }

    suspend fun onDeviceAttached(device: UsbDevice) = withContext(Dispatchers.IO) {
        UsbLogger.info(TAG, "USB Phys Attached: ${device.productName}")
        processDetectedDevice(device)
    }

    private suspend fun processDetectedDevice(device: UsbDevice) {
        val vid = device.vendorId
        val pid = device.productId
        val signature = UsbDeviceDatabase.detect(vid, pid)
        val detectedMode = detectModeFromDescriptors(device)

        _events.emit(UsbConnectionEvent.DeviceDetected(device, signature, detectedMode))

        if (!usbManager.hasPermission(device)) {
            UsbPermissionGuard.requestPermission(context, usbManager, device, ACTION_USB_PERMISSION)
        } else {
            openConnection(device, detectedMode)
        }
    }

    suspend fun onPermissionResult(device: UsbDevice, granted: Boolean) = withContext(Dispatchers.IO) {
        if (granted) {
            _events.emit(UsbConnectionEvent.DevicePermissionGranted(device))
            val mode = detectModeFromDescriptors(device)
            openConnection(device, mode)
        } else {
            _events.emit(UsbConnectionEvent.DevicePermissionDenied(device))
        }
    }

    private fun detectModeFromDescriptors(device: UsbDevice): ConnectionMode {
        val snapshot = UsbSnapshotFactory.from(device)
        val detection = protocolDetector.detect(snapshot)
        return detection.toConnectionMode()
    }

    // ── Safe Lifecycle Management ──────────────────────────────
    private suspend fun openConnection(device: UsbDevice, mode: ConnectionMode) = withContext(Dispatchers.IO) {
        connectMutex.withLock {
            try {
                closeDeviceInternal()

                val connection = UsbPermissionGuard.safeOpenDevice(usbManager, device)
                    ?: run {
                        _events.emit(UsbConnectionEvent.ConnectionFailed("Safe open failed"))
                        return@withContext
                    }

                val endpoints = UsbEndpointResolver.resolve(device, mode)
                if (endpoints == null || !UsbEndpointResolver.validate(endpoints, mode)) {
                    _events.emit(UsbConnectionEvent.ConnectionFailed("Endpoint resolution failed"))
                    connection.close()
                    return@withContext
                }

                if (!connection.claimInterface(endpoints.usbInterface, true)) {
                    UsbLogger.warn(TAG, "Exclusive interface claim failed (force claim still alive)")
                }

                activeDevice = device
                activeConnection = connection
                activeEndpoints = endpoints
                
                // Initialize Serial Transfer Queue
                val queue = UsbTransferQueue(connection, endpoints)
                transferQueue = queue

                // Setup Health Watchdog
                watchdog?.stop()
                val dog = UsbConnectionWatchdog(
                    scope = sessionScope,
                    pingProvider = { pingDevice(connection) },
                    disconnectHandler = { onDeviceDetached(device) }
                )
                dog.start()
                watchdog = dog

                UsbLogger.info(TAG, "SESSION ACTIVE: ${device.productName} via $mode")
                _events.emit(UsbConnectionEvent.ConnectionOpened(device, connection, mode))

            } catch (e: Exception) {
                UsbLogger.error(TAG, "OPEN FAILED: ${e.message}", e)
                _events.emit(UsbConnectionEvent.ConnectionFailed(e.message ?: "USB Error"))
            }
        }
    }

    private fun pingDevice(connection: UsbDeviceConnection): Boolean {
        return try {
            val buf = ByteArray(2)
            val result = connection.controlTransfer(0x80, 0x00, 0, 0, buf, 2, 1000)
            result >= 0
        } catch (e: Exception) { false }
    }

    suspend fun onDeviceDetached(device: UsbDevice) = withContext(Dispatchers.IO) {
        if (activeDevice?.deviceId == device.deviceId) {
            closeDevice()
            _events.emit(UsbConnectionEvent.DeviceDisconnected(device.productName ?: "Generic Device"))
        }
    }

    /**
     * Public thread-safe close. Can be called from any thread.
     */
    fun closeDevice() {
        sessionScope.launch(Dispatchers.IO) {
            connectMutex.withLock {
                closeDeviceInternal()
            }
        }
    }

    private fun closeDeviceInternal() {
        try {
            watchdog?.stop()
            activeEndpoints?.let { activeConnection?.releaseInterface(it.usbInterface) }
            activeConnection?.close()
        } catch (e: Exception) {
            Log.d(TAG, "Cleanup exception: ${e.message}")
        } finally {
            activeDevice = null
            activeConnection = null
            activeEndpoints = null
            transferQueue = null
            watchdog = null
        }
    }

    // ── Data API (Queue Aware) ────────────────────────────────
    suspend fun write(data: ByteArray) = transferQueue?.write(data) 
        ?: TransferResult(false, 0, null, "Not connected")

    suspend fun read(size: Int = 512) = transferQueue?.read(size)
        ?: TransferResult(false, 0, null, "Not connected")

    suspend fun exchange(cmd: ByteArray, respSize: Int = 512) = transferQueue?.exchange(cmd, respSize)
        ?: Pair(TransferResult(false, 0, null, "Not connected"), TransferResult(false, 0, null, "Not connected"))

    fun getConnectionHealth(): StateFlow<ConnectionHealth>? = watchdog?.health

    fun isConnected() = activeConnection != null
    fun getActiveConnection() = activeConnection
}
