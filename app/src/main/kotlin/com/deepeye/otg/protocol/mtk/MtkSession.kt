package com.deepeye.otg.protocol.mtk

import android.util.Log
import com.deepeye.otg.usb.UsbTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-level session manager for MTK devices (BROM/Preloader).
 */
class MtkSession(private val transport: UsbTransport) {
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    /**
     * Attempts to establish a BROM-level handshake.
     */
    suspend fun connect(): Boolean {
        Log.i("MtkSession", "Attempting BROM connection...")
        if (MtkBromProtocol.handshake(transport)) {
            _isConnected.value = true
            Log.i("MtkSession", "BROM Connection Established")
            return true
        }
        return false
    }

    /**
     * Reads the hardware code from the chipset.
     */
    suspend fun getHwCode(): Int? {
        if (!_isConnected.value) return null
        return MtkBromProtocol.readHwCode(transport)
    }

    /**
     * Executes DA injection sequence (Stage 4.2).
     * @param address SRAM address for DA execution (default 0x400800 for many MTK)
     */
    suspend fun loadDownloadAgent(daBytes: ByteArray, address: Long = 0x400800L): Boolean {
        if (!_isConnected.value) return false
        
        Log.i("MtkSession", "Starting DA Injection Chain...")
        
        // 1. Send DA to SRAM
        val sendDaResult = MtkBromProtocol.sendDa(transport, address, daBytes)
        if (sendDaResult.isFailure) {
            Log.e("MtkSession", "SRAM injection failed")
            return false
        }

        // 2. Verify DA checksum before execution jump
        val checksumResult = MtkBromProtocol.verifyDaChecksum(transport, daBytes, sessionId = "mtk-session")
        if (checksumResult.isFailure) {
            Log.e("MtkSession", "DA checksum verification failed", checksumResult.exceptionOrNull())
            return false
        }
        
        // 3. Transmit JUMP command only after checksum verification
        if (!MtkBromProtocol.jumpDa(transport, address)) {
            Log.e("MtkSession", "Execution jump failed")
            return false
        }
        
        Log.i("MtkSession", "DA Injection SUCCESS - Handing off to DA Protocol")
        return true
    }

    /**
     * Closes the session.
     */
    fun close() {
        _isConnected.value = false
        // Low-level BROM doesn't have an explicit 'close' usually, it just times out or jumps.
    }
}
