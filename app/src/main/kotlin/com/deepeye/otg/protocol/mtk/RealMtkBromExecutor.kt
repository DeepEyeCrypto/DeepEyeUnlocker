package com.deepeye.otg.protocol.mtk

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import com.deepeye.otg.data.gsmg.ProtocolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

// =============================================================================
// RealMtkBromExecutor.kt
// REAL MTK BROM Classic protocol — VID:0x0E8D PID:0x0003
// Chips: MT6765, MT6768, MT6769, MT6785, MT6762, MT6761, MT6580
// Brands: Realme, Samsung MTK, Infinix, Tecno, Itel, Micromax,
//         Xiaomi MTK, OPPO MTK, Nokia MTK, Motorola MTK
//
// BROM handshake (CONFIRMED — 4 byte XOR sequence):
//   0xA0 → 0x5F
//   0x0A → 0xF5
//   0x50 → 0xAF
//   0x05 → 0xFA
// Post-handshake: device sends 8 bytes (hwCode[2] hwSub[2] hwVer[2] swVer[2])
// =============================================================================

import android.content.Context

class RealMtkBromExecutor(
    private val usbManager: UsbManager,
    private val context:    Context,
) {
    companion object {
        // BROM handshake bytes (exact — never change)
        private val BROM_HS_SEND = byteArrayOf(0xA0.toByte(), 0x0A, 0x50, 0x05)
        private val BROM_HS_EXPECT = byteArrayOf(
            0x5F.toByte(), 0xF5.toByte(), 0xAF.toByte(), 0xFA.toByte()
        )

        // Commands
        private const val CMD_SEND_DA     = 0xD7.toByte()
        private const val CMD_JUMP_DA     = 0xD5.toByte()
        private const val CMD_READ32      = 0xD1.toByte()
        private const val CMD_WRITE32     = 0xD4.toByte()
        private const val CMD_GET_TARGET  = 0xD8.toByte()

        private const val ACK             = 0x5A.toByte()
        private const val NACK            = 0xA5.toByte()

        private const val TIMEOUT_BYTE   = 2000
        private const val TIMEOUT_CHUNK  = 5000
        private const val TIMEOUT_ERASE  = 30_000
        private const val DA_CHUNK_SIZE  = 4096
    }

    // ── FRP erase ─────────────────────────────────────────────────────────

    suspend fun eraseFrp(
        device:    UsbDevice,
        daBytes:   ByteArray,
        sessionId: String,
        onProgress:(Int, String) -> Unit,
    ): ProtocolResult = withContext(Dispatchers.IO) {
        Timber.d("[MTK_BROM] eraseFrp start sessionId=$sessionId " +
                 "device=${device.deviceName}")

        val conn = usbManager.openDevice(device)
            ?: return@withContext ProtocolResult.UsbTransportError(
                reason    = "Cannot open device — permission denied?",
                sessionId = sessionId,
            )

        try {
            val epOut = findBulkOut(device, sessionId)
                ?: return@withContext ProtocolResult.UsbTransportError(
                    reason = "Bulk OUT not found on IF#1", sessionId = sessionId)
            val epIn  = findBulkIn(device, sessionId)
                ?: return@withContext ProtocolResult.UsbTransportError(
                    reason = "Bulk IN not found on IF#1", sessionId = sessionId)

            // Claim interfaces
            if (!conn.claimInterface(device.getInterface(0), true)) {
                return@withContext ProtocolResult.UsbTransportError(
                    reason = "Cannot claim IF#0", sessionId = sessionId)
            }
            if (device.interfaceCount > 1) {
                conn.claimInterface(device.getInterface(1), true)
            }

            // Phase 1: BROM handshake
            onProgress(5, "BROM handshake")
            val chipInfo = performHandshake(conn, epOut, epIn, sessionId)
                .getOrElse { return@withContext it as ProtocolResult }

            Timber.d("[MTK_BROM] handshake OK chip=${chipInfo.chipName} " +
                     "hw_code=0x${chipInfo.hwCode.toString(16)} " +
                     "sessionId=$sessionId")

            // Phase 2: GET_TARGET — check SLA/DAA flags
            onProgress(15, "Checking security flags")
            val targetConfig = getTargetConfig(conn, epOut, epIn, sessionId)
                .getOrElse { return@withContext it as ProtocolResult }

            if (targetConfig.slaRequired) {
                Timber.w("[MTK_BROM] SLA required — not implemented " +
                         "sessionId=$sessionId")
                return@withContext ProtocolResult.AuthenticationFailed(
                    reason    = "SLA (Serial Link Authentication) required. " +
                                "Device has security enabled. " +
                                "SLA key needed for this device.",
                    sessionId = sessionId,
                    authType  = "MTK_SLA",
                )
            }

            // Phase 3: SEND_DA
            onProgress(25, "Sending Download Agent")
            sendDa(conn, epOut, epIn, daBytes, sessionId) { pct ->
                onProgress(25 + (pct * 30 / 100), "DA upload $pct%")
            }.onFailure { return@withContext it as ProtocolResult }

            // Phase 4: JUMP_DA
            onProgress(60, "Jumping to DA")
            jumpDa(conn, epOut, epIn, sessionId)
                .onFailure { return@withContext it as ProtocolResult }

            // Phase 5: Erase FRP via DA command
            onProgress(75, "Erasing FRP partition")
            eraseFrpPartition(conn, epOut, epIn, sessionId)
                .onFailure { return@withContext it as ProtocolResult }

            onProgress(100, "FRP erase complete")
            Timber.d("[MTK_BROM] frp_erase DONE chip=${chipInfo.chipName} " +
                     "sessionId=$sessionId")

            ProtocolResult.FrpErased(
                method    = "MTK_BROM_CLASSIC_DA",
                partition = "frp",
                sessionId = sessionId,
            )
        } catch (e: Exception) {
            Timber.e("[MTK_BROM] error: ${e.message} sessionId=$sessionId")
            ProtocolResult.UsbTransportError(
                reason    = e.message ?: "Unknown",
                sessionId = sessionId,
            )
        } finally {
            runCatching { conn.close() }
            Timber.d("[MTK_BROM] conn closed sessionId=$sessionId")
        }
    }

    // ── Read device info (no DA needed) ──────────────────────────────────

    suspend fun readInfo(
        device:    UsbDevice,
        sessionId: String,
    ): ProtocolResult = withContext(Dispatchers.IO) {
        val conn = usbManager.openDevice(device)
            ?: return@withContext ProtocolResult.UsbTransportError(
                reason = "Cannot open device", sessionId = sessionId)
        try {
            val epOut = findBulkOut(device, sessionId) ?: return@withContext ProtocolResult.UsbTransportError("No bulk OUT", sessionId = sessionId)
            val epIn  = findBulkIn(device, sessionId) ?: return@withContext ProtocolResult.UsbTransportError("No bulk IN", sessionId = sessionId)
            conn.claimInterface(device.getInterface(0), true)

            val info = performHandshake(conn, epOut, epIn, sessionId)
                .getOrElse { return@withContext it as ProtocolResult }

            ProtocolResult.DeviceInfoRead(
                imei      = null,
                imei2     = null,
                serial    = null,
                ecid      = null,
                chipName  = info.chipName,
                iosVersion= null,
                btMac     = null,
                wifiMac   = null,
                sessionId = sessionId,
            )
        } finally {
            runCatching { conn.close() }
        }
    }

    // ── Phase: BROM handshake ─────────────────────────────────────────────

    private fun performHandshake(
        conn:      UsbDeviceConnection,
        epOut:     UsbEndpoint,
        epIn:      UsbEndpoint,
        sessionId: String,
    ): Result<BromChipInfo> {
        // Send 4 bytes + verify XOR responses one at a time
        for (i in BROM_HS_SEND.indices) {
            val sent = byteArrayOf(BROM_HS_SEND[i])
            val n    = conn.bulkTransfer(epOut, sent, 1, TIMEOUT_BYTE)
            if (n != 1) {
                Timber.e("[MTK_BROM] hs step=$i send FAILED n=$n " +
                         "sessionId=$sessionId")
                return Result.failure(ProtocolResult.ProtocolHandshakeFailed(
                    reason    = "Handshake step $i: send failed (n=$n)",
                    sessionId = sessionId,
                    sentByte  = BROM_HS_SEND[i].toInt() and 0xFF,
                ) as Exception)
            }

            val rx = ByteArray(1)
            val r  = conn.bulkTransfer(epIn, rx, 1, TIMEOUT_BYTE)
            Timber.d("[MTK_BROM] hs step=$i sent=0x${BROM_HS_SEND[i].toString(16)} " +
                     "rx=0x${rx[0].toString(16)} expected=0x${BROM_HS_EXPECT[i].toString(16)} " +
                     "sessionId=$sessionId")

            if (r != 1 || rx[0] != BROM_HS_EXPECT[i]) {
                return Result.failure(ProtocolResult.ProtocolHandshakeFailed(
                    reason       = "Handshake step $i: got 0x${rx[0].toString(16)} " +
                                   "expected 0x${BROM_HS_EXPECT[i].toString(16)}",
                    sessionId    = sessionId,
                    sentByte     = BROM_HS_SEND[i].toInt() and 0xFF,
                    receivedByte = rx[0].toInt() and 0xFF,
                ) as Exception)
            }
        }

        // Read 8-byte chip info
        val info = ByteArray(8)
        val n    = conn.bulkTransfer(epIn, info, 8, TIMEOUT_BYTE)
        if (n < 8) {
            return Result.failure(ProtocolResult.ProtocolHandshakeFailed(
                reason    = "Chip info recv $n/8 bytes",
                sessionId = sessionId,
            ) as Exception)
        }

        val hwCode = ((info[0].toInt() and 0xFF) shl 8) or (info[1].toInt() and 0xFF)
        val hwSub  = ((info[2].toInt() and 0xFF) shl 8) or (info[3].toInt() and 0xFF)

        Timber.d("[MTK_BROM] chip_info hwCode=0x${hwCode.toString(16)} " +
                 "hwSub=0x${hwSub.toString(16)} sessionId=$sessionId")

        val chip = com.deepeye.otg.data.MtkChipDatabase.find(hwCode)

        return Result.success(BromChipInfo(
            hwCode   = hwCode,
            hwSub    = hwSub,
            chipName = chip?.chipName ?: "MTK 0x${hwCode.toString(16)}",
        ))
    }

    // ── Phase: GET_TARGET (check SLA/DAA) ────────────────────────────────

    private fun getTargetConfig(
        conn:      UsbDeviceConnection,
        epOut:     UsbEndpoint,
        epIn:      UsbEndpoint,
        sessionId: String,
    ): Result<TargetConfig> {
        val cmd = byteArrayOf(CMD_GET_TARGET)
        val n   = conn.bulkTransfer(epOut, cmd, 1, TIMEOUT_BYTE)
        if (n != 1) return Result.failure(ProtocolResult.UsbTransportError(
            reason = "GET_TARGET send failed", sessionId = sessionId) as Exception)

        val buf = ByteArray(4)
        conn.bulkTransfer(epIn, buf, 4, TIMEOUT_BYTE)

        val config = ((buf[0].toInt() and 0xFF) shl 24) or
                     ((buf[1].toInt() and 0xFF) shl 16) or
                     ((buf[2].toInt() and 0xFF) shl 8)  or
                     (buf[3].toInt() and 0xFF)

        val secureBoot = (config and 0x01) != 0
        val slaRequired= (config and 0x04) != 0
        val daaRequired= (config and 0x08) != 0

        Timber.d("[MTK_BROM] target_config=0x${config.toString(16)} " +
                 "secureBoot=$secureBoot sla=$slaRequired daa=$daaRequired " +
                 "sessionId=$sessionId")

        return Result.success(TargetConfig(secureBoot, slaRequired, daaRequired))
    }

    // ── Phase: SEND_DA ────────────────────────────────────────────────────

    private fun sendDa(
        conn:       UsbDeviceConnection,
        epOut:      UsbEndpoint,
        epIn:       UsbEndpoint,
        daBytes:    ByteArray,
        sessionId:  String,
        onProgress: (Int) -> Unit,
    ): Result<Unit> {
        val DA_LOAD_ADDR = 0x200000

        // SEND_DA: cmd[1] + addr[4BE] + len[4BE] + sigLen[4BE] = 13 bytes
        val total = daBytes.size
        val header = ByteArray(13)
        header[0]  = CMD_SEND_DA
        header[1]  = ((DA_LOAD_ADDR ushr 24) and 0xFF).toByte()
        header[2]  = ((DA_LOAD_ADDR ushr 16) and 0xFF).toByte()
        header[3]  = ((DA_LOAD_ADDR ushr 8)  and 0xFF).toByte()
        header[4]  = (DA_LOAD_ADDR           and 0xFF).toByte()
        header[5]  = ((total ushr 24) and 0xFF).toByte()
        header[6]  = ((total ushr 16) and 0xFF).toByte()
        header[7]  = ((total ushr 8)  and 0xFF).toByte()
        header[8]  = (total           and 0xFF).toByte()
        header[9]  = 0x00; header[10] = 0x00  // sigLen = 0
        header[11] = 0x00; header[12] = 0x00

        val hn = conn.bulkTransfer(epOut, header, header.size, TIMEOUT_BYTE)
        if (hn != header.size) return Result.failure(
            ProtocolResult.UsbTransportError(
                reason = "SEND_DA header failed: $hn", sessionId = sessionId,
            ) as Exception)

        // Read ACK
        val ack = ByteArray(1)
        conn.bulkTransfer(epIn, ack, 1, TIMEOUT_BYTE)
        if (ack[0] != ACK) return Result.failure(
            ProtocolResult.ProtocolHandshakeFailed(
                reason       = "SEND_DA no ACK: 0x${ack[0].toString(16)}",
                sessionId    = sessionId,
                receivedByte = ack[0].toInt() and 0xFF,
            ) as Exception)

        // Send DA in chunks, compute checksum
        var sent  = 0
        var csum  = 0
        var offset= 0
        while (offset < total) {
            val len   = minOf(DA_CHUNK_SIZE, total - offset)
            val chunk = daBytes.copyOfRange(offset, offset + len)
            chunk.forEach { csum = (csum + (it.toInt() and 0xFF)) and 0xFFFF }

            val n = conn.bulkTransfer(epOut, chunk, len, TIMEOUT_CHUNK)
            if (n < 0) return Result.failure(ProtocolResult.UsbTransportError(
                reason = "DA data failed at offset=$offset", sessionId = sessionId,
                transferred = n) as Exception)

            sent   += n
            offset += len
            onProgress((sent * 100) / total)
        }

        // ZLP if needed
        if (total % epOut.maxPacketSize == 0) {
            conn.bulkTransfer(epOut, ByteArray(0), 0, 2000)
        }

        // Read 2-byte BE checksum from device
        val csBuf = ByteArray(2)
        conn.bulkTransfer(epIn, csBuf, 2, TIMEOUT_BYTE)
        val deviceCs = ((csBuf[0].toInt() and 0xFF) shl 8) or (csBuf[1].toInt() and 0xFF)

        if (deviceCs != csum) {
            Timber.e("[MTK_BROM] checksum MISMATCH expected=0x${csum.toString(16)} " +
                     "actual=0x${deviceCs.toString(16)} sessionId=$sessionId")
            return Result.failure(ProtocolResult.DaChecksumMismatch(
                expected = csum, actual = deviceCs, sessionId = sessionId) as Exception)
        }

        Timber.d("[MTK_BROM] DA checksum OK 0x${csum.toString(16)} sessionId=$sessionId")
        return Result.success(Unit)
    }

    // ── Phase: JUMP_DA ────────────────────────────────────────────────────

    private fun jumpDa(
        conn:      UsbDeviceConnection,
        epOut:     UsbEndpoint,
        epIn:      UsbEndpoint,
        sessionId: String,
    ): Result<Unit> {
        val DA_LOAD_ADDR = 0x200000
        val cmd = ByteArray(5)
        cmd[0] = CMD_JUMP_DA
        cmd[1] = ((DA_LOAD_ADDR ushr 24) and 0xFF).toByte()
        cmd[2] = ((DA_LOAD_ADDR ushr 16) and 0xFF).toByte()
        cmd[3] = ((DA_LOAD_ADDR ushr 8)  and 0xFF).toByte()
        cmd[4] = (DA_LOAD_ADDR           and 0xFF).toByte()

        val n = conn.bulkTransfer(epOut, cmd, cmd.size, TIMEOUT_BYTE)
        if (n != cmd.size) return Result.failure(ProtocolResult.UsbTransportError(
            reason = "JUMP_DA send failed: $n", sessionId = sessionId) as Exception)

        val ack = ByteArray(1)
        conn.bulkTransfer(epIn, ack, 1, TIMEOUT_BYTE)
        Timber.d("[MTK_BROM] jump_da ack=0x${ack[0].toString(16)} sessionId=$sessionId")

        return if (ack[0] == ACK) Result.success(Unit)
        else Result.failure(ProtocolResult.ProtocolHandshakeFailed(
            reason = "JUMP_DA NACK: 0x${ack[0].toString(16)}",
            sessionId = sessionId, receivedByte = ack[0].toInt() and 0xFF,
        ) as Exception)
    }

    // ── Phase: erase FRP partition ────────────────────────────────────────

    private fun eraseFrpPartition(
        conn:      UsbDeviceConnection,
        epOut:     UsbEndpoint,
        epIn:      UsbEndpoint,
        sessionId: String,
    ): Result<Unit> {
        val nameBytes = ("frp\u0000").toByteArray(Charsets.UTF_8)
        val cmd = byteArrayOf(0x71.toByte()) + nameBytes

        val n = conn.bulkTransfer(epOut, cmd, cmd.size, TIMEOUT_BYTE)
        Timber.d("[MTK_BROM] erase_frp cmd sent=$n sessionId=$sessionId")

        val result = ByteArray(1)
        val r = conn.bulkTransfer(epIn, result, 1, TIMEOUT_ERASE)
        Timber.d("[MTK_BROM] erase_frp result=0x${result[0].toString(16)} " +
                 "recv=$r sessionId=$sessionId")

        return if (r == 1 && result[0] == ACK) {
            Timber.d("[MTK_BROM] frp erase ACK sessionId=$sessionId")
            Result.success(Unit)
        } else {
            Result.failure(ProtocolResult.PartitionNotFound(
                reason = "FRP erase NACK: 0x${result[0].toString(16)}",
                sessionId = sessionId, partitionName = "frp") as Exception)
        }
    }

    // ── Endpoint helpers ──────────────────────────────────────────────────

    private fun findBulkOut(device: UsbDevice, sessionId: String): UsbEndpoint? {
        val iface = runCatching {
            if (device.interfaceCount > 1) device.getInterface(1)
            else device.getInterface(0)
        }.getOrNull() ?: return null
        return (0 until iface.endpointCount)
            .map { iface.getEndpoint(it) }
            .firstOrNull {
                it.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK &&
                it.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT
            }.also {
                if (it != null) Timber.d("[MTK_BROM] bulk_out addr=${it.address} " +
                    "maxPkt=${it.maxPacketSize} sessionId=$sessionId")
            }
    }

    private fun findBulkIn(device: UsbDevice, sessionId: String): UsbEndpoint? {
        val iface = runCatching {
            if (device.interfaceCount > 1) device.getInterface(1)
            else device.getInterface(0)
        }.getOrNull() ?: return null
        return (0 until iface.endpointCount)
            .map { iface.getEndpoint(it) }
            .firstOrNull {
                it.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK &&
                it.direction == android.hardware.usb.UsbConstants.USB_DIR_IN
            }.also {
                if (it != null) Timber.d("[MTK_BROM] bulk_in addr=${it.address} " +
                    "maxPkt=${it.maxPacketSize} sessionId=$sessionId")
            }
    }

    data class BromChipInfo(val hwCode: Int, val hwSub: Int, val chipName: String)
    data class TargetConfig(val secureBoot: Boolean, val slaRequired: Boolean, val daaRequired: Boolean)

    private fun loadDa(hwCode: Int, context: Context): ByteArray? {
        // Try chip-specific first
        val specific = when (hwCode) {
            0x6765 -> "da/mt6765_da.bin"
            0x6769 -> "da/mt6769_da.bin"
            0x6768 -> "da/mt6768_da.bin"
            0x6785 -> "da/mt6785_da.bin"
            0x6762 -> "da/mt6762_da.bin"
            0x6761 -> "da/mt6761_da.bin"
            0x6580 -> "da/mt6580_da.bin"
            else   -> null
        }
        specific?.let { path ->
            runCatching { return context.assets.open(path).readBytes() }
        }

        // Fallback: MTK_DA_V5.bin (covers all classic BROM chips)
        runCatching { return context.assets.open("da/MTK_DA_V5.bin").readBytes() }

        Timber.e("[MTK_BROM] NO DA found!\n" +
                 "Add mtkclient/Loader/MTK_DA_V5.bin to assets/da/")
        return null
    }
}
