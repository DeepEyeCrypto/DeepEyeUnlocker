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
    // BROM Handshake — 4-byte sync sequence
    fun handshake(): Result<String> = runCatching {
        val sync     = byteArrayOf(0xA0.toByte(), 0x0A, 0x50, 0x05)
        val expected = byteArrayOf(0x5F.toByte(), 0xF5.toByte(), 0xAF.toByte(), 0xFA.toByte())
        for ((s, e) in sync.zip(expected)) {
            write(byteArrayOf(s))
            val resp = read(1)
            check(resp.isNotEmpty() && resp[0] == e) {
                "Handshake failed: got ${resp.getOrNull(0)?.toHex()}, expected ${e.toHex()}"
            }
        }
        "BROM handshake OK ✓"
    }

    // Get hardware code → chip info
    fun getHwCode(): Result<MtkChipInfo> = runCatching {
        write(byteArrayOf(0xFD.toByte()))
        val resp = readExact(2)
        val code = ((resp[0].toInt() and 0xFF) shl 8) or (resp[1].toInt() and 0xFF)
        MtkChipInfo(
            hwCode   = code,
            chipName = chipNameFromCode(code),
            arch     = archFromCode(code),
        )
    }

    // Disable MTK watchdog
    fun disableWatchdog(): Result<Unit> = runCatching {
        val cmd = byteArrayOf(0xD1.toByte(), 0x00, 0xD4.toByte(), 0x00, 0x00, 0x00, 0x01, 0x00)
        write(cmd)
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
        write(cmd)
        val ack = read(2)
        check(ack.isNotEmpty() && ack[0] == 0x00.toByte()) {
            "DA send rejected: ${ack.getOrNull(0)?.toHex()}"
        }
        // Transfer DA in 512-byte chunks with progress
        for (chunk in daBytes.toList().chunked(512)) {
            write(chunk.toByteArray())
        }
        val done = read(2)
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
        write(cmd)
    }

    // ── Chip DB ───────────────────────────────────────────────
    private fun chipNameFromCode(code: Int) = when (code) {
        0x6765 -> "MT6765 Helio G35"
        0x6768 -> "MT6768 Helio G85"
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
