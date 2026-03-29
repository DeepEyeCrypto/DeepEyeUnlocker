package com.deepeye.otg.usb

import android.hardware.usb.UsbDevice
import com.deepeye.otg.usb.DeviceMatrix.OemBrand

/**
 * Extension to detect OEM brand from UsbDevice properties.
 */
fun UsbDevice.detectOemBrand(): OemBrand {
    val manufacturer = manufacturerName?.lowercase() ?: ""
    val product = productName?.lowercase() ?: ""
    
    return when {
        manufacturer.contains("samsung") -> OemBrand.SAMSUNG
        manufacturer.contains("xiaomi") || product.contains("redmi") || product.contains("poco") -> OemBrand.XIAOMI
        manufacturer.contains("oppo") -> OemBrand.OPPO
        manufacturer.contains("vivo") -> OemBrand.VIVO
        manufacturer.contains("realme") -> OemBrand.REALME
        manufacturer.contains("oneplus") -> OemBrand.ONEPLUS
        manufacturer.contains("google") -> OemBrand.GOOGLE
        manufacturer.contains("motorola") -> OemBrand.MOTOROLA
        manufacturer.contains("lge") || manufacturer.contains("lg electronics") -> OemBrand.LG
        manufacturer.contains("huawei") -> OemBrand.HUAWEI
        manufacturer.contains("apple") -> OemBrand.APPLE
        else -> OemBrand.GENERIC
    }
}
