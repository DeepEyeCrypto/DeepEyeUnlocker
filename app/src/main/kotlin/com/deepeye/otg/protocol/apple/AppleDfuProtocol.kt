package com.deepeye.otg.protocol.apple

import android.util.Log
import com.deepeye.otg.usb.UsbTransport
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Apple DFU / Checkm8 Discovery Protocol (Stage 20.1).
 * Handles low-level DFU control transfers for A-series and M-series chips.
 */
object AppleDfuProtocol {
    private const val TAG = "AppleDfuProtocol"

    // DFU Standard Request Codes
    private const val DFU_DETACH = 0x00
    private const val DFU_DNLOAD = 0x01
    private const val DFU_UPLOAD = 0x02
    private const val DFU_GETSTATUS = 0x03
    private const val DFU_CLRSTATUS = 0x04
    private const val DFU_GETSTATE = 0x05
    private const val DFU_ABORT = 0x06

    /**
     * DFU status response structure.
     */
    data class DfuStatus(
        val status: Byte,
        val pollTimeout: Int,
        val state: Byte,
        val iString: Byte
    )

    /**
     * Fetches current DFU status via control transfer.
     */
    suspend fun getStatus(transport: UsbTransport): DfuStatus? {
        val buffer = ByteArray(6)
        val res = transport.controlTransfer(
            requestType = 0xA1, // DFU Class In
            request = DFU_GETSTATUS,
            value = 0,
            index = 0,
            buffer = buffer,
            length = 6,
            timeout = 1000
        )
        
        if (res.isSuccess) {
            return DfuStatus(
                status = buffer[0],
                pollTimeout = (buffer[1].toInt() and 0xFF) or 
                             ((buffer[2].toInt() and 0xFF) shl 8) or 
                             ((buffer[3].toInt() and 0xFF) shl 16),
                state = buffer[4],
                iString = buffer[5]
            )
        }
        return null
    }

    /**
     * Resets DFU state to IDLE.
     */
    suspend fun clearStatus(transport: UsbTransport): Boolean {
        return transport.controlTransfer(
            requestType = 0x21, // DFU Class Out
            request = DFU_CLRSTATUS,
            value = 0,
            index = 0,
            buffer = null,
            length = 0,
            timeout = 500
        ).isSuccess
    }

    /**
     * Aborts current DFU operation.
     */
    suspend fun abort(transport: UsbTransport): Boolean {
        return transport.controlTransfer(
            requestType = 0x21,
            request = DFU_ABORT,
            value = 0,
            index = 0,
            buffer = null,
            length = 0,
            timeout = 500
        ).isSuccess
    }

    /**
     * Apple-specific Detach Request (Stage 20.2).
     * Used in checkm8 to manipulate the USB stack.
     */
    suspend fun detach(transport: UsbTransport): Boolean {
        Log.i(TAG, "Sending kUSBDeviceRequestDetach...")
        return transport.controlTransfer(
            requestType = 0x21,
            request = DFU_DETACH,
            value = 0,
            index = 0,
            buffer = null,
            length = 0,
            timeout = 500
        ).isSuccess
    }

    /**
     * Standard handshake to confirm device is responsive to DFU commands.
     */
    suspend fun handshake(transport: UsbTransport): Boolean {
        Log.i(TAG, "Initiating Apple DFU handshake...")
        val status = getStatus(transport)
        if (status != null) {
            Log.i(TAG, "Apple DFU responsive. State: ${status.state}")
            return true
        }
        // Fallback to clear
        return clearStatus(transport)
    }
}
