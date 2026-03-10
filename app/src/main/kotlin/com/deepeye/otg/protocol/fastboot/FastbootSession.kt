package com.deepeye.otg.protocol.fastboot

import android.util.Log
import com.deepeye.otg.usb.UsbTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-level session manager for Fastboot devices.
 */
class FastbootSession(private val transport: UsbTransport) {
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    /**
     * Attempts to verify communication.
     */
    suspend fun connect(): Boolean {
        Log.i("FastbootSession", "Attempting Fastboot connection...")
        val res = FastbootProtocol.executeCommand(transport, "getvar:version-bootloader")
        if (res.type == FastbootProtocol.ResponseType.OKAY) {
            _isConnected.value = true
            Log.i("FastbootSession", "Fastboot Handshake Established: ${res.message}")
            return true
        }
        return false
    }

    /**
     * Executes OEM Unlock sequence.
     */
    suspend fun unlockBootloader(): Boolean {
        if (!_isConnected.value) return false
        val res = FastbootProtocol.executeCommand(transport, "oem unlock")
        if (res.type == FastbootProtocol.ResponseType.FAIL) {
            // Some newer devices use 'flashing unlock'
            val flRes = FastbootProtocol.executeCommand(transport, "flashing unlock")
            return flRes.type == FastbootProtocol.ResponseType.OKAY
        }
        return res.type == FastbootProtocol.ResponseType.OKAY
    }

    /**
     * Reboots the device.
     */
    suspend fun reboot(): Boolean {
        val res = FastbootProtocol.executeCommand(transport, "reboot")
        return res.type == FastbootProtocol.ResponseType.OKAY
    }

    fun close() {
        _isConnected.value = false
    }
}
