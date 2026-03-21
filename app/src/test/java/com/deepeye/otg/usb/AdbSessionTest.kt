package com.deepeye.otg.usb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque

class AdbSessionTest {

    @Test
    fun `connect reads fragmented header and payload`() = kotlinx.coroutines.test.runTest {
        val response = AdbMessage(
            command = AdbProtocol.A_CNXN,
            arg0 = AdbProtocol.CONNECT_VERSION,
            arg1 = 8192,
            data = "device::ro.product.name=test".toByteArray()
        ).serialize()

        val transport = FakeAdbTransport(
            readChunks = ArrayDeque(
                listOf(
                    response.copyOfRange(0, 10),
                    response.copyOfRange(10, 24),
                    response.copyOfRange(24, 30),
                    response.copyOfRange(30, response.size)
                )
            )
        )

        val session = AdbSession(transport)

        assertTrue(session.connect())
        assertEquals(1, transport.writes.size)
        val writtenHeader = transport.writes.first().copyOfRange(0, 4)
        assertEquals("CNXN", String(writtenHeader))
    }
}

private class FakeAdbTransport(
    private val readChunks: ArrayDeque<ByteArray> = ArrayDeque()
) : UsbTransport {
    val writes = mutableListOf<ByteArray>()

    override suspend fun open(): Result<Unit> = Result.success(Unit)

    override suspend fun send(data: ByteArray, timeoutMs: Int): Result<Int> {
        writes += data.copyOf()
        return Result.success(data.size)
    }

    override suspend fun receive(length: Int, timeoutMs: Int): Result<ByteArray> {
        return readChunks.pollFirst()?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("No queued response"))
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
    ): Result<Int> = Result.failure(UnsupportedOperationException())

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
        interfaces = emptyList()
    )

    override suspend fun write(data: ByteArray, timeoutMs: Int?): TransferResult {
        writes += data.copyOf()
        return TransferResult.Success(data.size, data.copyOf())
    }

    override suspend fun read(expectedSize: Int, timeoutMs: Int?): TransferResult {
        return readChunks.pollFirst()?.let { TransferResult.Success(it.size, it) }
            ?: TransferResult.Timeout
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