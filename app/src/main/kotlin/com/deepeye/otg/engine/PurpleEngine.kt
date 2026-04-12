package com.deepeye.otg.engine

import android.content.Context
import android.util.Log
import com.deepeye.otg.protocol.apple.AppleDfuProtocol
import com.deepeye.otg.usb.UsbLifecycleManager
import com.deepeye.otg.usb.UsbTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Orchestrates Purple Mode (Diagnostic/Serial Spoofing) operations.
 */
@Singleton
class PurpleEngine @Inject constructor(
    private val context: Context,
    private val usbLifecycleManager: UsbLifecycleManager
) {
    private val TAG = "PurpleEngine"

    private val _status = MutableStateFlow("Idle")
    val status = _status.asStateFlow()

    suspend fun enterPurpleMode(): Boolean {
        _status.value = "Initiating Purple Mode sequence..."
        val transport = getDfuTransport() ?: return false

        try {
            // 1. Handshake
            _status.value = "Connecting via DFU..."
            AppleDfuProtocol.handshake(transport)

            // 2. Send Serial Spoofing payload
            _status.value = "Sending Serial Spoofing payload..."
            val purplePayload = com.deepeye.otg.exploit.payloads.ApplePayloadProvider.getPurpleModePayload(context)
            if (!AppleDfuProtocol.sendPayload(transport, purplePayload) { _status.value = "Purple: $it%" }) {
                _status.value = "Payload delivery failed"
                return false
            }

            // 3. Finalize and enter Diag
            _status.value = "Entering Purple Mode..."
            AppleDfuProtocol.pollUntilState(transport, AppleDfuProtocol.STATE_MANIFEST_SYNC)

            _status.value = "Device is now in Purple Mode. Ready for Syscfg edits."
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Purple Mode entry failed", e)
            _status.value = "Error: ${e.message}"
            return false
        }
    }

    private fun getDfuTransport(): UsbTransport? {
        return usbLifecycleManager.getTransport()
    }
}
