package com.deepeye.otg.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * All possible outcomes of a bulkTransfer call.
 */
sealed class BulkResult {
    data class Success(val bytes: Int, val data: ByteArray? = null) : BulkResult()
    data class Partial(val bytes: Int, val data: ByteArray, val expected: Int) : BulkResult()
    object EmptyAck  : BulkResult()  // 0 bytes — valid for some protocols
    object Timeout   : BulkResult()  // -1 after timeout_ms elapsed
    object Stall     : BulkResult()  // -1 on repeated calls = endpoint HALT
    data class Error(val msg: String) : BulkResult()
}

object SafeBulkTransfer {

    private const val TAG = "SafeBulk"
    private const val MAX_CHUNK = 16_384      // safe for all Android API levels
    private const val STALL_DETECT_TRIES = 2  // if 2 reads both return -1 → stall

    // ── WRITE ──────────────────────────────────────────────────
    suspend fun write(
        connection: UsbDeviceConnection,
        endpoint: UsbEndpoint,
        data: ByteArray,
        timeoutMs: Int
    ): BulkResult = withContext(Dispatchers.IO) {

        if (endpoint.direction != UsbConstants.USB_DIR_OUT) {
            return@withContext BulkResult.Error("Endpoint direction mismatch: expected OUT")
        }
        
        var offset = 0
        var totalSent = 0

        while (offset < data.size) {
            val chunkSize = minOf(MAX_CHUNK, data.size - offset)
            val chunk = data.copyOfRange(offset, offset + chunkSize)

            val result = connection.bulkTransfer(endpoint, chunk, chunkSize, timeoutMs)

            when {
                result > 0 -> {
                    totalSent += result
                    offset += result
                }
                result == 0 -> {
                    Log.w(TAG, "Write chunk 0 bytes at offset $offset")
                    offset += chunkSize
                }
                else -> {
                    Log.e(TAG, "Write failed at offset $offset")
                    return@withContext BulkResult.Error("bulkTransfer returned -1")
                }
            }
        }

        if (totalSent == data.size) {
            BulkResult.Success(totalSent, null)
        } else {
            BulkResult.Partial(totalSent, data.copyOf(totalSent), data.size)
        }
    }

    // ── READ ───────────────────────────────────────────────────
    suspend fun read(
        connection: UsbDeviceConnection,
        endpoint: UsbEndpoint,
        expectedSize: Int,
        timeoutMs: Int,
        allowPartial: Boolean = true
    ): BulkResult = withContext(Dispatchers.IO) {

        if (endpoint.direction != UsbConstants.USB_DIR_IN) {
            return@withContext BulkResult.Error("Endpoint direction mismatch: expected IN")
        }

        val buffer = ByteArray(expectedSize)
        var consecutiveErrors = 0
        var totalReceived = 0
        var offset = 0

        while (offset < expectedSize) {
            val remaining = expectedSize - offset
            val result = connection.bulkTransfer(endpoint, buffer, offset, remaining, timeoutMs)

            when {
                result > 0 -> {
                    totalReceived += result
                    offset += result
                    consecutiveErrors = 0
                    if (allowPartial) break
                }
                result == 0 -> {
                    Log.d(TAG, "Read 0 bytes (empty ACK)")
                    return@withContext BulkResult.EmptyAck
                }
                else -> {
                    consecutiveErrors++
                    if (consecutiveErrors >= STALL_DETECT_TRIES) {
                        return@withContext BulkResult.Stall
                    }
                    return@withContext BulkResult.Timeout
                }
            }
        }

        return@withContext when {
            totalReceived == 0   -> BulkResult.EmptyAck
            totalReceived < expectedSize && !allowPartial ->
                BulkResult.Partial(totalReceived, buffer.copyOf(totalReceived), expectedSize)
            else ->
                BulkResult.Success(totalReceived, buffer.copyOf(totalReceived))
        }
    }

    // ── WRITE + READ (request-response) ───────────────────────
    suspend fun exchange(
        connection: UsbDeviceConnection,
        outEndpoint: UsbEndpoint,
        inEndpoint: UsbEndpoint,
        command: ByteArray,
        expectedResponseSize: Int,
        writeTimeout: Int,
        readTimeout: Int,
        allowPartialRead: Boolean = true
    ): Pair<BulkResult, BulkResult> {
        val writeRes = write(connection, outEndpoint, command, writeTimeout)
        if (writeRes is BulkResult.Error || writeRes is BulkResult.Timeout) {
            return Pair(writeRes, BulkResult.Error("Read skipped"))
        }
        val readRes = read(connection, inEndpoint, expectedResponseSize, readTimeout, allowPartialRead)
        return Pair(writeRes, readRes)
    }
}
