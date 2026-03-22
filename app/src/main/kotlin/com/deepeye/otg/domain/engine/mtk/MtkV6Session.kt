package com.deepeye.otg.domain.engine.mtk

import android.content.Context
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * MtkV6Session implements the MTK V6 protocol for modern Dimensity chipsets.
 * Priority: Protocol correctness, zero crashes, and observability.
 */
class MtkV6Session(
    private val context: Context,
    private val conn: UsbDeviceConnection,
    private val epIn: UsbEndpoint,
    private val epOut: UsbEndpoint,
) {
    private var sessionKey: ByteArray? = null

    /**
     * Completes the V6 key exchange (key:02)
     */
    fun performKeyExchange(hwCode: Int, sessionId: String): Result<Unit> {
        Timber.d("[MTK_V6] initiating key_exchange key:02 sessionId=$sessionId")
        
        try {
            // Read key descriptor (should be 0x02)
            val descriptor = ByteArray(1)
            val read = conn.bulkTransfer(epIn, descriptor, 1, 5000)
            if (read < 1 || descriptor[0].toInt() != 0x02) {
                return Result.failure(V6Error.KeyExchangeFailed("Invalid key descriptor: ${descriptor.getOrNull(0)}"))
            }

            // Generate 16B challenge
            val challenge = ByteArray(16)
            SecureRandom().nextBytes(challenge)
            Timber.d("[MTK_V6] key_exchange challenge=${challenge.toHex()} sessionId=$sessionId")

            // Send challenge
            val sent = conn.bulkTransfer(epOut, challenge, 16, 5000)
            if (sent < 16) return Result.failure(V6Error.KeyExchangeFailed("Failed to send challenge"))

            // Receive response
            val response = ByteArray(16)
            val respRead = conn.bulkTransfer(epIn, response, 16, 5000)
            if (respRead < 16) return Result.failure(V6Error.KeyExchangeFailed("Failed to receive response"))

            // Derive session key: SHA256(challenge + hw_code_bytes)
            val hwCodeBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(hwCode).array()
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(challenge)
            digest.update(hwCodeBytes)
            sessionKey = digest.digest()
            
            Timber.d("[MTK_V6] key_exchange complete sessionKey derived sessionId=$sessionId")
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[MTK_V6] key_exchange exception sessionId=$sessionId")
            return Result.failure(V6Error.KeyExchangeFailed(e.message ?: "Unknown error"))
        }
    }

    /**
     * Selects and loads DA binary based on hwCode
     */
    fun selectDa(hwCode: Int, sessionId: String): Result<ByteArray> {
        val daPath = when (hwCode) {
            0x1209 -> "da/mt6835t_da.bin"
            else -> return Result.failure(V6Error.DaNotFound(hwCode))
        }

        return try {
            val daBytes = context.assets.open(daPath).use { it.readBytes() }
            Timber.d("[MTK_V6] da_select hw_code=0x${Integer.toHexString(hwCode)} size=${daBytes.size} sessionId=$sessionId")
            Result.success(daBytes)
        } catch (e: Exception) {
            Timber.e(e, "[MTK_V6] da_select failed for hw_code=0x${Integer.toHexString(hwCode)} sessionId=$sessionId")
            Result.failure(V6Error.DaNotFound(hwCode))
        }
    }

    /**
     * Uploads DA binary with progress tracking
     */
    fun uploadDa(
        daBytes: ByteArray,
        onProgress: (Int) -> Unit,
        sessionId: String,
    ): Result<Unit> {
        val totalSize = daBytes.size
        var bytesSent = 0
        val chunkSize = 4096

        Timber.d("[MTK_V6] da_upload starting size=$totalSize sessionId=$sessionId")

        while (bytesSent < totalSize) {
            val length = minOf(chunkSize, totalSize - bytesSent)
            val chunk = daBytes.copyOfRange(bytesSent, bytesSent + length)
            
            val transferred = conn.bulkTransfer(epOut, chunk, length, 5000)
            if (transferred < 0) {
                return Result.failure(V6Error.DaUploadFailed(bytesSent))
            }
            
            bytesSent += transferred
            val pct = (bytesSent.toLong() * 100 / totalSize).toInt()
            onProgress(pct)
            Timber.d("[MTK_V6] da_upload pct=$pct sent=$bytesSent sessionId=$sessionId")
        }

        // ZLP if totalSent is multiple of maxPacketSize
        if (bytesSent % epOut.maxPacketSize == 0) {
            conn.bulkTransfer(epOut, ByteArray(0), 0, 2000)
            Timber.d("[MTK_V6] da_upload sent ZLP sessionId=$sessionId")
        }

        Timber.d("[MTK_V6] da_upload complete sessionId=$sessionId")
        return Result.success(Unit)
    }

    /**
     * Reads partition list from device after DA jump
     */
    fun readPartitionList(sessionId: String): Result<List<V6Partition>> {
        // DA cmd: 0x70 READ_PARTITION_TABLE
        Timber.d("[MTK_V6] read_partition_list sessionId=$sessionId")
        // Implementation of 0x70 command and TLV parsing...
        return Result.success(emptyList()) // Stub for now
    }

    /**
     * Erases a partition by name
     */
    fun erasePartition(name: String, sessionId: String): Result<Unit> {
        // DA cmd: 0x71 ERASE_PARTITION
        Timber.d("[MTK_V6] erase_partition name=$name sessionId=$sessionId")
        // Implementation of 0x71 command...
        return Result.success(Unit) // Stub
    }

    /**
     * Writes data to a partition
     */
    fun writePartition(
        name: String,
        data: ByteArray,
        onProgress: (Int) -> Unit,
        sessionId: String
    ): Result<Unit> {
        // DA cmd: 0x73 WRITE_PARTITION
        Timber.d("[MTK_V6] write_partition name=$name size=${data.size} sessionId=$sessionId")
        // Implementation of 0x73 command...
        return Result.success(Unit) // Stub
    }

    /**
     * FRP Erase: writes zeros to "frp" partition
     */
    fun eraseFrp(sessionId: String): Result<Unit> {
        Timber.d("[MTK_V6] frp_erase starting sessionId=$sessionId")
        val partitions = readPartitionList(sessionId).getOrElse { return Result.failure(it) }
        val frp = partitions.firstOrNull { it.name == "frp" } 
            ?: return Result.failure(V6Error.PartitionNotFound("frp"))
        
        val zeros = ByteArray(frp.size.toInt())
        return writePartition("frp", zeros, {}, sessionId)
    }
}

data class V6Partition(
    val name: String,
    val offset: Long,
    val size: Long
)
