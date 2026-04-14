package com.deepeye.otg.protocol.mtk

import com.deepeye.otg.logging.SafeLog
import com.deepeye.otg.usb.UsbTransport
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MTK BROM Protocol implementation (Low-level).
 * Following Stage 4.1 - 4.2 for High-Assurance Handshakes.
 */
object MtkBromProtocol {
    private const val TAG = "MtkBromProtocol"
    private const val HANDSHAKE_TIMEOUT_MS = 3_000
    private const val COMMAND_TIMEOUT_MS = 3_000
    private const val READ_TIMEOUT_MS = 5_000
    private const val STATUS_TIMEOUT_MS = 2_000
    private const val HANDSHAKE_RETRIES = 3
    private const val SEND_DA_TIMEOUT_MS = 15_000
    private const val CMD_WRITE32 = 0xD4.toByte()
    private const val CMD_SEND_DA = 0xD7.toByte()
    private const val CMD_JUMP_DA = 0xD5.toByte()

    private val HANDSHAKE_SEQ = byteArrayOf(0xA0.toByte(), 0x0A.toByte(), 0x50.toByte(), 0x05.toByte())

    sealed class MtkError(message: String) : Exception(message) {
        data class DaChecksumMismatch(val expected: Int, val actual: Int) :
            MtkError("DA checksum mismatch expected=0x%04X actual=0x%04X".format(expected, actual))

        data class TransportFailure(val operation: String, val details: String) :
            MtkError("$operation failed: $details")
    }

    private data class StatusAwarePayload(
        val statusOk: Boolean,
        val data: ByteArray
    )

    /**
     * Handshake logic: Echo Cmd ^ 0xFF.
     */
    suspend fun handshake(transport: UsbTransport): Boolean {
        SafeLog.i(TAG, "Executing initial handshake sequence...")
        repeat(HANDSHAKE_RETRIES) { attempt ->
            var handshakeOk = true

            for (byte in HANDSHAKE_SEQ) {
                val sendRes = transport.send(byteArrayOf(byte), timeoutMs = HANDSHAKE_TIMEOUT_MS)
                if (sendRes.isFailure) {
                    handshakeOk = false
                    break
                }

                val recvRes = transport.receive(1, timeoutMs = HANDSHAKE_TIMEOUT_MS)
                if (recvRes.isFailure) {
                    handshakeOk = false
                    break
                }

                val actual = recvRes.getOrNull()?.getOrNull(0)
                val expected = (byte.toInt() xor 0xFF).toByte()
                if (actual != expected) {
                    SafeLog.e(TAG, "Mirror mismatch: expected 0x%02X, got 0x%02X".format(expected, actual))
                    handshakeOk = false
                    break
                }
            }

            if (handshakeOk) {
                return true
            }

            if (attempt + 1 < HANDSHAKE_RETRIES) {
                SafeLog.w(TAG, "[MTK_BROM] handshake retry ${attempt + 1}/$HANDSHAKE_RETRIES")
                delay(300)
            }
        }

        SafeLog.e(TAG, "[MTK_BROM] handshake failed after $HANDSHAKE_RETRIES attempts")
        return false
    }

    /**
     * Reads the Hardware Code (0xFD).
     */
    suspend fun readHwCode(transport: UsbTransport): Int? {
        val sendRes = transport.send(byteArrayOf(0xFD.toByte()), timeoutMs = COMMAND_TIMEOUT_MS)
        if (sendRes.isFailure) return null

        val payload = readStatusAwarePayload(transport, expectedBytes = 2)
        if (!payload.statusOk) {
            SafeLog.w(TAG, "[MTK_BROM] HW_CODE status byte mismatch — continuing with payload parsing")
        }

        if (payload.data.size >= 2) {
            val data = payload.data
            val hwCode = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            SafeLog.i(TAG, "Chipset HW_CODE: 0x%04X".format(hwCode))
            return hwCode
        }

        SafeLog.e(TAG, "[MTK_BROM] HW_CODE read failed: got ${payload.data.size} byte(s)")
        return null
    }

