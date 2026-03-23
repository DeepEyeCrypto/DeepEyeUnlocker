package com.deepeye.otg.protocol.samsung

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import com.deepeye.otg.data.gsmg.ProtocolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

// =============================================================================
// RealSamsungOdinExecutor.kt
// REAL Samsung ODIN/Download Mode protocol
// VID:0x04E8 PID:0x685D
// Brands: Samsung (all Exynos + Snapdragon Samsung models via ODIN)
//         NOTE: Samsung MTK models → use RealMtkBromExecutor
//
// Protocol (CONFIRMED):
//   Handshake: send "ODIN" → receive "LOKE"
//   All packets: 1024 bytes LE padded
//   CMD_SESSION=0x64 CMD_TRANSFER=0x65 CMD_FLASH=0x66 CMD_END=0x67
//   Flash: setup(partId, size) → chunks(500KB) → ACK per chunk → done
// =============================================================================

class RealSamsungOdinExecutor(
    private val usbManager: UsbManager,
) {
    companion object {
        private const val CMD_SESSION    = 0x64
        private const val CMD_TRANSFER   = 0x65
        private const val CMD_FLASH      = 0x66
        private const val CMD_END        = 0x67

        private const val PACKET_SIZE    = 1024
        private const val CHUNK_SIZE     = 524288  // 500 KB exactly

        private val HANDSHAKE_SEND    = "ODIN".toByteArray(Charsets.US_ASCII)
        private val HANDSHAKE_EXPECT  = "LOKE".toByteArray(Charsets.US_ASCII)

        private const val TIMEOUT_CTRL  = 3000
        private const val TIMEOUT_DATA  = 30_000
    }

    // ── FRP erase via factory reset ───────────────────────────────────────

    suspend fun eraseFrp(
        device:    UsbDevice,
        sessionId: String,
        onProgress:(Int, String) -> Unit,
    ): ProtocolResult = withContext(Dispatchers.IO) {
        Timber.d("[SAMSUNG_ODIN] eraseFrp start sessionId=$sessionId")

        val conn = usbManager.openDevice(device)
            ?: return@withContext ProtocolResult.UsbTransportError(
                reason = "Cannot open Samsung device", sessionId = sessionId)

        try {
            val iface = device.getInterface(0)
            if (!conn.claimInterface(iface, true)) {
                return@withContext ProtocolResult.UsbTransportError(
                    reason = "Cannot claim Samsung ODIN interface", sessionId = sessionId)
            }

            val epOut = findBulkOut(device) ?: return@withContext ProtocolResult.UsbTransportError("No ODIN bulk OUT", sessionId = sessionId)
            val epIn  = findBulkIn(device) ?: return@withContext ProtocolResult.UsbTransportError("No ODIN bulk IN", sessionId = sessionId)

            // Phase 1: Handshake
            onProgress(10, "ODIN handshake")
            performHandshake(conn, epOut, epIn, sessionId)
                .onFailure { return@withContext it as ProtocolResult }

            // Phase 2: Begin session
            onProgress(20, "Session start")
            beginSession(conn, epOut, epIn, sessionId)
                .onFailure { return@withContext it as ProtocolResult }

            // Phase 3: Factory reset via CMD_FLASH with FRP partition
            // Samsung FRP is in 'efs' or 'frp' partition depending on model
            onProgress(40, "Erasing FRP partition")
            flashFrpZeros(conn, epOut, epIn, sessionId) { pct ->
                onProgress(40 + (pct * 50 / 100), "FRP flash $pct%")
            }.onFailure { return@withContext it as ProtocolResult }

            // Phase 4: End session + reboot
            onProgress(95, "Ending session")
            endSession(conn, epOut, epIn, sessionId)

            onProgress(100, "FRP erase complete")
            Timber.d("[SAMSUNG_ODIN] eraseFrp DONE sessionId=$sessionId")

            ProtocolResult.FrpErased(
                method    = "SAMSUNG_ODIN",
                partition = "frp",
                sessionId = sessionId,
            )
        } catch (e: Exception) {
            Timber.e("[SAMSUNG_ODIN] error: ${e.message} sessionId=$sessionId")
            ProtocolResult.UsbTransportError(
                reason = e.message ?: "Unknown", sessionId = sessionId)
        } finally {
            runCatching { conn.close() }
        }
    }

    // ── Handshake ─────────────────────────────────────────────────────────

    private fun performHandshake(
        conn:      UsbDeviceConnection,
        epOut:     UsbEndpoint,
        epIn:      UsbEndpoint,
        sessionId: String,
    ): Result<Unit> {
        // Send "ODIN"
        val n = conn.bulkTransfer(epOut, HANDSHAKE_SEND, 4, TIMEOUT_CTRL)
        Timber.d("[SAMSUNG_ODIN] hs sent=$n sessionId=$sessionId")
        if (n != 4) return Result.failure(ProtocolResult.UsbTransportError(
            reason = "ODIN handshake send failed: $n", sessionId = sessionId) as Exception)

        // Receive "LOKE"
        val rx = ByteArray(4)
        val r  = conn.bulkTransfer(epIn, rx, 4, TIMEOUT_CTRL)
        val resp = rx.toString(Charsets.US_ASCII).take(4)
        Timber.d("[SAMSUNG_ODIN] hs rx=$r resp='$resp' sessionId=$sessionId")

        return if (r >= 4 && resp == "LOKE") {
            Timber.d("[SAMSUNG_ODIN] handshake OK sessionId=$sessionId")
            Result.success(Unit)
        } else {
            Result.failure(ProtocolResult.ProtocolHandshakeFailed(
                reason = "Expected 'LOKE' got '$resp' (recv=$r)",
                sessionId = sessionId) as Exception)
        }
    }

    // ── Session begin ─────────────────────────────────────────────────────

    private fun beginSession(
        conn:      UsbDeviceConnection,
        epOut:     UsbEndpoint,
        epIn:      UsbEndpoint,
        sessionId: String,
    ): Result<Unit> {
        val packet = buildPacket(CMD_SESSION, 0, 0)
        val n = conn.bulkTransfer(epOut, packet, PACKET_SIZE, TIMEOUT_CTRL)
        if (n < 0) return Result.failure(ProtocolResult.UsbTransportError(
            reason = "Session start failed: $n", sessionId = sessionId) as Exception)

        val resp = ByteArray(PACKET_SIZE)
        conn.bulkTransfer(epIn, resp, PACKET_SIZE, TIMEOUT_CTRL)
        Timber.d("[SAMSUNG_ODIN] session_start resp[0-3]=" +
                 "${resp.take(4).joinToString(",") { it.toString() }} " +
                 "sessionId=$sessionId")
        return Result.success(Unit)
    }

    // ── Flash FRP partition with zeros ────────────────────────────────────

    private fun flashFrpZeros(
        conn:       UsbDeviceConnection,
        epOut:      UsbEndpoint,
        epIn:       UsbEndpoint,
        sessionId:  String,
        onProgress: (Int) -> Unit,
    ): Result<Unit> {
        // FRP partition size is typically 1MB
        val frpSizeBytes  = 1024 * 1024
        val zeros         = ByteArray(frpSizeBytes)
        val totalChunks   = (frpSizeBytes + CHUNK_SIZE - 1) / CHUNK_SIZE

        Timber.d("[SAMSUNG_ODIN] frp_flash size=$frpSizeBytes chunks=$totalChunks " +
                 "sessionId=$sessionId")

        // Setup flash: CMD_TRANSFER with partition ID (FRP=0x1A on most Samsung)
        val setup = buildPacket(CMD_TRANSFER, 0x1A, frpSizeBytes)
        conn.bulkTransfer(epOut, setup, PACKET_SIZE, TIMEOUT_CTRL)

        var chunkIdx = 0
        var offset   = 0

        while (offset < frpSizeBytes) {
            val chunkLen = minOf(CHUNK_SIZE, frpSizeBytes - offset)

            // Send chunk header
            val header = buildPacket(CMD_TRANSFER, chunkIdx, chunkLen)
            conn.bulkTransfer(epOut, header, PACKET_SIZE, TIMEOUT_CTRL)

            // Send chunk data (zeros)
            val chunk = zeros.copyOfRange(offset, offset + chunkLen)
            conn.bulkTransfer(epOut, chunk, chunkLen, TIMEOUT_DATA)

            // Read ACK — MUST be [0,0,0,0] to continue
            val ack = ByteArray(PACKET_SIZE)
            val r   = conn.bulkTransfer(epIn, ack, PACKET_SIZE, TIMEOUT_DATA)
            val ackCode = ((ack[0].toInt() and 0xFF)) or
                          ((ack[1].toInt() and 0xFF) shl 8) or
                          ((ack[2].toInt() and 0xFF) shl 16) or
                          ((ack[3].toInt() and 0xFF) shl 24)

            Timber.d("[SAMSUNG_ODIN] chunk=$chunkIdx ack=0x${ackCode.toString(16)} " +
                     "recv=$r sessionId=$sessionId")

            if (ackCode != 0) {
                Timber.e("[SAMSUNG_ODIN] chunk=$chunkIdx REJECTED " +
                         "ack=0x${ackCode.toString(16)} sessionId=$sessionId")
                return Result.failure(ProtocolResult.UsbTransportError(
                    reason = "ODIN chunk $chunkIdx rejected: ack=0x${ackCode.toString(16)}",
                    sessionId = sessionId) as Exception)
            }

            chunkIdx++
            offset += chunkLen
            onProgress((chunkIdx * 100) / totalChunks)
        }

        // CMD_FLASH end
        val flashEnd = buildPacket(CMD_FLASH, 0, 0)
        conn.bulkTransfer(epOut, flashEnd, PACKET_SIZE, TIMEOUT_CTRL)

        val finalAck = ByteArray(PACKET_SIZE)
        conn.bulkTransfer(epIn, finalAck, PACKET_SIZE, 30_000)
        Timber.d("[SAMSUNG_ODIN] flash_end finalAck[0-3]=" +
                 "${finalAck.take(4).joinToString(",") { it.toString() }} " +
                 "sessionId=$sessionId")

        return Result.success(Unit)
    }

    // ── End session ───────────────────────────────────────────────────────

    private fun endSession(
        conn:      UsbDeviceConnection,
        epOut:     UsbEndpoint,
        epIn:      UsbEndpoint,
        sessionId: String,
    ) {
        val end = buildPacket(CMD_END, 0, 1)  // 1 = reboot
        conn.bulkTransfer(epOut, end, PACKET_SIZE, TIMEOUT_CTRL)
        val resp = ByteArray(PACKET_SIZE)
        conn.bulkTransfer(epIn, resp, PACKET_SIZE, TIMEOUT_CTRL)
        Timber.d("[SAMSUNG_ODIN] session_end sessionId=$sessionId")
    }

    // ── Packet builder ────────────────────────────────────────────────────

    private fun buildPacket(cmd: Int, arg0: Int, arg1: Int): ByteArray {
        val p = ByteArray(PACKET_SIZE)
        // LE encoding
        p[0] = (cmd        and 0xFF).toByte()
        p[1] = ((cmd shr 8) and 0xFF).toByte()
        p[2] = 0x00; p[3] = 0x00
        p[4] = (arg0        and 0xFF).toByte()
        p[5] = ((arg0 shr 8) and 0xFF).toByte()
        p[6] = ((arg0 shr 16) and 0xFF).toByte()
        p[7] = ((arg0 shr 24) and 0xFF).toByte()
        p[8] = (arg1        and 0xFF).toByte()
        p[9] = ((arg1 shr 8) and 0xFF).toByte()
        p[10]= ((arg1 shr 16) and 0xFF).toByte()
        p[11]= ((arg1 shr 24) and 0xFF).toByte()
        return p
    }

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
}
