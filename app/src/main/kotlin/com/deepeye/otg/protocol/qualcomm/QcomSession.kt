package com.deepeye.otg.protocol.qualcomm

import android.util.Log
import com.deepeye.otg.usb.UsbTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-level session manager for Qualcomm devices (EDL/Sahara).
 */
class QcomSession(private val transport: UsbTransport) {
    private val _isInSahara = MutableStateFlow(false)
    val isInSahara = _isInSahara.asStateFlow()

    /**
     * Attempts to establish a Sahara handshake.
     */
    suspend fun connect(): Boolean {
        Log.i("QcomSession", "Attempting Sahara handshake...")
        if (SaharaProtocol.handshake(transport)) {
            _isInSahara.value = true
            Log.i("QcomSession", "Sahara Session Active")
            return true
        }
        return false
    }

    /**
     * Terminates the Sahara session.
     */
    suspend fun close() {
        if (_isInSahara.value) {
            SaharaProtocol.done(transport)
            _isInSahara.value = false
        }
    }
}
