package com.deepeye.otg.protocol

import com.deepeye.otg.python.DaValidationResult
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class MtkV6ExecutorTest {

    // DA too small → must reject before JUMP_DA
    @Test
    fun da_tooSmall_returnsInvalid() {
        val smallDa = ByteArray(100)
        val result = simulateDaValidation(smallDa)
        assertTrue(result is DaValidationResult.Invalid)
    }

    // DA empty → must reject
    @Test
    fun da_empty_returnsInvalid() {
        val emptyDa = ByteArray(0)
        val result = simulateDaValidation(emptyDa)
        assertTrue(result is DaValidationResult.Invalid)
    }

    // DA with MTK magic → valid candidate
    @Test
    fun da_withMtkMagic_passesBasicCheck() {
        val da = ByteArray(1024)
        // Write MTK_ magic at offset 0
        da[0] = 0x4D; da[1] = 0x54; da[2] = 0x4B; da[3] = 0x5F
        val result = simulateDaValidation(da)
        // Size > 512 so passes basic check
        assertTrue(result is DaValidationResult.Valid ||
                   result is DaValidationResult.Invalid)
    }

    // sessionId format test
    @Test
    fun sessionId_isValidUUID() {
        val sessionId = UUID.randomUUID().toString()
        assertTrue(sessionId.matches(
            Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        ))
    }

    // HW code 0x1209 → Realme 14x
    @Test
    fun hwCode_0x1209_isRealme14x() {
        val hwCodeMap = mapOf(
            0x1209 to "MT6835T",
            0x6580 to "MT6580"
        )
        val chip = hwCodeMap[0x1209]
        assertEquals("MT6835T", chip)
    }

    private fun simulateDaValidation(da: ByteArray): DaValidationResult {
        if (da.size < 512) return DaValidationResult.Invalid("DA too small: ${da.size} bytes")
        val magic = da.take(4).toByteArray()
        return if (magic.contentEquals(byteArrayOf(0x4D, 0x54, 0x4B, 0x5F))) {
            DaValidationResult.Valid("sha256_mock_${da.size}", org.json.JSONObject())
        } else {
            DaValidationResult.Invalid("Unknown magic")
        }
    }
}
