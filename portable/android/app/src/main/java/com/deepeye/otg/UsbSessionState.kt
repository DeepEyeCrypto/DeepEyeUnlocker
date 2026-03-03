package com.deepeye.otg

data class UsbSessionState(
    val deviceKey: String? = null,          // "VID:PID:deviceId"
    val vid: Int? = null,
    val pid: Int? = null,
    val deviceId: Int? = null,
    val hasPermission: Boolean = false,
    val connectionFd: Int? = null,
    val protocol: ProtocolClass = ProtocolClass.UNKNOWN,
    val state: ConnState = ConnState.DISCONNECTED,
    val lastError: String? = null,
    val lastErrorAtMs: Long = 0L
)

enum class ConnState {
    DISCONNECTED,
    DEVICE_FOUND,
    PERMISSION_PENDING,
    PERMISSION_DENIED,
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