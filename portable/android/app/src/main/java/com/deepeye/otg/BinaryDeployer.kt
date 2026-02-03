package com.deepeye.otg

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class BinaryDeployer(private val context: Context) {

    // Pre-calculated SHA-256 hashes for binary integrity
    private val trustedHashes = mapOf(
        "magiskboot" to "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855", // Mock hash
        "ksu_patcher" to "HASH_PLACEHOLDER"
    )

    fun deploy(binaryName: String): File? {
        val targetFile = File(context.filesDir, binaryName)
        
        // Extraction
        try {
            context.assets.open("tools/$binaryName").use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            return null
        }

        // Integrity Check
        if (!verifyHash(targetFile, trustedHashes[binaryName])) {
            targetFile.delete()
            return null
        }

        targetFile.setExecutable(true)
        return targetFile
    }

    private fun verifyHash(file: File, expectedHash: String?): Boolean {
        if (expectedHash == null || expectedHash == "HASH_PLACEHOLDER") return true // Bypass for dev
        
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        val input = file.inputStream()
        
        var bytesRead = input.read(buffer)
        while (bytesRead != -1) {
            digest.update(buffer, 0, bytesRead)
            bytesRead = input.read(buffer)
        }
        
        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        return actualHash.equals(expectedHash, ignoreCase = true)
    }
}
