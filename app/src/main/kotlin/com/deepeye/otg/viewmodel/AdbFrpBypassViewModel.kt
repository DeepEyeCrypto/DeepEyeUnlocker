package com.deepeye.otg.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.engine.AdbFrpBypassEngine
import com.deepeye.otg.engine.BypassResult
import com.deepeye.otg.engine.CommandResult
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
    val lastResult: BypassResult? = null,
    val log: String = "",
    val deviceModel: String = "Unknown",
    val connectionType: String = "MTP"
)

data class AdbBypassMethod(
    val id: String,
    val name: String,
    val description: String,
    val risk: Risk,
    val requiresRoot: Boolean,
    val successRate: String
)

enum class Risk { LOW, MEDIUM, HIGH }

@HiltViewModel
class AdbFrpBypassViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AdbFrpBypassUiState())
    val uiState: StateFlow<AdbFrpBypassUiState> = _uiState.asStateFlow()

    // ADB executor function - inject your ADB connection here
    private lateinit var adbExecutor: (String) -> Result<String>

    fun setAdbExecutor(executor: (String) -> Result<String>) {
        adbExecutor = executor
    }

    private val engine by lazy { AdbFrpBypassEngine(adbExecutor) }

    // Methods list for UI
    val bypassMethods = listOf(
        AdbBypassMethod(
            id = "method1",
            name = "Settings DB Clear",
            description = "Clears FRP flags via Settings provider",
            risk = Risk.LOW,
            requiresRoot = false,
            successRate = "~70%"
        ),
        AdbBypassMethod(
            id = "method2",
            name = "Package Manager",
            description = "Disables setup wizard & clears data",
            risk = Risk.MEDIUM,
            requiresRoot = false,
            successRate = "~80%"
        ),
        AdbBypassMethod(
            id = "method3",
            name = "Realme Specific",
            description = "Realme RMX series FRP partition clear",
            risk = Risk.MEDIUM,
            requiresRoot = true,
            successRate = "~85%"
        ),
        AdbBypassMethod(
            id = "method4",
            name = "FRP Database",
            description = "Removes Google accounts from system DB",
            risk = Risk.HIGH,
            requiresRoot = true,
            successRate = "~90%"
        ),
        AdbBypassMethod(
            id = "method5",
            name = "Account Manager",
            description = "Clears all Google accounts via AccountManager",
            risk = Risk.MEDIUM,
            requiresRoot = false,
            successRate = "~75%"
        ),
        AdbBypassMethod(
            id = "method6",
            name = "Activity Manager",
            description = "Forces launcher & disables setup wizard",
            risk = Risk.LOW,
            requiresRoot = false,
            successRate = "~65%"
        )
    )

    fun runMethod(methodId: String) {
        if (!::adbExecutor.isInitialized) {
            _uiState.update { it.copy(
                log = it.log + "\n❌ ERROR: ADB executor not initialized!\n"
            )}
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(
                isRunning = true,
                currentMethod = methodId,
                log = it.log + "\n▶ Starting $methodId...\n"
            )}

            val result = when (methodId) {
                "method1" -> engine.method1_clearSettingsDb()
                "method2" -> engine.method2_packageManagerBypass()
                "method3" -> engine.method3_realmeSpecific()
                "method4" -> engine.method4_frpDatabaseClear()
                "method5" -> engine.method5_accountManagerClear()
                "method6" -> engine.method6_activityManagerBypass()
                else -> null
            }

            result?.let { r ->
                val log = buildString {
                    appendLine("━━━━━━━━━━━━━━━━━━━━")
                    appendLine("Method: ${r.method}")
                    appendLine("Status: ${if (r.success) "✅ SUCCESS" else "❌ FAILED"}")
                    appendLine("Success Rate: ${(r.successRate * 100).toInt()}%")
                    appendLine("━━━━━━━━━━━━━━━━━━━━")
                    r.commands.forEach { cmd ->
                        appendLine("${if (cmd.success) "✓" else "✗"} ${cmd.command}")
                        appendLine("  > ${cmd.output.take(100)}")
                    }
                    appendLine("━━━━━━━━━━━━━━━━━━━━\n")
                }
                _uiState.update { it.copy(
                    isRunning = false,
                    currentMethod = null,
                    lastResult = r,
                    log = it.log + log
                )}
            }
        }
    }

    fun runAllMethods() {
        if (!::adbExecutor.isInitialized) {
            _uiState.update { it.copy(
                log = it.log + "\n❌ ERROR: ADB executor not initialized!\n"
            )}
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(
                isRunning = true,
                log = "▶ Starting ALL methods sequentially...\n\n"
            )}

            listOf("method1", "method2", "method3", "method4", "method5", "method6")
                .forEach { methodId ->
                    if (_uiState.value.isRunning) {
                        runMethod(methodId)
                        kotlinx.coroutines.delay(1000)
                    }
                }

            _uiState.update { it.copy(
                isRunning = false,
                log = it.log + "\n✅ All methods completed!\n"
            )}
        }
    }

    fun setDeviceModel(model: String) {
        _uiState.update { it.copy(deviceModel = model) }
    }

    fun clearLog() {
        _uiState.update { it.copy(log = "") }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("[AdbFrpBypassViewModel] Cleared")
    }
}
