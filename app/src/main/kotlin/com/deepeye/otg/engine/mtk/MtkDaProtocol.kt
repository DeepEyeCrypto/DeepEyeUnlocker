package com.deepeye.otg.engine.mtk

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint

/**
 * MTK DA (Download Agent) Protocol Implementation
 * 
 * After CMD_JUMP_DA executes, the DA firmware boots and communicates
 * using a DIFFERENT protocol than BROM. DA uses sync bytes and
 * different command set.
 * 
 * Protocol Flow:
 * 1. BROM uploads DA via CMD_SEND_DA
 * 2. CMD_JUMP_DA executes DA
 * 3. DA boots and sends sync byte 0xC0
 * 4. Host sends ACK 0x5A
 * 5. DA sends version/info
 * 6. Host can now send DA commands (read/write partitions, erase FRP, etc.)
 * 
 * DA Commands (different from BROM):
 * - DA_CMD_READ16 (0xA2): Read 16-bit value from memory
 * - DA_CMD_WRITE16 (0xA1): Write 16-bit value to memory
 * - DA_CMD_READ32 (0xA9): Read 32-bit value from memory
 * - DA_CMD_WRITE32 (0xA4): Write 32-bit value to memory
 * - DA_CMD_SDMMC_READ (0xB1): Read from eMMC/SD
 * - DA_CMD_SDMMC_WRITE (0xB0): Write to eMMC/SD
 * - DA_CMD_EMMC_PART (0xB2): Get partition info
 * - DA_CMD_FORMAT (0xC0): Erase partition (used for FRP)
 * 
 * @author DeepEye Team
 * @since 2027.0.0 (Stage 3/10)
 */
object MtkDaProtocol {

    // ══════════════════════════════════════════
    // DA SYNC & ACKNOWLEDGMENT
    // ══════════════════════════════════════════

    /** DA sends this byte after booting to signal readiness */
    const val DA_SYNC = 0xC0

    /** Host sends this to acknowledge DA sync */
    const val DA_ACK = 0x5A

    /** DA sends this on error/negative acknowledgment */
    const val DA_NAK = 0xA5

    // ══════════════════════════════════════════
    // DA COMMANDS (sent TO DA after it's running)
    // ══════════════════════════════════════════

    /** Read 16-bit value from memory address */
    const val DA_CMD_READ16 = 0xA2

    /** Write 16-bit value to memory address */
    const val DA_CMD_WRITE16 = 0xA1

    /** Write 32-bit value to memory address */
    const val DA_CMD_WRITE32 = 0xA4

    /** Read 32-bit value from memory address */
    const val DA_CMD_READ32 = 0xA9

    /** Write to SD/MMC (eMMC flash) */
    const val DA_CMD_SDMMC_WRITE = 0xB0

    /** Read from SD/MMC (eMMC flash) */
    const val DA_CMD_SDMMC_READ = 0xB1

    /** Get eMMC partition information */
    const val DA_CMD_EMMC_PART = 0xB2

    /** Erase/format partition (used for FRP bypass) */
    const val DA_CMD_FORMAT = 0xC0

    /** Reboot device via DA */
    const val DA_CMD_REBOOT = 0xC9

    // ══════════════════════════════════════════
    // DA STATUS CODES
    // ══════════════════════════════════════════

    /** Operation successful */
    const val STATUS_OK = 0x0000

    /** General error */
    const val STATUS_ERR = 0x0001

    /** Bad argument */
    const val STATUS_BAD_ARG = 0x0002

    /** Command unsupported */
    const val STATUS_UNSUPPORTED = 0x0003

    // ══════════════════════════════════════════
    // DA BOOT SYNCHRONIZATION
    // ══════════════════════════════════════════

    /**
     * Wait for DA to boot and send sync byte (0xC0)
     * 
     * After CMD_JUMP_DA, DA firmware needs time to execute.
     * Once ready, DA sends 0xC0 to signal it's listening.
     * 
     * Some DA versions may re-enumerate USB (change PID from 0x0003 to 0x0002).
     * 
     * @param conn USB connection
     * @param epIn USB bulk IN endpoint
     * @param onLog Logging callback
     * @param timeoutMs Maximum wait time (default 5000ms)
     * @return true if DA sync received, false if timeout
     */
    fun waitForDaSync(
        conn: UsbDeviceConnection,
        epIn: UsbEndpoint,
        onLog: (String) -> Unit,
        timeoutMs: Int = 5000
    ): Boolean {
        val start = System.currentTimeMillis()
        onLog("⏳ Waiting for DA sync byte (0xC0)...")

        while (System.currentTimeMillis() - start < timeoutMs) {
            val buf = ByteArray(1)
            val r = conn.bulkTransfer(epIn, buf, 1, 500)
            
            if (r > 0) {
                val b = buf[0].toInt().and(0xFF)
                onLog("  DA sent: 0x${b.toString(16).padStart(2, '0')}")
                
                if (b == DA_SYNC) {
                    onLog("✅ DA sync received (0xC0) — DA is running!")
                    return true
                } else if (b == DA_NAK) {
                    onLog("❌ DA sent NAK (0xA5) — DA rejected something")
                    return false
                }
                // Ignore other bytes - DA may send debug/info bytes
            }
        }

        onLog("❌ DA sync timeout (${timeoutMs}ms) — DA may not have booted")
        onLog("💡 Possible causes:")
        onLog("   - DA binary incompatible with chip")
        onLog("   - DA upload failed silently")
        onLog("   - USB re-enumeration needed (check for PID change)")
        return false
    }

