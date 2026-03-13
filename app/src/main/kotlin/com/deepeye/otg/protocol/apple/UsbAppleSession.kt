package com.deepeye.otg.protocol.apple

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import com.deepeye.otg.protocol.apple.model.AppleDeviceMode
import com.deepeye.otg.protocol.apple.model.AppleDeviceProfile
import com.deepeye.otg.protocol.apple.model.PairingState
import com.deepeye.otg.usb.UsbLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

// ──────────────────────────────────────────────────────────────
// USB Apple Session — Device Enumeration + Metadata Collection
// DeepEye OTG — Protocol / Apple Module (Part 3)
// ──────────────────────────────────────────────────────────────

private const val TAG = "UsbAppleSession"

/**
 * Session lifecycle state.
 */
enum class AppleSessionState {
    DISCONNECTED,
    ENUMERATING,
    CONNECTED,
    QUERYING_METADATA,
    READY,
    ERROR
}

/**
 * Session event for structured logging and trace recording.
 */
data class AppleSessionEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val type: String,       // e.g. "CONNECT", "ENUM", "QUERY", "DISCONNECT", "ERROR"
    val message: String,
    val data: Map<String, String> = emptyMap()
)

/**
 * Manages a USB Host session with a connected Apple device.
 *
 * Responsibilities:
 * - USB device enumeration and Apple VID filtering
 * - Connection lifecycle management
 * - Device profile construction from USB descriptors
 * - Metadata querying (where legitimately accessible)
 * - Structured event logging for trace recording
 *
 * Design:
 * - Coroutine-based, no blocking on main thread
 * - All operations are read-only / non-destructive
 * - Events emitted as Flow for UI and TraceRecorder consumption
 * - No bypass, exploit, or unauthorized access logic
 */
