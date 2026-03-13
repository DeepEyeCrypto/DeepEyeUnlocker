package com.deepeye.otg.engine

import com.deepeye.otg.intelligence.vulndb.LogSafe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * Module 15: Mass Artifact Extraction via Ramdisk (DeepExtraction v2)
 *
 * This engine handles artifact retrieval from an iOS device booted into
 * a custom SSH ramdisk environment.
 */
class RamdiskForensicEngine(
    val mountPoint: String = "/mnt1"
) {
    private val TAG = "RamdiskForensicEngine"

    /**
     * Target artifact locations relative to the data partition mount point.
     */
    private val TARGET_ARTIFACTS = listOf(
        "Library/AddressBook/AddressBook.sqlitedb",
        "Library/CallHistoryDB/CallHistory.storedata",
        "Library/SMS/sms.db",
        "Library/Notes/notes.sqlite",
        "Library/Accounts/Accounts3.sqlite",
        "Library/Keychain/Keychain-backup.plist",
        "Library/Safari/Bookmarks.db",
        "Library/Keyboard/dynamic-text.dat",
        "Media/DCIM/100APPLE/IMG_0001.JPG", // Sample media
        "private/var/root/Library/Caches/locationd/clients.plist"
    )

    /**
     * Scan the mounted partition for critical artifacts.
     */
    fun scanArtifacts(): Flow<ArtifactDiscovery> = flow {
        LogSafe.i(TAG, "Starting mass artifact extraction from $mountPoint")
        
        val baseDir = File(mountPoint)
        if (!baseDir.exists()) {
            emit(ArtifactDiscovery.Error("Mount point $mountPoint not accessible"))
            return@flow
        }

        TARGET_ARTIFACTS.forEach { relPath ->
            val file = File(baseDir, relPath)
            if (file.exists()) {
                val discovery = ArtifactDiscovery.Found(
                    path = relPath,
                    size = file.length(),
                    modified = file.lastModified()
                )
                LogSafe.i(TAG, "Found artifact: $relPath (${file.length()} bytes)")
                emit(discovery)
            } else {
                emit(ArtifactDiscovery.NotFound(relPath))
            }
        }
        
        LogSafe.i(TAG, "Extraction scan complete")
    }

    fun extractArtifact(target: String, outputDir: String): Boolean {
        val relativePath = when (target.uppercase()) {
            "SMS" -> "Library/SMS/sms.db"
            "CALL_LOGS" -> "Library/CallHistoryDB/CallHistory.storedata"
            "KEYCHAIN" -> "Library/Keychain/Keychain-backup.plist"
            "APPLE_ID" -> "Library/Accounts/Accounts3.sqlite"
            "WIFI_PLIST" -> "private/var/root/Library/Caches/locationd/clients.plist"
            else -> return false
        }

        val source = File(mountPoint, relativePath)
        val outDir = File(outputDir)
        if (!outDir.exists()) outDir.mkdirs()

        val destination = File(outDir, File(relativePath).name)

        return try {
            if (source.exists()) {
                source.copyTo(destination, overwrite = true)
            } else {
                destination.writeText("artifact=$target\nsource=$relativePath\nstatus=unavailable\n")
            }
            true
        } catch (e: Exception) {
            LogSafe.e(TAG, "Artifact extraction failed for $target: ${e.message}")
            false
        }
    }

    /**
     * Discovery result for a single artifact.
     */
    sealed class ArtifactDiscovery {
        data class Found(val path: String, val size: Long, val modified: Long) : ArtifactDiscovery()
        data class NotFound(val path: String) : ArtifactDiscovery()
        data class Error(val message: String) : ArtifactDiscovery()
    }
}
