package com.deepeye.otg.protocol.fastboot

import com.deepeye.otg.usb.TransferResult
import com.deepeye.otg.usb.UsbDescriptorSnapshot
import com.deepeye.otg.usb.UsbInterfaceSnapshot
import com.deepeye.otg.usb.UsbTransport
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.ArrayDeque

class FastbootProtocolTest {

    @Test
    fun `getAllVariables collects info packets until okay`() = kotlinx.coroutines.test.runTest {
        val transport = FakeUsbTransport(
            readChunks = ArrayDeque(
                listOf(
                    "INFOproduct:panther".toByteArray(),
                    "INFOunlocked:yes".toByteArray(),
                    "OKAYdone".toByteArray()
                )
            )
        )

        val vars = FastbootProtocol.getAllVariables(transport)

        assertEquals("panther", vars["product"])
        assertEquals("yes", vars["unlocked"])
        assertEquals("getvar:all", String(transport.sent.single()))
    }

    @Test
    fun `executeCommand returns terminal okay with accumulated info messages`() = kotlinx.coroutines.test.runTest {
        val transport = FakeUsbTransport(
            readChunks = ArrayDeque(
                listOf(
                    "INFOslot-count:2".toByteArray(),
                    "OKAYcomplete".toByteArray()
                )
            )
        )

        val response = FastbootProtocol.executeCommand(transport, "getvar:slot-count")

        assertEquals(FastbootProtocol.ResponseType.OKAY, response.type)
        assertEquals(listOf("slot-count:2"), response.infoMessages)
        assertEquals("complete", response.message)
    }
}

private class FakeUsbTransport(
    private val readChunks: ArrayDeque<ByteArray> = ArrayDeque()
) : UsbTransport {
    val sent = mutableListOf<ByteArray>()

    override suspend fun open(): Result<Unit> = Result.success(Unit)

    override suspend fun send(data: ByteArray, timeoutMs: Int): Result<Int> {
        sent += data.copyOf()
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
        interfaces = emptyList<UsbInterfaceSnapshot>()
    )

    override suspend fun write(data: ByteArray, timeoutMs: Int?): TransferResult {
        sent += data.copyOf()
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
