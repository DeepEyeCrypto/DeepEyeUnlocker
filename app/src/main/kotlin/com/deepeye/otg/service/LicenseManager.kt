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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager for license state, secure persistence, and server validation.
 */
@Singleton
class LicenseManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudClient: CloudClient
) {
    companion object {
        private const val TAG = "DeepEye-Licensing"
        private const val PREFS_FILE = "deepeye_secure_vault"
    }

    private val _licenseState = MutableStateFlow(LicenseStatus.UNREGISTERED)
    val licenseState: StateFlow<LicenseStatus> = _licenseState.asStateFlow()

    private val _currentLicense = MutableStateFlow<DeepEyeLicense?>(null)
    val currentLicense: StateFlow<DeepEyeLicense?> = _currentLicense.asStateFlow()

    private var vault: android.content.SharedPreferences? = null

    init {
        initializeVault()
    }

    private fun initializeVault() {
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
        }
    }

    private fun loadFromVault() {
        val key = vault?.getString("lic_key", null) ?: return
        val hwidFromVault = vault?.getString("lic_hwid", null) ?: ""
        val tierStr = vault?.getString("lic_tier", "SAFE") ?: "SAFE"
        val expiryTs = vault?.getLong("lic_expiry", 0L) ?: 0L
        val serverTs = vault?.getLong("lic_server_ts", 0L) ?: 0L

        if (hwidFromVault != HWIDEngine.getHWID()) {
            Log.e(TAG, "[VAULT] License HWID mismatch! Identity theft detected, clearing vault.")
            clearVault()
            return
        }

        val license = DeepEyeLicense(
            key = key,
            hwid = hwidFromVault,
            status = LicenseStatus.ACTIVE,
            tier = try { PolicyTier.valueOf(tierStr) } catch (e: Exception) { PolicyTier.SAFE },
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

        val result = cloudClient.activate(key, hwid)
        
        if (result.status == CloudClient.CloudResponse.SUCCESS) {
            val lic = DeepEyeLicense(
                key = key,
                hwid = hwid,
                status = LicenseStatus.ACTIVE,
                tier = try { PolicyTier.valueOf(result.tier ?: "SAFE") } catch (e: Exception) { PolicyTier.SAFE },
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

    /**
     * Stage 6: Zero-Knowledge Heartbeat.
     * Validates license state without leaking raw HWID in every request.
     */
    suspend fun performHeartbeat(): Boolean {
        val lic = _currentLicense.value ?: return false
        val ts = System.currentTimeMillis()
        val signature = calculateSignature(lic.key, lic.hwid, ts)

        Log.d(TAG, "[HEARTBEAT] Emitting ZK-nonce for key: ${lic.key.take(8)}...")
        val result = cloudClient.checkLicense(lic.key, signature, ts)

        if (result.status == CloudClient.CloudResponse.SUCCESS) {
            val updatedLic = lic.copy(
                tier = try { PolicyTier.valueOf(result.tier ?: "SAFE") } catch (e: Exception) { PolicyTier.SAFE },
                expiryDate = result.expiry?.takeIf { it > 0 }?.let { Date(it) },
                serverTimestamp = ts
            )
            _currentLicense.value = updatedLic
            _licenseState.value = LicenseStatus.ACTIVE
            saveToVault(updatedLic)
            return true
        } else {
            Log.w(TAG, "[HEARTBEAT] Handshake failed: ${result.status}. Suspending license.")
            _licenseState.value = mapCloudToStatus(result.status)
            if (result.status == CloudClient.CloudResponse.REVOKED || result.status == CloudClient.CloudResponse.INVALID_KEY) {
                clearVault()
            }
            return false
        }
    }

    private fun calculateSignature(key: String, hwid: String, ts: Long): String {
        return try {
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            val secretKey = javax.crypto.spec.SecretKeySpec(key.toByteArray(), "HmacSHA256")
            mac.init(secretKey)
            val data = "$hwid|$ts".toByteArray()
            mac.doFinal(data).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "error_signature_${System.currentTimeMillis()}"
        }
    }

    private fun mapCloudToStatus(status: CloudClient.CloudResponse): LicenseStatus = when (status) {
        CloudClient.CloudResponse.REVOKED -> LicenseStatus.SUSPENDED
        else -> LicenseStatus.UNREGISTERED
    }
}
