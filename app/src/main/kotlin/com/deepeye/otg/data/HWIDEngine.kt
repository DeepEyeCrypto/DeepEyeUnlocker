package com.deepeye.otg.data

import android.os.Build
import java.security.MessageDigest
import java.util.Locale

/**
 * Generates a deterministic Hardware ID (HWID) for license binding.
 * Uses persistent device identifiers passed through a SHA-256 hash.
 */
object HWIDEngine {

    fun getHWID(): String {
        val rawId = buildString {
            append(Build.BOARD)
            append(Build.BRAND)
            append(Build.DEVICE)
            append(Build.HARDWARE)
            append(Build.MANUFACTURER)
            append(Build.MODEL)
            append(Build.PRODUCT)
            append(Build.TYPE)
            append(Build.SOC_MANUFACTURER ?: "UNKNOWN")
            append(Build.SOC_MODEL ?: "UNKNOWN")
            // Note: Secure.ANDROID_ID or Build.SERIAL could be added if READ_PHONE_STATE is available,
            // but for generic OTG unlocker, we prefer non-permission-based stable fingerprint.
        }

        return sha256(rawId).take(16).uppercase(Locale.US)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
