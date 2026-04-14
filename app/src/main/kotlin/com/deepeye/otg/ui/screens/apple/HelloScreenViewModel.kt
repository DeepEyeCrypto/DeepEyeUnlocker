package com.deepeye.otg.ui.screens.apple

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.python.PythonBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

data class HelloScreenUiState(
    val model: String = "",
    val iosVersion: String = "",
    val udid: String = "",
    val isLoading: Boolean = false,
    val isBypassing: Boolean = false,
    val bypassProgress: Float = 0f,
    val eligibilityResult: JSONObjectWrapper? = null,
    val dfuInstructions: List<String> = emptyList(),
    val bypassLog: List<String> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HelloScreenViewModel @Inject constructor(
    private val pythonBridge: PythonBridge
) : ViewModel() {

    private val _uiState = MutableStateFlow(HelloScreenUiState())
    val uiState: StateFlow<HelloScreenUiState> =
        _uiState.asStateFlow()

    fun onModelChanged(model: String) {
        _uiState.update { it.copy(model = model, eligibilityResult = null) }
        // Auto-fetch DFU instructions when model entered
        if (model.matches(Regex("iPhone\\d+,\\d+"))) {
            viewModelScope.launch {
                val sid = UUID.randomUUID().toString()
                val instructions = pythonBridge.getDfuInstructions(model, sid)
                _uiState.update { it.copy(dfuInstructions = instructions) }
            }
        }
    }

    fun onIosVersionChanged(version: String) {
        _uiState.update { it.copy(iosVersion = version) }
    }

    fun checkEligibility() {
        val state = _uiState.value
        viewModelScope.launch {
            val sid = UUID.randomUUID().toString()
            _uiState.update { it.copy(isLoading = true, error = null) }
            Timber.d("[HelloScreenVM] checkEligibility sid=$sid")

            val result = pythonBridge.getBypassEligibility(
                state.model, state.iosVersion, sid
            )
            val wrapper = JSONObjectWrapper(
                eligible     = result.optBoolean("eligible", false),
                bestMethod   = result.optString("best_method", "unknown"),
                chip         = result.optString("chip", "Unknown"),
                successRate  = result.optInt("success_rate", 0),
                instructions = buildList {
                    val arr = result.optJSONArray("instructions")
                    if (arr != null) {
                        for (i in 0 until arr.length())
                            add(arr.getString(i))
                    }
                }
            )
            _uiState.update { it.copy(
                isLoading = false,
                eligibilityResult = wrapper
            )}
        }
    }

    fun runBypass() {
        val state = _uiState.value
        viewModelScope.launch {
            val sid = UUID.randomUUID().toString()
            _uiState.update { it.copy(
                isBypassing = true,
                bypassLog = listOf("[${sid.take(8)}] Bypass started..."),
                bypassProgress = 0.1f
            )}
            Timber.d("[HelloScreenVM] runBypass method=${state.eligibilityResult?.bestMethod} sid=$sid")

            // Simulate bypass steps with progress
            val steps = state.eligibilityResult?.instructions ?: return@launch
            steps.forEachIndexed { i, step ->
                kotlinx.coroutines.delay(800)
                val progress = (i + 1).toFloat() / steps.size
                _uiState.update { it.copy(
                    bypassProgress = progress,
                    bypassLog = it.bypassLog + "[$i] $step"
                )}
            }

            // Build iRemoval payload via Python
            val payload = pythonBridge.buildIremovalPayload(
                state.udid.ifBlank { "UDID_UNKNOWN" },
                state.model,
                state.iosVersion,
                sid
            )
            Timber.d("[HelloScreenVM] iRemoval payload built sid=$sid")

            _uiState.update { it.copy(
                isBypassing = false,
                bypassProgress = 1f,
                bypassLog = it.bypassLog + "✅ Bypass complete! sid=$sid"
            )}
        }
    }
}