class UsbAppleSession(
    private val usbManager: UsbManager,
    private val traceRecorder: TraceRecorder? = null
) {
    private val _state = MutableStateFlow(AppleSessionState.DISCONNECTED)
    val state: StateFlow<AppleSessionState> = _state.asStateFlow()

    private val _profile = MutableStateFlow<AppleDeviceProfile?>(null)
    val profile: StateFlow<AppleDeviceProfile?> = _profile.asStateFlow()

    private val _events = MutableStateFlow<List<AppleSessionEvent>>(emptyList())
    val events: StateFlow<List<AppleSessionEvent>> = _events.asStateFlow()

    private var activeConnection: UsbDeviceConnection? = null
    private var activeDevice: UsbDevice? = null

    // ── Enumeration ─────────────────────────────────────────────

    /**
     * Enumerate all connected USB devices and filter for Apple devices.
     *
     * @return list of detected Apple device profiles
     */
    suspend fun enumerateAppleDevices(): List<AppleDeviceProfile> =
        withContext(Dispatchers.IO) {
            _state.value = AppleSessionState.ENUMERATING
            emitEvent("ENUM", "Starting Apple device enumeration")

            val devices = usbManager.deviceList.values
            Log.i(TAG, "USB bus: ${devices.size} device(s) connected")

            val appleDevices = devices.filter {
                it.vendorId == AppleDeviceProfile.APPLE_VID
            }

            Log.i(TAG, "Found ${appleDevices.size} Apple device(s)")

            val profiles = appleDevices.map { device ->
                buildProfileFromUsbDevice(device)
            }

            for (p in profiles) {
                emitEvent("ENUM", "Detected: ${p.displayName}", mapOf(
                    "vid" to "0x${p.usbVendorId.toString(16).uppercase()}",
                    "pid" to "0x${p.usbProductId.toString(16).uppercase()}",
                    "mode" to p.deviceMode.name,
                    "serial" to (p.usbSerialNumber ?: "N/A")
                ))
                UsbLogger.info(TAG, "Apple device: ${p.displayName} " +
                        "[PID=0x${p.usbProductId.toString(16)}] " +
                        "mode=${p.deviceMode}")
            }

            _state.value = if (profiles.isEmpty()) {
                AppleSessionState.DISCONNECTED
            } else {
                AppleSessionState.CONNECTED
            }

            profiles
        }

    /**
     * Enumerate and return all connected devices (not just Apple).
     * Useful for connection-state logging and research.
     */
    suspend fun enumerateAllDevices(): List<UsbDeviceInfo> =
        withContext(Dispatchers.IO) {
            usbManager.deviceList.values.map { device ->
                UsbDeviceInfo(
                    vendorId = device.vendorId,
                    productId = device.productId,
                    deviceName = device.deviceName,
                    productName = device.productName ?: "Unknown",
                    manufacturerName = device.manufacturerName ?: "Unknown",
                    serialNumber = device.serialNumber,
                    deviceClass = device.deviceClass,
                    deviceSubclass = device.deviceSubclass,
                    interfaceCount = device.interfaceCount,
                    isApple = device.vendorId == AppleDeviceProfile.APPLE_VID,
                    enumeratedAt = System.currentTimeMillis()
                )
            }
        }

    // ── Connection ──────────────────────────────────────────────

    /**
     * Open a USB connection to a specific Apple device.
     *
     * @param device the [UsbDevice] to connect to (must have USB permission)
     * @return the constructed [AppleDeviceProfile], or null on failure
     */
    suspend fun connect(device: UsbDevice): AppleDeviceProfile? =
        withContext(Dispatchers.IO) {
            try {
                if (device.vendorId != AppleDeviceProfile.APPLE_VID) {
                    emitEvent("ERROR", "Not an Apple device: VID=0x${device.vendorId.toString(16)}")
                    return@withContext null
                }

                emitEvent("CONNECT", "Opening USB connection to ${device.deviceName}")
                _state.value = AppleSessionState.CONNECTED

                val connection = usbManager.openDevice(device)
                if (connection == null) {
                    emitEvent("ERROR", "Failed to open device — permission denied or device gone")
                    _state.value = AppleSessionState.ERROR
                    return@withContext null
                }

                activeConnection = connection
                activeDevice = device

                val profile = buildProfileFromUsbDevice(device, connection)
                _profile.value = profile

                emitEvent("CONNECT", "Connected to ${profile.displayName}", mapOf(
                    "mode" to profile.deviceMode.name,
                    "udid" to (profile.udid ?: "N/A"),
                    "ecid" to (profile.ecid ?: "N/A"),
                    "ios" to (profile.iosVersion ?: "N/A")
                ))

                UsbLogger.info(TAG, "Connected: ${profile.displayName}")

                // Try to query extended metadata
                _state.value = AppleSessionState.QUERYING_METADATA
                val enriched = queryExtendedMetadata(profile, connection)
                _profile.value = enriched
                _state.value = AppleSessionState.READY

                enriched
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                emitEvent("ERROR", "Connection failed: ${e.message}")
                UsbLogger.error(TAG, "Connection failed", e)
                _state.value = AppleSessionState.ERROR
                null
            }
        }

    /**
     * Disconnect and release resources.
     */
    fun disconnect() {
        emitEvent("DISCONNECT", "Closing Apple USB session")
        try {
            activeConnection?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing connection", e)
        }
        activeConnection = null
        activeDevice = null
        _profile.value = null
        _state.value = AppleSessionState.DISCONNECTED
        UsbLogger.info(TAG, "Disconnected")
    }

    // ── Profile Building ────────────────────────────────────────

    /**
     * Build an [AppleDeviceProfile] from USB descriptor data.
     */
    private fun buildProfileFromUsbDevice(
        device: UsbDevice,
        connection: UsbDeviceConnection? = null
    ): AppleDeviceProfile {
        // Try to read string descriptors if we have a connection
        val serial = connection?.serial ?: device.serialNumber
        val product = device.productName
        val manufacturer = device.manufacturerName

        return AppleDeviceProfile.fromUsbDescriptor(
            vendorId = device.vendorId,
            productId = device.productId,
            serialNumber = serial,
            productString = product,
            manufacturerString = manufacturer
        )
    }

    /**
     * Attempt to query extended metadata via standard protocols.
     * Only gathers data from legitimately accessible interfaces.
     *
     * In DFU/Recovery: parse serial string fields.
     * In Normal mode: attempt lockdown-style device info query.
     */
    private suspend fun queryExtendedMetadata(
        profile: AppleDeviceProfile,
        connection: UsbDeviceConnection
    ): AppleDeviceProfile {
        return when (profile.deviceMode) {
            AppleDeviceMode.DFU, AppleDeviceMode.RECOVERY -> {
                // Already parsed from serial in fromUsbDescriptor
                emitEvent("QUERY", "DFU/Recovery metadata from serial string")
                profile
            }
            AppleDeviceMode.NORMAL -> {
                emitEvent("QUERY", "Normal mode — attempting device info query")
                // In normal mode, full device info requires pairing.
                // Without pairing, we can only see USB descriptors.
                // Mark pairing state appropriately.
                profile.copy(
                    pairingState = PairingState.UNKNOWN,
                    observationSource = "usb_host_normal_mode"
                )
            }
            else -> {
                emitEvent("QUERY", "Unknown mode — limited metadata available")
                profile
            }
        }
    }

    // ── Event Management ────────────────────────────────────────

    private fun emitEvent(type: String, message: String, data: Map<String, String> = emptyMap()) {
        val event = AppleSessionEvent(
            type = type,
            message = message,
            data = data
        )
        val current = _events.value.toMutableList()
        current.add(event)
        // Keep last 1000 events in memory
        if (current.size > 1000) {
            current.removeAt(0)
        }
        _events.value = current

        // Forward to trace recorder if attached
        traceRecorder?.recordEvent(event)

        Log.d(TAG, "[$type] $message")
    }

    /**
     * Get current session events for export.
     */
    fun getSessionEvents(): List<AppleSessionEvent> = _events.value

    /**
     * Clear session event history.
     */
    fun clearEvents() {
        _events.value = emptyList()
    }
}

// ──────────────────────────────────────────────────────────────
// Supporting Data Classes
// ──────────────────────────────────────────────────────────────

/**
 * Generic USB device info for enumeration logs.
 */
data class UsbDeviceInfo(
    val vendorId: Int,
    val productId: Int,
    val deviceName: String,
    val productName: String,
    val manufacturerName: String,
    val serialNumber: String?,
    val deviceClass: Int,
    val deviceSubclass: Int,
    val interfaceCount: Int,
    val isApple: Boolean,
    val enumeratedAt: Long
) {
    val vidPidHex: String
        get() = "VID:0x${vendorId.toString(16).uppercase()} PID:0x${productId.toString(16).uppercase()}"
}
