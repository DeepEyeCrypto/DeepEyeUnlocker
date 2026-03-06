package com.deepeye.otg

sealed class UsbSessionState {
    object Idle : UsbSessionState()
    
    data class DeviceFound(
        val deviceName: String,
        val vid: Int,
        val pid: Int
    ) : UsbSessionState()

    data class PermissionPending(val deviceName: String) : UsbSessionState()

    data class ConnectedReady(
        val deviceName: String
    ) : UsbSessionState()

    data class Error(
        val message: String
    ) : UsbSessionState()
}

enum class ConnState {
    DISCONNECTED,
    DEVICE_FOUND,
    PERMISSION_PENDING,
    PERMISSION_DENIED,
    REENUMERATION_WAIT,
    CONNECTED_PROTOCOL_DETECT,
    CONNECTED_READY,
    CONNECTED_MTP_ONLY,
    ERROR
}