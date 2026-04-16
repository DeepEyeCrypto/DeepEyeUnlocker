package com.deepeye.otg.engine

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Enhanced MTK BROM Engine for RMX3845 (Helio G99 / MT6789)
 * Provides complete BROM handshake and chip detection
 */
class MtkBromEngine(
    private val connection: UsbDeviceConnection,
    private val bulkOut: UsbEndpoint,
    private val bulkIn: UsbEndpoint
) {
    companion object {
        // MT6789 / Helio G99 chip ID
        const val MT6789_CHIP_ID = 0x6789

        // BROM handshake sequence (standard MTK)
        val HANDSHAKE_SEND = byteArrayOf(
            0xA0.toByte(), 0x0A.toByte(),
            0x50.toByte(), 0x05.toByte()
        )
        val HANDSHAKE_EXPECT = byteArrayOf(
            0x5F.toByte(), 0xF5.toByte(),
            0xAF.toByte(), 0xFA.toByte()
        )

        // MTK commands
        const val CMD_GET_HW_CODE      = 0xFD.toByte()
        const val CMD_GET_TARGET_CONFIG = 0xD8.toByte()
        const val CMD_GET_HW_SW_VER    = 0xFB.toByte()
        const val CMD_SEND_DA          = 0xD7.toByte()
        const val CMD_JUMP_DA          = 0xD5.toByte()

        // Timeouts
        const val TIMEOUT_MS = 5000
        const val SHORT_TIMEOUT = 2000
    }

    // ────────────────────────────────────────────────────
    // 1. BROM Handshake
    // ────────────────────────────────────────────────────
    suspend fun performHandshake(): BromResult = withContext(Dispatchers.IO) {
        Timber.d("[BROM] Starting MTK handshake for MT6789 (RMX3845)")

        HANDSHAKE_SEND.forEachIndexed { i, byte ->
            val sent = connection.bulkTransfer(bulkOut, byteArrayOf(byte), 1, TIMEOUT_MS)
            if (sent < 0) {
                return@withContext BromResult.Error(
                    stage = "Handshake",
                    message = "Handshake send failed at byte $i"
                )
            }

            val buf = ByteArray(1)
            val received = connection.bulkTransfer(bulkIn, buf, 1, TIMEOUT_MS)
            val resp = if (received > 0) buf[0] else null

            if (resp != HANDSHAKE_EXPECT[i]) {
                Timber.e("[BROM] Handshake byte $i mismatch: " +
                    "sent=0x${byte.toString(16).uppercase()} " +
                    "expected=0x${HANDSHAKE_EXPECT[i].toString(16).uppercase()} " +
                    "got=${resp?.toString(16)?.uppercase() ?: "null"}")

                return@withContext BromResult.Error(
                    stage = "Handshake",
                    message = "Handshake failed at byte $i — Device may not be in BROM mode",
                    hint = "Try: Vol↓ + Power, then connect OTG cable"
                )
            }
            Timber.d("[BROM] Handshake byte $i OK (0x${byte.toString(16).uppercase()} → 0x${resp.toString(16).uppercase()})")
        }

        Timber.d("[BROM] ✅ Handshake SUCCESS")
        BromResult.Success("Handshake completed successfully")
    }

    // ────────────────────────────────────────────────────
    // 2. Get Chip Info (HW Code)
    // ────────────────────────────────────────────────────
    suspend fun getHwCode(): BromResult = withContext(Dispatchers.IO) {
        Timber.d("[BROM] Getting HW code")

        val cmdSent = connection.bulkTransfer(bulkOut, byteArrayOf(CMD_GET_HW_CODE), 1, TIMEOUT_MS)
        if (cmdSent < 0) {
            return@withContext BromResult.Error(
                stage = "HW Code",
                message = "Failed to send HW_CODE command"
            )
        }

        val resp = ByteArray(2)
        val received = connection.bulkTransfer(bulkIn, resp, 2, TIMEOUT_MS)
        if (received < 2) {
            return@withContext BromResult.Error(
                stage = "HW Code",
                message = "No response to HW_CODE command (got $received bytes)"
            )
        }

        val hwCode = ((resp[0].toInt() and 0xFF) shl 8) or (resp[1].toInt() and 0xFF)
        Timber.d("[BROM] HW Code: 0x${hwCode.toString(16).uppercase()}")

        val chipInfo = MtkChipDatabase.getChipInfo(hwCode)

        BromResult.Success(
            data = chipInfo,
            message = "Chip detected: ${chipInfo.name}"
        )
    }

    // ────────────────────────────────────────────────────
    // 3. Get Target Config (Security status)
    // ────────────────────────────────────────────────────
    suspend fun getTargetConfig(): BromResult = withContext(Dispatchers.IO) {
        Timber.d("[BROM] Getting target config")

        val cmdSent = connection.bulkTransfer(bulkOut, byteArrayOf(CMD_GET_TARGET_CONFIG), 1, TIMEOUT_MS)
        if (cmdSent < 0) {
            return@withContext BromResult.Error(
                stage = "Target Config",
                message = "Failed to send GET_TARGET_CONFIG command"
            )
        }

        val resp = ByteArray(4)
        val received = connection.bulkTransfer(bulkIn, resp, 4, TIMEOUT_MS)
        if (received < 4) {
            return@withContext BromResult.Error(
                stage = "Target Config",
                message = "No target config response (got $received bytes)"
            )
        }

        val config = (resp[0].toInt() and 0xFF) shl 24 or
                     (resp[1].toInt() and 0xFF) shl 16 or
                     (resp[2].toInt() and 0xFF) shl 8  or
                     (resp[3].toInt() and 0xFF)

        val secured  = (config shr 1)  and 1
        val slaEn    = (config shr 2)  and 1
        val daaSig   = (config shr 3)  and 1

        val info = BromTargetConfig(
            rawConfig    = config,
            secureBoot   = secured == 1,
            slaEnabled   = slaEn == 1,
            daSignature  = daaSig == 1
        )

        Timber.d("[BROM] Target Config: 0x${config.toString(16).uppercase()}")
        Timber.d("[BROM] Secure Boot: ${if (info.secureBoot) "❌ ENABLED" else "✅ DISABLED"}")
        Timber.d("[BROM] SLA: ${if (info.slaEnabled) "❌ ENABLED" else "✅ DISABLED"}")
        Timber.d("[BROM] DAA Sig: ${if (info.daSignature) "❌ REQUIRED" else "✅ NOT REQUIRED"}")

        BromResult.Success(
            data = info,
            message = "Target config read successfully"
        )
    }

    // ────────────────────────────────────────────────────
    // 4. Full Connect Sequence (Handshake → HW → Config)
    // ────────────────────────────────────────────────────
    suspend fun fullConnect(): BromConnectInfo = withContext(Dispatchers.IO) {
        Timber.d("[BROM] Full connect sequence for RMX3845 (MT6789)")

        // Step 1: Handshake
        val handshake = performHandshake()
        if (handshake is BromResult.Error) {
            return@withContext BromConnectInfo(
                success = false,
                stage = handshake.stage,
                error = handshake.message,
                hint = handshake.hint ?: """
                    Device BROM mode mein nahi hai.
                    RMX3845 ke liye:
                    1. Phone OFF karo
                    2. Vol↓ dabakar rakho
                    3. OTG cable connect karo
                    4. "BROM" screen aane tak wait karo
                """.trimIndent()
            )
        }

        // Step 2: Get HW Code
        val hwCodeResult = getHwCode()
        val chipInfo = if (hwCodeResult is BromResult.Success && hwCodeResult.data is MtkChipInfo) {
            hwCodeResult.data
        } else {
            MtkChipInfo(
                code = 0,
                name = "Unknown",
                arch = "Unknown"
            )
        }

        // Step 3: Get Target Config
        val targetConfigResult = getTargetConfig()
        val targetConfig = if (targetConfigResult is BromResult.Success && targetConfigResult.data is BromTargetConfig) {
            targetConfigResult.data
        } else {
            null
        }

        val isCorrectChip = chipInfo.code == MT6789_CHIP_ID

        BromConnectInfo(
            success = true,
            stage = "Connected",
            chipInfo = chipInfo,
            targetConfig = targetConfig,
            hint = if (isCorrectChip) {
                "✅ RMX3845 (MT6789) detected! Ready for operations."
            } else {
                "⚠️ Connected but chip mismatch (expected MT6789). Proceed carefully."
            }
        )
    }
}

