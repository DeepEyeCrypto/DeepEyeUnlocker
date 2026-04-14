package com.deepeye.otg.protocol.ios

import com.deepeye.otg.data.gsmg.ProtocolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import com.deepeye.otg.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

// =============================================================================
// RealServerBypassExecutor.kt
// REAL server-side activation bypass — actual HTTPS calls
// Used for: A12+ iCloud bypass, IMEI registration, carrier unlock
// =============================================================================

class RealServerBypassExecutor(
    private val pythonBridge: com.deepeye.otg.python.PythonBridge
) {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS    = 60_000

        // Bypass server endpoints (configure per deployment)
        // These should come from BuildConfig or encrypted config
        private const val BYPASS_BASE_URL    = "https://api.deepeye-unlock.com/v1/ios" // Fallback: BuildConfig.BYPASS_SERVER_URL
        private const val API_VERSION        = "v2"
    }

    // ── A12+ iCloud Bypass (WiFi only) ────────────────────────────────────

    suspend fun requestBypassToken(
        ecid:      String,
        serial:    String?,
        iosVersion:String,
        sessionId: String,
    ): ProtocolResult = withContext(Dispatchers.IO) {
        Timber.d("[SERVER] requestBypassToken ecid=$ecid " +
                 "ios=$iosVersion sessionId=$sessionId")

        val activationPayload = pythonBridge.buildIosActivationRequest(
            udid = ecid,
            imei = "",
            serial = serial ?: "",
            model = "Unknown",
            iosVersion = iosVersion,
            sessionId = sessionId
        )

        val payload = JSONObject().apply {
            put("ecid",        ecid)
            put("serial",      serial ?: "")
            put("ios_version", iosVersion)
            put("bypass_type", "wifi_only")
            put("session_id",  sessionId)
            put("apple_activation_request", activationPayload)
        }

        val response = httpPost(
            endpoint  = "$BYPASS_BASE_URL/$API_VERSION/bypass/token",
            body      = payload.toString(),
            sessionId = sessionId,
        )

        return@withContext when {
            response == null -> ProtocolResult.ServerError(
                reason    = "No response from bypass server",
                sessionId = sessionId,
                httpCode  = -1,
            )
            response.has("token") -> {
                val token = response.getString("token")
                Timber.d("[SERVER] token received len=${token.length} " +
                         "sessionId=$sessionId")
                ProtocolResult.ServerBypassComplete(
                    token     = token,
                    sessionId = sessionId,
                )
            }
            response.has("error") -> ProtocolResult.ServerError(
                reason    = response.getString("error"),
                sessionId = sessionId,
                httpCode  = response.optInt("code", -1),
            )
            else -> ProtocolResult.ServerError(
                reason    = "Unexpected server response: $response",
                sessionId = sessionId,
            )
        }
    }

    // ── IMEI Registration (Full Signal) ──────────────────────────────────

    suspend fun registerImei(
        imei:      String,
        ecid:      String,
        sessionId: String,
    ): ProtocolResult = withContext(Dispatchers.IO) {
        Timber.d("[SERVER] registerImei imei=${imei.take(8)}xxxx " +
                 "sessionId=$sessionId")

        // Luhn check before sending
        val validation = pythonBridge.validateImei(imei, sessionId)
        if (!validation.isValid) {
            return@withContext ProtocolResult.ServerError(
                reason    = "IMEI failed Luhn validation via Python: $imei",
                sessionId = sessionId,
            )
        }

        val payload = JSONObject().apply {
            put("imei",       imei)
            put("ecid",       ecid)
            put("session_id", sessionId)
        }

        val response = httpPost(
            endpoint  = "$BYPASS_BASE_URL/$API_VERSION/imei/register",
            body      = payload.toString(),
            sessionId = sessionId,
        )

        return@withContext when {
            response == null -> ProtocolResult.ServerError(
                reason    = "IMEI registration: no server response",
                sessionId = sessionId,
                httpCode  = -1,
            )
            response.optBoolean("success", false) -> {
                Timber.d("[SERVER] IMEI registered sessionId=$sessionId")
                ProtocolResult.ActivationBypassed(
                    method        = "SERVER_IMEI_REGISTRATION",
                    signalEnabled = true,
                    untethered    = true,
                    sessionId     = sessionId,
                )
            }
            else -> ProtocolResult.ServerError(
                reason    = response.optString("error", "Registration failed"),
                sessionId = sessionId,
                httpCode  = response.optInt("code", -1),
            )
        }
    }

    // ── Carrier Unlock ────────────────────────────────────────────────────

    suspend fun requestCarrierUnlock(
        imei:      String,
        sessionId: String,
    ): ProtocolResult = withContext(Dispatchers.IO) {
        Timber.d("[SERVER] carrierUnlock imei=${imei.take(8)}xxxx " +
                 "sessionId=$sessionId")

        val validation = pythonBridge.validateImei(imei, sessionId)
        if (!validation.isValid) {
            return@withContext ProtocolResult.ServerError(
                reason    = "IMEI Luhn check failed via Python",
                sessionId = sessionId,
            )
        }

        val payload = JSONObject().apply {
            put("imei",       imei)
            put("operation",  "carrier_unlock")
            put("session_id", sessionId)
        }

        val response = httpPost(
            endpoint  = "$BYPASS_BASE_URL/$API_VERSION/carrier/unlock",
            body      = payload.toString(),
            sessionId = sessionId,
        )

        return@withContext if (response?.optBoolean("success", false) == true) {
            Timber.d("[SERVER] carrier_unlock OK sessionId=$sessionId")
            ProtocolResult.GenericSuccess(
                operation = "CARRIER_UNLOCK",
                sessionId = sessionId,
            )
        } else {
            ProtocolResult.ServerError(
                reason    = response?.optString("error") ?: "Unlock failed",
                sessionId = sessionId,
            )
        }
    }

    // ── HTTP helper ───────────────────────────────────────────────────────

    private fun httpPost(
        endpoint:  String,
        body:      String,
        sessionId: String,
    ): JSONObject? {
        return try {
            val url  = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod      = "POST"
                connectTimeout     = CONNECT_TIMEOUT_MS
                readTimeout        = READ_TIMEOUT_MS
                doOutput           = true
                setRequestProperty("Content-Type",  "application/json")
                setRequestProperty("X-Session-Id",  sessionId)
                setRequestProperty("X-App-Version", BuildConfig.VERSION_NAME)
            }

            conn.outputStream.bufferedWriter().use { it.write(body) }

            val code     = conn.responseCode
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            Timber.d("[SERVER] http_response code=$code " +
                     "len=${response.length} sessionId=$sessionId")

            JSONObject(response)
        } catch (e: Exception) {
            Timber.e("[SERVER] http_error: ${e.message} sessionId=$sessionId")
            null
        }
    }

}
