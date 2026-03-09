package com.deepeye.otg.service

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Manages extraction and provisioning of binary assets (DA, Firehose, FDL)
 * required for low-level hardware communication.
 */
object BinaryAssetManager {
    private const val TAG = "DeepEye-Assets"

    /**
     * Extracts an asset from the APK to the application's cache directory.
     * Returns the File handle for use in NativeBridge paths.
     */
    fun extractAsset(context: Context, assetName: String): File? {
        val cacheFile = File(context.cacheDir, assetName)
        
        // Always re-extract in DEV for safety, or check CRC in PROD
        try {
            context.assets.open(assetName).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "[ASSET] Extracted: $assetName -> ${cacheFile.absolutePath}")
            return cacheFile
        } catch (e: Exception) {
            Log.w(TAG, "[ASSET] Missing or failed to extract: $assetName (${e.message})")
            return null
        }
    }

    /**
     * Reads an asset directly into memory. Used for MTK DA injection.
     */
    fun readAssetBytes(context: Context, assetName: String): ByteArray? {
        return try {
            context.assets.open(assetName).use { it.readBytes() }
        } catch (e: Exception) {
            Log.w(TAG, "[ASSET] Read error: $assetName (${e.message})")
            null
        }
    }

    /**
     * Finds a compatible Firehose programmer in the assets.
     * Logic: look for "prog_firehose_{vid}_{pid}.elf" or generic "prog_firehose.elf".
     */
    fun getFirehoseProgrammer(context: Context, vid: Int, pid: Int): File? {
        val specific = "prog_firehose_${"%04X".format(vid)}_${"%04X".format(pid)}.elf".lowercase()
        return extractAsset(context, specific) ?: extractAsset(context, "prog_firehose.elf")
    }

    /**
     * Returns the primary Download Agent for MTK.
     */
    fun getMtkDa(context: Context): ByteArray? {
        return readAssetBytes(context, "MTK_AllInOne_DA.bin")
            ?: readAssetBytes(context, "da_agent.bin")
    }
}