// Result types
sealed class BromResult {
    data class Success(val data: Any? = null, val message: String = "Success") : BromResult()
    data class Error(
        val stage: String,
        val message: String,
        val hint: String? = null
    ) : BromResult()
}

data class BromConnectInfo(
    val success: Boolean,
    val stage: String,
    val chipInfo: MtkChipInfo = MtkChipInfo(0, "Unknown", "Unknown"),
    val targetConfig: BromTargetConfig? = null,
    val error: String = "",
    val hint: String = ""
)

data class MtkChipInfo(
    val code: Int,
    val name: String,
    val arch: String
)

data class BromTargetConfig(
    val rawConfig: Int,
    val secureBoot: Boolean,
    val slaEnabled: Boolean,
    val daSignature: Boolean
)

/**
 * MTK Chip Database — maps HW codes to chip names
 */
object MtkChipDatabase {
    fun getChipInfo(hwCode: Int): MtkChipInfo {
        val (name, arch) = when (hwCode) {
            0x6789 -> "MT6789 Helio G99 — RMX3845 ✅" to "ARM64"
            0x6785 -> "MT6785 Helio G95" to "ARM64"
            0x6781 -> "MT6781 Helio G80/G85" to "ARM64"
            0x6768 -> "MT6768 Helio G85" to "ARM64"
            0x6765 -> "MT6765 Helio G35/P35" to "ARM64"
            0x6762 -> "MT6762 Helio G25/P22" to "ARM64"
            0x6761 -> "MT6761 Helio A22" to "ARM32"
            0x6771 -> "MT6771 Helio P60/P70" to "ARM64"
            0x6779 -> "MT6779 Helio P90" to "ARM64"
            0x6833 -> "MT6833 Dimensity 700" to "ARM64"
            0x6853 -> "MT6853 Dimensity 720" to "ARM64"
            0x6873 -> "MT6873 Dimensity 800" to "ARM64"
            0x6877 -> "MT6877 Dimensity 900" to "ARM64"
            0x6879 -> "MT6879 Dimensity 1080" to "ARM64"
            0x6895 -> "MT6895 Dimensity 8100" to "ARM64"
            0x6983 -> "MT6983 Dimensity 9000/9200" to "ARM64"
            0x6735 -> "MT6735" to "ARM64"
            0x6737 -> "MT6737" to "ARM64"
            0x6739 -> "MT6739" to "ARM32"
            0x6580 -> "MT6580" to "ARM32"
            0x6582 -> "MT6582" to "ARM32"
            0x6589 -> "MT6589" to "ARM32"
            0x6592 -> "MT6592" to "ARM32"
            0x6595 -> "MT6595" to "ARM32"
            else -> "Unknown MTK 0x${hwCode.toString(16).uppercase()}" to "Unknown"
        }
        return MtkChipInfo(hwCode, name, arch)
    }
}
