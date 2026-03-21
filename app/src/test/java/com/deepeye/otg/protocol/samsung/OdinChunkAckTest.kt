package com.deepeye.otg.protocol.samsung

import com.deepeye.otg.protocol.RecordingUsbTransport
import com.deepeye.otg.usb.TransferResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayDeque

class OdinChunkAckTest {

    @Test
    fun `all acks ok completes flash`() = runTest {
        val transport = RecordingUsbTransport(
            readQueue = ArrayDeque(
                listOf(
                    ackSuccess(),
                    ackSuccess(),
                    ackSuccess()
                )
            )
        )

        val result = OdinProtocol.flashPartition(
            transport = transport,
            payload = ByteArray(8) { it.toByte() },
            chunkSize = 4,
            sessionId = "odin-ok"
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf(1024, 4, 1024, 4, 1024), transport.writes.map { it.size })
        assertEquals(0x65, littleEndianInt(transport.writes.first()))
    }

    @Test
    fun `chunk 3 rejected aborts remaining chunks`() = runTest {
        val transport = RecordingUsbTransport(
            readQueue = ArrayDeque(
                listOf(
                    ackSuccess(),
                    ackSuccess(),
                    ackSuccess(),
                    ackFailure(1)
                )
            )
        )

        val result = OdinProtocol.flashPartition(
            transport = transport,
            payload = ByteArray(20) { it.toByte() },
            chunkSize = 4,
            sessionId = "odin-reject"
        )

        assertTrue(result.exceptionOrNull() is OdinProtocol.OdinError.ChunkRejected)
        assertEquals(8, transport.writes.size)
    }

    @Test
    fun `final ack timeout returns final ack timeout`() = runTest {
        val transport = RecordingUsbTransport(
            readQueue = ArrayDeque(listOf(ackSuccess()))
        )

        val result = OdinProtocol.flashPartition(
            transport = transport,
            payload = ByteArray(4) { it.toByte() },
            chunkSize = 4,
            sessionId = "odin-final-timeout"
        )

        assertTrue(result.exceptionOrNull() is OdinProtocol.OdinError.FinalAckTimeout)
    }

    @Test
    fun `ack bytes with zero status are accepted`() = runTest {
        val transport = RecordingUsbTransport(
            readQueue = ArrayDeque(listOf(ackSuccess()))
        )

        val result = OdinProtocol.readOdinAck(
            transport = transport,
            chunkIndex = 0,
            sessionId = "odin-ack"
        )

        assertTrue(result.isSuccess)
        assertEquals(1024, (transport.read(1024) as? TransferResult.Success)?.data?.size ?: 1024)
    }

    private fun ackSuccess(): TransferResult.Success =
        TransferResult.Success(1024, ackPacket(statusCode = 0))

    private fun ackFailure(statusCode: Int): TransferResult.Success =
        TransferResult.Success(1024, ackPacket(statusCode = statusCode))

    private fun ackPacket(statusCode: Int): ByteArray {
        val buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(statusCode)
        return buffer.array()
    }

    private fun littleEndianInt(bytes: ByteArray): Int =
        ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
}