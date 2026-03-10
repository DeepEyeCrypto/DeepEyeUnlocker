package com.deepeye.otg.usb.gpt

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * GUID Partition Table (GPT) Structures.
 * Based on UEFI Specification.
 */
object GptStructure {
    const val LBA_SIZE = 512
    const val HEADER_MIN_SIZE = 92
    const val ENTRY_MIN_SIZE = 128

    const val GPT_SIGNATURE = 0x5452415020494645L // "EFI PART"

    data class GptHeader(
        val signature: Long,
        val revision: Int,
        val headerSize: Int,
        val headerCrc: Int,
        val currentLba: Long,
        val backupLba: Long,
        val firstUsableLba: Long,
        val lastUsableLba: Long,
        val partitionEntriesLba: Long,
        val numEntries: Int,
        val entrySize: Int
    )

    data class GptEntry(
        val typeGuid: String,
        val partitionGuid: String,
        val firstLba: Long,
        val lastLba: Long,
        val attributes: Long,
        val name: String
    ) {
        val sizeInBlocks: Long get() = (lastLba - firstLba + 1)
        val sizeInBytes: Long get() = sizeInBlocks * LBA_SIZE
    }

    fun parseHeader(data: ByteArray): GptHeader? = parseHeaderStrict(data)

    private fun ByteBuffer.skip(bytes: Int) {
        this.position(this.position() + bytes)
    }

    /**
     * Corrected parseHeader because of reserved gaps.
     */
    fun parseHeaderStrict(data: ByteArray): GptHeader? {
        if (data.size < 92) return null
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        
        val sig = buf.long
        if (sig != GPT_SIGNATURE) return null

        val rev = buf.getInt()
        val size = buf.getInt()
        val crc = buf.getInt()
        buf.getInt() // reserved

        val current = buf.long
        val backup = buf.long
        val firstUsable = buf.long
        val lastUsable = buf.long
        
        buf.skip(16) // Disk GUID
        
        val entriesLba = buf.long
        val numEntries = buf.getInt()
        val entrySize = buf.getInt()
        
        return GptHeader(sig, rev, size, crc, current, backup, firstUsable, lastUsable, entriesLba, numEntries, entrySize)
    }

    fun parseEntry(data: ByteArray): GptEntry? {
        if (data.size < 128) return null
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        
        val typeGuid = readGuid(buf)
        if (typeGuid == "00000000-0000-0000-0000-000000000000") return null // Unused entry

        val partGuid = readGuid(buf)
        val first = buf.long
        val last = buf.long
        val attr = buf.long
        
        val nameBytes = ByteArray(72)
        buf.get(nameBytes)
        val name = String(nameBytes, Charsets.UTF_16LE).takeWhile { it != '\u0000' }

        return GptEntry(typeGuid, partGuid, first, last, attr, name)
    }

    private fun readGuid(buf: ByteBuffer): String {
        val d1 = buf.getInt()
        val d2 = buf.getShort()
        val d3 = buf.getShort()
        val d4 = ByteArray(8)
        buf.get(d4)
        
        return "%08x-%04x-%04x-%02x%02x-%02x%02x%02x%02x%02x%02x".format(
            d1, d2, d3, d4[0], d4[1], d4[2], d4[3], d4[4], d4[5], d4[6], d4[7]
        )
    }
}
