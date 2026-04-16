package com.deepeye.otg.engine

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * MTK SLA (Serial Link Authentication) Bypass Engine
 * Supports both Helio G series and Dimensity series
 * Fixed for RMX3845 (MT6789 Helio G99)
 */
class MtkSlaBypassEngine(
    private val connection: UsbDeviceConnection,
    private val bulkOut: UsbEndpoint,
    private val bulkIn: UsbEndpoint
) {
    companion object {
        const val TIMEOUT_MS = 5000
        const val CMD_GET_SLA_CHALLENGE = 0xC8.toByte()
    }

    /**
     * Run SLA bypass using existing BROM connection
     * NO re-handshake - device is already connected!
     */
    suspend fun runSlaBypass(chipId: Int): SlaResult = withContext(Dispatchers.IO) {
        Timber.d("[SLA] Starting SLA bypass for chip 0x${chipId.toString(16).uppercase()}")

        // Determine chip family and SLA method
        val chipFamily = when (chipId) {
            // Helio G series — SLA method A
            0x6789 -> "Helio G99 (RMX3845)"
            0x6785 -> "Helio G95"
            0x6781 -> "Helio G85/G80"
            0x6768 -> "Helio G85"
            0x6765 -> "Helio G35/P35"
            0x6762 -> "Helio G25/P22"
            // Dimensity series — SLA method B
            0x6833 -> "Dimensity 700"
            0x6853 -> "Dimensity 720"
            0x6873 -> "Dimensity 800"
            0x6877 -> "Dimensity 900"
            0x6879 -> "Dimensity 1080"
            0x6895 -> "Dimensity 8100"
            0x6983 -> "Dimensity 9000/9200"
            else -> "Unknown MTK 0x${chipId.toString(16).uppercase()}"
        }

        Timber.d("[SLA] Chip family: $chipFamily")

        val slaMethod = when (chipId) {
            in 0x6760..0x6799 -> SlaMethod.HELIO_G   // Helio G series
            in 0x6830..0x6990 -> SlaMethod.DIMENSITY  // Dimensity
            else -> SlaMethod.GENERIC
        }

        Timber.d("[SLA] SLA method: $slaMethod")

        // Execute chip-specific SLA bypass
        val result = when (slaMethod) {
            SlaMethod.HELIO_G -> helioGSlaBypass(chipId)
            SlaMethod.DIMENSITY -> dimensitySlaBypass(chipId)
            SlaMethod.GENERIC -> genericSlaBypass(chipId)
        }

        result
    }

    /**
     * Helio G Series SLA Bypass (MT6789, MT6785, etc.)
     * Many Helio G99 units have SLA disabled!
     */
    private suspend fun helioGSlaBypass(chipId: Int): SlaResult = withContext(Dispatchers.IO) {
        Timber.d("[SLA] Helio G series SLA bypass sequence")

        try {
            // Step 1: Try to get SLA challenge
            val cmdSent = connection.bulkTransfer(
                bulkOut,
                byteArrayOf(CMD_GET_SLA_CHALLENGE),
                1,
                TIMEOUT_MS
            )

            if (cmdSent < 0) {
                Timber.w("[SLA] No response to SLA challenge — device may not need SLA")
                return@withContext SlaResult.Skipped(
                    "SLA not required on this ${getChipName(chipId)} unit"
                )
            }

            // Step 2: Read challenge (16 bytes)
            val challenge = ByteArray(0x10)
            val readLen = connection.bulkTransfer(
                bulkIn,
                challenge,
                0x10,
                TIMEOUT_MS
            )

            if (readLen <= 0) {
                Timber.w("[SLA] No challenge response — SLA likely disabled")
                return@withContext SlaResult.Skipped(
                    "SLA disabled on this ${getChipName(chipId)} unit — proceeding to DA load"
                )
            }

            Timber.d("[SLA] Challenge received: ${challenge.toHexString()}")

            // Step 3: Send null auth payload (bypass for unlocked devices)
            val nullAuth = ByteArray(0x100) { 0x00 }
            val authSent = connection.bulkTransfer(
                bulkOut,
                nullAuth,
                nullAuth.size,
                TIMEOUT_MS
            )

            if (authSent < 0) {
                return@withContext SlaResult.Error("Failed to send auth payload")
            }

            // Step 4: Read auth response (2 bytes status)
            val authResp = ByteArray(2)
            val respLen = connection.bulkTransfer(
                bulkIn,
                authResp,
                2,
                TIMEOUT_MS
            )

            if (respLen < 2) {
                return@withContext SlaResult.Error("Invalid auth response length")
            }

            val status = ((authResp[0].toInt() and 0xFF) shl 8) or
                    (authResp[1].toInt() and 0xFF)

            when (status) {
                0x0000 -> {
                    Timber.d("[SLA] ✅ SLA bypassed!")
                    SlaResult.Success("SLA bypassed on ${getChipName(chipId)}!")
                }
                0x0001 -> {
                    Timber.d("[SLA] ✅ SLA not enforced")
                    SlaResult.Skipped("SLA not enforced on this unit")
                }
                else -> {
                    Timber.e("[SLA] ❌ SLA failed: 0x${status.toString(16)}")
                    SlaResult.Error("SLA auth rejected: 0x${status.toString(16)}")
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "[SLA] Exception during Helio G SLA bypass")
            SlaResult.Error("SLA exception: ${e.message}")
        }
    }

    /**
     * Dimensity Series SLA Bypass
     * Newer chips require certificate-based auth
     */
    private suspend fun dimensitySlaBypass(chipId: Int): SlaResult = withContext(Dispatchers.IO) {
        Timber.d("[SLA] Dimensity series SLA bypass sequence")

        try {
            // Same challenge-response as Helio G but with cert
            val cmdSent = connection.bulkTransfer(
                bulkOut,
                byteArrayOf(CMD_GET_SLA_CHALLENGE),
                1,
                TIMEOUT_MS
            )

            if (cmdSent < 0) {
                return@withContext SlaResult.Skipped(
                    "SLA challenge failed — may not be required"
                )
            }

            val challenge = ByteArray(0x10)
            val readLen = connection.bulkTransfer(
                bulkIn,
                challenge,
                0x10,
                TIMEOUT_MS
            )

            if (readLen <= 0) {
                return@withContext SlaResult.Skipped("SLA disabled on this Dimensity unit")
            }

            Timber.d("[SLA] Dimensity challenge: ${challenge.toHexString()}")

            // For Dimensity, we send bypass payload instead of cert
            val bypassPayload = buildDimensityBypassPayload(chipId)
            val payloadSent = connection.bulkTransfer(
                bulkOut,
                bypassPayload,
                bypassPayload.size,
                TIMEOUT_MS
            )

            if (payloadSent < 0) {
                return@withContext SlaResult.Error("Failed to send bypass payload")
            }

            val authResp = ByteArray(2)
            val respLen = connection.bulkTransfer(
                bulkIn,
                authResp,
                2,
                TIMEOUT_MS
            )

            if (respLen < 2) {
                return@withContext SlaResult.Error("Invalid response")
            }

            val status = ((authResp[0].toInt() and 0xFF) shl 8) or
                    (authResp[1].toInt() and 0xFF)

            if (status == 0x0000 || status == 0x0001) {
                SlaResult.Success("SLA bypassed on ${getChipName(chipId)}!")
            } else {
                SlaResult.Error("SLA rejected: 0x${status.toString(16)}")
            }

        } catch (e: Exception) {
            Timber.e(e, "[SLA] Exception during Dimensity SLA bypass")
            SlaResult.Error("SLA exception: ${e.message}")
        }
    }

    /**
     * Generic SLA Bypass (fallback for unknown chips)
     */
    private suspend fun genericSlaBypass(chipId: Int): SlaResult = withContext(Dispatchers.IO) {
        Timber.d("[SLA] Generic SLA bypass for 0x${chipId.toString(16).uppercase()}")

        // Try simple challenge first
        return@withContext helioGSlaBypass(chipId)
    }

    /**
     * Build Dimensity-specific bypass payload
     */
    private fun buildDimensityBypassPayload(chipId: Int): ByteArray {
        // Generic bypass payload for Dimensity chips
        // This is a simplified version - real payload may vary
        return byteArrayOf(
            0xA0.toByte(), 0x0A, 0x50.toByte(), 0x05,  // Handshake
            0x01, 0x00, 0x00, 0x00  // Bypass flag
        )
    }

    /**
     * Get chip name from ID
     */
    private fun getChipName(chipId: Int): String = when (chipId) {
        0x6789 -> "MT6789 Helio G99"
        0x6785 -> "MT6785 Helio G95"
        0x6781 -> "MT6781 Helio G85"
        0x6768 -> "MT6768 Helio G85"
        0x6765 -> "MT6765 Helio G35"
        0x6877 -> "MT6877 Dimensity 900"
        0x6879 -> "MT6879 Dimensity 1080"
        0x6895 -> "MT6895 Dimensity 8100"
        0x6983 -> "MT6983 Dimensity 9000"
        else -> "MTK 0x${chipId.toString(16).uppercase()}"
    }

    /**
     * Extension function to convert ByteArray to hex string
     */
    private fun ByteArray.toHexString(): String =
        joinToString(" ") { "0x${it.toInt().and(0xFF).toString(16).padStart(2, '0')}" }
}

/**
 * SLA bypass result types
 */
sealed class SlaResult {
    data class Success(val message: String) : SlaResult()
    data class Skipped(val message: String) : SlaResult()  // SLA not needed!
    data class Error(val message: String) : SlaResult()
}

/**
 * SLA method types
 */
enum class SlaMethod {
    HELIO_G,      // Helio G series (MT67xx)
    DIMENSITY,    // Dimensity series (MT68xx/69xx)
    GENERIC       // Unknown/fallback
}
