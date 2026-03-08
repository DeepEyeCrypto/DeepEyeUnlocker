package com.deepeye.otg.domain.models

import java.util.Date

enum class LicenseStatus {
    UNREGISTERED,
    TRIAL,
    ACTIVE,
    EXPIRED,
    SUSPENDED
}

data class DeepEyeLicense(
    val key: String,
    val hwid: String,
    val status: LicenseStatus,
    val tier: PolicyTier,
    val expiryDate: Date?,
    val serverTimestamp: Long
)

/**
 * User profile combining identity and dynamic entitlements.
 */
data class UserProfile(
    val userId: String,
    val role: com.deepeye.otg.policy.UserRole,
    val license: DeepEyeLicense? = null,
    val isResearcher: Boolean = false
)
