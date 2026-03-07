package com.deepeye.otg.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.os.Build
import android.util.Log
import kotlinx.coroutines.delay

enum class OemType {
    XIAOMI, SAMSUNG, OPPO, REALME, VIVO, HUAWEI, HONOR,
    ONEPLUS, MOTOROLA, SONY, GOOGLE, UNKNOWN
}

object OemCompatibilityLayer {

    private const val TAG = "OemCompat"

    val currentOem: OemType by lazy {
        val mfr = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        when {
            mfr.contains("xiaomi") || brand.contains("xiaomi") ||
            brand.contains("redmi") || brand.contains("poco")
                -> OemType.XIAOMI
            mfr.contains("samsung")
                -> OemType.SAMSUNG
            mfr.contains("oppo") || brand.contains("oppo")
                -> OemType.OPPO
            brand.contains("realme")
                -> OemType.REALME
            mfr.contains("vivo") || brand.contains("vivo") ||
            brand.contains("iqoo")
                -> OemType.VIVO
            mfr.contains("huawei")
                -> OemType.HUAWEI
            brand.contains("honor")
                -> OemType.HONOR
            else -> OemType.UNKNOWN
        }
    }

    suspend fun postClaimInterfaceDelay() {
        val delayMs = when (currentOem) {
            OemType.XIAOMI  -> 50L
            OemType.VIVO    -> 30L
            OemType.HUAWEI,
            OemType.HONOR   -> 100L
            else            -> 0L
        }
        if (delayMs > 0) {
            delay(delayMs)
        }
    }

    fun sanitizeBuffer(data: ByteArray, endpoint: UsbEndpoint): ByteArray {
        if (currentOem != OemType.SAMSUNG) return data
        val maxPacket = endpoint.maxPacketSize
        if (maxPacket <= 0 || data.size % maxPacket == 0) return data

        val paddedSize = ((data.size / maxPacket) + 1) * maxPacket
        return data.copyOf(paddedSize)
    }

    suspend fun openDeviceWithRetry(
        usbManager: android.hardware.usb.UsbManager,
        device: android.hardware.usb.UsbDevice
    ): UsbDeviceConnection? {
        val maxRetries = if (currentOem == OemType.HUAWEI || currentOem == OemType.HONOR) 3 else 1
        repeat(maxRetries) { attempt ->
            val conn = try { usbManager.openDevice(device) } catch (e: Exception) { null }
            if (conn != null) return conn
            if (attempt < maxRetries - 1) delay(200L)
        }
        return null
    }

    fun permissionRequestCode(device: android.hardware.usb.UsbDevice): Int =
        if (currentOem == OemType.SAMSUNG) device.deviceId else 0

    fun getOtgSettingsIntent(): android.content.Intent? =
        when (currentOem) {
            OemType.VIVO -> android.content.Intent().apply {
                setClassName("com.vivo.easyshare", "com.vivo.easyshare.ui.UsbOtgActivity")
            }
            OemType.XIAOMI -> android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            else -> null
        }

    fun isBatteryOptimized(context: android.content.Context): Boolean {
        if (currentOem != OemType.XIAOMI) return false
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        return !pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun getCompatReport(): String = buildString {
        appendLine("OEM: $currentOem (${Build.MANUFACTURER}/${Build.BRAND})")
        appendLine("API: ${Build.VERSION.SDK_INT}")
        when (currentOem) {
            OemType.XIAOMI  -> appendLine("Workaround: 50ms post-claim delay + ForegroundService required.")
            OemType.SAMSUNG -> appendLine("Workaround: Buffer padding + unique requestCode required.")
            OemType.VIVO    -> appendLine("Workaround: Manual OTG toggle in settings.")
            OemType.HUAWEI, OemType.HONOR -> appendLine("Workaround: 3x retry on openDevice().")
            else -> appendLine("Status: No known hardware quirks.")
        }
    }
}
