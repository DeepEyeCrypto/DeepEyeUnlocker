package com.deepeye.otg.usb.gpt

import android.util.Log
import com.deepeye.otg.usb.TransferResult
import com.deepeye.otg.usb.UsbTransport

/**
 * High-assurance GPT Parser.
 * Uses a transport to read and decode the partition table.
 */
class GptParser(private val transport: UsbTransport) {
    companion object {
        private const val TAG = "GptParser"
    }

    suspend fun readPartitions(): List<GptStructure.GptEntry> {
        Log.i(TAG, "Attempting to read GPT Partition Table...")

        // 1. Read LBA 1 (GPT Header)
        // Note: LBA 0 is MBR, usually skipped for pure GPT but contains the protective MBR.
        val headerSector = readLba(1) ?: return emptyList()
        val header = GptStructure.parseHeaderStrict(headerSector) ?: run {
            Log.e(TAG, "Failed to parse GPT Header at LBA 1")
            return emptyList()
        }

        Log.i(TAG, "GPT Header found: ${header.numEntries} entries, entries starting at LBA ${header.partitionEntriesLba}")

        // 2. Read Partition Entries
        val entries = mutableListOf<GptStructure.GptEntry>()
        val entriesPerSector = GptStructure.LBA_SIZE / header.entrySize
        val totalSectorsNeeded = (header.numEntries + entriesPerSector - 1) / entriesPerSector
        
        for (i in 0 until totalSectorsNeeded) {
            val currentLba = header.partitionEntriesLba + i
            val sector = readLba(currentLba) ?: break
            
            for (j in 0 until entriesPerSector) {
                val offset = j * header.entrySize
                val entryContent = sector.copyOfRange(offset, offset + header.entrySize)
                val entry = GptStructure.parseEntry(entryContent)
                if (entry != null) {
                    entries.add(entry)
                }
            }
            
            if (entries.size >= header.numEntries) break
        }

        Log.i(TAG, "Successfully parsed ${entries.size} partitions")
        return entries
    }

    /**
     * Reads a single 512-byte LBA.
     * Implementation depends on the underlying protocol (EDL/DA) 
     * but here we use the generic UsbTransport.
     * 
     * Note: Pure UsbTransport might not support direct "ReadLBA" 
     * without a protocol handshake (like Firehose READ).
     * This method assumes the transport has been 'prepared' or 
     * is in a mode where raw reads are mapped.
     */
    private suspend fun readLba(lba: Long): ByteArray? {
        // In reality, we'd send a Protocol Command here.
        // For now, this is a skeleton that protocols will use.
        // E.g. SaharaProtocol.readLba(transport, lba)
        return null 
    }
}
