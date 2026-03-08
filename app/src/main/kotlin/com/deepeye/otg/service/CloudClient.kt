package com.deepeye.otg.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Low-level client for DeepEye Cloud authentication.
 * Uses built-in HttpURLConnection + org.json to maintain zero-dependency footprint.
 */
object CloudClient {
    private const val TAG = "DeepEye-Cloud"
    private const val API_BASE = "https://api.deepeye.cloud/v1"

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
        try {
            val url = URL("$API_BASE/activate")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val payload = JSONObject().apply {
                put("license_key", key)
                put("hwid", hwid)
                put("platform", "android")
                put("version", "v2026.18")
            }

            Log.i(TAG, "[CLOUD] POST $url | key=$key HWID=$hwid")
            
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val responseCode = conn.responseCode
            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            
            Log.d(TAG, "[CLOUD] Response ($responseCode): $responseBody")

            if (responseCode == 200) {
                val json = JSONObject(responseBody)
                ActivationResult(
                    status = CloudResponse.SUCCESS,
                    tier = json.optString("tier", "SAFE"),
                    expiry = json.optLong("expiry_ts", 0L),
                    message = json.optString("message", "Success")
                )
            } else {
                handleError(responseCode, responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[CLOUD] Connection failed: ${e.message}")
            ActivationResult(CloudResponse.NETWORK_ERROR, message = e.message)
        }
    }

    suspend fun fetchModelDatabase(): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_BASE/models")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "[SYNC] Failed to fetch model DB: ${e.message}")
            null
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
