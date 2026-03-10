package com.deepeye.otg.protocol.samsung

import android.util.Log
import com.deepeye.otg.usb.TransferResult
import com.deepeye.otg.usb.UsbTransport
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Samsung Odin (Loke/Download Mode) Protocol (Stage 6.1).
 */
object OdinProtocol {
    private const val TAG = "OdinProtocol"

    private const val ODIN_SIGNATURE = "ODIN"

    /**
     * Handshake logic for Samsung Download Mode.
     */
    suspend fun handshake(transport: UsbTransport): Boolean {
        Log.i(TAG, "Executing Odin handshake...")
        
        // 1. Send "ODIN" to start
        val start = ODIN_SIGNATURE.toByteArray()
        if (!transport.write(start).isSuccess) return false

        // 2. Read response
        val res = transport.read(8)
        if (res is TransferResult.Success && res.data != null) {
            val response = String(res.data)
            Log.i(TAG, "Odin Response: $response")
            return response.contains("LOKE") || response.contains("ODIN")
        }
        
        return false
    }

    /**
     * Read PIT (Partition Information Table).
     */
    suspend fun readPit(transport: UsbTransport): ByteArray? {
        val cmd = byteArrayOf(0x04.toByte()) // PIT_READ command
        transport.write(cmd)
        
        val head = transport.read(4) 
        if (head is TransferResult.Success && head.data != null) {
            val size = ByteBuffer.wrap(head.data).order(ByteOrder.LITTLE_ENDIAN).getInt()
            Log.i(TAG, "PIT Size: $size bytes")
            
            val data = transport.read(size)
            if (data is TransferResult.Success) return data.data
        }
        return null
    }

    /**
     * Reboot device from Download mode.
     */
    suspend fun reboot(transport: UsbTransport): Boolean {
        val cmd = byteArrayOf(0x08.toByte()) // REBOOT
        return transport.write(cmd).isSuccess
    }
}
