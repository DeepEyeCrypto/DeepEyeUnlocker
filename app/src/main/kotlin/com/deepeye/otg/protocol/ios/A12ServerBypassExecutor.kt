package com.deepeye.otg.protocol.ios

import com.deepeye.otg.BuildConfig
import com.deepeye.otg.data.gsmg.ProtocolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A12ServerBypassExecutor
 * Orchestrates GSMG/iRemoval A12+ Server-side bypasses.
 * Supports: Full Signal, Fake Erase (Data Preserved), and WiFi-only modes.
 */
@Singleton
class A12ServerBypassExecutor @Inject constructor() {

    private val BASE_URL get() = BuildConfig.BYPASS_SERVER_URL

    suspend fun fullSignalBypass(
        imei: String,
        ecid: String,
        sessionId: String
    ): ProtocolResult = withContext(Dispatchers.IO) {
        Timber.d("[A12_SVR] Starting Full Signal Bypass for IMEI: $imei")
        
        if (!validateLuhn(imei)) {
            return@withContext ProtocolResult.GenericFailure("Invalid IMEI (Luhn check failed)", "VALIDATION", sessionId)
        }

        val payload = JSONObject().apply {
            put("action", "full_signal")
            put("imei", imei)
            put("ecid", ecid)
            put("session_id", sessionId)
        }

        val response = performRequest("/a12/bypass", payload)
        
        if (response?.optBoolean("success") == true) {
            ProtocolResult.ActivationBypassed(
                method = "GSMG_SERVER_SIGNAL",
                signalEnabled = true,
                untethered = true,
                sessionId = sessionId
            )
        } else {
            ProtocolResult.GenericFailure(response?.optString("error") ?: "Server Error", "SERVER", sessionId)
        }
    }

    suspend fun fakeEraseBypass(
        ecid: String,
        sessionId: String
    ): ProtocolResult = withContext(Dispatchers.IO) {
        Timber.d("[A12_SVR] Starting Fake Erase Bypass for ECID: $ecid")
        
        val payload = JSONObject().apply {
            put("action", "fake_erase")
            put("ecid", ecid)
            put("preserve_data", true)
            put("session_id", sessionId)
        }

        val response = performRequest("/a12/exploit", payload)
        
        if (response?.optBoolean("success") == true) {
            ProtocolResult.GenericSuccess("Fake Erase Complete - Data Preserved", sessionId)
        } else {
            ProtocolResult.GenericFailure(response?.optString("error") ?: "Exploit Failed", "SERVER", sessionId)
        }
    }

    suspend fun wifiBypass(
        ecid: String,
        sessionId: String
    ): ProtocolResult = withContext(Dispatchers.IO) {
        Timber.d("[A12_SVR] Starting WiFi-only Bypass for ECID: $ecid")
        
        val payload = JSONObject().apply {
            put("action", "wifi_only")
            put("ecid", ecid)
            put("session_id", sessionId)
        }

        val response = performRequest("/a12/bypass", payload)
        
        if (response?.optBoolean("success") == true) {
            ProtocolResult.ActivationBypassed("GSMG_WIFI", false, true, sessionId)
        } else {
            ProtocolResult.GenericFailure(response?.optString("error") ?: "WiFi Bypass Failed", "SERVER", sessionId)
        }
    }

    private fun performRequest(endpoint: String, payload: JSONObject): JSONObject? {
        return try {
            val url = URL("$BASE_URL$endpoint")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("X-API-Key", "DEEPEYE_IREMOVAL_SECRET_2026")
            
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray())
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(responseBody)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "[A12_SVR] Request Failed")
            null
        }
    }

    private fun validateLuhn(imei: String): Boolean {
        if (imei.length != 15) return false
        var sum = 0
        for (i in 0 until 15) {
            var n = imei[i] - '0'
            if (i % 2 != 0) {
                n *= 2
                if (n > 9) n = (n % 10) + 1
            }
            sum += n
        }
        return sum % 10 == 0
    }
}
