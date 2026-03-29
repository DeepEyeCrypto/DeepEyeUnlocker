package com.deepeye.otg.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import com.deepeye.otg.util.bulkIn
import com.deepeye.otg.util.bulkOut
import kotlinx.coroutines.delay

class BromExecutor(
    private val connection: UsbDeviceConnection,
    private val outEndpoint: UsbEndpoint,
    private val inEndpoint: UsbEndpoint,
    private val sessionId: String
) {

    companion object {
        private val HANDSHAKE_SEND = byteArrayOf(0xA0.toByte(), 0x0A, 0x50, 0x05)
        private val HANDSHAKE_EXPECT = byteArrayOf(
            0x5F.toByte(),
            0xF5.toByte(),
            0xAF.toByte(),
            0xFA.toByte()
        )
        private const val BROM_TIMEOUT = 3000
    }

    suspend fun handshake(): BromResult {
        val wake = byteArrayOf(0xFF.toByte())
        repeat(26) {
            val wakeResult = connection.bulkOut(
                ep = outEndpoint,
                data = wake,
                len = wake.size,
                timeoutMs = BROM_TIMEOUT,
                sessionId = sessionId,
                tag = "USB_SESSION"
            )
            if (wakeResult < 0) {
                return BromResult.Error("Wake sequence failed at step=${it + 1}, result=$wakeResult")
            }
            delay(20)
        }

        val sendResult = connection.bulkOut(
            ep = outEndpoint,
            data = HANDSHAKE_SEND,
            len = HANDSHAKE_SEND.size,
            timeoutMs = BROM_TIMEOUT,
            sessionId = sessionId,
            tag = "USB_SESSION"
        )
        if (sendResult < HANDSHAKE_SEND.size) {
            return BromResult.Error("Handshake send failed: $sendResult")
        }

        delay(50)

        val echo = ByteArray(4)
        val readResult = connection.bulkIn(
            ep = inEndpoint,
            buf = echo,
            len = echo.size,
            timeoutMs = BROM_TIMEOUT,
            sessionId = sessionId,
            tag = "USB_SESSION"
        )
        if (readResult < 4) {
            return BromResult.Error("Handshake echo incomplete: $readResult bytes")
        }

        if (!echo.contentEquals(HANDSHAKE_EXPECT)) {
            return BromResult.Error(
                "Handshake mismatch. got=${echo.toHexString()} expected=${HANDSHAKE_EXPECT.toHexString()}"
            )
        }

        return BromResult.Connected
    }

    suspend fun disableWatchdog(): BromResult {
        val wdtAddr = byteArrayOf(0x10, 0x00, 0x70, 0x00)
        val wdtDisable = byteArrayOf(0x22, 0x00, 0x22, 0x24)

        val addrResult = connection.bulkOut(
            ep = outEndpoint,
            data = wdtAddr,
            len = wdtAddr.size,
            timeoutMs = BROM_TIMEOUT,
            sessionId = sessionId,
            tag = "USB_SESSION"
        )
        if (addrResult < 0) return BromResult.Error("WDT address write failed: $addrResult")

        delay(20)

        val valueResult = connection.bulkOut(
            ep = outEndpoint,
            data = wdtDisable,
            len = wdtDisable.size,
            timeoutMs = BROM_TIMEOUT,
            sessionId = sessionId,
            tag = "USB_SESSION"
        )
        if (valueResult < 0) return BromResult.Error("WDT disable write failed: $valueResult")

        delay(100)
        return BromResult.Connected
    }

    suspend fun sendDownloadAgent(daBytes: ByteArray): BromResult {
        val chunkSize = 512
        var offset = 0

        while (offset < daBytes.size) {
            val end = minOf(offset + chunkSize, daBytes.size)
            val chunk = daBytes.copyOfRange(offset, end)
            val result = connection.bulkOut(
                ep = outEndpoint,
                data = chunk,
                len = chunk.size,
                timeoutMs = BROM_TIMEOUT,
                sessionId = sessionId,
                tag = "USB_SESSION"
            )
            if (result < 0) {
                return BromResult.Error("DA transfer failed at offset=$offset")
            }
            offset = end
            delay(20)
        }

        delay(500)

        val ack = ByteArray(2)
        val ackResult = connection.bulkIn(
            ep = inEndpoint,
            buf = ack,
            len = ack.size,
            timeoutMs = BROM_TIMEOUT,
            sessionId = sessionId,
            tag = "USB_SESSION"
        )
        if (ackResult < 2) return BromResult.Error("No DA ACK received")

        return if (ack[0] == 0x5A.toByte() && ack[1] == 0x5A.toByte()) {
            BromResult.DaReady
        } else {
            BromResult.Error("DA ACK invalid: ${ack.toHexString()}")
        }
    }

    private fun ByteArray.toHexString(): String =
        joinToString(" ") { "0x%02X".format(it) }
}

sealed class BromResult {
    data object Connected : BromResult()
    data object DaReady : BromResult()
    data class Error(val reason: String) : BromResult()
}

