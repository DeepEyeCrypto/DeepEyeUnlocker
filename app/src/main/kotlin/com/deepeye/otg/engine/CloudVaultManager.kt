package com.deepeye.otg.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages remote synchronization of activation token backups.
 * Integrates with the application's cloud infrastructure (F3arRa1n Vault).
 */
@Singleton
class CloudVaultManager @Inject constructor(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val TAG = "CloudVaultManager"
    private val VAULT_BASE_URL = "https://vault.deepeye.security/api/v1"

    private val _syncStatus = MutableStateFlow<VaultSyncStatus>(VaultSyncStatus.Idle)
    val syncStatus = _syncStatus.asStateFlow()

    sealed class VaultSyncStatus {
        object Idle : VaultSyncStatus()
        object Syncing : VaultSyncStatus()
        data class Success(val lastSync: Long) : VaultSyncStatus()
        data class Error(val message: String) : VaultSyncStatus()
    }

    /**
     * Uploads a local token backup to the Cloud Vault.
     */
    suspend fun uploadToVault(backupFile: File): Boolean = withContext(Dispatchers.IO) {
        _syncStatus.value = VaultSyncStatus.Syncing
        Log.i(TAG, "Uploading ${backupFile.name} to Cloud Vault...")
        
        try {
            if (!backupFile.exists()) {
                _syncStatus.value = VaultSyncStatus.Error("File not found: ${backupFile.absolutePath}")
                return@withContext false
            }

            // Get device ID for vault organization
            val deviceId = getDeviceIdentifier()
            
            // Create multipart request
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", backupFile.name, 
                    backupFile.asRequestBody("application/octet-stream".toMediaType()))
                .addFormDataPart("device_id", deviceId)
                .addFormDataPart("timestamp", System.currentTimeMillis().toString())
                .addFormDataPart("checksum", calculateFileChecksum(backupFile))
                .build()

            val request = Request.Builder()
                .url("$VAULT_BASE_URL/backups/upload")
                .header("Authorization", getAuthToken())
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                _syncStatus.value = VaultSyncStatus.Success(System.currentTimeMillis())
                Log.i(TAG, "Cloud Vault Sync SUCCESS: ${backupFile.name}")
                true
            } else {
                val errorMsg = "Upload failed: ${response.code} ${response.message}"
                _syncStatus.value = VaultSyncStatus.Error(errorMsg)
                Log.e(TAG, errorMsg)
                false
            }
        } catch (e: Exception) {
            _syncStatus.value = VaultSyncStatus.Error(e.message ?: "Unknown error")
            Log.e(TAG, "Cloud Vault Sync FAIL", e)
            false
        }
    }

    /**
     * Fetches a list of backups available in the Cloud Vault for a specific device.
     */
    suspend fun fetchCloudBackups(deviceId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching cloud backups for device: $deviceId")
            
            val request = Request.Builder()
                .url("$VAULT_BASE_URL/backups?device_id=$deviceId")
                .header("Authorization", getAuthToken())
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "[]"
                val backups = parseBackupList(body)
                Log.d(TAG, "Found ${backups.size} backups in cloud vault")
                backups
            } else {
                Log.w(TAG, "Failed to fetch backups: ${response.code}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching cloud backups", e)
            emptyList()
        }
    }

    /**
     * Downloads a specific backup from the Cloud Vault.
     */
    suspend fun downloadBackup(backupId: String, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading backup: $backupId")
            
            val request = Request.Builder()
                .url("$VAULT_BASE_URL/backups/$backupId/download")
                .header("Authorization", getAuthToken())
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Backup downloaded successfully: ${destinationFile.absolutePath}")
                true
            } else {
                Log.e(TAG, "Download failed: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading backup", e)
            false
        }
    }

    /**
     * Deletes a backup from the Cloud Vault.
     */
    suspend fun deleteBackup(backupId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting backup: $backupId")
            
            val request = Request.Builder()
                .url("$VAULT_BASE_URL/backups/$backupId")
                .header("Authorization", getAuthToken())
                .delete()
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting backup", e)
            false
        }
    }

    /**
     * Gets unique device identifier for vault organization.
     */
    private fun getDeviceIdentifier(): String {
        val prefs = context.getSharedPreferences("deepeye_vault", Context.MODE_PRIVATE)
        return prefs.getString("device_id", null) ?: run {
            val newId = "device_${System.currentTimeMillis()}"
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
    }

    /**
     * Gets authentication token for vault API.
     */
    private fun getAuthToken(): String {
        val prefs = context.getSharedPreferences("deepeye_auth", Context.MODE_PRIVATE)
        return prefs.getString("vault_token", "Bearer anonymous") ?: "Bearer anonymous"
    }

    /**
     * Calculates SHA-256 checksum for file integrity verification.
     */
    private fun calculateFileChecksum(file: File): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating checksum", e)
            ""
        }
    }

    /**
     * Parses backup list from JSON response.
     */
    private fun parseBackupList(jsonString: String): List<String> {
        return try {
            val jsonArray = org.json.JSONArray(jsonString)
            val backups = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val backup = jsonArray.getJSONObject(i)
                backups.add(backup.optString("id", backup.optString("filename", "")))
            }
            backups
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing backup list", e)
            emptyList()
        }
    }
}
