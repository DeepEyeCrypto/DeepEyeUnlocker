package com.deepeye.otg.usb

import android.util.Log
import android.hardware.usb.*
import android.content.Context
import com.deepeye.otg.data.ConnectionMode
import com.deepeye.otg.data.UsbDeviceDatabase
import com.deepeye.otg.data.UsbDeviceSignature
import com.deepeye.otg.logging.SafeLog
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
@javax.inject.Singleton
class UsbSessionManager @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val usbManager: android.hardware.usb.UsbManager
) {
    companion object {
        private const val TAG = "DeepEye-UsbMgr"
    }

    // ── State ─────────────────────────────────────────────────
    private val _events = MutableSharedFlow<UsbConnectionEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<UsbConnectionEvent> = _events.asSharedFlow()

    private val connectMutex = Mutex()
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var currentUsbDevice: UsbDevice? = null
        private set

    private var activeConnection: UsbDeviceConnection? = null
    private var activeEndpoints: ResolvedEndpoints? = null
    private var activeDeviceKey: String? = null
    
    private var activeTransport: BulkTransport? = null
    private var watchdog: UsbConnectionWatchdog? = null
    private val protocolDetector = ProtocolDetector()

    @Volatile private var isInitialized = false

    // ── Init ──────────────────────────────────────────────────
    suspend fun initAsync() = withContext(Dispatchers.IO) {
        if (!context.packageManager.hasSystemFeature("android.hardware.usb.host")) {
            UsbLogger.error(TAG, "[MODE] otg-unsupported reason=\"USB host NOT supported on this device\"")
            _events.emit(UsbConnectionEvent.NoOtgSupport)
            return@withContext
        }

        usbManager.deviceList.values.forEach { device ->
            processDetectedDevice(device)
        }
        isInitialized = true
    }

    suspend fun onDeviceAttached(device: UsbDevice) = withContext(Dispatchers.IO) {
        UsbLogger.info(TAG, "[MODE] phys-attach product=\"${device.productName}\" vid=0x${"%04X".format(device.vendorId)} pid=0x${"%04X".format(device.productId)}")
        processDetectedDevice(device)
    }

    private fun deviceKey(device: UsbDevice): String =
        "${device.vendorId}:${device.productId}:${device.deviceId}"

    private suspend fun processDetectedDevice(device: UsbDevice) {
        val vid = device.vendorId
        val pid = device.productId
        val signature = UsbDeviceDatabase.detect(vid, pid)
        val detectedMode = detectModeFromDescriptors(device)

        _events.emit(UsbConnectionEvent.DeviceDetected(device, signature, detectedMode))

        if (!usbManager.hasPermission(device)) {
            UsbPermissionGuard.requestPermission(
                context,
                usbManager,
                device,
                UsbPermissionGuard.ACTION_USB_PERMISSION
            )
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
                closeSessionInternal()

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

                currentUsbDevice = device
                activeConnection = connection
                activeEndpoints = endpoints
                activeDeviceKey = deviceKey(device)
                
                // Initialize Serial Transfer Queue
                val snapshot = UsbSnapshotFactory.from(device)
                activeTransport = BulkTransport(connection, endpoints, snapshot)

                // Setup Health Watchdog
                watchdog?.stop()
                val dog = UsbConnectionWatchdog(
                    scope = sessionScope,
                    pingProvider = { pingDevice(connection) },
                    disconnectHandler = { onDeviceDetached(device) }
                )
                dog.start()
                watchdog = dog

        UsbLogger.info(
            TAG,
            "[MODE] session-opened key=${deviceKey(device)} mode=$mode product=\"${device.productName}\""
        )
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
        val detachedKey = deviceKey(device)
        if (activeDeviceKey == detachedKey) {
            UsbLogger.info(TAG, "[MODE] phys-detach key=$detachedKey")
            closeDevice()
            _events.emit(UsbConnectionEvent.DeviceDisconnected(device.productName ?: "Generic Device"))
        } else {
            UsbLogger.info(TAG, "[MODE] phys-detach-ignored key=$detachedKey active=$activeDeviceKey")
        }
    }

    /**
     * Public thread-safe close. Can be called from any thread.
     */
    fun closeDevice() {
        sessionScope.launch(Dispatchers.IO) {
            connectMutex.withLock {
                closeSessionInternal()
            }
        }
    }

    private fun closeSessionInternal() {
        try {
            watchdog?.stop()
            activeEndpoints?.let { activeConnection?.releaseInterface(it.usbInterface) }
            activeConnection?.close()
        } catch (e: Exception) {
            SafeLog.d(TAG, "Cleanup exception: ${e.message}")
        } finally {
            currentUsbDevice = null
            activeConnection = null
            activeEndpoints = null
            activeTransport = null
            watchdog = null
            activeDeviceKey = null
        }
    }

    // ── Data API (Queue Aware) ────────────────────────────────
    suspend fun write(data: ByteArray) = activeTransport?.write(data) 
        ?: TransferResult.IOError("Not connected")
 
    suspend fun read(size: Int = 512) = activeTransport?.read(size)
        ?: TransferResult.IOError("Not connected")
 
    suspend fun exchange(cmd: ByteArray, respSize: Int = 512) = activeTransport?.exchange(cmd, respSize)
        ?: Pair(TransferResult.IOError("Not connected"), TransferResult.IOError("Not connected"))

    fun getConnectionHealth(): StateFlow<ConnectionHealth>? = watchdog?.health

    fun isConnected() = activeConnection != null
    fun getActiveConnection() = activeConnection
}
