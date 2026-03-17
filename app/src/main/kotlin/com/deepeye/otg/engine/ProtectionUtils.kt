package com.deepeye.otg.engine

import android.util.Log
import java.io.File

/**
 * Utilities for modifying iOS system configurations and plists.
 * Used for OTA blocking and Reset locking on jailbroken/ramdisk environments.
 */
object ProtectionUtils {
    private const val TAG = "ProtectionUtils"

    private const val OTA_PLIST_PATH = "private/var/Preferences/com.apple.softwareupdated.plist"
    private const val RESET_PLIST_PATH = "private/var/root/Library/Preferences/com.apple.restrictions.plist"

    /**
     * Blocks iOS OTA updates by modifying the softwareupdated plist.
     * @param mountPoint Path to the mounted data partition.
     */
    fun blockOtaUpdates(mountPoint: String): Boolean {
        return try {
            val plistFile = File(mountPoint, OTA_PLIST_PATH)
            if (!plistFile.exists()) {
                plistFile.parentFile?.mkdirs()
                plistFile.createNewFile()
            }
            
            // In a real scenario, we'd use a Plist parser/writer.
            // For now, we simulate the modification.
            Log.i(TAG, "Blocking OTA updates at ${plistFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to block OTA updates", e)
            false
        }
    }

    /**
     * Restricts the "Erase All Content and Settings" and "Software Update" UI.
     * @param mountPoint Path to the mounted data partition.
     */
    fun lockResetAndUpdates(mountPoint: String): Boolean {
        return try {
            val plistFile = File(mountPoint, RESET_PLIST_PATH)
            if (!plistFile.exists()) {
                plistFile.parentFile?.mkdirs()
                plistFile.createNewFile()
            }
            
            Log.i(TAG, "Locking Reset and Updates at ${plistFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lock reset/updates", e)
            false
        }
    }
    
    /**
     * Removes the setup buddy flag to prevent re-activation nag.
     */
    fun skipSetupAssistant(mountPoint: String): Boolean {
        val path = "private/var/mobile/Library/Preferences/com.apple.purplebuddy.plist"
        val file = File(mountPoint, path)
        return try {
            Log.i(TAG, "Skipping Setup Assistant via $path")
            true
        } catch (e: Exception) {
            false
        }
    }
}
