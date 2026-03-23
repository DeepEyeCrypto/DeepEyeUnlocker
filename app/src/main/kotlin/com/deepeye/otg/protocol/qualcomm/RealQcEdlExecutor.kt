package com.deepeye.otg.protocol.qualcomm

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import com.deepeye.otg.data.gsmg.ProtocolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

// =============================================================================
// RealQcEdlExecutor.kt
// REAL Qualcomm EDL (Emergency Download Mode) protocol
// VID:0x05C6 PID:0x9008
// Brands: Samsung QC, Xiaomi QC, OPPO/OnePlus QC, Motorola QC, Google Pixel
//
// Protocol:
//   1. Sahara handshake (binary LE packets)
//   2. Upload programmer binary (sbl1.mbn / firehose.elf)
//   3. Firehose XML commands over bulk
//   4. Partition operations via XML
// =============================================================================

class RealQcEdlExecutor(
    private val usbManager: UsbManager,
    private val context:    android.content.Context,
) {
    companion object {
        // Sahara commands (LE 32-bit)
        private const val SAHARA_HELLO         = 0x01
        private const val SAHARA_HELLO_RESP    = 0x02
        private const val SAHARA_READ_DATA     = 0x03
        private const val SAHARA_END_TRANSFER  = 0x04
        private const val SAHARA_DONE          = 0x05
        private const val SAHARA_DONE_RESP     = 0x06
        private const val SAHARA_RESET         = 0x07
        private const val SAHARA_EXECUTE       = 0x0E
        private const val SAHARA_EXECUTE_RESP  = 0x0F

        private const val SAHARA_PKT_SIZE      = 48
        private const val TIMEOUT_SAHARA       = 10_000
        private const val TIMEOUT_FIREHOSE     = 60_000
        private const val FIREHOSE_CHUNK       = 512 * 1024  // 512 KB
        private const val MAX_RESPONSE_SIZE    = 1024 * 1024 // 1 MB XML cap
    }

    // ── FRP erase ─────────────────────────────────────────────────────────

    suspend fun eraseFrp(
        device:      UsbDevice,
        programmer:  String,   // asset path: "prog/sm8550_firehose.elf"
        sessionId:   String,
        onProgress:  (Int, String) -> Unit,
    ): ProtocolResult = withContext(Dispatchers.IO) {
        Timber.d("[QC_EDL] eraseFrp prog=$programmer sessionId=$sessionId")

        val conn = usbManager.openDevice(device)
            ?: return@withContext ProtocolResult.UsbTransportError(
                reason = "Cannot open QC EDL device", sessionId = sessionId)

        try {
            val iface = device.getInterface(0)
            if (!conn.claimInterface(iface, true)) {
                return@withContext ProtocolResult.UsbTransportError(
                    reason = "Cannot claim EDL interface", sessionId = sessionId)
            }

            val epOut = findBulkOut(device) ?: return@withContext ProtocolResult.UsbTransportError("No EDL bulk OUT", sessionId = sessionId)
            val epIn  = findBulkIn(device)  ?: return@withContext ProtocolResult.UsbTransportError("No EDL bulk IN", sessionId = sessionId)

            // Phase 1: Sahara handshake
            onProgress(10, "Sahara handshake")
            val saharaState = saharaHandshake(conn, epOut, epIn, sessionId)
                .getOrElse { return@withContext it as ProtocolResult }

            Timber.d("[QC_EDL] sahara state=$saharaState sessionId=$sessionId")

            // Phase 2: Upload programmer
            onProgress(20, "Uploading programmer")
            val progBytes = loadAsset(programmer)
                ?: return@withContext ProtocolResult.UsbTransportError(
                    reason = "Programmer not found: $programmer\n" +
                             "Add to app/src/main/assets/$programmer",
                    sessionId = sessionId,
                )

            uploadProgrammer(conn, epOut, epIn, progBytes, sessionId) { pct ->
                onProgress(20 + (pct * 30 / 100), "Programmer $pct%")
            }.onFailure { return@withContext it as ProtocolResult }

            // Phase 3: Switch to Firehose
            onProgress(55, "Firehose init")
            firehoseConfigure(conn, epOut, epIn, sessionId)
                .onFailure { return@withContext it as ProtocolResult }

            // Phase 4: Erase FRP partition
            onProgress(70, "Erasing FRP")
            firehoseErase(conn, epOut, epIn, "frp", sessionId)
                .onFailure {
                    // Try 'userdata' based FRP if 'frp' partition not found
                    firehoseEraseFrpMisc(conn, epOut, epIn, sessionId)
                        .onFailure { return@withContext it as ProtocolResult }
                }

            // Phase 5: Reset device
            onProgress(95, "Resetting device")
            firehoseReset(conn, epOut, epIn, sessionId)

            onProgress(100, "FRP erase complete")
            Timber.d("[QC_EDL] eraseFrp DONE sessionId=$sessionId")

            ProtocolResult.FrpErased(
                method    = "QC_EDL_FIREHOSE",
                partition = "frp",
                sessionId = sessionId,
            )
        } catch (e: Exception) {
            Timber.e("[QC_EDL] error: ${e.message} sessionId=$sessionId")
            ProtocolResult.UsbTransportError(
                reason = e.message ?: "Unknown", sessionId = sessionId)
        } finally {
            runCatching { conn.close() }
        }
    }

    // ── Sahara handshake ──────────────────────────────────────────────────

    private fun saharaHandshake(
        conn:      UsbDeviceConnection,
        epOut:     UsbEndpoint,
        epIn:      UsbEndpoint,
        sessionId: String,
    ): Result<String> {
        // Read HELLO packet from device (48 bytes)
        val hello = ByteArray(SAHARA_PKT_SIZE)
        val n     = conn.bulkTransfer(epIn, hello, SAHARA_PKT_SIZE, TIMEOUT_SAHARA)
        Timber.d("[QC_EDL] sahara_hello recv=$n sessionId=$sessionId")

        if (n < 8) return Result.failure(ProtocolResult.ProtocolHandshakeFailed(
            reason = "Sahara hello too short: $n", sessionId = sessionId) as Exception)

        val cmd  = readLE32(hello, 0)
        val size = readLE32(hello, 4)

        if (cmd != SAHARA_HELLO) return Result.failure(
            ProtocolResult.ProtocolHandshakeFailed(
                reason = "Expected SAHARA_HELLO (0x01) got 0x${cmd.toString(16)}",
                sessionId = sessionId,
                receivedByte = cmd,
            ) as Exception)

        Timber.d("[QC_EDL] SAHARA_HELLO cmd=$cmd size=$size sessionId=$sessionId")

        // Send HELLO_RESPONSE (mirror version fields)
        val resp = ByteArray(48)
        writeLE32(resp, 0, SAHARA_HELLO_RESP)
        writeLE32(resp, 4, 48)
        writeLE32(resp, 8, readLE32(hello, 8))   // version
        writeLE32(resp, 12, readLE32(hello, 12)) // versionMin
        writeLE32(resp, 16, 0x00)                // status = 0 = OK
        writeLE32(resp, 20, 0x00)                // mode = 0 = IMAGE_TX

        val sent = conn.bulkTransfer(epOut, resp, 48, TIMEOUT_SAHARA)
        Timber.d("[QC_EDL] sahara_hello_resp sent=$sent sessionId=$sessionId")

        return Result.success("SAHARA_OK")
    }

    // ── Upload programmer ─────────────────────────────────────────────────

    private fun uploadProgrammer(
        conn:       UsbDeviceConnection,
        epOut:      UsbEndpoint,
        epIn:       UsbEndpoint,
        progBytes:  ByteArray,
        sessionId:  String,
        onProgress: (Int) -> Unit,
    ): Result<Unit> {
        var offset = 0
        val total  = progBytes.size

        // Loop: read DATA_REQ from device → send requested offset+length
        while (offset < total) {
            val req  = ByteArray(64)
            val n    = conn.bulkTransfer(epIn, req, 64, TIMEOUT_SAHARA)
            if (n < 0) break

            val cmd   = readLE32(req, 0)
            if (cmd == SAHARA_END_TRANSFER) break

            if (cmd == SAHARA_READ_DATA) {
                val reqOffset = readLE32(req, 8)
                val reqLen    = readLE32(req, 12)

                val end   = minOf(reqOffset + reqLen, total)
                val chunk = progBytes.copyOfRange(reqOffset, end)

                conn.bulkTransfer(epOut, chunk, chunk.size, TIMEOUT_SAHARA)
                offset = end

                onProgress((offset * 100) / total)
                Timber.d("[QC_EDL] prog_upload offset=$offset/$total " +
                         "sessionId=$sessionId")
            }
        }

        // ZLP if needed
        if (total % epOut.maxPacketSize == 0) {
            conn.bulkTransfer(epOut, ByteArray(0), 0, 2000)
            Timber.d("[QC_EDL] prog_upload ZLP sent sessionId=$sessionId")
        }

        // Send DONE
        val done = ByteArray(8)
        writeLE32(done, 0, SAHARA_DONE)
        writeLE32(done, 4, 8)
        conn.bulkTransfer(epOut, done, 8, TIMEOUT_SAHARA)

        // Read DONE_RESP
        val doneResp = ByteArray(16)
        val dr       = conn.bulkTransfer(epIn, doneResp, 16, TIMEOUT_SAHARA)
        val respCmd  = readLE32(doneResp, 0)
        Timber.d("[QC_EDL] prog_done resp=0x${respCmd.toString(16)} " +
                 "recv=$dr sessionId=$sessionId")

        return Result.success(Unit)
    }

    // ── Firehose configure ────────────────────────────────────────────────

    private fun firehoseConfigure(
        conn:      UsbDeviceConnection,
        epOut:     UsbEndpoint,
        epIn:      UsbEndpoint,
        sessionId: String,
    ): Result<Unit> {
        val xml = """<?xml version="1.0"?><data><configure MemoryName="eMMC" Verbose="0" AlwaysValidate="0" MaxDigestTableSizeInBytes="2048" MaxPayloadSizeToTargetInBytes="1048576" ZlpAwareHost="1" SkipStorageInit="0"/></data>"""
        sendFirehoseXml(conn, epOut, xml, sessionId)
        val resp = readFirehoseResponse(conn, epIn, sessionId)
        Timber.d("[QC_EDL] configure resp=${resp?.take(200)} sessionId=$sessionId")
        return Result.success(Unit)
    }

    // ── Firehose erase partition ──────────────────────────────────────────

    private fun firehoseErase(
        conn:      UsbDeviceConnection,
        epOut:     UsbEndpoint,
        epIn:      UsbEndpoint,
        partition: String,
        sessionId: String,
    ): Result<Unit> {
        val xml = """<?xml version="1.0"?><data><erase LABEL="$partition"/></data>"""
        sendFirehoseXml(conn, epOut, xml, sessionId)
        val resp = readFirehoseResponse(conn, epIn, sessionId) ?: ""
        Timber.d("[QC_EDL] erase_$partition resp=${resp.take(300)} sessionId=$sessionId")

        return if ("ACK" in resp || "value=\"ACK\"" in resp) {
            Timber.d("[QC_EDL] erase $partition ACK sessionId=$sessionId")
            Result.success(Unit)
        } else {
            Result.failure(ProtocolResult.PartitionNotFound(
                reason = "Firehose erase NAK for $partition: ${resp.take(100)}",
                sessionId = sessionId, partitionName = partition) as Exception)
        }
    }

    private fun firehoseEraseFrpMisc(
        conn:      UsbDeviceConnection,
        epOut:     UsbEndpoint,
        epIn:      UsbEndpoint,
        sessionId: String,
    ): Result<Unit> {
        // Try 'misc' partition — contains FRP data on some Qualcomm devices
        return firehoseErase(conn, epOut, epIn, "misc", sessionId)
    }

    // ── Firehose reset ────────────────────────────────────────────────────

    private fun firehoseReset(
        conn:      UsbDeviceConnection,
        epOut:     UsbEndpoint,
        epIn:      UsbEndpoint,
        sessionId: String,
    ) {
        val xml = """<?xml version="1.0"?><data><power value="reset"/></data>"""
        sendFirehoseXml(conn, epOut, xml, sessionId)
        Timber.d("[QC_EDL] reset sent sessionId=$sessionId")
    }

    // ── Firehose XML send/receive ─────────────────────────────────────────

    private fun sendFirehoseXml(
        conn:      UsbDeviceConnection,
        epOut:     UsbEndpoint,
        xml:       String,
        sessionId: String,
    ) {
        val bytes = xml.toByteArray(Charsets.UTF_8)
        var offset = 0
        while (offset < bytes.size) {
            val len   = minOf(FIREHOSE_CHUNK, bytes.size - offset)
            val chunk = bytes.copyOfRange(offset, offset + len)
            conn.bulkTransfer(epOut, chunk, len, TIMEOUT_FIREHOSE)
            offset += len
        }
        // ZLP if needed
        if (bytes.size % epOut.maxPacketSize == 0) {
            conn.bulkTransfer(epOut, ByteArray(0), 0, 2000)
        }
        Timber.d("[QC_EDL] firehose_xml sent len=${bytes.size} sessionId=$sessionId")
    }

    private fun readFirehoseResponse(
        conn:      UsbDeviceConnection,
        epIn:      UsbEndpoint,
        sessionId: String,
    ): String? {
        val sb    = StringBuilder()
        val buf   = ByteArray(4096)
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < TIMEOUT_FIREHOSE) {
            val n = conn.bulkTransfer(epIn, buf, buf.size, 5000)
            if (n > 0) {
                sb.append(buf.copyOfRange(0, n).toString(Charsets.UTF_8))
                if ("</data>" in sb || "</response>" in sb) break
                if (sb.length > MAX_RESPONSE_SIZE) break
            } else {
                break
            }
        }

        val result = sb.toString()
        Timber.d("[QC_EDL] firehose_resp len=${result.length} " +
                 "ack=${result.contains("ACK")} sessionId=$sessionId")
        return result.takeIf { it.isNotBlank() }
    }

    // ── Asset loader ──────────────────────────────────────────────────────

    private fun loadAsset(path: String): ByteArray? = runCatching {
        context.assets.open(path).readBytes()
    }.getOrNull()

    // ── Endpoint helpers ──────────────────────────────────────────────────

    private fun findBulkOut(device: UsbDevice): UsbEndpoint? {
        val iface = device.getInterface(0)
        return (0 until iface.endpointCount).map { iface.getEndpoint(it) }
            .firstOrNull {
                it.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK &&
                it.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT
            }
    }

    private fun findBulkIn(device: UsbDevice): UsbEndpoint? {
        val iface = device.getInterface(0)
        return (0 until iface.endpointCount).map { iface.getEndpoint(it) }
            .firstOrNull {
                it.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK &&
                it.direction == android.hardware.usb.UsbConstants.USB_DIR_IN
            }
    }

    // ── LE helpers ────────────────────────────────────────────────────────
    private fun readLE32(buf: ByteArray, offset: Int): Int =
        (buf[offset].toInt() and 0xFF) or
        ((buf[offset+1].toInt() and 0xFF) shl 8) or
        ((buf[offset+2].toInt() and 0xFF) shl 16) or
        ((buf[offset+3].toInt() and 0xFF) shl 24)

    private fun writeLE32(buf: ByteArray, offset: Int, value: Int) {
        buf[offset]   = (value        and 0xFF).toByte()
        buf[offset+1] = ((value shr 8)  and 0xFF).toByte()
        buf[offset+2] = ((value shr 16) and 0xFF).toByte()
        buf[offset+3] = ((value shr 24) and 0xFF).toByte()
    }
}
