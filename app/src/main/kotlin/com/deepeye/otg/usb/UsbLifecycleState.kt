package com.deepeye.otg.usb

import android.hardware.usb.UsbDevice
import com.deepeye.otg.data.ConnectionMode

/**
 * Complete USB session state machine.
 */
sealed class UsbLifecycleState {

    // ── Pre-connection ───────────────────────────────────────
    object Idle : UsbLifecycleState()

    data class DeviceDetected(
        val device: UsbDevice,
        val detectedMode: ConnectionMode,
        val brand: String,
        val chipset: String
    ) : UsbLifecycleState()

    data class PermissionPending(
        val device: UsbDevice
    ) : UsbLifecycleState()

    data class PermissionDenied(
        val device: UsbDevice,
        val deviceName: String
    ) : UsbLifecycleState()

    data class Connecting(
        val device: UsbDevice,
        val mode: ConnectionMode
    ) : UsbLifecycleState()

    // ── Active session ───────────────────────────────────────
    data class Connected(
        val deviceName: String,
        val mode: ConnectionMode,
        val brand: String,
        val endpoints: ResolvedEndpoints,
        val sessionId: String = java.util.UUID.randomUUID().toString()
    ) : UsbLifecycleState()

    // ── Health degraded but still connected ──────────────────
    data class Degraded(
        val deviceName: String,
        val mode: ConnectionMode,
        val missedPings: Int,
        val maxPings: Int
    ) : UsbLifecycleState()

    // ── Connection unrecoverably lost ─────────────────────────
    data class Dead(
        val deviceName: String,
        val reason: String
    ) : UsbLifecycleState()

    // ── Operation in progress ─────────────────────────────────
    data class Operating(
        val deviceName: String,
        val mode: ConnectionMode,
        val operationName: String,
        val progress: Float    // 0.0f–1.0f
    ) : UsbLifecycleState()

    // ── Error ─────────────────────────────────────────────────
    data class Error(
        val message: String,
        val recoverable: Boolean = true
    ) : UsbLifecycleState()

    // ── No OTG support ────────────────────────────────────────
    object NoOtgSupport : UsbLifecycleState()

    // ── Helpers ───────────────────────────────────────────────
    val isConnected: Boolean get() = this is Connected || this is Operating || this is Degraded
    val isIdle: Boolean get() = this is Idle
    val hasError: Boolean get() = this is Error || this is Dead || this is NoOtgSupport
}
