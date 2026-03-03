package com.deepeye.otg

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.util.Log

enum class DetectedProtocol {
    UNKNOWN,
    QUALCOMM_EDL,
    MTK_BROM,
    MTK_PRELOADER,
    FASTBOOT,
    SAMSUNG_ODIN,
    MTP_ONLY
}

data class ProtocolDetectionResult(
    val protocol: DetectedProtocol,
    val ifaceDump: String
)

class ProtocolProbe(private val connection: UsbDeviceConnection, private val device: UsbDevice) {

    private val TAG = "DeepEye-Probe"
    private var bulkIn: UsbEndpoint? = null
    private var bulkOut: UsbEndpoint? = null

    fun detect(): ProtocolDetectionResult {
        Log.i(TAG, "[PROTO] Starting protocol probe for ${device.vendorId}:${device.productId}")

        val ifaceDescriptions = mutableListOf<String>()
        var classifiedMtpOnly = true

        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            val desc = "iface[$i]: class=${iface.interfaceClass},sub=${iface.interfaceSubclass},proto=${iface.interfaceProtocol}, eps=${iface.endpointCount}"
            ifaceDescriptions.add(desc)

            var hasBulkIn = false
            var hasBulkOut = false
            var tmpBulkIn: UsbEndpoint? = null
            var tmpBulkOut: UsbEndpoint? = null

            for (e in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(e)
                val epDesc = "ep${e}:${dir(ep)}-${type(ep)}"
                ifaceDescriptions.add("  - $epDesc")
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == UsbConstants.USB_DIR_IN) {
                        hasBulkIn = true; tmpBulkIn = ep
                    } else {
                        hasBulkOut = true; tmpBulkOut = ep
                    }
                }
            }

            // Heuristic: only treat as MTP-friendly if class is still image (6) or MSC (8) and no bulk pair suitable for probes
            if (!isMtpClass(iface)) {
                classifiedMtpOnly = false
            }

            // Prefer the first interface with bulk IN/OUT for probing
            if (bulkIn == null && hasBulkIn && hasBulkOut) {
                bulkIn = tmpBulkIn
                bulkOut = tmpBulkOut
            }
        }

        val ifaceDump = ifaceDescriptions.joinToString(separator = "\n")

        if (bulkIn == null || bulkOut == null) {
            return ProtocolDetectionResult(
                protocol = if (classifiedMtpOnly) DetectedProtocol.MTP_ONLY else DetectedProtocol.UNKNOWN,
                ifaceDump = ifaceDump
            )
        }

        // 1. FASTBOOT Probe (Safe ASCII)
        if (probeFastboot()) return ProtocolDetectionResult(DetectedProtocol.FASTBOOT, ifaceDump)

        // 2. MTK BROM Probe (Sync bytes)
        if (probeMtkBrom()) return ProtocolDetectionResult(DetectedProtocol.MTK_BROM, ifaceDump)

        // 3. Qualcomm EDL Probe (Sahara Hello)
        if (probeQualcommSahara()) return ProtocolDetectionResult(DetectedProtocol.QUALCOMM_EDL, ifaceDump)

        // 4. Samsung Odin TODO

        return ProtocolDetectionResult(
            protocol = if (classifiedMtpOnly) DetectedProtocol.MTP_ONLY else DetectedProtocol.UNKNOWN,
            ifaceDump = ifaceDump
        )
    }

    private fun isMtpClass(iface: UsbInterface): Boolean {
        return iface.interfaceClass == UsbConstants.USB_CLASS_STILL_IMAGE || iface.interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE
    }

    private fun dir(ep: UsbEndpoint): String = if (ep.direction == UsbConstants.USB_DIR_IN) "IN" else "OUT"
    private fun type(ep: UsbEndpoint): String = when (ep.type) {
        UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
        UsbConstants.USB_ENDPOINT_XFER_INT -> "INT"
        UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISO"
        else -> "CTRL"
    }

    private fun probeFastboot(): Boolean {
        try {
            val cmd = "getvar:version".toByteArray()
            val buffer = ByteArray(64)
            
            // Send
            connection.bulkTransfer(bulkOut, cmd, cmd.size, 150)
            
            // Read
            val len = connection.bulkTransfer(bulkIn, buffer, buffer.size, 150)
            if (len > 0) {
                val response = String(buffer, 0, len)
                Log.d(TAG, "[PROTO] Fastboot probe response: $response")
                if (response.startsWith("OKAY") || response.startsWith("FAIL")) return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "[PROTO] Fastboot probe failed: ${e.message}")
        }
        return false
    }

    private fun probeMtkBrom(): Boolean {
        try {
            // MTK Start Cmd: A0 0A 50 05
            val cmd = byteArrayOf(0xA0.toByte(), 0x0A, 0x50, 0x05)
            val buffer = ByteArray(64)
            
            connection.bulkTransfer(bulkOut, cmd, cmd.size, 200)
            val len = connection.bulkTransfer(bulkIn, buffer, buffer.size, 200)
            
            if (len > 0) {
                Log.d(TAG, "[PROTO] MTK probe response len=$len")
                if (buffer[0] == 0x5F.toByte() || buffer[0] == 0x00.toByte()) return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "[PROTO] MTK probe failed: ${e.message}")
        }
        return false
    }

    private fun probeQualcommSahara(): Boolean {
        try {
            val buffer = ByteArray(1024)
            val len = connection.bulkTransfer(bulkIn, buffer, buffer.size, 200)
            if (len > 0) {
                 if (buffer[0] == 0x01.toByte()) {
                     Log.d(TAG, "[PROTO] Qualcomm Sahara probe detected HELLO")
                     return true
                 }
            }
        } catch (e: Exception) {
             Log.d(TAG, "[PROTO] Qualcomm probe failed: ${e.message}")
        }
        return false
    }
}
