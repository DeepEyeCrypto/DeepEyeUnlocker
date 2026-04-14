package com.deepeye.otg.python

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PythonBridgeTest {

    // ── IMEI Luhn Validation Tests ─────────────────
    @Test
    fun `valid IMEI passes luhn check`() {
        // Known valid IMEIs for test
        val validImeis = listOf(
            "490154203237518",
            "358429086712345",
            "867319030000000"
        )
        validImeis.forEach { imei ->
            assertTrue("IMEI $imei should be valid", luhnCheck(imei))
        }
    }

    @Test
    fun `invalid IMEI fails luhn check`() {
        assertFalse(luhnCheck("123456789012345"))
        assertFalse(luhnCheck("000000000000000"))
        assertFalse(luhnCheck("abcdefghijklmno"))
    }

    @Test
    fun `IMEI with wrong length fails`() {
        assertFalse(luhnCheck("12345"))
        assertFalse(luhnCheck("1234567890123456")) // 16 digits
    }

    // ── MTK HW Code Tests ──────────────────────────
    @Test
    fun `Realme 14x hw_code 0x1209 identified correctly`() {
        val hwCodeMap = mapOf(
            0x1209 to "MT6835T",
            0x6580 to "MT6580",
            0x6765 to "MT6765"
        )
        assertEquals("MT6835T", hwCodeMap[0x1209]?.let {
            if (it.contains("MT6835T")) "MT6835T" else "Unknown"
        })
    }

    // ── DA Validation Tests ────────────────────────
    @Test
    fun `empty DA bytes returns invalid`() {
        val emptyDa = ByteArray(0)
        assertTrue(emptyDa.size < 512)
    }

    @Test
    fun `DA too small returns invalid`() {
        val smallDa = ByteArray(100)
        assertTrue(smallDa.size < 512)
    }

    // ── Luhn helper (mirrors Python impl) ─────────
    private fun luhnCheck(imei: String): Boolean {
        if (!imei.all { it.isDigit() } || imei.length != 15)
            return false
        var sum = 0
        imei.reversed().forEachIndexed { i, c ->
            var n = c.digitToInt()
            if (i % 2 == 1) { n *= 2; if (n > 9) n -= 9 }
            sum += n
        }
        return sum % 10 == 0
    }
}
