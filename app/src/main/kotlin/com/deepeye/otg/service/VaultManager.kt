package com.deepeye.otg.service

import android.util.Log
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File

/**
 * Stage 3 — Encrypted Vault Manager.
 * Handles password protection for exported forensic reports using AES-256.
 */
object VaultManager {
    private const val TAG = "DeepEye-Vault"

    fun encryptReport(reportFile: File, password: String): File? {
        if (!reportFile.exists()) return null
        
        val vaultFile = File(reportFile.parent, reportFile.nameWithoutExtension + ".deepvault")
        if (vaultFile.exists()) vaultFile.delete()

        try {
            val zipFile = ZipFile(vaultFile, password.toCharArray())
            val parameters = ZipParameters().apply {
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            }
            
            zipFile.addFile(reportFile, parameters)
            Log.i(TAG, "Vault created: ${vaultFile.absolutePath}")
            return vaultFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create vault: ${e.message}")
            return null
        }
    }

    fun decryptVault(vaultFile: File, password: String, outputDir: File): List<File> {
        if (!vaultFile.exists()) return emptyList()
        
        try {
            val zipFile = ZipFile(vaultFile, password.toCharArray())
            if (!zipFile.isValidZipFile) {
                Log.e(TAG, "Invalid vault format or wrong password")
                return emptyList()
            }
            
            zipFile.extractAll(outputDir.absolutePath)
            return outputDir.listFiles()?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ${e.message}")
            return emptyList()
        }
    }
}
