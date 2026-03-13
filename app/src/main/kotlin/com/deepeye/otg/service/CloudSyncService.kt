package com.deepeye.otg.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stage 4 — Hardened Cloud Sync & Forensic Persistence.
 * High-assurance transmission of forensic audit trails to the DeepEye Central Node
 * using Certificate Pinning and AES-256 encrypted vaults.
 */
@Singleton
class CloudSyncService @Inject constructor(
    private val client: OkHttpClient
) {
    companion object {
        private const val TAG = "DeepEye-CloudSync"
        private const val BASE_URL = "https://api.deepeye.security/v1"
    }

    /**
     * Uploads a forensic vault with progress tracking.
     */
    suspend fun uploadVault(
        vaultFile: File,
        licenseKey: String,
        onProgress: (Int) -> Unit
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!vaultFile.exists()) return@withContext false to "File not found"

        Log.i(TAG, "[HARDENING] Initiating secured upload for ${vaultFile.name}")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "vault",
                vaultFile.name,
                ProgressRequestBody(vaultFile, "application/octet-stream".toMediaTypeOrNull()!!, onProgress)
            )
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/forensics/upload-vault")
            .header("Authorization", "Bearer $licenseKey")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    Log.i(TAG, "[HARDENING] Security Handshake SUCCESS: $body")
                    true to "Upload successful"
                } else {
                    val error = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "[HARDENING] Security Handshake REJECTED (${response.code}): $error")
                    false to "Server rejected upload ($error)"
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "[HARDENING] TLS Pinning Failure or Network Error: ${e.message}")
            false to "Security/Network Error: ${e.message}"
        }
    }

    private class ProgressRequestBody(
        private val file: File,
        private val contentType: MediaType,
        private val onProgress: (Int) -> Unit
    ) : RequestBody() {
        override fun contentType(): MediaType = contentType
        override fun contentLength(): Long = file.length()

        override fun writeTo(sink: okio.BufferedSink) {
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var uploaded = 0L
                val total = contentLength()
                var read: Int
                
                while (input.read(buffer).also { read = it } != -1) {
                    sink.write(buffer, 0, read)
                    uploaded += read
                    onProgress(((uploaded * 100) / total).toInt())
                }
            }
        }
    }
}
