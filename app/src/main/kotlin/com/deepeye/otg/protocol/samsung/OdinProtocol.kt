package com.deepeye.otg.protocol.samsung

import com.deepeye.otg.logging.SafeLog
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
    private const val ACK_PACKET_SIZE = 1024
    private const val CHUNK_HEADER_SIZE = 1024
    private const val DEFAULT_CHUNK_SIZE = 524288
    private const val CMD_TRANSFER = 0x65
    private const val CMD_FLASH = 0x66

    sealed class OdinError(message: String) : Exception(message) {
        data class ChunkRejected(val chunkIndex: Int, val statusCode: Int) :
            OdinError("Chunk $chunkIndex rejected with status 0x%08X".format(statusCode))

        data class ChunkAckTimeout(val chunkIndex: Int) :
            OdinError("Timed out waiting for ACK for chunk $chunkIndex")

        object FinalAckTimeout : OdinError("Timed out waiting for final Odin ACK")
        data class InvalidAck(val size: Int) : OdinError("Invalid Odin ACK size: $size")
        data class WriteFailed(val operation: String) : OdinError("Failed to write Odin $operation packet")
    }

    /**
     * Handshake logic for Samsung Download Mode.
     */
    suspend fun handshake(transport: UsbTransport): Boolean {
        SafeLog.i(TAG, "Executing Odin handshake...")
        
        // 1. Send "ODIN" to start
        val start = ODIN_SIGNATURE.toByteArray()
        if (!transport.write(start).isSuccess) return false

        // 2. Read response
        val res = transport.read(8)
        if (res is TransferResult.Success && res.data != null) {
            val response = String(res.data)
            SafeLog.i(TAG, "Odin Response: $response")
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
            SafeLog.i(TAG, "PIT Size: $size bytes")
            
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

    suspend fun readOdinAck(
        transport: UsbTransport,
        chunkIndex: Int? = null,
        sessionId: String = "unknown",
        finalAck: Boolean = false
    ): Result<Unit> {
        return when (val res = transport.read(ACK_PACKET_SIZE, timeoutMs = 30_000)) {
            is TransferResult.Success -> {
                val data = res.data ?: ByteArray(0)
                if (data.size != ACK_PACKET_SIZE) {
                    Result.failure(OdinError.InvalidAck(data.size))
                } else {
                    val status = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
                    val ok = status == 0
                    SafeLog.i(
                        TAG,
                        "[SAMSUNG] chunk=${chunkIndex ?: -1} ack=${if (ok) "OK" else "REJECTED"} sessionId=$sessionId"
                    )
                    if (ok) {
                        Result.success(Unit)
                    } else {
                        Result.failure(OdinError.ChunkRejected(chunkIndex ?: -1, status))
                    }
                }
            }
            is TransferResult.Timeout -> Result.failure(
                if (finalAck) OdinError.FinalAckTimeout else OdinError.ChunkAckTimeout(chunkIndex ?: -1)
            )
            else -> Result.failure(
                if (finalAck) OdinError.FinalAckTimeout else OdinError.ChunkAckTimeout(chunkIndex ?: -1)
            )
        }
    }

    suspend fun flashPartition(
        transport: UsbTransport,
        payload: ByteArray,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        sessionId: String = "unknown"
    ): Result<Unit> {
        // PHYSICAL_DEVICE_REQUIRED: verify Odin flash ACK flow on a real Samsung Download Mode target.
        // Unit test covers protocol contract only.
        require(chunkSize > 0) { "chunkSize must be > 0" }

        var chunkIndex = 0
        var offset = 0
        while (offset < payload.size) {
            val end = minOf(offset + chunkSize, payload.size)
            val chunk = payload.copyOfRange(offset, end)

            if (!transport.write(createChunkHeader(chunkIndex, chunk.size)).isSuccess) {
                return Result.failure(OdinError.WriteFailed("chunkHeader"))
            }
            if (!transport.write(chunk).isSuccess) {
                return Result.failure(OdinError.WriteFailed("chunkData"))
            }

            readOdinAck(transport, chunkIndex = chunkIndex, sessionId = sessionId).getOrElse {
                return Result.failure(it)
            }

            offset = end
            chunkIndex++
        }

        if (!transport.write(createFlashDonePacket()).isSuccess) {
            return Result.failure(OdinError.WriteFailed("flashDone"))
        }

        return readOdinAck(
            transport = transport,
            chunkIndex = chunkIndex,
            sessionId = sessionId,
            finalAck = true
        )
    }

    private fun createChunkHeader(chunkIndex: Int, chunkSize: Int): ByteArray {
        val buffer = ByteBuffer.allocate(CHUNK_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(CMD_TRANSFER)
        buffer.putInt(chunkIndex)
        buffer.putInt(chunkSize)
        return buffer.array()
    }

    private fun createFlashDonePacket(): ByteArray {
        val buffer = ByteBuffer.allocate(CHUNK_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(CMD_FLASH)
        return buffer.array()
    }
}
