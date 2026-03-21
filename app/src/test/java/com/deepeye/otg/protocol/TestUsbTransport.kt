package com.deepeye.otg.protocol

import com.deepeye.otg.usb.TransferResult
import com.deepeye.otg.usb.UsbDescriptorSnapshot
import com.deepeye.otg.usb.UsbInterfaceSnapshot
import com.deepeye.otg.usb.UsbTransport
import java.util.ArrayDeque

class RecordingUsbTransport(
    private val receiveQueue: ArrayDeque<ByteArray> = ArrayDeque(),
    private val readQueue: ArrayDeque<TransferResult> = ArrayDeque(),
    private val controlResponses: ArrayDeque<Result<Int>> = ArrayDeque()
) : UsbTransport {
    val sent = mutableListOf<ByteArray>()
    val writes = mutableListOf<ByteArray>()

    override suspend fun open(): Result<Unit> = Result.success(Unit)

    override suspend fun send(data: ByteArray, timeoutMs: Int): Result<Int> {
        sent += data.copyOf()
        return Result.success(data.size)
    }

    override suspend fun receive(length: Int, timeoutMs: Int): Result<ByteArray> {
        return receiveQueue.pollFirst()?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("No queued receive response"))
    }

    override suspend fun sendAndReceive(
        data: ByteArray,
        receiveLength: Int,
        sendTimeout: Int,
        receiveTimeout: Int
    ): Result<ByteArray> {
        send(data, sendTimeout)
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
        return controlResponses.pollFirst() ?: Result.success(length)
    }

    override fun close() = Unit

    override val isOpen: Boolean = true

    override val deviceInfo: UsbDescriptorSnapshot = UsbDescriptorSnapshot(
        vendorId = 0,
        productId = 0,
        deviceClass = 0,
        deviceSubclass = 0,
        deviceProtocol = 0,
        manufacturerName = null,
        productName = null,
        interfaceCount = 0,
        interfaces = emptyList<UsbInterfaceSnapshot>()
    )

    override suspend fun write(data: ByteArray, timeoutMs: Int?): TransferResult {
        writes += data.copyOf()
        return TransferResult.Success(data.size, data.copyOf())
    }

    override suspend fun read(expectedSize: Int, timeoutMs: Int?): TransferResult {
        return readQueue.pollFirst() ?: TransferResult.Timeout
    }

    override suspend fun control(
        requestType: Int,
        request: Int,
        value: Int,
        index: Int,
        data: ByteArray?,
        timeoutMs: Int?
    ): TransferResult = TransferResult.IOError("Unsupported")
}