    // ══════════════════════════════════════════
    // DA ACKNOWLEDGMENT
    // ══════════════════════════════════════════

    /**
     * Send ACK to DA after receiving sync
     * 
     * @param conn USB connection
     * @param epOut USB bulk OUT endpoint
     */
    fun sendAck(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint
    ): Boolean {
        val ack = byteArrayOf(DA_ACK.toByte())
        val sent = conn.bulkTransfer(epOut, ack, 1, 1000)
        return sent == 1
    }

    // ══════════════════════════════════════════
    // DA INFORMATION EXCHANGE
    // ══════════════════════════════════════════

    /**
     * Read DA version/info after sync
     * 
     * After sending ACK, DA responds with version information:
     * - DA version string
     * - Supported features
     * - Chip compatibility info
     * 
     * @param conn USB connection
     * @param epOut USB bulk OUT endpoint
     * @param epIn USB bulk IN endpoint
     * @param onLog Logging callback
     * @return true if DA info received successfully
     */
    fun readDaInfo(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        onLog: (String) -> Unit
    ): Boolean {
        onLog("📋 Reading DA version info...")

        // Send ACK first
        if (!sendAck(conn, epOut)) {
            onLog("❌ Failed to send ACK to DA")
            return false
        }

        // Read DA info response (typically 20-40 bytes)
        val infoBuf = ByteArray(40)
        val r = conn.bulkTransfer(epIn, infoBuf, infoBuf.size, 3000)
        
        if (r > 0) {
            val info = infoBuf.take(r).joinToString(" ") {
                "0x${it.toInt().and(0xFF).toString(16).padStart(2, '0')}"
            }
            onLog("📋 DA info ($r bytes): $info")

            // Try to parse as ASCII string (some DA versions send version string)
            val asciiStr = infoBuf.take(r).toByteArray()
                .takeWhile { it.toInt().and(0xFF) in 0x20..0x7E }
                .toByteArray()
                .decodeToString()
            
            if (asciiStr.length > 3) {
                onLog("📋 DA version string: \"$asciiStr\"")
            }

            return true
        } else {
            onLog("⚠️ No DA info received (read=$r)")
            return false
        }
    }

    // ══════════════════════════════════════════
    // DA MEMORY OPERATIONS
    // ══════════════════════════════════════════

    /**
     * Read 32-bit value from memory via DA
     * 
     * @param conn USB connection
     * @param epOut USB bulk OUT endpoint
     * @param epIn USB bulk IN endpoint
     * @param address Memory address to read
     * @param onLog Logging callback
     * @return 32-bit value or -1 on failure
     */
    fun read32(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        address: Long,
        onLog: (String) -> Unit
    ): Int {
        onLog("📖 DA READ32: 0x${address.toString(16)}")

        // Send command
        val cmd = byteArrayOf(
            DA_CMD_READ32.toByte(),
            ((address shr 24) and 0xFF).toByte(),
            ((address shr 16) and 0xFF).toByte(),
            ((address shr 8) and 0xFF).toByte(),
            (address and 0xFF).toByte()
        )

        val sent = conn.bulkTransfer(epOut, cmd, cmd.size, 1000)
        if (sent < 0) {
            onLog("❌ READ32 command TX failed")
            return -1
        }

        // Read response (4 bytes + status)
        val resp = ByteArray(6)
        val r = conn.bulkTransfer(epIn, resp, resp.size, 2000)
        
        if (r >= 5) {
            val status = resp[0].toInt().and(0xFF)
            if (status == DA_ACK) {
                val value = ((resp[1].toInt().and(0xFF) shl 24) or
                           (resp[2].toInt().and(0xFF) shl 16) or
                           (resp[3].toInt().and(0xFF) shl 8) or
                           (resp[4].toInt().and(0xFF)))
                onLog("📖 READ32 result: 0x${value.toString(16).padStart(8, '0')}")
                return value
            } else {
                onLog("❌ READ32 failed with status: 0x${status.toString(16)}")
                return -1
            }
        } else {
            onLog("❌ READ32 response incomplete ($r bytes)")
            return -1
        }
    }

