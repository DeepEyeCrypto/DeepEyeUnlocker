package com.deepeye.otg.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.deepeye.otg.policy.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
     * Activate a license token. In production, this validates the token
     * against the backend, checks signature, and extracts the granted role.
     *
     * @param token  Signed JWT or license key from purchase/KYC
     * @return true if activation succeeded
     */
    fun activate(token: String): Boolean {
        Log.i(TAG, "[AUTH] Activating license token: ${token.take(16)}...")

        // ── Token validation (simplified) ────────────────────────
        // Production: verify JWT signature, check issuer, check device binding
        val grantedRole = parseTokenRole(token)
        if (grantedRole == null) {
            Log.e(TAG, "[AUTH] Invalid or expired token")
            return false
        }

        val expiry = parseTokenExpiry(token)

        // Persist
        prefs.edit()
            .putString(KEY_ROLE, grantedRole.name)
            .putString(KEY_LICENSE_TOKEN, token)
            .putLong(KEY_LICENSE_EXPIRY, expiry)
            .apply()

        _role.value = grantedRole
        Log.i(TAG, "[AUTH] Activated: role=${grantedRole.label}, expiry=$expiry")
        return true
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
