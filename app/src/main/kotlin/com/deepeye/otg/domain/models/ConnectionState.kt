package com.deepeye.otg.domain.models

sealed class ConnectionState {
    object Idle : ConnectionState()
    object DeviceDetected : ConnectionState()
    object PermissionPending : ConnectionState()
    object PermissionDenied : ConnectionState()
    object Opening : ConnectionState()
    object Open : ConnectionState()
    object Ready : ConnectionState()
    object Busy : ConnectionState()
    object Recovering : ConnectionState()
    object Disconnected : ConnectionState()
    data class Failed(val errorCode: String, val reason: String) : ConnectionState()
}
