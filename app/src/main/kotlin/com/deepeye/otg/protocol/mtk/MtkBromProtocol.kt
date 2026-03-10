package com.deepeye.otg.protocol.mtk

import android.util.Log
import com.deepeye.otg.usb.UsbTransport
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MTK BROM Protocol implementation (Low-level).
 * Following Stage 4.1 - 4.2 for High-Assurance Handshakes.
 */
object MtkBromProtocol {
    private const val TAG = "MtkBromProtocol"

    private val HANDSHAKE_SEQ = byteArrayOf(0xA0.toByte(), 0x0A.toByte(), 0x50.toByte(), 0x05.toByte())

    /**
     * Handshake logic: Echo Cmd ^ 0xFF.
     */
    suspend fun handshake(transport: UsbTransport): Boolean {
        Log.i(TAG, "Executing initial handshake sequence...")
        for (byte in HANDSHAKE_SEQ) {
            val sendRes = transport.send(byteArrayOf(byte), timeoutMs = 200)
            if (sendRes.isFailure) return false

            val recvRes = transport.receive(1, timeoutMs = 200)
            if (recvRes.isFailure) return false

            val actual = recvRes.getOrNull()?.get(0)
            val expected = (byte.toInt() xor 0xFF).toByte()
            if (actual != expected) {
                Log.e(TAG, "Mirror mismatch: expected 0x%02X, got 0x%02X".format(expected, actual))
                return false
            }
        }
        return true
    }

    /**
     * Reads the Hardware Code (0xFD).
     */
    suspend fun readHwCode(transport: UsbTransport): Int? {
        val sendRes = transport.send(byteArrayOf(0xFD.toByte()))
        if (sendRes.isFailure) return null
        
        val recvRes = transport.receive(4)
        val data = recvRes.getOrNull()
        if (data != null && data.size >= 2) {
            val hwCode = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            Log.i(TAG, "Chipset HW_CODE: 0x%04X".format(hwCode))
            return hwCode
        }
        return null
    }

    /**
     * Write 32-bit Memory Value (0xD5).
     */
    suspend fun write32(transport: UsbTransport, address: Long, value: Int): Boolean {
        val cmd = ByteBuffer.allocate(9).order(ByteOrder.BIG_ENDIAN)
        cmd.put(0xD5.toByte()) // WRITE32
        cmd.putInt(address.toInt())
        cmd.putInt(1) // count
        
        if (transport.send(cmd.array()).isFailure) return false
        if (transport.receive(2).isFailure) return false // Ack
        
        val valBuf = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value)
        if (transport.send(valBuf.array()).isFailure) return false
        return (transport.receive(2).isSuccess) // Success Ack
    }

    /**
     * SEND_DA (0xD7): Injects Download Agent.
     */
    suspend fun loadDa(transport: UsbTransport, address: Long, daData: ByteArray): Boolean {
        Log.i(TAG, "Injecting DA (%d bytes) to SRAM 0x%08X".format(daData.size, address))
        
        val cmd = ByteBuffer.allocate(13).order(ByteOrder.BIG_ENDIAN)
        cmd.put(0xD7.toByte()) // SEND_DA
        cmd.putInt(address.toInt())
        cmd.putInt(daData.size)
        cmd.putInt(0) // sig_len (0 for unsigned/early DAs)
        
        if (transport.send(cmd.array()).isFailure) return false
        if (transport.receive(2).isFailure) return false // Command Accept ACK
        
        // Stream binary data
        if (transport.send(daData, timeoutMs = 15000).isFailure) return false
        
        // Finalized ACK from ROM
        val finalAck = transport.receive(2)
        if (finalAck.isFailure) {
            Log.e(TAG, "DA transmission failed verification")
            return false
        }
        
        Log.i(TAG, "DA uploaded successfully")
        return true
    }

    /**
     * JUMP_DA (0xD9): Executes the injected code.
     */
    suspend fun jumpDa(transport: UsbTransport, address: Long): Boolean {
        Log.i(TAG, "Finalizing JUMP to SRAM 0x%08X".format(address))
        val cmd = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN)
        cmd.put(0xD9.toByte()) // JUMP_DA
        cmd.putInt(address.toInt())
        
        if (transport.send(cmd.array()).isFailure) return false
        if (transport.receive(2).isFailure) return false // Executed ACK
        
        // Wait for DA to send initial SYNC byte (0x5A)
        val sync = transport.receive(1, timeoutMs = 500)
        if (sync.isSuccess && sync.getOrNull()?.get(0) == 0x5A.toByte()) {
            Log.i(TAG, "DA initialized and took control")
            return true
        }
        
        Log.w(TAG, "DA executed but SYNC mismatch (Expected 0x5A)")
        return true // Still might be alive
    }
}
