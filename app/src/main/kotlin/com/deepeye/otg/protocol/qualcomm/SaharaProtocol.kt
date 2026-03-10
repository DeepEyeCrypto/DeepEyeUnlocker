package com.deepeye.otg.protocol.qualcomm

import android.util.Log
import com.deepeye.otg.usb.TransferResult
import com.deepeye.otg.usb.UsbTransport
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Qualcomm Sahara Protocol implementation (Stage 5.1).
 */
object SaharaProtocol {
    private const val TAG = "SaharaProtocol"

    const val SAHARA_HELLO = 0x01
    const val SAHARA_HELLO_RESP = 0x02
    const val SAHARA_READ_DATA = 0x03
    const val SAHARA_END_TRANSFER = 0x04
    const val SAHARA_DONE = 0x0D
    const val SAHARA_DONE_RESP = 0x0E
    const val SAHARA_RESET = 0x0F
    const val SAHARA_RESET_RESP = 0x10

    const val PACKET_SIZE_HELLO = 0x30

    /**
     * Handshake logic as per Stage 5.1.
     */
    suspend fun handshake(transport: UsbTransport): Boolean {
        Log.i(TAG, "Waiting for device HELLO...")
        
        // 1. Read HELLO from device
        val res = transport.read(PACKET_SIZE_HELLO, timeoutMs = 2000)
        if (res !is TransferResult.Success || res.data == null) {
            Log.e(TAG, "Sahara: Failed to read HELLO packet or timeout")
            return false
        }

        val buffer = ByteBuffer.wrap(res.data).order(ByteOrder.LITTLE_ENDIAN)
        val cmd = buffer.getInt()
        if (cmd != SAHARA_HELLO) {
            Log.e(TAG, "Sahara: Unexpected command 0x%02X".format(cmd))
            return false
        }

        val pktLen = buffer.getInt()
        val version = buffer.getInt()
        val minVer = buffer.getInt()
        val maxDataLen = buffer.getInt()
        val mode = buffer.getInt()

        Log.i(TAG, "Sahara HELLO: ver=$version, mode=$mode, maxLen=$maxDataLen")

        // 2. Send HELLO_RESP
        val resp = ByteBuffer.allocate(PACKET_SIZE_HELLO).order(ByteOrder.LITTLE_ENDIAN)
        resp.putInt(SAHARA_HELLO_RESP)
        resp.putInt(PACKET_SIZE_HELLO)
        resp.putInt(version) // Echo supported version
        resp.putInt(minVer)
        resp.putInt(0) // Status (0 = Success)
        resp.putInt(mode)
        // Rest are reserved/optional for this stage
        
        val writeRes = transport.write(resp.array())
        if (!writeRes.isSuccess) {
            Log.e(TAG, "Sahara: HELLO_RESP write failed")
            return false
        }

        Log.i(TAG, "Sahara handshake COMPLETED")
        return true
    }

    /**
     * Terminate Sahara session cleanly.
     */
    suspend fun done(transport: UsbTransport): Boolean {
        val pkt = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        pkt.putInt(SAHARA_DONE)
        pkt.putInt(8)
        return transport.write(pkt.array()).isSuccess
    }
}
