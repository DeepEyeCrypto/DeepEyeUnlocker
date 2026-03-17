package com.deepeye.otg.engine

import android.util.Log
import com.deepeye.otg.protocol.apple.AppleDfuProtocol
import com.deepeye.otg.usb.UsbLifecycleManager
import com.deepeye.otg.usb.UsbTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Orchestrates Jailbreak operations (Checkra1n, Palera1n).
 */
@Singleton
class JailbreakEngine @Inject constructor(
    private val usbLifecycleManager: UsbLifecycleManager
) {
    private val TAG = "JailbreakEngine"
    
    private val _status = MutableStateFlow("Idle")
    val status = _status.asStateFlow()

    suspend fun runCheckra1n(): Boolean {
        _status.value = "Starting checkra1n sequence..."
        val transport = getDfuTransport() ?: return false
        
        try {
            // 1. Handshake
            if (!AppleDfuProtocol.handshake(transport)) {
                _status.value = "DFU Handshake failed"
                return false
            }

            // 2. Deliver checkm8 exploit (Stage 1)
            _status.value = "Sending checkm8 payload (Stage 1)..."
            val exploitPayload = com.deepeye.otg.exploit.payloads.ApplePayloadProvider.getCheckm8Stage1("t8010")
            if (!AppleDfuProtocol.download(transport, 0, exploitPayload)) {
                _status.value = "Exploit delivery failed"
                return false
            }

            // 3. Wait for Stage 1 processing
            AppleDfuProtocol.pollUntilState(transport, AppleDfuProtocol.STATE_DNLOAD_IDLE)

            // 4. Deliver PongoOS (Stage 2)
            _status.value = "Uploading PongoOS..."
            val pongoPayload = com.deepeye.otg.exploit.payloads.ApplePayloadProvider.getPongoOsPayload()
            if (!AppleDfuProtocol.sendPayload(transport, pongoPayload) { _status.value = "PongoOS: $it%" }) {
                _status.value = "PongoOS delivery failed"
                return false
            }

            _status.value = "Jailbreak sequence completed. Waiting for PongoOS shell..."
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Checkra1n failed", e)
            _status.value = "Error: ${e.message}"
            return false
        }
    }

    suspend fun runPalera1n(): Boolean {
        _status.value = "Starting palera1n sequence..."
        val transport = getDfuTransport() ?: return false

        try {
            // Palera1n starts with checkm8
            if (!runCheckra1n()) return false

            _status.value = "Injecting Palera1n rootless bootstrap..."
            val bootstrap = com.deepeye.otg.exploit.payloads.ApplePayloadProvider.getPalera1nBootstrap()
            if (!AppleDfuProtocol.sendPayload(transport, bootstrap) { _status.value = "Bootstrap: $it%" }) {
                _status.value = "Bootstrap delivery failed"
                return false
            }
            
            _status.value = "Palera1n bootstrap finished."
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Palera1n failed", e)
            _status.value = "Error: ${e.message}"
            return false
        }
    }

    private fun getDfuTransport(): UsbTransport? {
        return usbLifecycleManager.getTransport()
    }
}
