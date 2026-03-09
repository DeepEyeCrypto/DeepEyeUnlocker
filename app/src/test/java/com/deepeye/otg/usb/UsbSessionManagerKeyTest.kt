package com.deepeye.otg.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito

/**
 * Lightweight test to validate that UsbSessionManager's internal deviceKey
 * format matches the required "${vendorId}:${productId}:${deviceId}" contract.
 */
class UsbSessionManagerKeyTest {

    @Test
    fun device_key_format_matches_spec() {
        val context = Mockito.mock(Context::class.java)
        val usbManager = Mockito.mock(UsbManager::class.java)
        val manager = UsbSessionManager(context, usbManager)

        val device = Mockito.mock(UsbDevice::class.java)
        Mockito.`when`(device.vendorId).thenReturn(0x18D1)
        Mockito.`when`(device.productId).thenReturn(0x4EE7)
        Mockito.`when`(device.deviceId).thenReturn(7)

        val method = UsbSessionManager::class.java.getDeclaredMethod("deviceKey", UsbDevice::class.java)
        method.isAccessible = true

        val key = method.invoke(manager, device) as String
        assertEquals("18D1:4EE7:7", key.uppercase())
    }
}

