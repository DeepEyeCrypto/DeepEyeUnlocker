package com.deepeye.otg

/**
 * Immutable snapshot of the USB session. Single source of truth.
 * Emitted via StateFlow from UsbConnectionController.
 */
data class UsbSessionState(
    val deviceKey: String? = null,              // "VID:PID:deviceId"
    val vid: Int? = null,
    val pid: Int? = null,
    val deviceId: Int? = null,
    val hasPermission: Boolean = false,
    val connectionFd: Int? = null,
    val protocol: ProtocolClass = ProtocolClass.UNKNOWN,
    val state: ConnState = ConnState.DISCONNECTED,
    val lastError: String? = null,
    val lastErrorAtMs: Long = 0L,
    val physicalDeviceKey: PhysicalDeviceKey? = null,
    val reEnumCount: Int = 0                    // how many re-enumerations for this physical device
)

/**
 * Identity of a physical device that survives USB re-enumeration.
 * When an MTK device transitions preloader→BROM, Android assigns a new deviceId
 * but the VID:PID and serial remain the same. We match on these fields to detect
 * that the "new" UsbDevice is actually the same physical phone.
 */
data class PhysicalDeviceKey(
    val vid: Int,
    val pid: Int,
    val serialNumber: String    // UsbDevice.serialNumber or UsbDeviceConnection serial
)

enum class ConnState {
    DISCONNECTED,
    DEVICE_FOUND,
    PERMISSION_PENDING,
    PERMISSION_DENIED,
    REENUMERATION_WAIT,         // MTK mode-switch: old device gone, waiting for re-attach (2 s)
    CONNECTED_PROTOCOL_DETECT,
    CONNECTED_READY,
    CONNECTED_MTP_ONLY,
    ERROR
}

enum class ProtocolClass {
    UNKNOWN,
    MTK,
    QC,
    UNISOC,
    SAMSUNG,
    MTP_ONLY
}