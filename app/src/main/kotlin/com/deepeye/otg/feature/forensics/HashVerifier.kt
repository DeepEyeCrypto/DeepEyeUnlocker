package com.deepeye.otg.feature.forensics

import javax.inject.Inject

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

// ──────────────────────────────────────────────────────────────
// Hash Verifier — Integrity Verification for Acquired Files
// DeepEye OTG — Forensics Module (Part 6)
// ──────────────────────────────────────────────────────────────

private const val TAG = "HashVerifier"

/**
 * Hash verification result.
 */
data class VerificationResult(
    val filePath: String,
    val algorithm: String,
    val computedHash: String,
    val expectedHash: String?,
    val matches: Boolean?,     // null if no expected hash provided
    val fileSize: Long,
    val verifiedAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 0
)

/**
 * Batch verification result.
 */
data class BatchVerificationResult(
    val totalFiles: Int,
    val verified: Int,
    val mismatched: Int,
    val errors: Int,
    val results: List<VerificationResult>,
    val errorMessages: List<String>,
    val durationMs: Long
)

/**
 * Forensic hash verifier for chain-of-custody integrity.
 *
 * Supports:
 * - MD5, SHA-1, SHA-256, SHA-512
 * - Streaming hash for large files (no full memory load)
 * - Batch verification against manifest
 * - Chain-of-custody metadata generation
 */
class HashVerifier @Inject constructor() {

    companion object {
        const val MD5 = "MD5"
        const val SHA1 = "SHA-1"
        const val SHA256 = "SHA-256"
        const val SHA512 = "SHA-512"

        val SUPPORTED_ALGORITHMS = listOf(MD5, SHA1, SHA256, SHA512)
    }

    /**
     * Compute hash of a file using streaming (constant memory).
     *
     * @param file the file to hash
     * @param algorithm hash algorithm (MD5, SHA-256, etc.)
     * @return hex-encoded hash string
     */
    fun computeHash(file: File, algorithm: String = SHA256): String {
        val digest = MessageDigest.getInstance(algorithm)
        val buffer = ByteArray(8192)

        FileInputStream(file).use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Compute multiple hashes in a single pass.
     */
    fun computeMultiHash(file: File, algorithms: List<String> = listOf(MD5, SHA256)): Map<String, String> {
        val digests = algorithms.associateWith { MessageDigest.getInstance(it) }
        val buffer = ByteArray(8192)

        FileInputStream(file).use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digests.values.forEach { it.update(buffer, 0, bytesRead) }
            }
        }

        return digests.mapValues { (_, digest) ->
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * Verify a file against an expected hash.
     */
    fun verify(
        file: File,
        expectedHash: String,
        algorithm: String = SHA256
    ): VerificationResult {
        val start = System.currentTimeMillis()

        val computed = computeHash(file, algorithm)
        val matches = computed.equals(expectedHash, ignoreCase = true)

        val result = VerificationResult(
            filePath = file.absolutePath,
            algorithm = algorithm,
            computedHash = computed,
            expectedHash = expectedHash,
            matches = matches,
            fileSize = file.length(),
            durationMs = System.currentTimeMillis() - start
        )

        Log.i(TAG, "Verified ${file.name}: ${if (matches) "MATCH" else "MISMATCH"}")
        return result
    }

    /**
     * Batch verify files against a hash manifest.
     *
     * @param manifest map of file path → expected hash
     * @param algorithm hash algorithm to use
     */
    fun batchVerify(
        manifest: Map<String, String>,
        algorithm: String = SHA256
    ): BatchVerificationResult {
        val start = System.currentTimeMillis()
        val results = mutableListOf<VerificationResult>()
        val errors = mutableListOf<String>()
        var mismatched = 0

        for ((path, expectedHash) in manifest) {
            val file = File(path)
            if (!file.exists()) {
                errors.add("File not found: $path")
                continue
            }
            if (!file.canRead()) {
                errors.add("File not readable: $path")
                continue
            }

            try {
                val result = verify(file, expectedHash, algorithm)
                results.add(result)
                if (result.matches == false) mismatched++
            } catch (e: Exception) {
                errors.add("Error hashing $path: ${e.message}")
            }
        }

        return BatchVerificationResult(
            totalFiles = manifest.size,
            verified = results.size,
            mismatched = mismatched,
            errors = errors.size,
            results = results,
            errorMessages = errors,
            durationMs = System.currentTimeMillis() - start
        )
    }

    /**
     * Generate a hash manifest for a directory.
     *
     * @param directory root directory to hash
     * @param algorithm hash algorithm
     * @return manifest as map of relative path → hash
     */
    fun generateManifest(
        directory: File,
        algorithm: String = SHA256
    ): Map<String, String> {
        val manifest = mutableMapOf<String, String>()

        directory.walkTopDown().filter { it.isFile && it.canRead() }.forEach { file ->
            try {
                val hash = computeHash(file, algorithm)
                val relativePath = file.relativeTo(directory).path
                manifest[relativePath] = hash
            } catch (e: Exception) {
                Log.w(TAG, "Failed to hash ${file.absolutePath}: ${e.message}")
            }
        }

        Log.i(TAG, "Generated manifest: ${manifest.size} files hashed")
        return manifest
    }

    /**
     * Generate chain-of-custody metadata for an acquisition.
     */
    fun generateChainOfCustody(
        artifacts: List<ForensicArtifact>,
        examinerName: String,
        caseId: String,
        notes: String = ""
    ): ChainOfCustodyRecord {
        return ChainOfCustodyRecord(
            caseId = caseId,
            examinerName = examinerName,
            acquisitionTime = System.currentTimeMillis(),
            totalArtifacts = artifacts.size,
            totalBytes = artifacts.sumOf { it.sizeBytes },
            artifactHashes = artifacts.mapNotNull { artifact ->
                artifact.sha256?.let { hash ->
                    ArtifactHash(
                        path = artifact.path,
                        sha256 = hash,
                        md5 = artifact.md5,
                        sizeBytes = artifact.sizeBytes
                    )
                }
            },
            notes = notes
        )
    }
}

/**
 * Chain-of-custody record for forensic acquisitions.
 */
data class ChainOfCustodyRecord(
    val caseId: String,
    val examinerName: String,
    val acquisitionTime: Long,
    val totalArtifacts: Int,
    val totalBytes: Long,
    val artifactHashes: List<ArtifactHash>,
    val notes: String = ""
)

data class ArtifactHash(
    val path: String,
    val sha256: String,
    val md5: String?,
    val sizeBytes: Long
)
