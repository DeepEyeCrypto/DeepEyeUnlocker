package com.deepeye.otg.engine

import android.content.Context
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * MTK Download Agent (DA) Loader for MT6789 (Helio G99 / RMX3845)
 * Loads DA from assets, sends to BROM, jumps to DA, and performs FRP erase
 */
class MtkDaLoader(
    private val connection: UsbDeviceConnection,
    private val bulkOut: UsbEndpoint,
    private val bulkIn: UsbEndpoint,
    private val context: Context
) {
    companion object {
        // MT6789 specific DA configuration
        const val MT6789_DA_ADDR = 0x201000L
        const val MT6789_DA_ARG1 = 0x0L
        const val MT6789_DA_ARG2 = 0x0L

        // MTK BROM commands
        const val CMD_SEND_DA = 0xD7.toByte()
        const val CMD_JUMP_DA = 0xD5.toByte()

        // Protocol constants
        const val ACK = 0x5A.toByte()
        const val NACK = 0xA5.toByte()
        const val TIMEOUT_MS = 10000
        const val CHUNK_SIZE = 4096

        // DA asset paths to try (in order of preference)
        val DA_ASSET_PATHS = listOf(
            "da/MTK_DA_V6.bin",  // Latest DA version
            "da/MTK_DA_V5.bin",  // Fallback DA version
            "da/mt6789_da.bin",
            "da/mtk_all_in_one_da.bin"
        )
    }

    // ── Load DA bytes from assets ──────────────────────────────
    fun loadDaBytes(): ByteArray? {  // Made public for MtkExploitEngine
        DA_ASSET_PATHS.forEach { path ->
            try {
                val bytes = context.assets.open(path).readBytes()
                Timber.d("[DA] Loaded from assets: $path (${bytes.size} bytes)")
                return bytes
            } catch (e: Exception) {
                Timber.d("[DA] Not found: $path")
            }
        }

        Timber.w("[DA] No DA file found in assets!")
        return null
    }

    // ── Send DA to device ──────────────────────────────────────
    suspend fun sendDa(): DaResult = withContext(Dispatchers.IO) {
        Timber.d("[DA] Starting DA send for MT6789 (RMX3845)")

        val daBytes = loadDaBytes()
            ?: return@withContext DaResult.Error("DA payload not found in assets")

        Timber.d("[DA] DA size: ${daBytes.size} bytes (${daBytes.size / 1024} KB)")

        try {
            // 1. Send CMD_SEND_DA header
            val cmdHeader = byteArrayOf(CMD_SEND_DA)
            val cmdSent = connection.bulkTransfer(bulkOut, cmdHeader, 1, TIMEOUT_MS)
            if (cmdSent < 0) {
                return@withContext DaResult.Error("SEND_DA command send failed")
            }

            // Wait for ACK
            val cmdAck = readByte()
            if (cmdAck != ACK) {
                return@withContext DaResult.Error(
                    "SEND_DA command rejected: 0x${cmdAck?.toString(16) ?: "null"}"
                )
            }
            Timber.d("[DA] SEND_DA command accepted")

            // 2. Send load address (4 bytes big-endian)
            val addrBytes = intToBytes(MT6789_DA_ADDR.toInt())
            connection.bulkTransfer(bulkOut, addrBytes, 4, TIMEOUT_MS)
            Timber.d("[DA] DA address: 0x${MT6789_DA_ADDR.toString(16).uppercase()}")

            // 3. Send DA size (4 bytes big-endian)
            val sizeBytes = intToBytes(daBytes.size)
            connection.bulkTransfer(bulkOut, sizeBytes, 4, TIMEOUT_MS)
            Timber.d("[DA] DA size: ${daBytes.size} bytes")

            // 4. Send signature length (0 = unsigned)
            val sigBytes = intToBytes(0)
            connection.bulkTransfer(bulkOut, sigBytes, 4, TIMEOUT_MS)

            // Wait for header ACK
            delay(100)
            val headerAck = readByte()
            if (headerAck != ACK) {
                return@withContext DaResult.Error(
                    "DA header rejected: 0x${headerAck?.toString(16) ?: "null"}"
                )
            }
            Timber.d("[DA] DA header accepted")

            // 5. Send DA payload in chunks with checksum
            var offset = 0
            var checksum = 0

            while (offset < daBytes.size) {
                val end = minOf(offset + CHUNK_SIZE, daBytes.size)
                val chunk = daBytes.copyOfRange(offset, end)

                val sent = connection.bulkTransfer(bulkOut, chunk, chunk.size, TIMEOUT_MS)
                if (sent < 0) {
                    return@withContext DaResult.Error(
                        "DA write failed at offset $offset"
                    )
                }

                // Calculate XOR checksum
                chunk.forEach { b -> checksum = checksum xor (b.toInt() and 0xFF) }
                offset = end

                val progress = (offset * 100L / daBytes.size).toInt()
                if (progress % 10 == 0) {
                    Timber.d("[DA] Progress: $progress% ($offset/${daBytes.size})")
                }
            }

            // 6. Send checksum
            val checksumByte = byteArrayOf((checksum and 0xFF).toByte())
            connection.bulkTransfer(bulkOut, checksumByte, 1, TIMEOUT_MS)

            // Wait for checksum ACK
            delay(200)
            val checksumAck = readByte()
            if (checksumAck != ACK) {
                return@withContext DaResult.Error(
                    "DA checksum mismatch: 0x${checksumAck?.toString(16) ?: "null"}"
                )
            }

            Timber.d("[DA] ✅ DA sent successfully! (${daBytes.size} bytes)")
            DaResult.Success("DA loaded — ${daBytes.size / 1024} KB")

        } catch (e: Exception) {
            Timber.e(e, "[DA] Exception during DA send")
            DaResult.Error("DA send exception: ${e.message}")
        }
    }

    // ── Jump to DA entry point ─────────────────────────────────
    suspend fun jumpToDa(): DaResult = withContext(Dispatchers.IO) {
        Timber.d("[DA] Jumping to DA at 0x${MT6789_DA_ADDR.toString(16).uppercase()}")

        try {
            // Send JUMP_DA command
            val cmdHeader = byteArrayOf(CMD_JUMP_DA)
            val cmdSent = connection.bulkTransfer(bulkOut, cmdHeader, 1, TIMEOUT_MS)
            if (cmdSent < 0) {
                return@withContext DaResult.Error("JUMP_DA command send failed")
            }

            // Wait for ACK
            val ack = readByte()
            if (ack != ACK) {
                return@withContext DaResult.Error(
                    "JUMP_DA rejected: 0x${ack?.toString(16) ?: "null"}"
                )
            }

            // Send jump address + arguments
            val jumpData = byteArrayOf(
                *intToBytes(MT6789_DA_ADDR.toInt()),
                *intToBytes(MT6789_DA_ARG1.toInt()),
                *intToBytes(MT6789_DA_ARG2.toInt())
            )
            connection.bulkTransfer(bulkOut, jumpData, jumpData.size, TIMEOUT_MS)

            // Wait for final ACK
            delay(500)
            val jumpAck = readByte()
            if (jumpAck != ACK) {
                return@withContext DaResult.Error(
                    "DA jump failed: 0x${jumpAck?.toString(16) ?: "null"}"
                )
            }

            Timber.d("[DA] ✅ DA jump successful!")
            DaResult.Success("DA executing at 0x${MT6789_DA_ADDR.toString(16).uppercase()}!")

        } catch (e: Exception) {
            Timber.e(e, "[DA] Exception during DA jump")
            DaResult.Error("DA jump exception: ${e.message}")
        }
    }

    // ── Full DA Load Sequence (Send + Jump) ────────────────────
    suspend fun loadAndJump(): DaResult = withContext(Dispatchers.IO) {
        Timber.d("[DA] Starting full DA load sequence for MT6789")

        // Step 1: Send DA
        val sendResult = sendDa()
        if (sendResult is DaResult.Error) {
            return@withContext sendResult
        }

        // Small delay before jump
        delay(500)

        // Step 2: Jump to DA
        val jumpResult = jumpToDa()
        if (jumpResult is DaResult.Error) {
            return@withContext jumpResult
        }

        DaResult.Success("✅ DA loaded and executing on MT6789!")
    }

    // ── Erase FRP Partition (after DA is running) ──────────────
    suspend fun eraseFrpPartition(): DaResult = withContext(Dispatchers.IO) {
        Timber.d("[DA] Erasing FRP partition")

        try {
            // DA ERASE command: 0xDB
            val partitionName = "frp"
            val eraseCmd = byteArrayOf(
                0xDB.toByte(),  // ERASE command
                *partitionName.toByteArray(),
                0x00.toByte()   // Null terminator
            )

            val sent = connection.bulkTransfer(bulkOut, eraseCmd, eraseCmd.size, TIMEOUT_MS)
            if (sent < 0) {
                return@withContext DaResult.Error("FRP erase command send failed")
            }

            // Wait for response (4 bytes status)
            delay(2000)  // Erase takes time
            val resp = readBytes(4)
            if (resp == null || resp.size < 2) {
                return@withContext DaResult.Error("No response to FRP erase")
            }

            val status = ((resp[0].toInt() and 0xFF) shl 8) or (resp[1].toInt() and 0xFF)

            if (status == 0) {
                Timber.d("[DA] ✅ FRP partition erased successfully!")
                DaResult.Success("FRP partition erased — Google lock removed!")
            } else {
                Timber.w("[DA] FRP erase failed with status: 0x${status.toString(16)}")
                DaResult.Error("FRP erase failed: status=0x${status.toString(16)}")
            }

        } catch (e: Exception) {
            Timber.e(e, "[DA] Exception during FRP erase")
            DaResult.Error("FRP erase exception: ${e.message}")
        }
    }

    // ── Full Sequence: DA Load → FRP Erase ─────────────────────
    suspend fun fullFrpBypass(): DaResult = withContext(Dispatchers.IO) {
        Timber.d("[DA] Starting full FRP bypass sequence")

        // Step 1: Load and jump to DA
        val loadResult = loadAndJump()
        if (loadResult is DaResult.Error) {
            return@withContext loadResult
        }

        // Step 2: Wait for DA to initialize
        delay(1000)

        // Step 3: Erase FRP partition
        val eraseResult = eraseFrpPartition()
        if (eraseResult is DaResult.Error) {
            return@withContext eraseResult
        }

        DaResult.Success("✅ FRP bypass complete! DA loaded + FRP erased!")
    }

    // ── Helper Functions ───────────────────────────────────────

    private fun intToBytes(value: Int): ByteArray = byteArrayOf(
        (value shr 24).toByte(),
        (value shr 16).toByte(),
        (value shr 8).toByte(),
        value.toByte()
    )

    private fun readByte(): Byte? {
        val buf = ByteArray(1)
        val read = connection.bulkTransfer(bulkIn, buf, 1, TIMEOUT_MS)
        return if (read > 0) buf[0] else null
    }

    private fun readBytes(size: Int): ByteArray? {
        val buf = ByteArray(size)
        val read = connection.bulkTransfer(bulkIn, buf, size, TIMEOUT_MS)
        return if (read > 0) buf else null
    }
}

// Result types
sealed class DaResult {
    data class Success(val message: String) : DaResult()
    data class Error(val message: String) : DaResult()
}
