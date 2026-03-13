package com.deepeye.otg.usb

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Top-level manager for ADB orchestration (Stage 7.4).
 * Refactored for AdbSession API changes.
 */
class AdbManager(
    private val lifecycleManager: UsbLifecycleManager
) {
    companion object {
        private const val TAG = "DeepEye-AdbMgr"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isBusy = MutableStateFlow(false)
    val isBusy = _isBusy.asStateFlow()

    private val _lastLog = MutableStateFlow<String?>(null)
    val lastLog = _lastLog.asStateFlow()

    fun runShellCommand(command: String, onResult: (String) -> Unit) {
        scope.launch {
            if (_isBusy.value) return@launch
            _isBusy.value = true
            _lastLog.value = "Executing: $command"

            try {
                val transport = lifecycleManager.getTransport()
                if (transport == null) {
                    _lastLog.value = "Error: Not connected to any USB device"
                    return@launch
                }

                val session = AdbSession(transport)
                val connected = session.connect()
                if (!connected) {
                    _lastLog.value = "Error: ADB Handshake failed"
                    return@launch
                }

                _lastLog.value = "Opening shell: $command"
                val localId = session.open("shell:$command")
                if (localId == null) {
                    _lastLog.value = "Error: Failed to open shell stream"
                    return@launch
                }

                _lastLog.value = "Waiting for output..."
                val output = StringBuilder()
                
                // Read until stream closes or timeout (simple loop for now)
                repeat(10) {
                    val part = session.readString(localId)
                    if (part != null) {
                        output.append(part)
                    } else {
                        // Stream likely closed or timed out
                    }
                }

                val finalOutput = output.toString().trim()
                _lastLog.value = "Output received: ${finalOutput.take(50)}..."
                onResult(finalOutput)
                
                // Clean up stream
                session.close(localId)

            } catch (e: Exception) {
                Log.e(TAG, "ADB Command failed", e)
                _lastLog.value = "Exception: ${e.message}"
            } finally {
                _isBusy.value = false
            }
        }
    }
}
