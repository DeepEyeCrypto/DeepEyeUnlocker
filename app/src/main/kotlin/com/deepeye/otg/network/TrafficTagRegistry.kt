package com.deepeye.otg.network

/**
 * Centralized registry for TrafficStats tags.
 * Used for strict network attribution and billing compliance in Android.
 */
object TrafficTagRegistry {
    const val TAG_UPDATE = 0x1001
    const val TAG_TELEMETRY = 0x1002
    const val TAG_DIAGNOSTICS = 0x1003
}
