package com.deepeye.otg.service

import android.util.Log
import com.deepeye.otg.NativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Stage 300.1 — MTK Real-time File System Decryptor.
 * Orchestrates decryption of userdata partitions.
 */
object MtkFsDecryptor {
    private const val TAG = "DeepEye-Decrypt"

    /**
     * Attempts to initialize the decryption layer for a connected MTK device.
     */
    suspend fun decryptUserdata(handle: Long): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Initializing MTK Userdata Decryption...")

        // 1. Extract specialized keyblob from TEE/RPMB
        val keyBlob = NativeBridge.readRpmb(handle)
        if (keyBlob.isEmpty()) {
            Log.e(TAG, "Failed to extract key material from RPMB")
            return@withContext false
        }

        // 2. Initialize real-time decryptor via NDK
        val success = NativeBridge.mtkDecryptFs(handle, "userdata", keyBlob)
        
        if (success) {
            Log.i(TAG, "MTK Decryption layer ACTIVE. Userdata is now accessible.")
        } else {
            Log.e(TAG, "NDK Decryption layer failed to initialize.")
        }
        
        success
    }

    /**
     * Lists files in a decrypted path on the MTK userdata partition.
     */
    suspend fun listFolder(handle: Long, path: String): String = withContext(Dispatchers.IO) {
        NativeBridge.fsListDirectory(handle, "userdata", path)
    }

    /**
     * Reads a decrypted file from the partition.
     */
    suspend fun readFile(handle: Long, path: String): ByteArray = withContext(Dispatchers.IO) {
        NativeBridge.fsReadFile(handle, "userdata", path)
    }
}
