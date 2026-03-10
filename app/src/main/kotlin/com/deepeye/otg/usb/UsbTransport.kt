package com.deepeye.otg.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min
import kotlin.math.pow

/**
 * Unified result for all USB transport operations.
 */
sealed class TransferResult {
    data class Success(val bytes: Int, val data: ByteArray? = null, val retryCount: Int = 0) : TransferResult()
    data class Partial(val bytes: Int, val data: ByteArray, val expected: Int) : TransferResult()
    object Timeout : TransferResult()
    object DeviceGone : TransferResult()
    object Stall : TransferResult()
    data class IOError(val exception: Throwable) : TransferResult() {
        constructor(msg: String) : this(IllegalStateException(msg))
        val msg: String get() = exception.message ?: exception::class.java.simpleName
    }
    data class ProtocolError(val code: Int, val msg: String) : TransferResult() {
        constructor(msg: String) : this(-1, msg)
    }
    data class NullConnection(val message: String) : TransferResult()

    val isSuccess: Boolean get() = this is Success
}

/**
 * Core transport interface for protocol sessions (Stage 3.1).
 */
interface UsbTransport {
    suspend fun open(): Result<Unit>
    suspend fun send(data: ByteArray, timeoutMs: Int = 5000): Result<Int>
    suspend fun receive(length: Int, timeoutMs: Int = 5000): Result<ByteArray>
    suspend fun sendAndReceive(
        data: ByteArray,
        receiveLength: Int,
        sendTimeout: Int = 5000,
        receiveTimeout: Int = 5000
    ): Result<ByteArray>
    suspend fun controlTransfer(
        requestType: Int,
        request: Int,
        value: Int,
        index: Int,
        buffer: ByteArray?,
        length: Int,
        timeout: Int
    ): Result<Int>
    fun close()
    val isOpen: Boolean
    val deviceInfo: UsbDescriptorSnapshot

    // Compatibility API used by existing code.
    suspend fun write(data: ByteArray, timeoutMs: Int? = null): TransferResult
    suspend fun read(expectedSize: Int, timeoutMs: Int? = null): TransferResult
    suspend fun control(
        requestType: Int,
        request: Int,
        value: Int,
        index: Int,
        data: ByteArray?,
        timeoutMs: Int? = null
    ): TransferResult
}

/**
 * Standard implementation for Bulk-based protocols (ADB, Fastboot, BROM, EDL).
 */
