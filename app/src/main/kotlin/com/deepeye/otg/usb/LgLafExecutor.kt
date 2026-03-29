package com.deepeye.otg.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import com.deepeye.otg.util.bulkIn
import com.deepeye.otg.util.bulkOut
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LgLafExecutor @Inject constructor() {

    suspend fun sendCommand(
        connection: UsbDeviceConnection,
        outEp: UsbEndpoint,
        inEp: UsbEndpoint,
        command: String,
        arg1: Int,
        arg2: Int,
        data: ByteArray,
        sessionId: String
    ): LgLafResult {
        val header = buildHeader(command, arg1, arg2, data.size)
        val txHead = connection.bulkOut(outEp, header, sessionId = sessionId, tag = "LG_LAF")
        if (txHead < 0) return LgLafResult.Error("LAF header TX failed")

        if (data.isNotEmpty()) {
            val txData = connection.bulkOut(outEp, data, sessionId = sessionId, tag = "LG_LAF")
            if (txData < 0) return LgLafResult.Error("LAF data TX failed")
        }

        delay(20)
        val ack = ByteArray(20)
        val rx = connection.bulkIn(inEp, ack, len = 20, sessionId = sessionId, tag = "LG_LAF")
        if (rx < 0) return LgLafResult.Error("LAF ACK RX failed")

        val validMagic = ack[0] == 0x34.toByte() && ack[1] == 0x12.toByte() &&
            ack[2] == 0x00.toByte() && ack[3] == 0x00.toByte()
        if (!validMagic) return LgLafResult.Error("LAF ACK magic mismatch")

        return LgLafResult.Success("LAF command $command success")
    }

    private fun buildHeader(command: String, arg1: Int, arg2: Int, dataLen: Int): ByteArray {
        val cmd = command.padEnd(4, ' ').take(4).toByteArray()
        return ByteArray(20).apply {
            this[0] = 0x34
            this[1] = 0x12
            this[2] = 0x00
            this[3] = 0x00
            this[4] = cmd[0]
            this[5] = cmd[1]
            this[6] = cmd[2]
            this[7] = cmd[3]
            writeLe32(this, 8, arg1)
            writeLe32(this, 12, arg2)
            writeLe32(this, 16, dataLen)
        }
    }

    private fun writeLe32(buf: ByteArray, off: Int, value: Int) {
        buf[off] = (value and 0xFF).toByte()
        buf[off + 1] = ((value shr 8) and 0xFF).toByte()
        buf[off + 2] = ((value shr 16) and 0xFF).toByte()
        buf[off + 3] = ((value shr 24) and 0xFF).toByte()
    }
}

sealed class LgLafResult {
    data class Success(val message: String) : LgLafResult()
    data class Error(val reason: String) : LgLafResult()
}