    private suspend fun readStatusAwarePayload(
        transport: UsbTransport,
        expectedBytes: Int
    ): StatusAwarePayload {
        var statusOk = false
        var collected = ByteArray(0)

        val statusProbe = transport.receive(1, timeoutMs = STATUS_TIMEOUT_MS).getOrNull()
        val statusByte = statusProbe?.getOrNull(0)
        if (statusByte != null) {
            if (statusByte == 0x00.toByte()) {
                statusOk = true
            } else {
                collected = byteArrayOf(statusByte)
            }
        }

        val remainingPrimary = (expectedBytes - collected.size).coerceAtLeast(0)
        if (remainingPrimary > 0) {
            val primary = transport.receive(remainingPrimary, timeoutMs = READ_TIMEOUT_MS).getOrNull()
            if (primary != null) {
                collected += primary
            }
        }

        if (collected.size < expectedBytes) {
            val fallbackRemaining = (4 - collected.size).coerceAtLeast(0)
            if (fallbackRemaining > 0) {
                val fallback = transport.receive(fallbackRemaining, timeoutMs = COMMAND_TIMEOUT_MS).getOrNull()
                if (fallback != null) {
                    collected += fallback
                }
            }
        }

        return StatusAwarePayload(statusOk = statusOk, data = collected)
    }

    /**
     * Write 32-bit Memory Value (0xD4).
     */
    suspend fun write32(transport: UsbTransport, address: Long, value: Int): Boolean {
        val cmd = ByteBuffer.allocate(9).order(ByteOrder.BIG_ENDIAN)
        cmd.put(CMD_WRITE32)
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
        return sendDa(transport, address, daData).isSuccess &&
            verifyDaChecksum(transport, daData).isSuccess
    }

    suspend fun sendDa(transport: UsbTransport, address: Long, daData: ByteArray): Result<Unit> {
        SafeLog.i(TAG, "Injecting DA (%d bytes) to SRAM 0x%08X".format(daData.size, address))
        
        val cmd = ByteBuffer.allocate(13).order(ByteOrder.BIG_ENDIAN)
        cmd.put(CMD_SEND_DA)
        cmd.putInt(address.toInt())
        cmd.putInt(daData.size)
        cmd.putInt(0) // sig_len (0 for unsigned/early DAs)
        
        if (transport.send(cmd.array()).isFailure) {
            return Result.failure(MtkError.TransportFailure("send_da_command", "unable to send SEND_DA header"))
        }
        if (transport.receive(2).isFailure) {
            return Result.failure(MtkError.TransportFailure("send_da_command", "missing SEND_DA accept ACK"))
        }
        
        // Stream binary data
        if (transport.send(daData, timeoutMs = SEND_DA_TIMEOUT_MS).isFailure) {
            return Result.failure(MtkError.TransportFailure("send_da_payload", "unable to stream DA payload"))
        }

        SafeLog.i(TAG, "DA payload transmitted, awaiting checksum verification")
        return Result.success(Unit)
    }

    suspend fun verifyDaChecksum(
        transport: UsbTransport,
        daBytes: ByteArray,
        sessionId: String = "unknown"
    ): Result<Unit> {
        val expected = computeDaChecksum(daBytes)
        val checksumBytes = transport.receive(2).getOrElse {
            return Result.failure(
                MtkError.TransportFailure("verify_da_checksum", it.message ?: "missing checksum bytes")
            )
        }
        if (checksumBytes.size < 2) {
            return Result.failure(
                MtkError.TransportFailure("verify_da_checksum", "short checksum response (${checksumBytes.size} bytes)")
            )
        }

        val actual = ((checksumBytes[0].toInt() and 0xFF) shl 8) or (checksumBytes[1].toInt() and 0xFF)
        val matches = expected == actual
        SafeLog.i(
            TAG,
            "[MTK_BROM] da_checksum expected=0x%04X actual=0x%04X result=%s sessionId=%s"
                .format(expected, actual, if (matches) "OK" else "MISMATCH", sessionId)
        )

        return if (matches) {
            Result.success(Unit)
        } else {
            Result.failure(MtkError.DaChecksumMismatch(expected, actual))
        }
    }

    /**
     * JUMP_DA (0xD5): Executes the injected code.
     */
    suspend fun jumpDa(transport: UsbTransport, address: Long): Boolean {
        SafeLog.i(TAG, "Finalizing JUMP to SRAM 0x%08X".format(address))
        val cmd = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN)
        cmd.put(CMD_JUMP_DA)
        cmd.putInt(address.toInt())
        
        if (transport.send(cmd.array()).isFailure) return false
        if (transport.receive(2).isFailure) return false // Executed ACK
        
        // Wait for DA to send initial SYNC byte (0x5A)
        val sync = transport.receive(1, timeoutMs = 500)
        if (sync.isSuccess && sync.getOrNull()?.get(0) == 0x5A.toByte()) {
            SafeLog.i(TAG, "DA initialized and took control")
            return true
        }
        
        SafeLog.w(TAG, "DA executed but SYNC mismatch (Expected 0x5A)")
        return true // Still might be alive
    }

    private fun computeDaChecksum(daBytes: ByteArray): Int {
        return daBytes.fold(0) { acc, byte ->
            (acc + (byte.toInt() and 0xFF)) and 0xFFFF
        }
    }
}
