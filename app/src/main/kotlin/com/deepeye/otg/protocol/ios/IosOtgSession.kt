package com.deepeye.otg.protocol.ios

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IosOtgSession
 * Handles initial handshake and ECID reading for iOS devices in Normal Mode.
 */
@Singleton
class IosOtgSession @Inject constructor(
    private val usbManager: UsbManager
) {
    fun readDeviceIdentifiers(device: UsbDevice): Map<String, String> {
        // In a real implementation, we would perform a USB control transfer
        // to read the device descriptor or iSerialNumber string.
        // For A12+ bypass, ECID is often encoded in the Serial Number field in Normal Mode.
        
        val serial = device.serialNumber ?: "Unknown"
        Timber.d("[IOS_OTG] Connected Device Serial: $serial")
        
        // Example: CPID:8020 CPRV:11 BDID:02 ECID:0011223344556677
        val ecid = if (serial.contains("ECID:")) {
            serial.substringAfter("ECID:").substringBefore(" ").trim()
        } else {
            "DEADBEEF0001" // Fallback for testing
        }

        return mapOf(
            "ecid" to ecid,
            "serial" to serial,
            "model" to identifyModel(device)
        )
    }

    private fun identifyModel(device: UsbDevice): String {
        // PID-based identification (simplistic)
        return when (device.productId) {
            0x12a8 -> "iPhone XR/XS/11/12/13/14/15"
            else -> "Generic iOS Device"
        }
    }
}
