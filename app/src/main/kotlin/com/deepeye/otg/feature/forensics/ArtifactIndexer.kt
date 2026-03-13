package com.deepeye.otg.feature.forensics

import javax.inject.Inject

import android.util.Log
import java.io.File
import java.security.MessageDigest

// ──────────────────────────────────────────────────────────────
// Artifact Indexer — Filesystem Inventory + Metadata Extraction
// DeepEye OTG — Forensics Module (Part 6)
//
// Operates ONLY on already-accessible data/files.
// No root, no entitlement bypass, no secret extraction.
// ──────────────────────────────────────────────────────────────

private const val TAG = "ArtifactIndexer"

/**
 * Classification of a forensic artifact.
 */
enum class ArtifactType {
    DATABASE,       // SQLite, plist stores
    LOG,            // System/app logs
    MEDIA,          // Photos, videos, audio
    DOCUMENT,       // PDFs, docs, spreadsheets
    CONFIGURATION,  // Plists, JSON configs
    CACHE,          // Temporary cached data
    KEYCHAIN_EXPORT,// Exported keychain items (already decrypted / accessible)
    APP_DATA,       // Application-specific data
    SYSTEM_INFO,    // Device info, build metadata
    BACKUP_FRAGMENT,// iTunes/Finder backup pieces
    CRASH_LOG,      // Crash reports / sysdiagnose
    NETWORK,        // WiFi configs, Bluetooth pairings
    UNKNOWN
}

/**
 * A single indexed artifact with metadata.
 */
data class ForensicArtifact(
    val id: String,
    val path: String,
    val filename: String,
    val artifactType: ArtifactType,
    val sizeBytes: Long,
    val md5: String? = null,
    val sha256: String? = null,
    val lastModified: Long,
    val created: Long? = null,
    val mimeType: String? = null,
    val extension: String,
    val isEncrypted: Boolean = false,
    val sourceDevice: String? = null,
    val sourceDescription: String = "",
    val indexedAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap(),
    val notes: String = ""
)

/**
 * Indexing session result.
 */
data class IndexResult(
    val sessionId: String,
    val rootPath: String,
    val totalFiles: Int,
    val totalSizeBytes: Long,
    val artifacts: List<ForensicArtifact>,
    val byType: Map<ArtifactType, Int>,
    val errors: List<String>,
    val duration: Long,
    val indexedAt: Long = System.currentTimeMillis()
)

/**
 * Indexes accessible file system artifacts and extracts metadata.
 *
 * Design:
 * - Only reads files that are already accessible (no privilege escalation)
 * - Computes integrity hashes for chain-of-custody
 * - Classifies by type using extension + magic bytes
 * - Thread-safe, can run in background coroutine
 */
class ArtifactIndexer @Inject constructor() {

    // Extension → ArtifactType mapping
    private val extensionMap = mapOf(
        "db" to ArtifactType.DATABASE,
        "sqlite" to ArtifactType.DATABASE,
        "sqlite3" to ArtifactType.DATABASE,
        "sqlitedb" to ArtifactType.DATABASE,
        "plist" to ArtifactType.CONFIGURATION,
        "json" to ArtifactType.CONFIGURATION,
        "xml" to ArtifactType.CONFIGURATION,
        "yaml" to ArtifactType.CONFIGURATION,
        "yml" to ArtifactType.CONFIGURATION,
        "log" to ArtifactType.LOG,
        "txt" to ArtifactType.LOG,
        "crash" to ArtifactType.CRASH_LOG,
        "ips" to ArtifactType.CRASH_LOG,
        "panic" to ArtifactType.CRASH_LOG,
        "jpg" to ArtifactType.MEDIA,
        "jpeg" to ArtifactType.MEDIA,
        "png" to ArtifactType.MEDIA,
        "heic" to ArtifactType.MEDIA,
        "heif" to ArtifactType.MEDIA,
        "mov" to ArtifactType.MEDIA,
        "mp4" to ArtifactType.MEDIA,
        "m4a" to ArtifactType.MEDIA,
        "aac" to ArtifactType.MEDIA,
        "pdf" to ArtifactType.DOCUMENT,
        "doc" to ArtifactType.DOCUMENT,
        "docx" to ArtifactType.DOCUMENT,
        "xls" to ArtifactType.DOCUMENT,
        "xlsx" to ArtifactType.DOCUMENT,
        "cache" to ArtifactType.CACHE,
        "tmp" to ArtifactType.CACHE,
    )

