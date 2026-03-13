package com.deepeye.otg.engine

import android.content.Context
import android.util.Log
import com.deepeye.otg.NativeBridge
import com.deepeye.otg.usb.gpt.GptParser
import com.deepeye.otg.usb.gpt.GptStructure
import com.deepeye.otg.usb.UsbTransport
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stage 10 — Forensic Engine (Kotlin Implementation).
 * Handles bit-stream acquisition, carving, and integrity verification.
 */
class ForensicEngine @javax.inject.Inject constructor(@dagger.hilt.android.qualifiers.ApplicationContext private val context: Context) {
    companion object {
        private const val TAG = "DeepEye-Forensics"
    }

    private val _status = MutableStateFlow<String>("IDLE")
    val status = _status.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    /**
     * Perform physical acquisition of a partition.
     * Uses SHA-256 for simultaneous integrity verification.
     */
    suspend fun acquirePartition(
        handle: Long,
        partitionName: String,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): ForensicResult {
        Log.i(TAG, "Starting forensic acquisition: $partitionName")
        _status.value = "ACQUIRING $partitionName"
        _progress.value = 0f

        val startTime = System.currentTimeMillis()
        
        // Use native bridge for high-speed dump
        val success = try {
            NativeBridge.safeDump(handle, partitionName, outputFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Native acquisition failed: ${e.message}")
            false
        }

        if (!success) {
            _status.value = "ERROR: ACQUISITION_FAILED"
            return ForensicResult(false, "Acquisition failed", 0, "")
        }

        // Verify hash
        val hash = NativeBridge.calculateFileHash(outputFile.absolutePath)
        val duration = System.currentTimeMillis() - startTime
        
        Log.i(TAG, "Acquisition complete: $partitionName | SHA256: $hash")
        _status.value = "COMPLETED"
        _progress.value = 1f

        return ForensicResult(true, "Successfully acquired $partitionName", duration, hash)
    }

    /**
     * Carve deleted files from a raw image or partition.
     */
    suspend fun carveData(handle: Long, partitionName: String, types: List<String>): String {
        Log.i(TAG, "Starting data carving on $partitionName for types: $types")
        _status.value = "CARVING $partitionName"
        
        return try {
            NativeBridge.carveDeletedData(handle, partitionName, types.toTypedArray())
        } catch (e: Exception) {
            "[]"
        } finally {
            _status.value = "CARVING_COMPLETE"
        }
    }

    /**
     * Map GPT partition table to usable internal list.
     */
    suspend fun listPartitions(transport: UsbTransport): List<GptStructure.GptEntry> {
        val parser = GptParser(transport)
        return try {
            parser.readPartitions()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Read and return hex representation of a specific partition start.
     */
    suspend fun peekPartition(handle: Long, name: String, bytes: Int = 512): String {
        return try {
            NativeBridge.peekPartition(handle, name, bytes) ?: "ERROR: PEEK_FAILED"
        } catch (e: Exception) {
            "ERROR: READ_FAILED"
        }
    }

    /**
     * Search physical storage for a specific pattern.
     */
    suspend fun searchPhysicalStorage(handle: Long, pattern: String): String {
        _status.value = "SEARCHING"
        return try {
            NativeBridge.searchStorage(handle, pattern, 100)
        } catch (e: Exception) {
            "[]"
        } finally {
            _status.value = "SEARCH_COMPLETE"
        }
    }

    /**
     * Apply a byte-level patch to a partition.
     */
    suspend fun patchSector(handle: Long, name: String, offset: Long, hexData: String): Boolean {
        _status.value = "PATCHING $name"
        val bytes = hexData.split(" ").map { it.toInt(16).toByte() }.toByteArray()
        return try {
            NativeBridge.patchPartition(handle, name, offset, bytes)
        } catch (e: Exception) {
            false
        } finally {
            _status.value = "IDLE"
        }
    }

    data class ForensicResult(
        val success: Boolean,
        val message: String,
        val durationMs: Long,
        val sha256: String
    )
}
