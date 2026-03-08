package com.deepeye.otg.service

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.deepeye.otg.data.HWIDEngine
import com.deepeye.otg.domain.models.DeepEyeLicense
import com.deepeye.otg.domain.models.LicenseStatus
import com.deepeye.otg.domain.models.PolicyTier
import com.deepeye.otg.policy.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * Singleton manager for license state, secure persistence, and server validation.
 */
object LicenseManager {
    private const val TAG = "DeepEye-Licensing"
    private const val PREFS_FILE = "deepeye_secure_vault"

    private val _licenseState = MutableStateFlow(LicenseStatus.UNREGISTERED)
    val licenseState: StateFlow<LicenseStatus> = _licenseState.asStateFlow()

    private val _currentLicense = MutableStateFlow<DeepEyeLicense?>(null)
    val currentLicense: StateFlow<DeepEyeLicense?> = _currentLicense.asStateFlow()

    private var vault: android.content.SharedPreferences? = null

    /**
     * Initializes the secure vault and loads existing license.
     * Must be called in Application.onCreate().
     */
    fun initialize(context: Context) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            vault = EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            loadFromVault()
        } catch (e: Exception) {
            Log.e(TAG, "[VAULT] Critical failure in secure storage: ${e.message}")
            // Fallback to in-memory if encrypted prefs fails (avoid hard crash)
        }
    }

    private fun loadFromVault() {
        val key = vault?.getString("lic_key", null) ?: return
        val hwidFromVault = vault?.getString("lic_hwid", null) ?: ""
        val tierStr = vault?.getString("lic_tier", "SAFE") ?: "SAFE"
        val expiryTs = vault?.getLong("lic_expiry", 0L) ?: 0L
        val serverTs = vault?.getLong("lic_server_ts", 0L) ?: 0L

        // Security check: Verify HWID binding matches current device
        if (hwidFromVault != HWIDEngine.getHWID()) {
            Log.e(TAG, "[VAULT] License HWID mismatch! Identity theft detected, clearing vault.")
            clearVault()
            return
        }

        val license = DeepEyeLicense(
            key = key,
            hwid = hwidFromVault,
            status = LicenseStatus.ACTIVE, // Assuming active if in vault; will be re-validated by server heartbeat later
            tier = PolicyTier.valueOf(tierStr),
            expiryDate = if (expiryTs > 0) Date(expiryTs) else null,
            serverTimestamp = serverTs
        )

        _currentLicense.value = license
        _licenseState.value = LicenseStatus.ACTIVE
        Log.i(TAG, "[VAULT] Protocol active — Tier: ${license.tier}")
    }

    suspend fun activate(key: String): Boolean {
        val hwid = HWIDEngine.getHWID()
        Log.i(TAG, "[ACTIVATION] Attempting gateway handshake — Key: $key")

        val result = CloudClient.activate(key, hwid)
        
        if (result.status == CloudClient.CloudResponse.SUCCESS) {
            val lic = DeepEyeLicense(
                key = key,
                hwid = hwid,
                status = LicenseStatus.ACTIVE,
                tier = PolicyTier.valueOf(result.tier ?: "SAFE"),
                expiryDate = result.expiry?.takeIf { it > 0 }?.let { Date(it) },
                serverTimestamp = System.currentTimeMillis()
            )

            saveToVault(lic)
            _currentLicense.value = lic
            _licenseState.value = LicenseStatus.ACTIVE
            Log.i(TAG, "[ACTIVATION] Successfully bound to ${lic.tier}")
            return true
        } else {
            Log.e(TAG, "[ACTIVATION] Handshake rejected: ${result.status} | ${result.message}")
            _licenseState.value = mapCloudToStatus(result.status)
            return false
        }
    }

    private fun saveToVault(lic: DeepEyeLicense) {
        vault?.edit()?.apply {
            putString("lic_key", lic.key)
            putString("lic_hwid", lic.hwid)
            putString("lic_tier", lic.tier.name)
            putLong("lic_expiry", lic.expiryDate?.time ?: 0L)
            putLong("lic_server_ts", lic.serverTimestamp)
            apply()
        }
    }

    fun clearVault() {
        vault?.edit()?.clear()?.apply()
        _currentLicense.value = null
        _licenseState.value = LicenseStatus.UNREGISTERED
    }

    fun getEffectiveRole(): UserRole {
        val lic = _currentLicense.value ?: return UserRole.CONSUMER
        return when (lic.tier) {
            PolicyTier.SAFE -> UserRole.CONSUMER
            PolicyTier.POLICY -> UserRole.TECHNICIAN
            PolicyTier.RESTRICTED -> UserRole.ENTERPRISE
            PolicyTier.NEVER -> UserRole.CONSUMER
        }
    }

    private fun mapCloudToStatus(status: CloudClient.CloudResponse): LicenseStatus = when (status) {
        CloudClient.CloudResponse.REVOKED -> LicenseStatus.SUSPENDED
        else -> LicenseStatus.UNREGISTERED
    }
}
