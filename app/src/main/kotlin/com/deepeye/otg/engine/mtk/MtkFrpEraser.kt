package com.deepeye.otg.engine.mtk

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MTK FRP (Factory Reset Protection) Eraser
 * 
 * Implements complete FRP bypass flow:
 * 1. Read partition table via DA
 * 2. Try all known FRP partition names
 * 3. Fallback to direct eMMC offset erase
 * 4. Auto-reboot after successful erase
 * 
 * FRP Bypass Flow:
 * - DA running → getPartitionTable() → Find "frp" partition
 * - formatPartition("frp") → Erase partition
 * - If fails → eraseFrpByOffset() → Direct eMMC write
 * - Success → rebootDevice() → Device reboots without FRP
 * 
 * @author DeepEye Team
 * @since 2027.0.0 (Stages 4+5+6/10)
 */
object MtkFrpEraser {

    // ══════════════════════════════════════════
    // FRP PARTITION NAMES (try all — different devices use different names)
    // ══════════════════════════════════════════

    private val FRP_PARTITION_NAMES = listOf(
        "frp",           // Most common (MediaTek standard)
        "FRP",           // Uppercase variant
        "oem_dontuse_p", // Some Realme/OPPO devices
        "persistent",    // Older MediaTek devices
        "misc",          // Some devices store FRP in misc partition
        "metadata",      // Android 10+ FRP storage
        "userdata_frp",  // Explicit userdata FRP region
        "config",        // Some devices use config partition
        "nvram"          // NVRAM sometimes contains FRP data
    )

    // ══════════════════════════════════════════
    // MAIN FRP ERASE FUNCTION
    // ══════════════════════════════════════════

    /**
     * Complete FRP erase flow
     * 
     * Tries multiple strategies:
     * 1. Read partition table to identify FRP partition
     * 2. Try formatting each known FRP partition name
     * 3. Fallback to direct eMMC offset erase (RMX3845-specific)
     * 4. Auto-reboot device after successful erase
     * 
     * @param conn USB connection (DA mode)
     * @param epOut USB bulk OUT endpoint
     * @param epIn USB bulk IN endpoint
     * @param onLog Logging callback
     * @return true if FRP erase successful, false otherwise
     */
    suspend fun eraseFrp(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        onLog("🔥 Starting FRP Erase...")
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        var erased = false
        var erasedPartitions = mutableListOf<String>()

        // STAGE 5: Read partition table (optional, for logging)
        onLog("📋 Step 1: Reading partition table...")
        val partitions = MtkDaProtocol.getPartitionTable(conn, epOut, epIn, onLog)
        
        if (partitions != null) {
            onLog("✅ Partition table read successfully")
            val frpParts = partitions.filter { (name, _) ->
                name.lowercase().contains("frp")
            }
            if (frpParts.isNotEmpty()) {
                onLog("🎯 Found FRP partitions: ${frpParts.keys}")
            }
        } else {
            onLog("⚠️ Partition table read failed — will try blind erase")
        }

        // STAGE 6: Try each FRP partition name
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        onLog("📋 Step 2: Trying FRP partition names...")
        
        for (partName in FRP_PARTITION_NAMES) {
            onLog("🎯 Trying: $partName")
            
            val success = MtkDaProtocol.formatPartition(
                conn, epOut, epIn, partName, onLog
            )
            
            if (success) {
                onLog("✅ $partName ERASED SUCCESSFULLY!")
                erased = true
                erasedPartitions.add(partName)
                // Don't break — erase ALL FRP-related partitions
            } else {
                onLog("  ↳ $partName not found or skipped")
            }
        }

        // Check if we successfully erased any partitions
        if (erased) {
            onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            onLog("🎉 FRP ERASE COMPLETE!")
            onLog("📋 Erased partitions: ${erasedPartitions.joinToString(", ")}")
            onLog("📱 Rebooting device...")
            onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            
            // Reboot device after successful erase
            MtkDaProtocol.rebootDevice(conn, epOut, epIn, onLog)
            return@withContext true
        }

        // Fallback: Try direct eMMC offset erase
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        onLog("⚠️ No FRP partitions found via name")
        onLog("💡 Trying direct eMMC offset erase (RMX3845)...")
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        erased = eraseFrpByOffset(conn, epOut, epIn, onLog)
        
        if (erased) {
            onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            onLog("🎉 FRP OFFSET ERASE COMPLETE!")
            onLog("📱 Rebooting device...")
            onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            
            MtkDaProtocol.rebootDevice(conn, epOut, epIn, onLog)
            return@withContext true
        }

        // All attempts failed
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        onLog("❌ FRP ERASE FAILED")
        onLog("💡 Try manual methods or check device compatibility")
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        return@withContext false
    }

    // ══════════════════════════════════════════
    // FALLBACK: ERASE FRP BY DIRECT EMMC OFFSET
    // ══════════════════════════════════════════

