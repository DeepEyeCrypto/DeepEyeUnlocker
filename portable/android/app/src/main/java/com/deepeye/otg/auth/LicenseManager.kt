package com.deepeye.otg.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.deepeye.otg.policy.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import android.provider.Settings

// ═══════════════════════════════════════════════════════════════════
//  LicenseManager — manages user role based on license/auth state.
//
//  Storage: SharedPreferences (encrypted in production via EncryptedSharedPrefs)
//  Default: CONSUMER (tier 1 only — safe operations)
//
//  Activation flow:
//    1. User purchases license or passes KYC
//    2. Backend returns signed license token
//    3. App calls activate(token) → validates → upgrades role
//    4. Role persists across app restarts
//
//  For dev/testing: call setRole(UserRole.DEV) directly.
// ═══════════════════════════════════════════════════════════════════

object LicenseManager {

    private const val TAG = "DeepEye-License"
    private const val PREFS_NAME = "deepeye_license"
    private const val KEY_ROLE = "user_role"
    private const val KEY_LICENSE_TOKEN = "license_token"
    private const val KEY_LICENSE_EXPIRY = "license_expiry"
    private const val KEY_DEVICE_ID = "device_id"

    private lateinit var prefs: SharedPreferences

    private val _role = MutableStateFlow(UserRole.CONSUMER)
    /** Current user role — observe this to react to role changes. */
    val role: StateFlow<UserRole> = _role.asStateFlow()

    /** Current role value (non-flow, for synchronous access). */
    val currentRole: UserRole get() = _role.value

    /**
     * Initialize from SharedPreferences. Call once in Application.onCreate or Activity.onCreate.
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedRole = prefs.getString(KEY_ROLE, null)
        val restored = savedRole?.let { name ->
            try { UserRole.valueOf(name) } catch (_: Exception) { null }
        } ?: UserRole.CONSUMER

        // Check expiry
        val expiry = prefs.getLong(KEY_LICENSE_EXPIRY, 0)
        val finalRole = if (expiry > 0 && System.currentTimeMillis() > expiry) {
            Log.w(TAG, "[AUTH] License expired — reverting to CONSUMER")
            prefs.edit().remove(KEY_LICENSE_TOKEN).remove(KEY_LICENSE_EXPIRY).apply()
            UserRole.CONSUMER
        } else {
            restored
        }

        _role.value = finalRole
        Log.i(TAG, "[AUTH] Initialized: role=${finalRole.label}, expiry=${if (expiry > 0) expiry else "none"}")
    }

    // ── Activation ───────────────────────────────────────────────

    /**
     * Activate a license token directly with the backend.
     * Hits POST /api/licenses/activate to verify signature and device binding.
     *
     * @param context Application context to fetch device ID
     * @param token   Signed JWT or license token
     */
    suspend fun activateFromBackend(context: Context, token: String): Result<UserRole> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "[AUTH] Activating token with backend: ${token.take(16)}...")

            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "unknown_device_id"

            // Connect to local backend (emulator uses 10.0.2.2 for localhost)
            // In production, use real HTTPS URL.
            val url = URL("http://10.0.2.2:5000/api/licenses/activate")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            // Sending body
            val requestBody = JSONObject().apply {
                put("token", token)
                put("deviceId", deviceId)
            }.toString()

            OutputStreamWriter(connection.outputStream).use { it.write(requestBody) }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = InputStreamReader(stream).use { it.readText() }
            val responseJson = JSONObject(responseText)

            if (responseCode !in 200..299) {
                val errorMsg = responseJson.optString("error", "Unknown backend error")
                val reason = responseJson.optString("reason", "")
                Log.e(TAG, "[AUTH] Backend activation failed: $errorMsg - $reason")
                return@withContext Result.failure(Exception("$errorMsg${if (reason.isNotEmpty()) " ($reason)" else ""}"))
            }

            // Success response: { "success": true, "role": "TECHNICIAN", "expiresAt": "2024-...", ... }
            val roleStr = responseJson.getString("role")
            val grantedRole = try { UserRole.valueOf(roleStr) } catch (e: Exception) {
                return@withContext Result.failure(Exception("Unknown role from server: $roleStr"))
            }

            val expiresAtIso = responseJson.optString("expiresAt", "")
            val expiryTimeMs = try {
                if (expiresAtIso.isNotEmpty()) {
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                        .parse(expiresAtIso)?.time ?: 0L
                } else 0L
            } catch (e: Exception) { 0L }

            // Persist
            prefs.edit()
                .putString(KEY_ROLE, grantedRole.name)
                .putString(KEY_LICENSE_TOKEN, token)
                .putLong(KEY_LICENSE_EXPIRY, expiryTimeMs)
                .apply()

            _role.value = grantedRole
            Log.i(TAG, "[AUTH] Activated via Backend: role=${grantedRole.label}, expiry=$expiresAtIso")
            
            Result.success(grantedRole)
        } catch (e: Exception) {
            Log.e(TAG, "[AUTH] Network error during activation", e)
            Result.failure(e)
        }
    }

    /**
     * Deactivate / revoke the current license.
     * Reverts to CONSUMER role.
     */
    fun deactivate() {
        Log.i(TAG, "[AUTH] Deactivating license")
        prefs.edit()
            .remove(KEY_ROLE)
            .remove(KEY_LICENSE_TOKEN)
            .remove(KEY_LICENSE_EXPIRY)
            .apply()
        _role.value = UserRole.CONSUMER
    }

    /**
     * For dev/testing: directly set the role without a token.
     * Persists the role change.
     */
    fun setRole(role: UserRole) {
        Log.i(TAG, "[AUTH] Direct role set: ${role.label}")
        prefs.edit().putString(KEY_ROLE, role.name).apply()
        _role.value = role
    }

    // ── License Info ─────────────────────────────────────────────

    /** Returns true if user has any license above CONSUMER. */
    val isLicensed: Boolean get() = _role.value.level > UserRole.CONSUMER.level

    /** Returns the stored license token, or null. */
    val licenseToken: String? get() =
        if (::prefs.isInitialized) prefs.getString(KEY_LICENSE_TOKEN, null) else null

    /** Returns license expiry epoch ms, or 0 if none. */
    val licenseExpiry: Long get() =
        if (::prefs.isInitialized) prefs.getLong(KEY_LICENSE_EXPIRY, 0) else 0

    /** Returns true if the license has expired. */
    val isExpired: Boolean get() {
        val exp = licenseExpiry
        return exp > 0 && System.currentTimeMillis() > exp
    }

    // ── Token Parsing (simplified — replace with real JWT in prod) ──

    /**
     * Parse role from token. Token format (simplified):
     *   "DEEPEYE-{ROLE}-{EXPIRY_EPOCH}-{SIGNATURE}"
     *   e.g., "DEEPEYE-TECHNICIAN-1741024800000-abc123"
     */
    private fun parseTokenRole(token: String): UserRole? {
        return try {
            val parts = token.split("-")
            if (parts.size < 3 || parts[0] != "DEEPEYE") return null
            UserRole.valueOf(parts[1])
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTokenExpiry(token: String): Long {
        return try {
            val parts = token.split("-")
            if (parts.size >= 3) parts[2].toLong() else 0
        } catch (_: Exception) {
            0
        }
    }
}
