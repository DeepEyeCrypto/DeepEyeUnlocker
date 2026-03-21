package com.deepeye.otg.protocol.mtk

import com.deepeye.otg.protocol.RecordingUsbTransport
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque

class MtkDaChecksumTest {

    @Test
    fun `correct checksum triggers jump da`() = runTest {
        val daBytes = byteArrayOf(0x10, 0x20, 0x30)
        val transport = RecordingUsbTransport(
            receiveQueue = ArrayDeque(
                listOf(
                    byteArrayOf(0x5F),
                    byteArrayOf(0xF5.toByte()),
                    byteArrayOf(0xAF.toByte()),
                    byteArrayOf(0xFA.toByte()),
                    byteArrayOf(0x00, 0x00),
                    checksumBytes(daBytes),
                    byteArrayOf(0x00, 0x00),
                    byteArrayOf(0x5A.toByte())
                )
            )
        )

        val session = MtkSession(transport)

        assertTrue(session.connect())
        assertTrue(session.loadDownloadAgent(daBytes))
        assertTrue(transport.sent.any { it.isNotEmpty() && it[0] == 0xD5.toByte() })
    }

    @Test
    fun `wrong checksum returns mismatch and skips jump da`() = runTest {
        val daBytes = byteArrayOf(0x10, 0x20, 0x30)
        val mismatch = MtkBromProtocol.verifyDaChecksum(
            transport = RecordingUsbTransport(
                receiveQueue = ArrayDeque(listOf(byteArrayOf(0x00, 0x00)))
            ),
            daBytes = daBytes,
            sessionId = "checksum-test"
        )
        assertTrue(mismatch.exceptionOrNull() is MtkBromProtocol.MtkError.DaChecksumMismatch)

        val transport = RecordingUsbTransport(
            receiveQueue = ArrayDeque(
                listOf(
                    byteArrayOf(0x5F),
                    byteArrayOf(0xF5.toByte()),
                    byteArrayOf(0xAF.toByte()),
                    byteArrayOf(0xFA.toByte()),
                    byteArrayOf(0x00, 0x00),
                    byteArrayOf(0x00, 0x00)
                )
            )
        )

        val session = MtkSession(transport)

        assertTrue(session.connect())
        assertFalse(session.loadDownloadAgent(daBytes))
        assertFalse(transport.sent.any { it.isNotEmpty() && it[0] == 0xD5.toByte() })
    }

    @Test
    fun `zero length da checksum is zero`() = runTest {
        val transport = RecordingUsbTransport(
            receiveQueue = ArrayDeque(listOf(byteArrayOf(0x00, 0x00)))
        )

        val result = MtkBromProtocol.verifyDaChecksum(
            transport = transport,
            daBytes = byteArrayOf(),
            sessionId = "zero-length"
        )

        assertTrue(result.isSuccess)
    }

    private fun checksumBytes(payload: ByteArray): ByteArray {
        val checksum = payload.fold(0) { acc, byte ->
            (acc + (byte.toInt() and 0xFF)) and 0xFFFF
        }
        return byteArrayOf(
            ((checksum shr 8) and 0xFF).toByte(),
            (checksum and 0xFF).toByte()
        )
    }
}