package com.deepeye.otg.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardened client for DeepEye Cloud authentication.
 * Transitions to OkHttp for TLS pinning and reliability.
 */
@Singleton
class CloudClient @Inject constructor(
    private val client: OkHttpClient
) {
    companion object {
        private const val TAG = "DeepEye-Cloud"
        private const val API_BASE = "https://api.deepeye.io/v1"

        @Volatile
        private var modelSyncClient: OkHttpClient? = null

        suspend fun fetchModelDatabase(currentVersion: Long): String? = withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$API_BASE/models?current=$currentVersion")
                .get()
                .build()

            try {
                val client = modelSyncClient ?: OkHttpClient.Builder().build()
                client.newCall(request).execute().use { response ->
                    when {
                        response.code == 304 -> "NOT_MODIFIED"
                        response.isSuccessful -> response.body?.string()
                        else -> null
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "[SYNC] Failed to fetch model DB: ${e.message}")
                null
            }
        }
    }

    init {
        modelSyncClient = client
    }

    enum class CloudResponse {
        SUCCESS,
        INVALID_KEY,
        HWID_MISMATCH,
        REVOKED,
        NETWORK_ERROR,
        SERVER_ERROR
    }

    data class ActivationResult(
        val status: CloudResponse,
        val tier: String? = null,
        val expiry: Long? = null,
        val message: String? = null
    )

    suspend fun activate(key: String, hwid: String): ActivationResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("license_key", key)
            put("hwid", hwid)
            put("platform", "android")
            put("version", "v2026.25")
        }

        val request = Request.Builder()
            .url("$API_BASE/activate")
            .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    ActivationResult(
                        status = CloudResponse.SUCCESS,
                        tier = json.optString("tier", "SAFE"),
                        expiry = json.optLong("expiry_ts", 0L),
                        message = json.optString("message", "Success")
                    )
                } else {
                    handleError(response.code, body)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[CLOUD] Connection failed (pinning violation?): ${e.message}")
            ActivationResult(CloudResponse.NETWORK_ERROR, message = e.message)
        }
    }

    suspend fun checkLicense(key: String, signature: String, timestamp: Long): ActivationResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("license_key", key)
            put("signature", signature)
            put("timestamp", timestamp)
        }

        val request = Request.Builder()
            .url("$API_BASE/heartbeat")
            .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    ActivationResult(
                        status = CloudResponse.SUCCESS,
                        tier = json.optString("tier", "SAFE"),
                        expiry = json.optLong("expiry_ts", 0L)
                    )
                } else {
                    handleError(response.code, body)
                }
            }
        } catch (e: Exception) {
            ActivationResult(CloudResponse.NETWORK_ERROR, message = e.message)
        }
    }

    private fun handleError(code: Int, body: String): ActivationResult {
        return try {
            val json = JSONObject(body)
            val error = json.optString("error_code")
            val status = when (error) {
                "INVALID_KEY" -> CloudResponse.INVALID_KEY
                "HWID_MISMATCH" -> CloudResponse.HWID_MISMATCH
                "REVOKED" -> CloudResponse.REVOKED
                else -> CloudResponse.SERVER_ERROR
            }
            ActivationResult(status, message = json.optString("message", "Request failed"))
        } catch (e: Exception) {
            ActivationResult(CloudResponse.SERVER_ERROR, message = "HTTP $code")
        }
    }
}
