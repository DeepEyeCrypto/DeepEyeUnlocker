package com.deepeye.otg.util

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import com.deepeye.otg.usb.DeviceMatrix
import timber.log.Timber

/**
 * USB extension functions with retry logic (3x retry as per project spec).
 * NEVER call raw .bulkTransfer() directly — use these helpers.
 *
 * Rules:
 * - All USB I/O must be on Dispatchers.IO (caller's responsibility)
 * - SessionId must be threaded through every log line
 * - Retry up to 3 times with exponential backoff (50ms, 100ms, 200ms)
 * - Log failures with tag and sessionId
 */
fun UsbDeviceConnection.bulkOut(
    ep:        UsbEndpoint,
    data:      ByteArray,
    len:       Int        = data.size,
    timeoutMs: Int        = 5000,
    sessionId: String     = "",
    tag:       String     = "USB",
): Int {
    var result = bulkTransfer(ep, data, len, timeoutMs)
    if (result < 0) {
        Thread.sleep(50)
        result = bulkTransfer(ep, data, len, timeoutMs)
        if (result < 0) {
            Thread.sleep(100)
            result = bulkTransfer(ep, data, len, timeoutMs)
            if (result < 0) {
                Timber.e("[$tag] bulkOut FAILED 3x n=$result sessionId=$sessionId")
                return result
            }
        }
    }
    return result
}

fun UsbDeviceConnection.bulkIn(
    ep:        UsbEndpoint,
    buf:       ByteArray,
    len:       Int        = buf.size,
    timeoutMs: Int        = 5000,
    sessionId: String     = "",
    tag:       String     = "USB",
): Int {
    var result = bulkTransfer(ep, buf, len, timeoutMs)
    if (result < 0) {
        Thread.sleep(50)
        result = bulkTransfer(ep, buf, len, timeoutMs)
        if (result < 0) {
            Thread.sleep(100)
            result = bulkTransfer(ep, buf, len, timeoutMs)
            if (result < 0) {
                Timber.e("[$tag] bulkIn FAILED 3x n=$result sessionId=$sessionId")
                return result
            }
        }
    }
    return result
}

/**
 * Zero-length packet (ZLP) helper for USB bulk transfers where packet size alignment is required.
 */
fun UsbDeviceConnection.sendZlp(
    ep:        UsbEndpoint,
    timeoutMs: Int        = 2000,
    sessionId: String     = "",
    tag:       String     = "USB",
): Int {
    return bulkOut(ep, ByteArray(0), 0, timeoutMs, sessionId, tag)
}

/**
 * Apple mode detection for USB devices.
 * Returns AppleMode.UNKNOWN if not an Apple device or unknown PID.
 */
fun UsbDevice.detectAppleMode(): DeviceMatrix.AppleMode {
    return DeviceMatrix.detectAppleMode(vendorId, productId)
}

fun UsbDevice.detectHydraProtocol(): DeviceMatrix.HydraProtocol? {
    return DeviceMatrix.detectHydraProtocol(vendorId, productId)
}

fun UsbDevice.detectMtkMode(): DeviceMatrix.MtkMode {
    return DeviceMatrix.detectMtkMode(vendorId, productId)
}

fun UsbDevice.isMtkDevice(): Boolean = vendorId == DeviceMatrix.MTK_VID

fun UsbDevice.getMtkChipFamily(): DeviceMatrix.MtkChipFamily {
    return DeviceMatrix.detectMtkChipFamily(productName)
}
