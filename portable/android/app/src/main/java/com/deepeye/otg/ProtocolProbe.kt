package com.deepeye.otg

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbConstants
import android.util.Log

enum class DetectedProtocol {
    UNKNOWN,
    QUALCOMM_EDL,
    MTK_BROM,
    MTK_PRELOADER,
    FASTBOOT,
    SAMSUNG_ODIN
}

class ProtocolProbe(private val connection: UsbDeviceConnection, private val deviceInterface: UsbInterface) {

    private val TAG = "DeepEye-Probe"
    private var bulkIn: UsbEndpoint? = null
    private var bulkOut: UsbEndpoint? = null

    init {
        for (i in 0 until deviceInterface.endpointCount) {
            val ep = deviceInterface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.direction == UsbConstants.USB_DIR_IN) bulkIn = ep
                else bulkOut = ep
            }
        }
    }

    fun detect(): DetectedProtocol {
        Log.i(TAG, "Starting protocol probe...")
        if (bulkIn == null || bulkOut == null) {
            Log.e(TAG, "Missing bulk endpoints. Cannot probe.")
            return DetectedProtocol.UNKNOWN
        }

        // 1. FASTBOOT Probe (Safe ASCII)
        if (probeFastboot()) return DetectedProtocol.FASTBOOT

        // 2. MTK BROM Probe (Sync bytes)
        if (probeMtkBrom()) return DetectedProtocol.MTK_BROM

        // 3. Qualcomm EDL Probe (Sahara Hello)
        if (probeQualcommSahara()) return DetectedProtocol.QUALCOMM_EDL

        // 4. Samsung Odin (Not implemented in probe yet, tricky)
        
        Log.w(TAG, "Probe yielded UNKNOWN protocol.")
        return DetectedProtocol.UNKNOWN
    }

    private fun probeFastboot(): Boolean {
        try {
            val cmd = "getvar:version".toByteArray()
            val buffer = ByteArray(64)
            
            // Send
            connection.bulkTransfer(bulkOut, cmd, cmd.size, 100)
            
            // Read
            val len = connection.bulkTransfer(bulkIn, buffer, buffer.size, 100)
            if (len > 0) {
                val response = String(buffer, 0, len)
                Log.d(TAG, "Fastboot probe response: $response")
                if (response.startsWith("OKAY") || response.startsWith("FAIL")) return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Fastboot probe failed: ${e.message}")
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
                // Check for MTK BROM signature inside response (often ~0x5F or HW code)
                // Simplified: Any valid response to the start command usually implies BROM/Preloader
                Log.d(TAG, "MTK probe response len=$len")
                // Validate header (simplified for probe)
                if (buffer[0] == 0x5F.toByte() || buffer[0] == 0x00.toByte()) return true 
            }
        } catch (e: Exception) {
            Log.d(TAG, "MTK probe failed: ${e.message}")
        }
        return false
    }

    private fun probeQualcommSahara(): Boolean {
        try {
            // Sahara Hello Command (0x01) - Minimal 48 bytes
            val cmd = ByteArray(48)
            cmd[0] = 0x01 // Command ID
            // ... rest zero is technically valid 'empty' hello for some loaders, 
            // but ideally we construct a proper packet. 
            // For probe, receiving ANY cached data or "Sahara" state is key.
            
            // Often simply reading first is enough if device initiated mode:
            val buffer = ByteArray(1024)
            val len = connection.bulkTransfer(bulkIn, buffer, buffer.size, 200)
            if (len > 0) {
                 // Check if it's a Sahara Hello Packet (Cmd 0x01, Len 0x30 usually)
                 if (buffer[0] == 0x01.toByte()) {
                     Log.d(TAG, "Qualcomm Sahara probe detected HELLO")
                     return true
                 }
            }
        } catch (e: Exception) {
             Log.d(TAG, "Qualcomm probe failed: ${e.message}")
        }
        return false
    }
}
