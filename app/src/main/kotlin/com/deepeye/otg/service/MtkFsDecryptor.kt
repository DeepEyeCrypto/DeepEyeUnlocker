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
     * Handles Stage 300.1: FBE + SD Card Adoptable Storage (Double-Layer).
     */
    suspend fun decryptUserdata(handle: Long): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Initializing MTK Userdata Decryption (Stage 300.1)...")

        // 1. Extract specialized FBE Master Key from TEE/RPMB
        val fbeMasterKey = NativeBridge.readRpmb(handle)
        if (fbeMasterKey.isEmpty()) {
            Log.e(TAG, "Failed to extract FBE master key from RPMB")
            return@withContext false
        }

        // 2. Initialize primary userdata decryptor via NDK
        val primarySuccess = NativeBridge.mtkDecryptFs(handle, "userdata", fbeMasterKey)
        if (!primarySuccess) {
            Log.e(TAG, "Primary userdata decryption layer failed.")
            return@withContext false
        }

        // 3. Stage 300.1: Check for Adoptable Storage (SD Card)
        // [RESEARCHER] Dimensity chips often map the SD card as a separate crypto-volume.
        val hasAdoptableStorage = NativeBridge.fsCheckVolume(handle, "sdcard_adoptable")
        if (hasAdoptableStorage) {
            Log.i(TAG, "Adoptable Storage detected. Attempting Double-Layer unwrap...")
            val sdKey = NativeBridge.extractAdoptableKey(handle, "userdata") // Key is often stored inside primary FS
            if (sdKey.isNotEmpty()) {
                val sdSuccess = NativeBridge.mtkDecryptFs(handle, "sdcard_adoptable", sdKey)
                if (sdSuccess) Log.i(TAG, "Double-Layer Decryption ACTIVE (Internal + SD).")
            }
        }

        Log.i(TAG, "MTK Decryption layer ACTIVE. Userdata is now accessible.")
        true
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