class BulkTransport(
    private val connection: UsbDeviceConnection,
    private val endpoints: ResolvedEndpoints
) : UsbTransport {

    private val mutex = Mutex()
    private val MAX_CHUNK = 16384
    private val MAX_RETRIES = 3
    private val BASE_DELAY_MS = 100L

    override val isOpen: Boolean
        get() = runCatching { connection.fileDescriptor >= 0 }.getOrDefault(false)

    override val deviceInfo: UsbDescriptorSnapshot = UsbDescriptorSnapshot(
        vendorId = 0,
        productId = 0,
        deviceClass = 0,
        deviceSubclass = 0,
        deviceProtocol = 0,
        manufacturerName = null,
        productName = null,
        interfaceCount = 1,
        interfaces = listOf(
            UsbInterfaceSnapshot(
                id = endpoints.interfaceIndex,
                interfaceClass = endpoints.usbInterface.interfaceClass,
                interfaceSubclass = endpoints.usbInterface.interfaceSubclass,
                interfaceProtocol = endpoints.usbInterface.interfaceProtocol,
                endpointCount = endpoints.usbInterface.endpointCount,
                endpoints = (0 until endpoints.usbInterface.endpointCount).map { idx ->
                    val ep = endpoints.usbInterface.getEndpoint(idx)
                    UsbEndpointSnapshot(
                        address = ep.address,
                        type = ep.type,
                        direction = ep.direction,
                        maxPacketSize = ep.maxPacketSize
                    )
                }
            )
        )
    )

    override suspend fun open(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isOpen) Result.failure(IllegalStateException("USB connection not open"))
        else Result.success(Unit)
    }

    override suspend fun send(data: ByteArray, timeoutMs: Int): Result<Int> {
        return when (val result = write(data, timeoutMs)) {
            is TransferResult.Success -> Result.success(result.bytes)
            is TransferResult.IOError -> Result.failure(result.exception)
            is TransferResult.NullConnection -> Result.failure(IllegalStateException(result.message))
            is TransferResult.Timeout -> Result.failure(IllegalStateException("USB send timeout"))
            is TransferResult.DeviceGone -> Result.failure(IllegalStateException("USB device gone"))
            is TransferResult.Stall -> Result.failure(IllegalStateException("USB endpoint stalled"))
            is TransferResult.Partial -> Result.failure(IllegalStateException("Partial USB send ${result.bytes}/${result.expected}"))
            is TransferResult.ProtocolError -> Result.failure(IllegalStateException(result.msg))
        }
    }

    override suspend fun receive(length: Int, timeoutMs: Int): Result<ByteArray> {
        return when (val result = read(length, timeoutMs)) {
            is TransferResult.Success -> Result.success(result.data ?: ByteArray(0))
            is TransferResult.IOError -> Result.failure(result.exception)
            is TransferResult.NullConnection -> Result.failure(IllegalStateException(result.message))
            is TransferResult.Timeout -> Result.failure(IllegalStateException("USB receive timeout"))
            is TransferResult.DeviceGone -> Result.failure(IllegalStateException("USB device gone"))
            is TransferResult.Stall -> Result.failure(IllegalStateException("USB endpoint stalled"))
            is TransferResult.Partial -> Result.success(result.data)
            is TransferResult.ProtocolError -> Result.failure(IllegalStateException(result.msg))
        }
    }

    override suspend fun sendAndReceive(
        data: ByteArray,
        receiveLength: Int,
        sendTimeout: Int,
        receiveTimeout: Int
    ): Result<ByteArray> {
        val sendResult = send(data, sendTimeout)
        if (sendResult.isFailure) return Result.failure(sendResult.exceptionOrNull()!!)
        return receive(receiveLength, receiveTimeout)
    }

    override suspend fun controlTransfer(
        requestType: Int,
        request: Int,
        value: Int,
        index: Int,
        buffer: ByteArray?,
        length: Int,
        timeout: Int
    ): Result<Int> {
        val safeBuffer = if (length == 0) null else (buffer ?: ByteArray(length))
        return when (val result = control(requestType, request, value, index, safeBuffer, timeout)) {
            is TransferResult.Success -> Result.success(result.bytes)
            is TransferResult.IOError -> Result.failure(result.exception)
            is TransferResult.NullConnection -> Result.failure(IllegalStateException(result.message))
            is TransferResult.Timeout -> Result.failure(IllegalStateException("USB control timeout"))
            is TransferResult.DeviceGone -> Result.failure(IllegalStateException("USB device gone"))
            is TransferResult.Stall -> Result.failure(IllegalStateException("USB endpoint stalled"))
            is TransferResult.Partial -> Result.failure(IllegalStateException("Partial control transfer"))
            is TransferResult.ProtocolError -> Result.failure(IllegalStateException(result.msg))
        }
    }

    override fun close() {
        runCatching {
            connection.releaseInterface(endpoints.usbInterface)
        }
        runCatching {
            connection.close()
        }
    }

    override suspend fun write(data: ByteArray, timeoutMs: Int?): TransferResult = withContext(Dispatchers.IO) {
        if (!isOpen) return@withContext TransferResult.NullConnection("USB connection is closed")
        val outEp = endpoints.bulkOut ?: return@withContext TransferResult.IOError("No BULK OUT endpoint")
        val timeout = timeoutMs ?: endpoints.recommendedTimeout

        mutex.withLock {
            var attempt = 0
            while (attempt < MAX_RETRIES) {
                val result = chunkedWrite(data, outEp, timeout)
                if (result is TransferResult.Success) return@withLock result.copy(retryCount = attempt)
                
                if (result is TransferResult.Stall) {
                    clearStall(outEp)
                }

                val delay = BASE_DELAY_MS * 2.0.pow(attempt.toDouble()).toLong()
                delay(min(delay, 2000L))
                attempt++
            }
            TransferResult.IOError("Bulk write failed after $MAX_RETRIES retries")
        }
    }

    override suspend fun read(expectedSize: Int, timeoutMs: Int?): TransferResult = withContext(Dispatchers.IO) {
        if (!isOpen) return@withContext TransferResult.NullConnection("USB connection is closed")
        val inEp = endpoints.bulkIn ?: return@withContext TransferResult.IOError("No BULK IN endpoint")
        val timeout = timeoutMs ?: endpoints.recommendedTimeout

        mutex.withLock {
            var attempt = 0
            while (attempt < MAX_RETRIES) {
                val buffer = ByteArray(expectedSize)
                val received = connection.bulkTransfer(inEp, buffer, expectedSize, timeout)
                
                when {
                    received >= 0 -> return@withLock TransferResult.Success(received, buffer.copyOf(received), attempt)
                    else -> {
                        if (isStalled(inEp)) clearStall(inEp)
                        if (!isOpen) return@withLock TransferResult.DeviceGone
                    }
                }
                
                val delay = BASE_DELAY_MS * 2.0.pow(attempt.toDouble()).toLong()
                delay(min(delay, 2000L))
                attempt++
            }
            TransferResult.Timeout
        }
    }

    override suspend fun control(
        requestType: Int,
        request: Int,
        value: Int,
        index: Int,
        data: ByteArray?,
        timeoutMs: Int?
    ): TransferResult = withContext(Dispatchers.IO) {
        if (!isOpen) return@withContext TransferResult.NullConnection("USB connection is closed")
        val timeout = timeoutMs ?: 1000
        val len = data?.size ?: 0
        val result = connection.controlTransfer(requestType, request, value, index, data, len, timeout)
        
        if (result >= 0) {
            TransferResult.Success(result, data)
        } else {
            TransferResult.IOError("Control transfer failed: $result")
        }
    }

    /** Helper for standard request-response cycles. */
    suspend fun exchange(
        command: ByteArray,
        expectedResponseSize: Int,
        writeTimeout: Int? = null,
        readTimeout: Int? = null
    ): Pair<TransferResult, TransferResult> {
        val w = write(command, writeTimeout)
        if (!w.isSuccess) return Pair(w, TransferResult.IOError("Read skipped"))
        val r = read(expectedResponseSize, readTimeout)
        return Pair(w, r)
    }

    private fun chunkedWrite(data: ByteArray, ep: UsbEndpoint, timeout: Int): TransferResult {
        var offset = 0
        while (offset < data.size) {
            if (!isOpen) return TransferResult.DeviceGone
            val chunkLen = min(MAX_CHUNK, data.size - offset)
            val sent = connection.bulkTransfer(ep, data, offset, chunkLen, timeout)
            if (sent < 0) return if (isStalled(ep)) TransferResult.Stall else TransferResult.IOError("Transfer error $sent")
            offset += sent
        }
        return TransferResult.Success(data.size, data)
    }

    private fun isStalled(ep: UsbEndpoint): Boolean {
        val buf = ByteArray(2)
        val r = connection.controlTransfer(0x82, 0x00, 0, ep.endpointNumber, buf, 2, 500)
        return r >= 0 && (buf[0].toInt() and 0x01) != 0
    }

    private fun clearStall(ep: UsbEndpoint) {
        connection.controlTransfer(0x02, 0x01, 0x00, ep.endpointNumber, null, 0, 500)
    }
}
