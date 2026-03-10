package com.deepeye.otg.protocol.fastboot

import android.util.Log
import com.deepeye.otg.usb.UsbTransport
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Fastboot Protocol implementation (Stage 7.1).
 * Supports standard 'getvar', 'oem', and 'flashing' commands.
 */
object FastbootProtocol {
    private const val TAG = "FastbootProtocol"

    data class FastbootResponse(
        val type: ResponseType,
        val message: String,
        val payload: ByteArray? = null
    )

    enum class ResponseType { OKAY, FAIL, DATA, INFO, UNKNOWN }

    /**
     * Sends a command and returns the response.
     */
    suspend fun executeCommand(transport: UsbTransport, command: String): FastbootResponse {
        Log.d(TAG, "Fastboot CMD: $command")
        val sendRes = transport.send(command.toByteArray())
        if (sendRes.isFailure) return FastbootResponse(ResponseType.FAIL, "Send failed: ${sendRes.exceptionOrNull()?.message}")

        return readResponse(transport)
    }

    /**
     * Reads a standard 4-byte prefix response.
     */
    private suspend fun readResponse(transport: UsbTransport): FastbootResponse {
        val recvRes = transport.receive(64) // Fastboot header is small
        if (recvRes.isFailure) return FastbootResponse(ResponseType.FAIL, "Receive failed")

        val data = recvRes.getOrNull() ?: return FastbootResponse(ResponseType.FAIL, "Empty response")
        if (data.size < 4) return FastbootResponse(ResponseType.UNKNOWN, "Truncated response")

        val prefix = String(data.take(4).toByteArray())
        val message = if (data.size > 4) String(data.drop(4).toByteArray()).trim() else ""

        return when (prefix) {
            "OKAY" -> FastbootResponse(ResponseType.OKAY, message)
            "FAIL" -> FastbootResponse(ResponseType.FAIL, message)
            "INFO" -> {
                // INFO responses can be multiple. Recursively read until OKAY/FAIL.
                Log.i(TAG, "Fastboot INFO: $message")
                readResponse(transport) // Note: In a real impl, we'd accumulate INFO lines.
                FastbootResponse(ResponseType.INFO, message) 
            }
            "DATA" -> {
                val hexLen = message
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
        // Multi-line INFO response handler needed here.
        executeCommand(transport, "getvar:all")
        return emptyMap() // Placeholder
    }
}
