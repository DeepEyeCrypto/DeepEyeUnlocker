package com.deepeye.otg.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import com.deepeye.otg.util.bulkIn
import com.deepeye.otg.util.bulkOut
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val MTK_BROM_MAGIC_HOST = byteArrayOf(0xA0.toByte(), 0x0A, 0x50, 0x05)
private val MTK_BROM_MAGIC_RESPONSE = byteArrayOf(0x5F, 0xF5.toByte(), 0xAF.toByte())

@Singleton
class MtkBromExecutor @Inject constructor() {

    suspend fun loadDa(
        connection: UsbDeviceConnection,
        outEp: UsbEndpoint,
        inEp: UsbEndpoint,
        daBytes: ByteArray,
        sessionId: String
    ): MtkBromResult {
        val txMagic = connection.bulkOut(outEp, MTK_BROM_MAGIC_HOST, sessionId = sessionId, tag = "MTK_BROM")
        if (txMagic < 0) return MtkBromResult.Error("BROM magic TX failed")
        delay(50)

        val rxMagic = ByteArray(3)
        val rx = connection.bulkIn(inEp, rxMagic, len = 3, sessionId = sessionId, tag = "MTK_BROM")
        if (rx < 0 || !rxMagic.contentEquals(MTK_BROM_MAGIC_RESPONSE)) {
            return MtkBromResult.Error("BROM magic response mismatch")
        }

        val daLen = ByteArray(4).apply {
            this[0] = (daBytes.size and 0xFF).toByte()
            this[1] = ((daBytes.size shr 8) and 0xFF).toByte()
            this[2] = ((daBytes.size shr 16) and 0xFF).toByte()
            this[3] = ((daBytes.size shr 24) and 0xFF).toByte()
        }
        connection.bulkOut(outEp, daLen, sessionId = sessionId, tag = "MTK_BROM")
        val txDa = connection.bulkOut(outEp, daBytes, sessionId = sessionId, tag = "MTK_BROM")
        if (txDa < 0) return MtkBromResult.Error("DA upload failed")
        delay(150)

        val ack = ByteArray(1)
        val ackRx = connection.bulkIn(inEp, ack, len = 1, sessionId = sessionId, tag = "MTK_BROM")
        if (ackRx < 0 || ack[0] != 0xC0.toByte()) {
            return MtkBromResult.Error("DA ACK failed")
        }

        delay(2000)
        Timber.d("[MTK_BROM] DA loaded size=${daBytes.size} sessionId=$sessionId")
        return MtkBromResult.Success("DA loaded")
    }
}

sealed class MtkBromResult {
    data class Success(val message: String) : MtkBromResult()
    data class Error(val reason: String) : MtkBromResult()
}

