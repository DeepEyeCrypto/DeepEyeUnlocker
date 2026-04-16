package com.deepeye.otg.device

import android.content.Context
import android.hardware.usb.UsbDevice
import kotlinx.serialization.Serializable

@Serializable
data class MtkChipInfo(
    val hwCode:   Int,
    val chipName: String,
    val arch:     String,
)

class MtkBromSession(context: Context, device: UsbDevice)
    : UsbSession(context, device, epInAddr = 0x81, epOutAddr = 0x01, timeoutMs = 10_000)
{
    companion object {
        private const val HANDSHAKE_TIMEOUT_MS = 3_000
        private const val COMMAND_TIMEOUT_MS = 3_000
        private const val READ_TIMEOUT_MS = 5_000
        private const val STATUS_TIMEOUT_MS = 2_000
        private const val HANDSHAKE_ATTEMPTS = 3
        private const val RETRY_SETTLE_MS = 300L
    }

    private data class BromWordRead(
        val payload: ByteArray,
        val statusOk: Boolean,
    )

    // BROM Handshake — 4-byte sync sequence
    fun handshake(): Result<String> = runCatching {
        val sync     = byteArrayOf(0xA0.toByte(), 0x0A, 0x50, 0x05)
        val expected = byteArrayOf(0x5F.toByte(), 0xF5.toByte(), 0xAF.toByte(), 0xFA.toByte())

        var lastReason = "No response from BROM"
        for (attempt in 0 until HANDSHAKE_ATTEMPTS) {
            val written = write(sync, HANDSHAKE_TIMEOUT_MS)
            if (written == sync.size) {
                val resp = readExact(4, HANDSHAKE_TIMEOUT_MS)
                if (resp.size == expected.size && resp.contentEquals(expected)) {
                    return@runCatching "BROM handshake OK ✓"
                }
                if (resp.isNotEmpty()) {
                    return@runCatching "BROM handshake replied: ${resp.toHexString()}"
                }
            } else {
                lastReason = "Handshake write failed (wrote $written/${sync.size} bytes)"
            }

            if (attempt + 1 < HANDSHAKE_ATTEMPTS) {
                Thread.sleep(RETRY_SETTLE_MS)
            }
            lastReason = "Handshake failed on attempt ${attempt + 1}/$HANDSHAKE_ATTEMPTS"
        }

        error(buildFailureMessage("❌ Handshake failed after 3 attempts", lastReason))
    }

    // Get hardware code → chip info
    fun getHwCode(): Result<MtkChipInfo> = runCatching {
        val written = write(byteArrayOf(0xFD.toByte()), COMMAND_TIMEOUT_MS)
        check(written > 0) {
            buildFailureMessage(
                "❌ Device Identification failed on BROM",
                "CMD_GET_HW_CODE (0xFD) could not be transmitted"
            )
        }

        val result = readStatusAwareWord()
        check(result.payload.size >= 2) {
            buildFailureMessage(
                "❌ Device Identification failed on BROM",
                "HW code read failed: got ${result.payload.size} byte(s)"
            )
        }

        val code = ((result.payload[0].toInt() and 0xFF) shl 8) or (result.payload[1].toInt() and 0xFF)
        MtkChipInfo(
            hwCode   = code,
            chipName = chipNameFromCode(code),
            arch     = archFromCode(code),
        )
    }

    private fun readStatusAwareWord(): BromWordRead {
        var statusOk = false
        var payload = ByteArray(0)

        val statusProbe = read(1, STATUS_TIMEOUT_MS)
        if (statusProbe.isNotEmpty()) {
            if (statusProbe[0] == 0x00.toByte()) {
                statusOk = true
            } else {
                payload += statusProbe
            }
        }

        if (payload.size < 2) {
            payload += readExact(2 - payload.size, READ_TIMEOUT_MS)
        }

        if (payload.size < 2) {
            payload += readExact(4 - payload.size, COMMAND_TIMEOUT_MS)
        }

        return BromWordRead(payload = payload, statusOk = statusOk)
    }

    private fun buildFailureMessage(title: String, reason: String): String = buildString {
        appendLine(title)
        appendLine(reason)
        appendLine("━━━━━━━━━━━━━━━━━━━━━")
        appendLine("🔧 Common fixes:")
        appendLine("  1. Use original/high-quality USB cable")
        appendLine("  2. Connect directly to PC USB 2.0 port")
        appendLine("  3. Hold Vol- button while connecting USB")
        appendLine("  4. Try different USB port on phone")
        appendLine("  5. Device must be POWERED OFF before connecting")
        appendLine("━━━━━━━━━━━━━━━━━━━━━")
    }.trim()

    private fun ByteArray.toHexString(): String = joinToString(" ") { it.toHex() }

    // Disable MTK watchdog
    fun disableWatchdog(): Result<Unit> = runCatching {
        val cmd = byteArrayOf(0xD1.toByte(), 0x00, 0xD4.toByte(), 0x00, 0x00, 0x00, 0x01, 0x00)
        write(cmd, COMMAND_TIMEOUT_MS)
    }

    // Send Download Agent binary
    fun sendDa(daBytes: ByteArray, daAddr: Long): Result<Unit> = runCatching {
        // Build DA_LOAD command
        val cmd = ByteArray(13).also {
            it[0]  = 0xD7.toByte()
            it[1]  = ((daAddr shr 24) and 0xFF).toByte()
            it[2]  = ((daAddr shr 16) and 0xFF).toByte()
            it[3]  = ((daAddr shr 8)  and 0xFF).toByte()
            it[4]  = (daAddr           and 0xFF).toByte()
            val len = daBytes.size.toLong()
            it[5]  = ((len shr 24) and 0xFF).toByte()
            it[6]  = ((len shr 16) and 0xFF).toByte()
            it[7]  = ((len shr 8)  and 0xFF).toByte()
            it[8]  = (len           and 0xFF).toByte()
            it[9]  = 0x00; it[10] = 0x00; it[11] = 0x00; it[12] = 0x00  // sig len
        }
        write(cmd, COMMAND_TIMEOUT_MS)
        val ack = read(2, READ_TIMEOUT_MS)
        check(ack.isNotEmpty() && ack[0] == 0x00.toByte()) {
            "DA send rejected: ${ack.getOrNull(0)?.toHex()}"
        }
        // Transfer DA in 512-byte chunks with progress
        for (chunk in daBytes.toList().chunked(512)) {
            write(chunk.toByteArray(), READ_TIMEOUT_MS)
        }
        val done = read(2, READ_TIMEOUT_MS)
        check(done.isNotEmpty() && done[0] == 0x00.toByte()) {
            "DA transfer failed: ${done.getOrNull(0)?.toHex()}"
        }
    }

    // Jump to DA entry point
    fun jumpDa(daAddr: Long): Result<Unit> = runCatching {
        val cmd = ByteArray(5).also {
            it[0] = 0xD5.toByte()
            it[1] = ((daAddr shr 24) and 0xFF).toByte()
            it[2] = ((daAddr shr 16) and 0xFF).toByte()
            it[3] = ((daAddr shr 8)  and 0xFF).toByte()
            it[4] = (daAddr           and 0xFF).toByte()
        }
        write(cmd, COMMAND_TIMEOUT_MS)
    }

    // ── Chip DB ───────────────────────────────────────────────
    private fun chipNameFromCode(code: Int) = when (code) {
        0x6789 -> "MT6789 Helio G99 — RMX3845 ✅"
        0x6765 -> "MT6765 Helio G35"
        0x6768 -> "MT6768 Helio G85"
        0x6781 -> "MT6781 Helio G80"
        0x6785 -> "MT6785 Helio G90T"
        0x6833 -> "MT6833 Dimensity 700"
        0x6853 -> "MT6853 Dimensity 720"
        0x6873 -> "MT6873 Dimensity 800"
        0x6877 -> "MT6877 Dimensity 900"
        0x6879 -> "MT6879 Dimensity 1080"
        0x6895 -> "MT6895 Dimensity 8100"
        0x6983 -> "MT6983 Dimensity 9200"
        0x6761 -> "MT6761 Helio A22"
        0x6771 -> "MT6771 Helio P60"
        0x6779 -> "MT6779 Helio P90"
        0x6735 -> "MT6735"
        0x6737 -> "MT6737"
        0x6739 -> "MT6739"
        0x6580 -> "MT6580"
        0x6582 -> "MT6582"
        0x6589 -> "MT6589"
        0x6592 -> "MT6592"
        0x6595 -> "MT6595"
        0x8195 -> "MT8195 Kompanio 1380"
        0x8173 -> "MT8173"
        else   -> "Unknown MTK (${code.toString(16).uppercase()})"
    }

    private fun archFromCode(code: Int) = when {
        code >= 0x6735 -> "ARM64"
        else           -> "ARM32"
    }

    private fun Byte.toHex() = "0x${Integer.toHexString(this.toInt() and 0xFF).uppercase()}"
}
