package com.deepeye.otg.usb

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Build

data class OtgCapabilityResult(
    val hasUsbHostFeature: Boolean,   // PackageManager check
    val hasOtgSupport: Boolean,       // UsbManager device list works
    val androidVersion: Int,          // >= 3.1 (API 12) needed
    val hostDeviceCount: Int,         // currently connected devices
    val recommendation: String
)

object OtgCapabilityChecker {

    fun check(context: Context): OtgCapabilityResult {
        val pm = context.packageManager
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        // Requirement: android.hardware.usb.host feature must be present for OTG
        val hasUsbHostFeature = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)

        // Try listing devices — if it returns anything, OTG is definitely live
        val deviceList = try {
            usbManager.deviceList
        } catch (e: Exception) { 
            emptyMap() 
        }

        val hasOtgSupport = hasUsbHostFeature || deviceList.isNotEmpty()
        val deviceCount = deviceList.size

        val recommendation = when {
            !hasUsbHostFeature && deviceCount == 0 ->
                "❌ This phone does NOT support USB OTG host mode. Use a different phone as host."
            !hasUsbHostFeature && deviceCount > 0 ->
                "⚠️ USB host feature flag missing but devices detected. OTG may work partially."
            Build.VERSION.SDK_INT < 12 ->
                "❌ Android version too old. Need Android 3.1+ (API 12) for USB Host support."
            deviceCount == 0 ->
                "✅ OTG supported. No device connected yet. Connect target phone via OTG cable."
            else ->
                "✅ OTG working. $deviceCount device(s) detected via USB Host interface."
        }

        return OtgCapabilityResult(
            hasUsbHostFeature = hasUsbHostFeature,
            hasOtgSupport = hasOtgSupport,
            androidVersion = Build.VERSION.SDK_INT,
            hostDeviceCount = deviceCount,
            recommendation = recommendation
        )
    }

    /**
     * Scans connected USB devices and returns a list of human-readable descriptions.
     */
    fun scanConnectedDevices(context: Context): List<String> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return usbManager.deviceList.values.map { device ->
            val vid = "0x${device.vendorId.toString(16).uppercase()}"
            val pid = "0x${device.productId.toString(16).uppercase()}"
            val name = device.productName ?: "Generic USB Device"
            val manu = device.manufacturerName ?: "Unknown Vendor"
            val ifaces = device.interfaceCount
            "[$vid:$pid] $name by $manu | $ifaces interface(s)"
        }
    }
}
