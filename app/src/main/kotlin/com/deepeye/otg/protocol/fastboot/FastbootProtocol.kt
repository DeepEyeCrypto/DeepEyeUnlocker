package com.deepeye.otg.protocol.fastboot

import com.deepeye.otg.logging.SafeLog
import com.deepeye.otg.usb.UsbTransport
import java.nio.charset.StandardCharsets

/**
 * Fastboot Protocol implementation (Stage 7.1).
 * Supports standard 'getvar', 'oem', and 'flashing' commands.
 */
object FastbootProtocol {
    private const val TAG = "FastbootProtocol"
    private const val MAX_COMMAND_BYTES = 64
    private const val RESPONSE_BUFFER_BYTES = 256

    data class FastbootResponse(
        val type: ResponseType,
        val message: String,
        val payload: ByteArray? = null,
        val infoMessages: List<String> = emptyList()
    )

    enum class ResponseType { OKAY, FAIL, DATA, INFO, UNKNOWN }

    /**
     * Sends a command and returns the response.
     */
    suspend fun executeCommand(transport: UsbTransport, command: String): FastbootResponse {
        if (command.toByteArray(StandardCharsets.US_ASCII).size > MAX_COMMAND_BYTES) {
            return FastbootResponse(ResponseType.FAIL, "Command too long: ${command.length} bytes")
        }

        SafeLog.d(TAG, "Fastboot CMD: $command")
        val sendRes = transport.send(command.toByteArray(StandardCharsets.US_ASCII))
        if (sendRes.isFailure) return FastbootResponse(ResponseType.FAIL, "Send failed: ${sendRes.exceptionOrNull()?.message}")

        return readResponse(transport)
    }

    /**
     * Reads response packets until a terminal packet (OKAY/FAIL/DATA) is reached.
     */
    private suspend fun readResponse(transport: UsbTransport): FastbootResponse {
        val infoMessages = mutableListOf<String>()

        while (true) {
            val packet = readSinglePacket(transport)
            when (packet.type) {
                ResponseType.INFO -> {
                    SafeLog.i(TAG, "Fastboot INFO: ${packet.message}")
                    infoMessages += packet.message
                }

                ResponseType.OKAY,
                ResponseType.FAIL,
                ResponseType.DATA,
                ResponseType.UNKNOWN -> {
                    return packet.copy(infoMessages = infoMessages)
                }
            }
        }
    }

    /**
     * Reads a single Fastboot status packet.
     */
    private suspend fun readSinglePacket(transport: UsbTransport): FastbootResponse {
        val recvRes = transport.receive(RESPONSE_BUFFER_BYTES)
        if (recvRes.isFailure) return FastbootResponse(ResponseType.FAIL, "Receive failed")

        val data = recvRes.getOrNull() ?: return FastbootResponse(ResponseType.FAIL, "Empty response")
        if (data.size < 4) return FastbootResponse(ResponseType.UNKNOWN, "Truncated response")

        val prefix = String(data, 0, 4, StandardCharsets.US_ASCII)
        val message = if (data.size > 4) {
            String(data, 4, data.size - 4, StandardCharsets.US_ASCII).trimEnd('\u0000', '\r', '\n', ' ')
        } else {
            ""
        }

        return when (prefix) {
            "OKAY" -> FastbootResponse(ResponseType.OKAY, message)
            "FAIL" -> FastbootResponse(ResponseType.FAIL, message)
            "INFO" -> FastbootResponse(ResponseType.INFO, message)
            "DATA" -> {
                val hexLen = message.removePrefix(":")
                val len = hexLen.toIntOrNull(16) ?: 0
                FastbootResponse(ResponseType.DATA, "Data incoming: $len bytes")
            }
            else -> FastbootResponse(ResponseType.UNKNOWN, "Unknown prefix: $prefix")
        }
    }

    /**
     * Helper to read all variables (getvar:all).
     */
    suspend fun getAllVariables(transport: UsbTransport): Map<String, String> {
        val response = executeCommand(transport, "getvar:all")
        if (response.type == ResponseType.FAIL) {
            SafeLog.w(TAG, "Fastboot getvar:all failed: ${response.message}")
            return emptyMap()
        }

        return response.infoMessages
            .mapNotNull { line ->
                val separator = line.indexOf(':')
                if (separator <= 0 || separator == line.lastIndex) {
                    null
                } else {
                    line.substring(0, separator).trim() to line.substring(separator + 1).trim()
                }
            }
            .toMap(LinkedHashMap())
    }
}
