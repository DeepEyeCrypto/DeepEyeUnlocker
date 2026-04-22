package com.deepeye.otg.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.core.executor.CommandExecutor
import com.deepeye.otg.core.executor.ExecutionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AdbFrpBypassUiState(
    val isRunning: Boolean = false,
    val currentMethod: String? = null,
    val log: String = "",
    val deviceModel: String = "Unknown",
    val connectionType: String = "MTP"
)

@HiltViewModel
class AdbFrpBypassViewModel @Inject constructor(
    private val executor: CommandExecutor
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdbFrpBypassUiState())
    val uiState: StateFlow<AdbFrpBypassUiState> = _uiState.asStateFlow()

    fun runBypass(serial: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true, log = "") }
            
            addLog("🔍 Connecting to device/serial: $serial...")
            val connResult = executor.runAdb(serial, listOf("get-state"))

            when (connResult) {
                is ExecutionResult.Done -> {
                    if (!connResult.success) {
                        addLog("❌ Device not connected: ${connResult.output}")
                        _uiState.update { it.copy(isRunning = false) }
                        return@launch
                    }
                }
                is ExecutionResult.Error -> {
                    addLog("❌ Error: ${connResult.msg}")
                    _uiState.update { it.copy(isRunning = false) }
                    return@launch
                }
                is ExecutionResult.Timeout -> {
                    addLog("❌ ADB timeout error.")
                    _uiState.update { it.copy(isRunning = false) }
                    return@launch
                }
            }

            addLog("✅ Device connected")
            addLog("⚡ Running Bypass Commands...")

            val bypassResult = executor.runAdb(
                serial,
                listOf("shell", "content", "insert", "--uri", "content://settings/secure", "--bind", "name:s:user_setup_complete", "--bind", "value:s:1")
            )

            addLog(when (bypassResult) {
                is ExecutionResult.Done ->
                    if (bypassResult.success) "✅ FRP flag cleared successfully!" else "⚠️ Command failure: ${bypassResult.output}"
                else -> "❌ Command failed execution completely"
            })

            addLog("⚡ Launching Setup Exit Intent...")
            val exitResult = executor.runAdb(
                serial,
                listOf("shell", "am", "start", "-n", "com.google.android.setupwizard/.SetupWizardExitActivity")
            )
            
            addLog(when (exitResult) {
                is ExecutionResult.Done ->
                    if (exitResult.success) "✅ Process Exit Intent fired" else "⚠️ Exit intent info: ${exitResult.output}"
                else -> "❌ Exit Intent failed"
            })

            _uiState.update { it.copy(isRunning = false) }
        }
    }

    private fun addLog(message: String) {
        _uiState.update { it.copy(log = it.log + message + "\n") }
    }

    fun clearLog() {
        _uiState.update { it.copy(log = "") }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("[AdbFrpBypassViewModel] Cleared")
    }
}