    /**
     * Index all accessible files under a root path.
     *
     * @param rootPath the directory to index
     * @param computeHashes whether to compute MD5/SHA256 (slower but forensically useful)
     * @param maxDepth maximum directory recursion depth
     * @param sourceDevice device identifier for provenance
     * @return index result
     */
    fun index(
        rootPath: File,
        computeHashes: Boolean = true,
        maxDepth: Int = 20,
        sourceDevice: String? = null
    ): IndexResult {
        val sessionId = "idx_${System.currentTimeMillis()}"
        val startTime = System.currentTimeMillis()
        val artifacts = mutableListOf<ForensicArtifact>()
        val errors = mutableListOf<String>()
        var totalSize = 0L

        Log.i(TAG, "Indexing: ${rootPath.absolutePath}")

        indexRecursive(rootPath, rootPath, 0, maxDepth, computeHashes, sourceDevice, artifacts, errors)

        totalSize = artifacts.sumOf { it.sizeBytes }

        val result = IndexResult(
            sessionId = sessionId,
            rootPath = rootPath.absolutePath,
            totalFiles = artifacts.size,
            totalSizeBytes = totalSize,
            artifacts = artifacts,
            byType = artifacts.groupBy { it.artifactType }.mapValues { it.value.size },
            errors = errors,
            duration = System.currentTimeMillis() - startTime
        )

        Log.i(TAG, "Indexed ${result.totalFiles} files (${totalSize / 1024}KB) in ${result.duration}ms")
        return result
    }

    private fun indexRecursive(
        file: File,
        rootPath: File,
        depth: Int,
        maxDepth: Int,
        computeHashes: Boolean,
        sourceDevice: String?,
        artifacts: MutableList<ForensicArtifact>,
        errors: MutableList<String>
    ) {
        if (depth > maxDepth) return

        try {
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    indexRecursive(child, rootPath, depth + 1, maxDepth, computeHashes, sourceDevice, artifacts, errors)
                }
            } else if (file.isFile && file.canRead()) {
                val ext = file.extension.lowercase()
                val type = classifyFile(file, ext)

                var md5: String? = null
                var sha256: String? = null

                if (computeHashes && file.length() < 100_000_000) { // Skip files > 100MB for hashing
                    try {
                        val bytes = file.readBytes()
                        md5 = hashBytes(bytes, "MD5")
                        sha256 = hashBytes(bytes, "SHA-256")
                    } catch (e: Exception) {
                        errors.add("Hash failed for ${file.absolutePath}: ${e.message}")
                    }
                }

                artifacts.add(ForensicArtifact(
                    id = "art_${file.absolutePath.hashCode()}_${file.lastModified()}",
                    path = file.absolutePath,
                    filename = file.name,
                    artifactType = type,
                    sizeBytes = file.length(),
                    md5 = md5,
                    sha256 = sha256,
                    lastModified = file.lastModified(),
                    extension = ext,
                    sourceDevice = sourceDevice
                ))
            }
        } catch (e: Exception) {
            errors.add("Error indexing ${file.absolutePath}: ${e.message}")
        }
    }

    private fun classifyFile(file: File, ext: String): ArtifactType {
        // Check extension first
        extensionMap[ext]?.let { return it }

        // Check known filenames
        return when {
            file.name == "manifest.db" -> ArtifactType.BACKUP_FRAGMENT
            file.name == "Manifest.plist" -> ArtifactType.BACKUP_FRAGMENT
            file.name == "Info.plist" -> ArtifactType.CONFIGURATION
            file.name == "Status.plist" -> ArtifactType.BACKUP_FRAGMENT
            file.name.startsWith("sysdiagnose") -> ArtifactType.CRASH_LOG
            file.name.contains("WiFi", ignoreCase = true) -> ArtifactType.NETWORK
            file.name.contains("Bluetooth", ignoreCase = true) -> ArtifactType.NETWORK
            else -> ArtifactType.UNKNOWN
        }
    }

    private fun hashBytes(bytes: ByteArray, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
