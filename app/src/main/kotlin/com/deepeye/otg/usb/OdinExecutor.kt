package com.deepeye.otg.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import com.deepeye.otg.util.bulkIn
import com.deepeye.otg.util.bulkOut
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val ODIN_CHUNK_SIZE = 131072

@Singleton
class OdinExecutor @Inject constructor() {

    suspend fun flashOdin(
        connection: UsbDeviceConnection,
        outEp: UsbEndpoint,
        inEp: UsbEndpoint,
        fileBytes: ByteArray,
        sessionId: String
    ): OdinResult {
        val handshake = "ODIN".toByteArray()
        val r = connection.bulkOut(outEp, handshake, sessionId = sessionId, tag = "ODIN")
        if (r < 0) return OdinResult.Error("ODIN handshake TX failed")

        val response = ByteArray(4)
        val h = connection.bulkIn(inEp, response, sessionId = sessionId, tag = "ODIN")
        if (h < 0 || String(response) != "LOKE") {
            return OdinResult.Error("ODIN handshake RX failed: ${String(response)}")
        }

        val cmdPacket = ByteArray(64).apply {
            this[0] = 0x65
            val size = fileBytes.size
            this[4] = (size and 0xFF).toByte()
            this[5] = ((size shr 8) and 0xFF).toByte()
            this[6] = ((size shr 16) and 0xFF).toByte()
            this[7] = ((size shr 24) and 0xFF).toByte()
        }
        connection.bulkOut(outEp, cmdPacket, sessionId = sessionId, tag = "ODIN")
        delay(20)

        var offset = 0
        while (offset < fileBytes.size) {
            val chunkSize = minOf(ODIN_CHUNK_SIZE, fileBytes.size - offset)
            val chunk = fileBytes.copyOfRange(offset, offset + chunkSize)
            val tx = connection.bulkOut(outEp, chunk, sessionId = sessionId, tag = "ODIN")
            if (tx < 0) return OdinResult.Error("ODIN TX failed at offset=$offset")

            val ack = ByteArray(8)
            val rx = connection.bulkIn(inEp, ack, sessionId = sessionId, tag = "ODIN")
            if (rx < 0 || ack[0] != 0x00.toByte()) {
                return OdinResult.Error("ODIN chunk ACK failed at offset $offset")
            }
            offset += chunkSize
            delay(20)
        }

        val endPacket = ByteArray(64).apply { this[0] = 0x67 }
        connection.bulkOut(outEp, endPacket, sessionId = sessionId, tag = "ODIN")
        delay(500)
        Timber.d("[ODIN] flash complete bytes=${fileBytes.size} sessionId=$sessionId")

        return OdinResult.Success("Odin flash complete: ${fileBytes.size} bytes")
    }
}

sealed class OdinResult {
    data class Success(val message: String) : OdinResult()
    data class Error(val reason: String) : OdinResult()
}

