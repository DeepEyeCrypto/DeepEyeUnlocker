package com.deepeye.otg.repair

import android.util.Log

/**
 * ── DeepEye Identity & Network Bridge ──────────────────────────
 * JNI layer integration for NVRAM/NV_ITEM repair operations.
 */
object NvBridge {

    private const val TAG = "DeepEye-NvBridge"

    // ── MTK Native Calls ─────────────────────────────────────────

    /** Reads IMEI1/IMEI2 from MTK NVRAM partition. Returns JSON. */
    external fun readMtkImei(handle: Long): String

    /** Writes new IMEI1/IMEI2 to MTK NVRAM. REQUIRES VALID LUHN. */
    external fun writeMtkImei(handle: Long, imei1: String, imei2: String): Boolean

    // ── QCOM Native Calls ────────────────────────────────────────

    /** Reads IMEI via DIAG command NV_ITEM 550. */
    external fun readQcomImei(handle: Long): String

    /** Writes arbitrary NV_ITEM to Qualcomm modem via DIAG. */
    external fun writeQcomNvItem(handle: Long, itemId: Int, data: ByteArray): Boolean

    // ── Kotlin Utils ─────────────────────────────────────────────

    /**
     * Luhn Checksum Validator (IMEI Standard)
     * 1. Every 2nd digit from right is doubled.
     * 2. If result > 9, subtract 9 (sum digits).
     * 3. Total sum mod 10 must be 0.
     */
    fun verifyImeiChecksum(imei: String): Boolean {
        if (imei.length != 15) return false
        
        var sum = 0
        for (i in 0 until 14) {
            var n = imei[i] - '0'
            if (i % 2 != 0) { // Every 2nd digit (0-indexed 1st, 3rd...)
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
        }
        
        val checkDigit = (10 - (sum % 10)) % 10
        return checkDigit == (imei[14] - '0')
    }

    /** Generates the 15th check digit for a 14-digit IMEI fragment. */
    fun generateCheckDigit(fragment: String): Int {
        if (fragment.length != 14) return -1
        
        var sum = 0
        for (i in 0 until 14) {
            var n = fragment[i] - '0'
            if (i % 2 != 0) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
        }
        return (10 - (sum % 10)) % 10
    }
}
