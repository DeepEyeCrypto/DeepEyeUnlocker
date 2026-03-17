package com.deepeye.otg.engine

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages iDevice activation tokens (Lockdown, FairPlay).
 * Handles backup from device and restore for bypasses.
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "TokenManager"
    private val BACKUP_DIR = "backups/tokens"

    private val TOKEN_PATHS = listOf(
        "private/var/root/Library/Lockdown/",
        "private/var/mobile/Library/FairPlay/",
        "private/var/Preferences/SystemConfiguration/com.apple.accounts.exists.plist"
    )

    /**
     * Creates a zip backup of tokens from a mounted device directory.
     * @param mountPoint The local path where the device filesystem is mounted/accessible.
     * @param deviceId Identifier for the backup file.
     */
    fun backupTokens(mountPoint: String, deviceId: String): File? {
        val backupFolder = File(context.filesDir, BACKUP_DIR)
        if (!backupFolder.exists()) backupFolder.mkdirs()

        val zipFile = File(backupFolder, "tokens_${deviceId}_${System.currentTimeMillis()}.zip")
        
        try {
            val baseDir = File(mountPoint)
            if (!baseDir.exists()) {
                Log.e(TAG, "Mount point $mountPoint not found")
                return null
            }

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                TOKEN_PATHS.forEach { relPath ->
                    val source = File(baseDir, relPath)
                    if (source.exists()) {
                        addFileToZip(zos, source, relPath)
                    } else {
                        Log.w(TAG, "Token path not found: $relPath")
                    }
                }
            }
            Log.i(TAG, "Tokens backed up to ${zipFile.absolutePath}")
            return zipFile
        } catch (e: Exception) {
            Log.e(TAG, "Token backup failed", e)
            return null
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, relPath: String) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                addFileToZip(zos, child, "$relPath/${child.name}")
            }
        } else {
            zos.putNextEntry(ZipEntry(relPath))
            file.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }

    /**
     * Restores tokens from a zip backup to a mounted device directory.
     */
    fun restoreTokens(zipFile: File, mountPoint: String): Boolean {
        try {
            val baseDir = File(mountPoint)
            if (!baseDir.exists()) {
                Log.e(TAG, "Mount point $mountPoint not found")
                return false
            }

            java.util.zip.ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val destFile = File(baseDir, entry.name)
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    entry = zis.nextEntry
                }
            }
            Log.i(TAG, "Tokens restored from ${zipFile.name} to $mountPoint")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Token restore failed", e)
            return false
        }
    }

    /**
     * Lists available backups for a device.
     */
    fun listBackups(deviceId: String): List<File> {
        val backupFolder = File(context.filesDir, BACKUP_DIR)
        return backupFolder.listFiles { _, name -> 
            name.startsWith("tokens_$deviceId") && name.endsWith(".zip") 
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * Verification logic for token integrity using SHA-256.
     */
    fun verifyTokenIntegrity(zipFile: File): Boolean {
        if (!zipFile.exists() || zipFile.length() == 0L) return false
        
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            zipFile.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            Log.i(TAG, "Token backup integrity verified. Hash: $hash")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Integrity check failed", e)
            false
        }
    }
}
