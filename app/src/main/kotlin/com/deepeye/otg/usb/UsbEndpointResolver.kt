package com.deepeye.otg.usb

import android.hardware.usb.*
import android.util.Log
import com.deepeye.otg.data.ConnectionMode
import android.os.Build

/**
 * Resolved endpoints for the current mode, with buffer and timeout tuning.
 */
data class ResolvedEndpoints(
    val interfaceIndex: Int,
    val usbInterface: UsbInterface,
    val bulkIn: UsbEndpoint?,
    val bulkOut: UsbEndpoint?,
    val interruptIn: UsbEndpoint?,
    val controlSupported: Boolean = true,  // always on ep 0
    val recommendedBufferSize: Int,
    val recommendedTimeout: Int
)

/**
 * Strategy-driven resolver for correct USB endpoint detection based on connect-mode.
 */
object UsbEndpointResolver {
    private const val TAG = "DeepEye-Resolv"

    fun resolve(
        device: UsbDevice,
        mode: ConnectionMode
    ): ResolvedEndpoints? {
        Log.d(TAG, "Resolving endpoints for mode=$mode on ${device.productName}")

        // Attempt all interfaces to find the best match for the connection mode
        for (ifIdx in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(ifIdx)
            val resolved = tryResolveInterface(usbInterface, ifIdx, mode)
            
            if (resolved != null) {
                Log.i(TAG, "SUCCESS resolved endpoints on IFT-$ifIdx: BULK-IN=${resolved.bulkIn != null}, BULK-OUT=${resolved.bulkOut != null}")
                return resolved
            }
        }

        Log.e(TAG, "FAILED to find suitable USB interface for $mode on ${device.productName} (Interfaces Checked: ${device.interfaceCount})")
        return null
    }

    private fun tryResolveInterface(
        usbInterface: UsbInterface,
        ifIdx: Int,
        mode: ConnectionMode
    ): ResolvedEndpoints? {
        if (usbInterface.endpointCount == 0) return null

        var bulkIn: UsbEndpoint? = null
        var bulkOut: UsbEndpoint? = null
        var interruptIn: UsbEndpoint? = null

        for (epIdx in 0 until usbInterface.endpointCount) {
            val ep = usbInterface.getEndpoint(epIdx)
            when (ep.type) {
                UsbConstants.USB_ENDPOINT_XFER_BULK -> {
                    if (ep.direction == UsbConstants.USB_DIR_IN) bulkIn = ep
                    else bulkOut = ep
                }
                UsbConstants.USB_ENDPOINT_XFER_INT -> {
                    if (ep.direction == UsbConstants.USB_DIR_IN) interruptIn = ep
                }
            }
        }

        // Most mobile unlock protocols require at least one bulk endpoint
        // Qualcomm EDL, MTK BROM always use Bulk-In and Bulk-Out
        if (bulkIn == null && bulkOut == null) return null

        // Protocol-specific buffer/timeout tuning
        val (bufSize, timeout) = when (mode) {
            ConnectionMode.ADB       -> 4096 to 1000
            ConnectionMode.FASTBOOT  -> 64 to 3000   // Protocol spec defines 64B handshake
            ConnectionMode.EDL       -> 16384 to 10000 // Large flashing chunks, requires high timeout
            ConnectionMode.BROM      -> 512 to 5000  // MTK handshakes are small
            ConnectionMode.PRELOADER -> 512 to 5000
            ConnectionMode.DIAG      -> 2048 to 2000 // For AT-style DIAG commands
            ConnectionMode.MTP       -> 16384 to 5000
            ConnectionMode.META      -> 512 to 3000
            ConnectionMode.ISP       -> 16384 to 15000 // Slow eMMC-style access
            ConnectionMode.TESTPOINT -> 512 to 5000
            ConnectionMode.ODIN      -> 16384 to 10000 // Samsung flashing chunks
            ConnectionMode.FDL       -> 4096 to 3000 // UniSoc FDL packets
            ConnectionMode.UNKNOWN   -> 512 to 1000  // Minimal defaults for unknown detection
        }

        return ResolvedEndpoints(
            interfaceIndex = ifIdx,
            usbInterface = usbInterface,
            bulkIn = bulkIn,
            bulkOut = bulkOut,
            interruptIn = interruptIn,
            recommendedBufferSize = bufSize,
            recommendedTimeout = timeout
        )
    }

    fun validate(endpoints: ResolvedEndpoints, mode: ConnectionMode): Boolean {
        // Mode-specific validation. Some only need BULK-OUT or vice versa.
        val needsBulkIn = mode != ConnectionMode.TESTPOINT
        val needsBulkOut = true
        
        if (needsBulkIn && endpoints.bulkIn == null) {
            Log.w(TAG, "Validation Failed: Mode $mode requires BULK-IN endpoint")
            return false
        }
        if (needsBulkOut && endpoints.bulkOut == null) {
            Log.w(TAG, "Validation Failed: Mode $mode requires BULK-OUT endpoint")
            return false
        }
        return true
    }

    // Returns safest chunk size according to Android's memory handling limits per API
    fun maxChunkSize(): Int {
        return 16384 // 16KB is conservative and safe for across all versions
    }
}
