package com.deepeye.otg.device

import android.content.Context
import android.hardware.usb.UsbDevice
import kotlinx.coroutines.flow.MutableStateFlow

class QcomSaharaSession(context: Context, device: UsbDevice)
    : UsbSession(context, device, epInAddr = 0x81, epOutAddr = 0x01, timeoutMs = 30_000)
{
    // Step 1: Sahara HELLO exchange
    fun hello(): Result<Int> = runCatching {
        val pkt = readExact(0x30)
        check(pkt.size >= 12 && pkt[0] == 0x01.toByte()) { "Bad Sahara HELLO packet" }
        val version = pkt.leInt32At(8)

        val resp = ByteArray(0x30).also {
            it[0]  = 0x02                           // HELLO_RESP command
            it[4]  = 0x30                           // packet length
            it[8]  = (version and 0xFF).toByte()
            it[9]  = ((version shr 8)  and 0xFF).toByte()
            it[10] = ((version shr 16) and 0xFF).toByte()
            it[11] = ((version shr 24) and 0xFF).toByte()
            it[12] = 0x02                           // IMAGE_TX mode
        }
        write(resp)
        version
    }

    // Step 2: Send EDL programmer (.mbn / .elf)
    fun sendProgrammer(
        programmer: ByteArray,
        onProgress: (Int) -> Unit = {}
    ): Result<Unit> = runCatching {
        val total = programmer.size
        while (true) {
            val pkt = readExact(0x14)
            when (pkt[0].toInt() and 0xFF) {
                0x03 -> { // READ_DATA
                    val offset = pkt.leInt32At(8)
                    val length = pkt.leInt32At(12)
                    check(offset + length <= total) { "Programmer too short for Sahara request" }
                    write(programmer.copyOfRange(offset, offset + length))
                    onProgress((offset + length) * 100 / total)
                }
                0x04 -> { // END_IMAGE_TX
                    val status = pkt.leInt32At(8)
                    check(status == 0) { "Sahara programmer rejected — status $status" }
                    return@runCatching
                }
                else -> error("Unexpected Sahara cmd: ${pkt[0].toInt().toString(16)}")
            }
        }
    }

    private fun ByteArray.leInt32At(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or
        ((this[offset+1].toInt() and 0xFF) shl 8) or
        ((this[offset+2].toInt() and 0xFF) shl 16) or
        ((this[offset+3].toInt() and 0xFF) shl 24)
}

// ── Firehose (XML protocol after programmer runs) ─────────────
class QcomFirehoseSession(context: Context, device: UsbDevice)
    : UsbSession(context, device, epInAddr = 0x81, epOutAddr = 0x01, timeoutMs = 60_000)
{
    val progress = MutableStateFlow<FlashProgress?>(null)

    // Configure Firehose
    fun configure(maxPayloadSize: Int = 1048576): Result<Unit> = runCatching {
        sendXml("""<?xml version="1.0" ?><data><configure MemoryName="eMMC" MaxPayloadSizeToTargetInBytes="$maxPayloadSize" /></data>""")
        val resp = readXmlResponse()
        check("ACK" in resp || "ack" in resp) { "Firehose configure failed: $resp" }
    }

    // Flash partition from raw image bytes
    suspend fun flashPartition(
        startSector:    String,
        data:           ByteArray,
        sectorSize:     Int = 512,
    ): Result<Unit> = runCatching {
        val numSectors = (data.size + sectorSize - 1) / sectorSize
        sendXml("""<?xml version="1.0" ?><data><program SECTOR_SIZE_IN_BYTES="$sectorSize" num_partition_sectors="$numSectors" physical_partition_number="0" start_sector="$startSector" /></data>""")
        val ack = readXmlResponse()
        check("ACK" in ack) { "Flash cmd rejected: $ack" }

        val total = data.size.toLong()
        var written = 0L
        for (chunk in data.toList().chunked(sectorSize)) {
            val padded = chunk.toByteArray().let { b ->
                if (b.size < sectorSize) b + ByteArray(sectorSize - b.size) { 0xFF.toByte() }
                else b
            }
            write(padded)
            written += chunk.size
            progress.emit(FlashProgress(startSector, written, total,
                (written * 100 / total).toInt()))
        }
        val done = readXmlResponse()
        check("ACK" in done) { "Flash data rejected: $done" }
    }

    // Erase sectors
    fun eraseSectors(startSector: Int, numSectors: Int): Result<Unit> = runCatching {
        sendXml("""<?xml version="1.0" ?><data><erase SECTOR_SIZE_IN_BYTES="512" start_sector="$startSector" num_partition_sectors="$numSectors" physical_partition_number="0" /></data>""")
        val resp = readXmlResponse()
        check("ACK" in resp) { "Erase failed: $resp" }
    }

    // Quick FRP erase (sector offset varies by device)
    fun eraseFrp(frpStartSector: Int = 1024, frpSectors: Int = 1): Result<Unit> =
        eraseSectors(frpStartSector, frpSectors)

    // Get storage/partition info
    fun getStorageInfo(): Result<String> = runCatching {
        sendXml("""<?xml version="1.0" ?><data><getStorageInfo physical_partition_number="0" /></data>""")
        readXmlResponse()
    }

    // Reboot device
    fun reset(): Result<Unit> = runCatching {
        sendXml("""<?xml version="1.0" ?><data><power DelayInSeconds="1" /></data>""")
    }

    private fun sendXml(xml: String) { write(xml.toByteArray()) }
    private fun readXmlResponse(): String {
        val buf = ByteArray(65536)
        val n = connection.bulkTransfer(epIn, buf, buf.size, 60_000)
        return if (n > 0) String(buf, 0, n) else ""
    }
}
