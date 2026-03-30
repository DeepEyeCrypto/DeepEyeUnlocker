package com.deepeye.otg.model

sealed class ActivationState {
    object NotActivated : ActivationState()
    object MDMLocked : ActivationState()
    object iCloudLocked : ActivationState()
    object CarrierLocked : ActivationState()
    data class Activated(
        val udid: String,
        val serial: String
    ) : ActivationState()
    data class Error(val reason: String) : ActivationState()
}

enum class BypassMethod {
    DNS_ACTIVATION,
    MDM_BYPASS,
    ACTIVATION_RECORD,
    ICLOUD_REQUIRED,
    CARRIER_UNLOCK,
    UNKNOWN
}

fun parseActivationState(raw: String): ActivationState {
    return when {
        raw.contains("Unactivated", ignoreCase = true) -> ActivationState.NotActivated
        raw.contains("MDMEnrollment", ignoreCase = true) -> ActivationState.MDMLocked
        raw.contains("iCloud", ignoreCase = true) || raw.contains("Activation Lock", ignoreCase = true) -> ActivationState.iCloudLocked
        raw.contains("CarrierLocked", ignoreCase = true) -> ActivationState.CarrierLocked
        raw.contains("Activated", ignoreCase = true) -> {
            val udid = extractField(raw, "UniqueDeviceID")
            val serial = extractField(raw, "SerialNumber")
            ActivationState.Activated(udid, serial)
        }
        else -> ActivationState.Error("Unknown state: $raw")
    }
}

fun selectBypassMethod(state: ActivationState): BypassMethod {
    return when (state) {
        is ActivationState.NotActivated -> BypassMethod.DNS_ACTIVATION
        is ActivationState.MDMLocked -> BypassMethod.MDM_BYPASS
        is ActivationState.iCloudLocked -> BypassMethod.ICLOUD_REQUIRED
        is ActivationState.CarrierLocked -> BypassMethod.CARRIER_UNLOCK
        is ActivationState.Activated -> BypassMethod.ACTIVATION_RECORD
        is ActivationState.Error -> BypassMethod.UNKNOWN
    }
}

private fun extractField(raw: String, key: String): String {
    val line = raw
        .lineSequence()
        .firstOrNull { it.trim().startsWith(key, ignoreCase = true) }
        ?: return "Unknown"
    return line.substringAfter(':', "Unknown").trim().ifBlank { "Unknown" }
}

