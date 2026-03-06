package com.deepeye.otg.usb

import android.hardware.usb.*
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min
import kotlin.math.pow

/**
 * Result of a USB transfer including retry statistics and potential error messages.
 */
data class TransferResult(
    val success: Boolean,
    val bytesTransferred: Int,
    val data: ByteArray? = null,
    val errorMsg: String? = null,
    val retryCount: Int = 0
)

/**
 * Thread-safe serial transfer queue for USB bulk operations.
 * Handles: Chunking (16KB), Exponential Backoff Retries, and Endpoint Stall Clearance.
 */
class UsbTransferQueue(
    private val connection: UsbDeviceConnection,
    private val endpoints: ResolvedEndpoints
) {
    companion object {
        private const val TAG = "DeepEye-TransferQ"
        private const val MAX_RETRIES = 3
        private const val BASE_DELAY_MS = 100L
        private const val MAX_CHUNK = 16384
        private const val STALL_CLEAR_REQUEST = 0x01  // CLEAR_FEATURE
        private const val ENDPOINT_HALT = 0x00
        private const val USB_DIR_RECIPIENT_ENDPOINT = 0x02
    }

    // Single-access mutex to prevent concurrent bulk transfers on the same device
    private val transferMutex = Mutex()

    /**
     * Serialized WRITE with automatic chunking and retry logic.
     */
    suspend fun write(
        data: ByteArray,
        timeoutMs: Int = endpoints.recommendedTimeout
    ): TransferResult = withContext(Dispatchers.IO) {
        val outEp = endpoints.bulkOut
            ?: return@withContext TransferResult(false, 0, null, "No BULK OUT endpoint")

        transferMutex.withLock {
            var attempt = 0
            var lastError = ""

            while (attempt < MAX_RETRIES) {
                val result = chunkedWrite(data, outEp, timeoutMs)
                if (result.success) {
                    return@withLock result.copy(retryCount = attempt)
                }

                lastError = result.errorMsg ?: "Unknown"
                Log.w(TAG, "Write attempt ${attempt + 1} failed: $lastError")

                // Detect stall → clear it before retry
                if (isEndpointStalled(outEp)) {
                    clearEndpointStall(outEp)
                    Log.i(TAG, "OUT Endpoint stall cleared")
                }

                val delay = BASE_DELAY_MS * 2.0.pow(attempt.toDouble()).toLong()
                delay(min(delay, 2000L))
                attempt++
            }

            TransferResult(false, 0, null, "Write failed after $MAX_RETRIES retries: $lastError", MAX_RETRIES)
        }
    }

    /**
     * Serialized READ with automatic stall detection and retry.
     */
    suspend fun read(
        expectedSize: Int = endpoints.recommendedBufferSize,
        timeoutMs: Int = endpoints.recommendedTimeout
    ): TransferResult = withContext(Dispatchers.IO) {
        val inEp = endpoints.bulkIn
            ?: return@withContext TransferResult(false, 0, null, "No BULK IN endpoint")

        transferMutex.withLock {
            var attempt = 0
            var lastError = ""

            while (attempt < MAX_RETRIES) {
                val buffer = ByteArray(expectedSize)
                val received = connection.bulkTransfer(inEp, buffer, expectedSize, timeoutMs)

                when {
                    received >= 0 -> {
                        return@withLock TransferResult(true, received, buffer.copyOf(received), retryCount = attempt)
                    }
                    else -> {
                        lastError = "bulkTransfer returned -1 (Timeout or Error)"
                        Log.w(TAG, "Read attempt ${attempt + 1}: $lastError")

                        if (isEndpointStalled(inEp)) {
                            clearEndpointStall(inEp)
                            Log.i(TAG, "IN Endpoint stall cleared")
                        }
                    }
                }

                val delay = BASE_DELAY_MS * 2.0.pow(attempt.toDouble()).toLong()
                delay(min(delay, 2000L))
                attempt++
            }

            TransferResult(false, 0, null, "Read failed after $MAX_RETRIES retries: $lastError", MAX_RETRIES)
        }
    }

    private fun chunkedWrite(data: ByteArray, endpoint: UsbEndpoint, timeout: Int): TransferResult {
        var offset = 0
        var totalSent = 0

        while (offset < data.size) {
            val chunkSize = min(MAX_CHUNK, data.size - offset)
            val chunk = data.copyOfRange(offset, offset + chunkSize)

            val sent = connection.bulkTransfer(endpoint, chunk, chunkSize, timeout)

            if (sent < 0) {
                return TransferResult(false, totalSent, null, "Chunk write failed at offset $offset (Code: $sent)")
            }

            totalSent += sent
            offset += chunkSize
        }

        return TransferResult(true, totalSent, data)
    }

    private fun isEndpointStalled(endpoint: UsbEndpoint): Boolean {
        val statusBuf = ByteArray(2)
        val result = connection.controlTransfer(0x82, 0x00, 0, endpoint.endpointNumber, statusBuf, 2, 1000)
        return result >= 0 && (statusBuf[0].toInt() and 0x01) != 0
    }

    private fun clearEndpointStall(endpoint: UsbEndpoint) {
        connection.controlTransfer(USB_DIR_RECIPIENT_ENDPOINT, STALL_CLEAR_REQUEST, ENDPOINT_HALT, endpoint.endpointNumber, null, 0, 1000)
    }

    suspend fun exchange(
        command: ByteArray,
        expectedResponseSize: Int = endpoints.recommendedBufferSize,
        writeTimeout: Int = endpoints.recommendedTimeout,
        readTimeout: Int = endpoints.recommendedTimeout
    ): Pair<TransferResult, TransferResult> {
        val writeResult = write(command, writeTimeout)
        if (!writeResult.success) {
            return Pair(writeResult, TransferResult(false, 0, null, "Read skipped — write failed"))
        }
        val readResult = read(expectedResponseSize, readTimeout)
        return Pair(writeResult, readResult)
    }
}