    /**
     * Write 32-bit value to memory via DA
     * 
     * @param conn USB connection
     * @param epOut USB bulk OUT endpoint
     * @param epIn USB bulk IN endpoint
     * @param address Memory address to write
     * @param value 32-bit value to write
     * @param onLog Logging callback
     * @return true if write successful
     */
    fun write32(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        address: Long,
        value: Int,
        onLog: (String) -> Unit
    ): Boolean {
        onLog("📝 DA WRITE32: 0x${address.toString(16)} = 0x${value.toString(16).padStart(8, '0')}")

        // Send command
        val cmd = byteArrayOf(
            DA_CMD_WRITE32.toByte(),
            ((address shr 24) and 0xFF).toByte(),
            ((address shr 16) and 0xFF).toByte(),
            ((address shr 8) and 0xFF).toByte(),
            (address and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )

        val sent = conn.bulkTransfer(epOut, cmd, cmd.size, 1000)
        if (sent < 0) {
            onLog("❌ WRITE32 command TX failed")
            return false
        }

        // Read ACK
        val resp = ByteArray(1)
        val r = conn.bulkTransfer(epIn, resp, 1, 2000)
        
        if (r > 0 && resp[0].toInt().and(0xFF) == DA_ACK) {
            onLog("✅ WRITE32 acknowledged")
            return true
        } else {
            onLog("❌ WRITE32 not acknowledged")
            return false
        }
    }

    // ══════════════════════════════════════════
    // DA PARTITION OPERATIONS (FRP Erase Prep)
    // ══════════════════════════════════════════

    /**
     * Get eMMC partition information via DA (STAGE 5)
     * 
     * This command queries DA for available partitions:
     * - preloader
     * - lk (Little Kernel bootloader)
     * - boot
     * - recovery
     * - system
     * - userdata
     * - frp (Factory Reset Protection) ← TARGET!
     * - nvram
     * - etc.
     * 
     * @param conn USB connection
     * @param epOut USB bulk OUT endpoint
     * @param epIn USB bulk IN endpoint
     * @param onLog Logging callback
     * @return Map of partition name to (offset, size) pair, or null on failure
     */
    fun getPartitionTable(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        onLog: (String) -> Unit
    ): Map<String, Pair<Long, Long>>? {  // name → (offset, size)
        onLog("📋 Reading partition table from eMMC...")

        // DA command 0xB2: GET_PARTITION_TABLE
        val cmd = byteArrayOf(0xB2.toByte(), 0x00)
        val sent = conn.bulkTransfer(epOut, cmd, 2, 1000)
        
        if (sent < 0) {
            onLog("❌ PARTITION_TABLE command TX failed")
            return null
        }

        // Read status (2 bytes)
        val statusBuf = ByteArray(2)
        val r1 = conn.bulkTransfer(epIn, statusBuf, 2, 3000)
        
        if (r1 < 2) {
            onLog("❌ No status response")
            return null
        }
        
        val status = (statusBuf[0].toInt().and(0xFF) shl 8) or statusBuf[1].toInt().and(0xFF)
        onLog("  Status: 0x${status.toString(16).padStart(4, '0')}")
        
        if (status != STATUS_OK) {
            onLog("⚠️ Cannot read partition table directly (status=0x${status.toString(16)})")
            return null
        }

        // Read count of partitions (2 bytes)
        val countBuf = ByteArray(2)
        val r2 = conn.bulkTransfer(epIn, countBuf, 2, 3000)
        
        if (r2 < 2) {
            onLog("❌ No partition count received")
            return null
        }
        
        val count = (countBuf[0].toInt().and(0xFF) shl 8) or countBuf[1].toInt().and(0xFF)
        onLog("  Partition count: $count")

        val partitions = mutableMapOf<String, Pair<Long, Long>>()

        // Read each partition: [name:64B][offset:8B][size:8B]
        repeat(count) { i ->
            val nameBuf = ByteArray(64)
            val r3 = conn.bulkTransfer(epIn, nameBuf, 64, 3000)
            
            if (r3 < 64) {
                onLog("⚠️ Partition name read incomplete")
                return null
            }
            
            val name = nameBuf.toString(Charsets.UTF_8).trimEnd('\u0000').trim()

            val offsetBuf = ByteArray(8)
            val r4 = conn.bulkTransfer(epIn, offsetBuf, 8, 3000)
            
            if (r4 < 8) {
                onLog("⚠️ Partition offset read incomplete")
                return null
            }
            
            val offset = offsetBuf.fold(0L) { acc, b -> 
                (acc shl 8) or b.toLong().and(0xFF) }

            val sizeBuf = ByteArray(8)
            val r5 = conn.bulkTransfer(epIn, sizeBuf, 8, 3000)
            
            if (r5 < 8) {
                onLog("⚠️ Partition size read incomplete")
                return null
            }
            
            val size = sizeBuf.fold(0L) { acc, b -> 
                (acc shl 8) or b.toLong().and(0xFF) }

            onLog("  [$i] $name: offset=0x${offset.toString(16)} size=${size/1024}KB")
            partitions[name] = Pair(offset, size)
        }

        // Log FRP-related partitions
        val frpRelated = partitions.filter { (k, _) ->
            k.lowercase().contains("frp") ||
            k.lowercase().contains("misc") ||
            k.lowercase().contains("metadata") ||
            k.lowercase().contains("userdata")
        }
        
        onLog("🎯 FRP-related partitions: ${frpRelated.keys}")
        return partitions
    }

    /**
     * Erase FRP partition via DA (REAL DA V6 FORMAT)
     * 
     * Uses DA_CMD_FORMAT (0xC4) with proper payload structure:
     * [0xC4][payload_len:2][name_len:2][flags:2][name:variable]
     * 
     * @param conn USB connection
     * @param epOut USB bulk OUT endpoint
     * @param epIn USB bulk IN endpoint
     * @param partitionName Partition to erase (e.g., "frp", "userdata-frp")
     * @param onLog Logging callback
     * @return true if erase successful
     */
    fun formatPartition(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        partitionName: String,
        onLog: (String) -> Unit
    ): Boolean {
        onLog("🗑️ Erasing partition: $partitionName")

        val nameBytes = partitionName.toByteArray(Charsets.UTF_8)
        val nameLen = nameBytes.size

        // Build DA FORMAT command payload
        // [name_len:2][flags:2][name:variable]
        val payload = ByteArray(4 + nameLen)
        payload[0] = ((nameLen shr 8) and 0xFF).toByte()
        payload[1] = (nameLen and 0xFF).toByte()
        payload[2] = 0x00  // flags high
        payload[3] = 0x00  // flags low
        nameBytes.copyInto(payload, 4)

        // Send command header: [0xC4][payload_len:2][payload]
        val cmd = ByteArray(3 + payload.size)
        cmd[0] = 0xC4.toByte()  // FORMAT command
        cmd[1] = ((payload.size shr 8) and 0xFF).toByte()
        cmd[2] = (payload.size and 0xFF).toByte()
        payload.copyInto(cmd, 3)

        val sent = conn.bulkTransfer(epOut, cmd, cmd.size, 2000)
        if (sent < 0) {
            onLog("❌ FORMAT command TX failed")
            return false
        }

        // Read response status (2 bytes)
        val resp = ByteArray(2)
        val r = conn.bulkTransfer(epIn, resp, 2, 5000)
        
        if (r >= 2) {
            val status = (resp[0].toInt().and(0xFF) shl 8) or resp[1].toInt().and(0xFF)
            onLog("  Format response: 0x${status.toString(16).padStart(4, '0')} ${if (status == STATUS_OK) "✅ ERASED!" else "❌ FAILED"}")
            return status == STATUS_OK
        } else {
            onLog("❌ No response to format command")
            return false
        }
    }

    // ══════════════════════════════════════════
    // DA FLUSH/CLEANUP
    // ══════════════════════════════════════════

    /**
     * Flush DA receive buffer (discard pending data)
     * 
     * @param conn USB connection
     * @param epIn USB bulk IN endpoint
     */
    fun flushDaBuffer(
        conn: UsbDeviceConnection,
        epIn: UsbEndpoint
    ) {
        val buf = ByteArray(512)
        repeat(5) {
            conn.bulkTransfer(epIn, buf, buf.size, 100)
        }
    }

    /**
     * Reboot device via DA
     * 
     * Sends DA_CMD_REBOOT (0xC9) to trigger device reboot.
     * Device will disconnect from USB and boot normally.
     * 
     * @param conn USB connection
     * @param epOut USB bulk OUT endpoint
     * @param epIn USB bulk IN endpoint
     * @param onLog Logging callback
     */
    fun rebootDevice(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        onLog: (String) -> Unit
    ) {
        onLog("🔄 Sending reboot command to DA...")
        
        val sent = conn.bulkTransfer(epOut, byteArrayOf(0xC9.toByte()), 1, 1000)
        if (sent > 0) {
            onLog("📱 Device rebooting...")
            Thread.sleep(500)
        } else {
            onLog("⚠️ Reboot command TX failed — device may need manual reboot")
        }
    }
}
