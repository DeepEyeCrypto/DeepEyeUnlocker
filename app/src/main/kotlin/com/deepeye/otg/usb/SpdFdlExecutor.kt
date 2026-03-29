package com.deepeye.otg.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import com.deepeye.otg.util.bulkIn
import com.deepeye.otg.util.bulkOut
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpdFdlExecutor @Inject constructor() {

    suspend fun sendFdlStages(
        connection: UsbDeviceConnection,
        outEp: UsbEndpoint,
        inEp: UsbEndpoint,
        fdl1: ByteArray,
        fdl2: ByteArray,
        sessionId: String
    ): SpdFdlResult {
        val connectFrame = byteArrayOf(
            0x7E, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00,
            0x9A.toByte(), 0x7E
        )
        if (connection.bulkOut(outEp, connectFrame, sessionId = sessionId, tag = "SPD_FDL") < 0) {
            return SpdFdlResult.Error("SPD connect TX failed")
        }

        val ack = ByteArray(64)
        if (connection.bulkIn(inEp, ack, sessionId = sessionId, tag = "SPD_FDL") < 0) {
            return SpdFdlResult.Error("SPD connect ACK failed")
        }

        if (connection.bulkOut(outEp, fdl1, sessionId = sessionId, tag = "SPD_FDL") < 0) {
            return SpdFdlResult.Error("FDL1 upload failed")
        }
        delay(100)

        if (connection.bulkOut(outEp, fdl2, sessionId = sessionId, tag = "SPD_FDL") < 0) {
            return SpdFdlResult.Error("FDL2 upload failed")
        }
        delay(50)
        return SpdFdlResult.Success("FDL1/FDL2 loaded")
    }

    fun buildHdlcFrame(command: Short, data: ByteArray): ByteArray {
        val payload = ByteArray(4 + data.size)
        payload[0] = ((command.toInt() shr 8) and 0xFF).toByte()
        payload[1] = (command.toInt() and 0xFF).toByte()
        payload[2] = ((data.size shr 8) and 0xFF).toByte()
        payload[3] = (data.size and 0xFF).toByte()
        data.copyInto(payload, 4)
        val crc = crc16(payload)
        return byteArrayOf(0x7E) + payload +
            byteArrayOf(((crc shr 8) and 0xFF).toByte(), (crc and 0xFF).toByte()) +
            byteArrayOf(0x7E)
    }

    private fun crc16(input: ByteArray): Int {
        var crc = 0xFFFF
        input.forEach { b ->
            crc = crc xor (b.toInt() and 0xFF)
            repeat(8) {
                crc = if ((crc and 1) != 0) (crc ushr 1) xor 0xA001 else crc ushr 1
            }
        }
        return crc and 0xFFFF
    }
}

sealed class SpdFdlResult {
    data class Success(val message: String) : SpdFdlResult()
    data class Error(val reason: String) : SpdFdlResult()
}

