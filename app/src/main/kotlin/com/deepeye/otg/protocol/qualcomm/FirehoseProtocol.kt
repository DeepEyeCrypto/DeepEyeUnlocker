package com.deepeye.otg.protocol.qualcomm

import android.util.Log
import com.deepeye.otg.usb.TransferResult
import com.deepeye.otg.usb.UsbTransport

/**
 * Qualcomm Firehose XML Protocol implementation (Stage 5.2).
 */
object FirehoseProtocol {
    private const val TAG = "FirehoseProtocol"
    private const val DEFAULT_RESPONSE_CHUNK_SIZE = 4096
    private const val MAX_RESPONSE_BYTES = 1024 * 1024

    sealed class FirehoseError(message: String) : Exception(message) {
        object Timeout : FirehoseError("Timed out waiting for Firehose response")
        object ResponseTooLarge : FirehoseError("Firehose response exceeded 1MB")
        data class WriteFailed(val operation: String) : FirehoseError("Firehose write failed during $operation")
        data class ReadFailed(val operation: String) : FirehoseError("Firehose read failed during $operation")
        data class Nak(val response: String) : FirehoseError("Firehose returned NAK: $response")
    }

    /**
     * Wrap XML string in a packet with proper padding for Firehose.
     */
    fun createPacket(xml: String): ByteArray {
        return xml.toByteArray(Charsets.UTF_8)
    }

    /**
     * Sends a configuration packet to Firehose.
     */
    suspend fun configure(transport: UsbTransport, maxPayloadSize: Int = 1048576): Boolean {
        val xml = """<?xml version="1.0" encoding="UTF-8" ?>
            |<data>
            |<configure MemoryName="emmc" MaxPayloadSizeToTargetInBytes="$maxPayloadSize" />
            |</data>""".trimMargin()
        
        Log.i(TAG, "Sending Firehose configuration...")
        return sendCommandResult(transport, xml).isSuccess
    }

    /**
     * Reads a chunk of data from the device.
     */
    suspend fun read(transport: UsbTransport, sector: Long, numSectors: Int): ByteArray? {
        val xml = """<?xml version="1.0" encoding="UTF-8" ?>
            |<data>
            |<read SECTOR_SIZE_IN_BYTES="512" num_partition_sectors="$numSectors" start_sector="$sector" />
            |</data>""".trimMargin()
        
        if (transport.write(createPacket(xml)).isSuccess) {
            // Firehose usually returns an ACK XML followed by raw data
            val ack = readFirehoseResponse(transport).getOrNull() ?: return null
            if (isAckResponse(ack)) {
                val dataRes = transport.read(numSectors * 512)
                if (dataRes is TransferResult.Success) return dataRes.data
            }
        }
        return null
    }

    /**
     * Sends XML command and returns the response.
     */
    suspend fun sendCommandResult(
        transport: UsbTransport,
        xml: String,
        timeoutMs: Int = 10_000,
        maxPacketSize: Int = DEFAULT_RESPONSE_CHUNK_SIZE,
        sessionId: String = "unknown"
    ): Result<String> {
        val writeRes = transport.write(createPacket(xml))
        if (!writeRes.isSuccess) {
            return Result.failure(FirehoseError.WriteFailed("send_command"))
        }

        val response = readFirehoseResponse(
            transport = transport,
            timeoutMs = timeoutMs,
            maxPacketSize = maxPacketSize,
            sessionId = sessionId
        ).getOrElse {
            return Result.failure(it)
        }

        if (isNakResponse(response)) {
            return Result.failure(FirehoseError.Nak(response))
        }
        return Result.success(response)
    }

    suspend fun sendCommand(transport: UsbTransport, xml: String): String {
        return sendCommandResult(transport, xml).getOrElse { "ERROR: ${it.message}" }
    }

    suspend fun readFirehoseResponse(
        transport: UsbTransport,
        timeoutMs: Int = 10_000,
        maxPacketSize: Int = DEFAULT_RESPONSE_CHUNK_SIZE,
        sessionId: String = "unknown"
    ): Result<String> {
        val startedAt = System.currentTimeMillis()
        val builder = StringBuilder()

        while (System.currentTimeMillis() - startedAt <= timeoutMs) {
            val remaining = (timeoutMs - (System.currentTimeMillis() - startedAt)).coerceAtLeast(1).toInt()
            when (val res = transport.read(maxPacketSize, timeoutMs = minOf(remaining, 2_000))) {
                is TransferResult.Success -> {
                    val chunk = String(res.data ?: ByteArray(0), Charsets.UTF_8)
                    builder.append(chunk)

                    if (builder.length > MAX_RESPONSE_BYTES) {
                        return Result.failure(FirehoseError.ResponseTooLarge)
                    }

                    val response = builder.toString()
                    if (response.contains("</data>", ignoreCase = true) ||
                        response.contains("</response>", ignoreCase = true)
                    ) {
                        Log.d(
                            TAG,
                            "[QC_EDL] firehose rx len=${response.length} ack=${isAckResponse(response)} sessionId=$sessionId"
                        )
                        return Result.success(response)
                    }
                }
                is TransferResult.Timeout -> {
                    // Keep looping until timeout budget is exhausted.
                }
                else -> return Result.failure(FirehoseError.ReadFailed("read_response"))
            }
        }

        return Result.failure(FirehoseError.Timeout)
    }

    suspend fun uploadFirehoseData(
        transport: UsbTransport,
        payload: ByteArray,
        maxPacketSize: Int,
        sessionId: String = "unknown"
    ): Result<Unit> {
        require(maxPacketSize > 0) { "maxPacketSize must be > 0" }

        var offset = 0
        while (offset < payload.size) {
            val end = minOf(offset + maxPacketSize, payload.size)
            val chunk = payload.copyOfRange(offset, end)
            val writeRes = transport.write(chunk, timeoutMs = 2_000)
            if (!writeRes.isSuccess) {
                return Result.failure(FirehoseError.WriteFailed("upload_data_chunk"))
            }
            offset = end
        }

        if (payload.size % maxPacketSize == 0) {
            val zlp = ByteArray(0)
            val zlpResult = transport.write(zlp, timeoutMs = 2_000)
            if (!zlpResult.isSuccess) {
                return Result.failure(FirehoseError.WriteFailed("upload_data_zlp"))
            }
            Log.d(TAG, "[QC_EDL] ZLP sent after final chunk sessionId=$sessionId")
        }

        return Result.success(Unit)
    }

    private fun isAckResponse(response: String): Boolean {
        return Regex("""value\s*=\s*[\"']ACK[\"']""", RegexOption.IGNORE_CASE).containsMatchIn(response)
    }

    private fun isNakResponse(response: String): Boolean {
        return Regex("""value\s*=\s*[\"']NAK[\"']""", RegexOption.IGNORE_CASE).containsMatchIn(response)
    }
}