    /**
     * Fallback method: Erase FRP by writing zeros to known eMMC offset
     * 
     * For RMX3845 (MT6789 Helio G99):
     * - FRP partition typically at LBA 0x5000
     * - LBA 0x5000 × 512 bytes = offset 0x00A00000
     * - FRP size: 1MB (0x100000 bytes)
     * 
     * This method writes 1MB of zeros directly to eMMC,
     * overwriting FRP data without needing partition table.
     * 
     * @param conn USB connection (DA mode)
     * @param epOut USB bulk OUT endpoint
     * @param epIn USB bulk IN endpoint
     * @param onLog Logging callback
     * @return true if offset erase successful
     */
    private fun eraseFrpByOffset(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        onLog: (String) -> Unit
    ): Boolean {
        // RMX3845 (MT6789) FRP partition offset
        // Typically at LBA 0x5000 * 512 = 0xA00000
        // Size: 1MB = 0x100000 bytes
        val FRP_OFFSET = 0x00A00000L
        val FRP_SIZE   = 0x00100000L  // 1MB

        onLog("💾 Writing zeros to FRP offset: 0x${FRP_OFFSET.toString(16)}")
        onLog("   Size: ${FRP_SIZE / 1024}KB (${FRP_SIZE / 1048576}MB)")

        // DA SDMMC write command (0xB0)
        val zeros = ByteArray(512)  // 1 sector at a time
        val sectors = (FRP_SIZE / 512).toInt()
        val startLba = FRP_OFFSET / 512

        onLog("📊 Sectors to write: $sectors (LBA 0x${startLba.toString(16)})")

        // Send write command header
        // [0xB0][start_lba:8][sector_count:4]
        val cmd = byteArrayOf(
            0xB0.toByte(),  // DA_CMD_SDMMC_WRITE
            
            // Start LBA (8 bytes, big-endian)
            ((startLba shr 56) and 0xFF).toByte(),
            ((startLba shr 48) and 0xFF).toByte(),
            ((startLba shr 40) and 0xFF).toByte(),
            ((startLba shr 32) and 0xFF).toByte(),
            ((startLba shr 24) and 0xFF).toByte(),
            ((startLba shr 16) and 0xFF).toByte(),
            ((startLba shr 8) and 0xFF).toByte(),
            (startLba and 0xFF).toByte(),
            
            // Sector count (4 bytes, big-endian)
            ((sectors shr 24) and 0xFF).toByte(),
            ((sectors shr 16) and 0xFF).toByte(),
            ((sectors shr 8) and 0xFF).toByte(),
            (sectors and 0xFF).toByte()
        )
        
        val sent = conn.bulkTransfer(epOut, cmd, cmd.size, 2000)
        if (sent < 0) {
            onLog("❌ Write command TX failed")
            return false
        }

        // Read ACK
        val ackBuf = ByteArray(2)
        val r1 = conn.bulkTransfer(epIn, ackBuf, 2, 3000)
        
        if (r1 < 2) {
            onLog("❌ No ACK received")
            return false
        }
        
        val ack = (ackBuf[0].toInt().and(0xFF) shl 8) or ackBuf[1].toInt().and(0xFF)
        if (ack != MtkDaProtocol.STATUS_OK) {
            onLog("❌ Write command rejected: 0x${ack.toString(16).padStart(4, '0')}")
            return false
        }

        onLog("✅ Write command accepted — zeroing sectors...")

        // Write zero sectors (1MB / 512 bytes = 2048 sectors)
        var written = 0
        repeat(sectors) { sector ->
            val w = conn.bulkTransfer(epOut, zeros, 512, 1000)
            if (w < 0) {
                onLog("❌ Sector $sector write failed")
                return false
            }
            written++
            
            // Progress logging every 256 sectors (128KB)
            if (written % 256 == 0) {
                val percent = written * 100 / sectors
                val mbWritten = (written * 512) / 1048576
                onLog("  📤 Zeroing: $percent% ($mbWritten MB / ${FRP_SIZE / 1048576} MB)")
            }
        }

        // Read final ACK
        val finalAckBuf = ByteArray(2)
        val r2 = conn.bulkTransfer(epIn, finalAckBuf, 2, 3000)
        
        if (r2 >= 2) {
            val finalAck = (finalAckBuf[0].toInt().and(0xFF) shl 8) or finalAckBuf[1].toInt().and(0xFF)
            onLog("  Final ACK: 0x${finalAck.toString(16).padStart(4, '0')}")
            
            if (finalAck == MtkDaProtocol.STATUS_OK) {
                onLog("✅ FRP offset zeroed successfully!")
                onLog("   $written sectors (${written * 512 / 1024}KB) written")
                return true
            } else {
                onLog("❌ Final ACK indicates failure: 0x${finalAck.toString(16)}")
                return false
            }
        } else {
            onLog("⚠️ No final ACK — check manually")
            onLog("   Device may still have been erased")
            return false
        }
    }
}
