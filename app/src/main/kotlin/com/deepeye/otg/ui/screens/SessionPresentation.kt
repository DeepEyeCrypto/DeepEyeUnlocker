package com.deepeye.otg.ui.screens

import androidx.compose.ui.graphics.Color
import com.deepeye.otg.ui.components.DeviceField
import com.deepeye.otg.ui.components.StatusIndicatorState
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.usb.UsbLifecycleState

data class SessionPresentation(
    val title: String,
    val subtitle: String,
    val badge: String,
    val accent: Color,
    val status: StatusIndicatorState,
    val fields: List<DeviceField>,
)

fun sessionPresentation(state: UsbLifecycleState): SessionPresentation = when (state) {
    is UsbLifecycleState.Connected -> SessionPresentation(
        title = state.deviceName,
        subtitle = "${state.brand} • ${state.chipset} • ${state.protocolFamily.name.replace('_', ' ')}",
        badge = state.brand.uppercase(),
        accent = when {
            state.protocolFamily.name.contains("APPLE") -> DeepEyeColors.PurpleDim
            state.protocolFamily.name.contains("MTK") || state.protocolFamily.name.contains("BROM") -> DeepEyeColors.Success
            state.protocolFamily.name.contains("QC") || state.protocolFamily.name.contains("EDL") -> DeepEyeColors.Warning
            else -> DeepEyeColors.PrimaryCyan
        },
        status = StatusIndicatorState.CONNECTED,
        fields = listOf(
            DeviceField("Model", state.deviceName),
            DeviceField("Chipset", state.chipset),
            DeviceField("VID / PID", "0x${state.vendorId.toString(16).uppercase()} / 0x${state.productId.toString(16).uppercase()}"),
            DeviceField("Session", state.sessionId.takeLast(8).uppercase()),
        ),
    )

    is UsbLifecycleState.DeviceDetected -> SessionPresentation(
        title = if (state.brand.isBlank()) "Device Detected" else state.brand,
        subtitle = "${state.chipset} • confidence ${state.confidence}%",
        badge = state.protocolFamily.name.replace('_', ' '),
        accent = DeepEyeColors.PrimaryCyan,
        status = StatusIndicatorState.SCANNING,
        fields = listOf(
            DeviceField("Brand", state.brand),
            DeviceField("Chipset", state.chipset),
            DeviceField("VID / PID", "0x${state.vendorId.toString(16).uppercase()} / 0x${state.productId.toString(16).uppercase()}"),
            DeviceField("Reason", state.detectionReason),
        ),
    )

    is UsbLifecycleState.Connecting -> SessionPresentation(
        title = "Connecting",
        subtitle = "Negotiating ${state.protocolFamily.name.replace('_', ' ')} transport",
        badge = state.mode.name.replace('_', ' '),
        accent = DeepEyeColors.PrimaryCyan,
        status = StatusIndicatorState.SCANNING,
        fields = listOf(
            DeviceField("Mode", state.mode.name.replace('_', ' ')),
            DeviceField("Protocol", state.protocolFamily.name.replace('_', ' ')),
            DeviceField("Device Key", state.deviceKey.takeLast(8).uppercase()),
            DeviceField("USB", "0x${state.device.vendorId.toString(16).uppercase()} / 0x${state.device.productId.toString(16).uppercase()}"),
        ),
    )

    is UsbLifecycleState.PermissionPending -> SessionPresentation(
        title = "USB Permission Required",
        subtitle = "Grant OTG access to continue scanning",
        badge = "PENDING",
        accent = DeepEyeColors.Warning,
        status = StatusIndicatorState.SCANNING,
        fields = listOf(
            DeviceField("VID / PID", "0x${state.device.vendorId.toString(16).uppercase()} / 0x${state.device.productId.toString(16).uppercase()}"),
            DeviceField("Device", state.device.deviceName.takeLast(12)),
        ),
    )

    is UsbLifecycleState.PermissionDenied -> SessionPresentation(
        title = "Permission Denied",
        subtitle = state.deviceName,
        badge = "DENIED",
        accent = DeepEyeColors.Error,
        status = StatusIndicatorState.ERROR,
        fields = listOf(
            DeviceField("VID / PID", "0x${state.device.vendorId.toString(16).uppercase()} / 0x${state.device.productId.toString(16).uppercase()}"),
            DeviceField("Device", state.deviceName),
        ),
    )

    is UsbLifecycleState.Error -> SessionPresentation(
        title = "Connection Error",
        subtitle = state.message,
        badge = "FAULT",
        accent = DeepEyeColors.Error,
        status = StatusIndicatorState.ERROR,
        fields = listOf(DeviceField("Status", if (state.recoverable) "Recoverable" else "Terminal")),
    )

    is UsbLifecycleState.Dead -> SessionPresentation(
        title = "Session Lost",
        subtitle = state.reason,
        badge = "OFFLINE",
        accent = DeepEyeColors.Error,
        status = StatusIndicatorState.ERROR,
        fields = listOf(DeviceField("Device", state.deviceName)),
    )

    is UsbLifecycleState.NoOtgSupport -> SessionPresentation(
        title = "OTG Not Supported",
        subtitle = "USB host mode is unavailable on this handset",
        badge = "HOST OFF",
        accent = DeepEyeColors.Warning,
        status = StatusIndicatorState.ERROR,
        fields = emptyList(),
    )

    else -> SessionPresentation(
        title = "Scanning for device",
        subtitle = "Connect via USB OTG to enumerate BROM, EDL, ADB, or Apple maintenance modes",
        badge = "IDLE",
        accent = DeepEyeColors.PrimaryCyan,
        status = StatusIndicatorState.DISCONNECTED,
        fields = emptyList(),
    )
}
