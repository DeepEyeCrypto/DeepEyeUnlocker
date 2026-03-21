package com.deepeye.otg.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import kotlinx.coroutines.Job

/**
 * Stage 200.1 — Device Session Container.
 * Encapsulates a single active hardware session.
 */
data class DeviceSession(
    val device: UsbDevice,
    val connection: UsbDeviceConnection,
    val usbInterface: UsbInterface,
    val endpoints: ResolvedEndpoints,
    val transport: BulkTransport,
    val sessionId: String,
    val deviceKey: String,
    val detection: DetectionResult,
    val snapshot: UsbDescriptorSnapshot,
    var watchdogJob: Job? = null,
    var missedPings: Int = 0
) {
    fun close() {
        watchdogJob?.cancel()
        transport.close()
    }
}
