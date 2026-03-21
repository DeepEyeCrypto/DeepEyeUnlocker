package com.deepeye.otg.protocol.qualcomm

import com.deepeye.otg.protocol.RecordingUsbTransport
import com.deepeye.otg.usb.TransferResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque

class FirehoseZlpTest {

    @Test
    fun `payload exact multiple of max packet sends zlp`() = runTest {
        val transport = RecordingUsbTransport()

        val result = FirehoseProtocol.uploadFirehoseData(
            transport = transport,
            payload = ByteArray(8) { it.toByte() },
            maxPacketSize = 4,
            sessionId = "zlp"
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf(4, 4, 0), transport.writes.map { it.size })
    }

    @Test
    fun `payload not multiple of max packet skips zlp`() = runTest {
        val transport = RecordingUsbTransport()

        val result = FirehoseProtocol.uploadFirehoseData(
            transport = transport,
            payload = ByteArray(6) { it.toByte() },
            maxPacketSize = 4,
            sessionId = "no-zlp"
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf(4, 2), transport.writes.map { it.size })
    }

    @Test
    fun `split xml response is accumulated until closing tag`() = runTest {
        val transport = RecordingUsbTransport(
            readQueue = ArrayDeque(
                listOf(
                    TransferResult.Success(17, "<response value=\"".toByteArray()),
                    TransferResult.Success(16, "ACK\"></response>".toByteArray())
                )
            )
        )

        val result = FirehoseProtocol.readFirehoseResponse(
            transport = transport,
            timeoutMs = 100,
            maxPacketSize = 64,
            sessionId = "split"
        )

        assertTrue(result.isSuccess)
        assertEquals("<response value=\"ACK\"></response>", result.getOrThrow())
    }

    @Test
    fun `nak response returns error`() = runTest {
        val transport = RecordingUsbTransport(
            readQueue = ArrayDeque(
                listOf(
                    TransferResult.Success(32, "<response value=\"NAK\"></response>".toByteArray())
                )
            )
        )

        val result = FirehoseProtocol.sendCommandResult(
            transport = transport,
            xml = "<data><nop /></data>",
            sessionId = "nak"
        )

        assertTrue(result.exceptionOrNull() is FirehoseProtocol.FirehoseError.Nak)
    }
}