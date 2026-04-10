package com.deepeye.otg.device

import android.content.Context
import android.hardware.usb.UsbDevice
import kotlinx.coroutines.flow.MutableStateFlow

data class FlashProgress(
    val partition: String,
    val written:   Long,
    val total:     Long,
    val percent:   Int,
)

class MtkDaSession(context: Context, device: UsbDevice)
    : UsbSession(context, device, epInAddr = 0x81, epOutAddr = 0x01, timeoutMs = 30_000)
{
    val progress = MutableStateFlow<FlashProgress?>(null)

    // Flash a partition with progress updates
    suspend fun flashPartition(partition: String, data: ByteArray): Result<Unit> = runCatching {
        val total = data.size.toLong()
        var written = 0L
        val CHUNK = 0x8000 // 32KB

        // Send FLASH command
        val cmd = byteArrayOf(0xD9.toByte()) +
                  partition.toByteArray() +
                  byteArrayOf(0x00) +
                  total.toBeBytes4()
        write(cmd)

        for (chunk in data.toList().chunked(CHUNK)) {
            val bytes = chunk.toByteArray()
            write(bytes)
            written += bytes.size
            progress.emit(FlashProgress(partition, written, total,
                (written * 100 / total).toInt()))
        }
    }

    // Read a partition
    fun readPartition(partition: String, size: Int): Result<ByteArray> = runCatching {
        val cmd = byteArrayOf(0xDA.toByte()) +
                  partition.toByteArray() +
                  byteArrayOf(0x00) +
                  size.toBeBytes4()
        write(cmd)
        readExact(size)
    }

    // Erase a partition
    fun erasePartition(partition: String): Result<Unit> = runCatching {
        val cmd = byteArrayOf(0xDB.toByte()) +
                  partition.toByteArray() +
                  byteArrayOf(0x00)
        write(cmd)
        val resp = read(1)
        check(resp.isNotEmpty() && resp[0] == 0x00.toByte()) {
            "Erase failed: ${resp.getOrNull(0)}"
        }
    }

    fun reboot(): Result<Unit> = runCatching { write(byteArrayOf(0xC9.toByte())) }

    // FRP erase shortcut
    fun eraseFrp(): Result<Unit> = erasePartition("frp")

    // Format userdata
    fun eraseUserdata(): Result<Unit> = erasePartition("userdata")

    // ── Helpers ───────────────────────────────────────────────
    private fun Long.toBeBytes4(): ByteArray = ByteArray(4) { i ->
        ((this shr (24 - i * 8)) and 0xFF).toByte()
    }
    private fun Int.toBeBytes4(): ByteArray = this.toLong().toBeBytes4()
}
