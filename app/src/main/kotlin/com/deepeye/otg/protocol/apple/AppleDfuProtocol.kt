package com.deepeye.otg.protocol.apple

import android.util.Log
import com.deepeye.otg.usb.UsbTransport
import kotlinx.coroutines.delay
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
     * Sends a data block to the device (DNLOAD).
     */
    suspend fun download(transport: UsbTransport, blockNum: Int, data: ByteArray?): Boolean {
        return transport.controlTransfer(
            requestType = 0x21,
            request = DFU_DNLOAD,
            value = blockNum,
            index = 0,
            buffer = data,
            length = data?.size ?: 0,
            timeout = 5000
        ).isSuccess
    }

    /**
     * Receives a data block from the device (UPLOAD).
     */
    suspend fun upload(transport: UsbTransport, blockNum: Int, length: Int): ByteArray? {
        val buffer = ByteArray(length)
        val res = transport.controlTransfer(
            requestType = 0xA1,
            request = DFU_UPLOAD,
            value = blockNum,
            index = 0,
            buffer = buffer,
            length = length,
            timeout = 5000
        )
        return if (res.isSuccess) buffer else null
    }

    /**
     * Sends a complete binary payload using DFU DNLOAD blocks.
     * Handles fragmentation and state synchronization.
     */
    suspend fun sendPayload(
        transport: UsbTransport,
        payload: ByteArray,
        onProgress: (Int) -> Unit = {}
    ): Boolean {
        Log.i(TAG, "Sending payload of size ${payload.size}...")
        val blockSize = 2048 // Standard DFU block size
        val totalBlocks = (payload.size + blockSize - 1) / blockSize

        for (i in 0 until totalBlocks) {
            val offset = i * blockSize
            val length = minOf(blockSize, payload.size - offset)
            val block = payload.sliceArray(offset until (offset + length))

            if (!download(transport, i, block)) {
                Log.e(TAG, "Failed to send block $i")
                return false
            }

            // Wait for device to process block
            if (!pollUntilState(transport, STATE_DNLOAD_IDLE)) {
                Log.e(TAG, "Device failed to return to DNLOAD_IDLE after block $i")
                return false
            }

            onProgress((i + 1) * 100 / totalBlocks)
        }

        // Send Zero-length packet to trigger MANIFEST_SYNC
        download(transport, totalBlocks, null)
        pollUntilState(transport, STATE_MANIFEST_SYNC)
        
        return true
    }

    /**
     * Polls the device until it reaches a target state or errors out.
     * Essential for checkm8/pongoOS payload execution.
     */
    suspend fun pollUntilState(
        transport: UsbTransport, 
        targetState: Byte, 
        maxRetries: Int = 15
    ): Boolean {
        repeat(maxRetries) {
            val status = getStatus(transport) ?: return false
            if (status.state == targetState) return true
            if (status.state == STATE_ERROR) {
                Log.e(TAG, "DFU Error state: ${status.status}")
                clearStatus(transport)
                return false
            }
            
            // Wait for requested timeout (pollTimeout is in ms)
            delay(status.pollTimeout.toLong().coerceIn(10, 1000))
        }
        return false
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

    // DFU State Constants
    const val STATE_IDLE = 2.toByte()
    const val STATE_DNLOAD_IDLE = 5.toByte()
    const val STATE_MANIFEST_SYNC = 6.toByte()
    const val STATE_UPLOAD_IDLE = 9.toByte()
    const val STATE_ERROR = 10.toByte()
}
