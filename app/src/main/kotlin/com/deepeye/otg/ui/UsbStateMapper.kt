package com.deepeye.otg.ui

import com.deepeye.otg.usb.UsbLifecycleState

/**
 * Maps UsbLifecycleState to UI-facing display model.
 */
data class UsbUiState(
    val statusLabel: String,
    val statusColor: Long,       // ARGB color value
    val showProgress: Boolean,
    val showConnectGuide: Boolean,
    val dotPulse: Boolean,
    val actionLabel: String?,
    val logPrefix: String
)

fun UsbLifecycleState.toUiState(): UsbUiState = when (this) {
    is UsbLifecycleState.Idle -> UsbUiState(
        "Waiting for device...", 0xFF6B7280, false, true, false,
        null, "[IDLE]"
    )
    is UsbLifecycleState.DeviceDetected -> UsbUiState(
        "Detected: $brand [$detectedMode]", 0xFF3B82F6, false,
        false, true, "Requesting permission...", "[DETECT]"
    )
    is UsbLifecycleState.PermissionPending -> UsbUiState(
        "Allow USB access...", 0xFFF59E0B, false, false, true,
        "Check dialog", "[PERM]"
    )
    is UsbLifecycleState.PermissionDenied -> UsbUiState(
        "Permission denied: $deviceName", 0xFFEF4444, false,
        true, false, "Reconnect & Allow", "[DENIED]"
    )
    is UsbLifecycleState.Connecting -> UsbUiState(
        "Connecting to ${device.productName}...", 0xFF8B5CF6,
        true, false, true, null, "[CONNECT]"
    )
    is UsbLifecycleState.Connected -> UsbUiState(
        "$deviceName • $mode", 0xFF059669, false, false, true,
        null, "[CONNECTED]"
    )
    is UsbLifecycleState.Degraded -> UsbUiState(
        "Unstable: $deviceName ($missedPings/$maxPings)",
        0xFFF59E0B, false, false, true, "Check cable", "[DEGRADED]"
    )
    is UsbLifecycleState.Dead -> UsbUiState(
        "Lost: $deviceName — $reason", 0xFFEF4444, false,
        true, false, "Reconnect device", "[DEAD]"
    )
    is UsbLifecycleState.Operating -> UsbUiState(
        "$operationName... ${(progress*100).toInt()}%",
        0xFF059669, true, false, true, null, "[OP]"
    )
    is UsbLifecycleState.Error -> UsbUiState(
        message, 0xFFEF4444, false, recoverable, false,
        if (recoverable) "Retry" else null, "[ERROR]"
    )
    is UsbLifecycleState.NoOtgSupport -> UsbUiState(
        "OTG not supported on this phone", 0xFFEF4444, false,
        false, false, null, "[NO_OTG]"
    )
}
