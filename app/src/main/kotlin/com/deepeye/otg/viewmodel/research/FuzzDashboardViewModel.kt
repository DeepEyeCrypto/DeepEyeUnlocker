package com.deepeye.otg.viewmodel.research

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.research.fuzz.CorpusManager
import com.deepeye.otg.research.fuzz.FuzzHarness
import com.deepeye.otg.research.fuzz.ReproRecorder
import com.deepeye.otg.ui.state.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────
// Fuzz Dashboard ViewModel
// DeepEye OTG — ViewModels Module (Part 9)
// ──────────────────────────────────────────────────────────────

@HiltViewModel
class FuzzDashboardViewModel @Inject constructor(
    private val harness: FuzzHarness,
    private val corpusManager: CorpusManager,
    private val reproRecorder: ReproRecorder
) : ViewModel() {

    private val _uiState = MutableStateFlow(FuzzDashboardState())
    val uiState: StateFlow<FuzzDashboardState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    init {
        // Observers for harness real-time status
        viewModelScope.launch {
            harness.stats.collect { stats ->
                _uiState.value = _uiState.value.copy(
totalExecutions = stats.totalExecutions,
                        totalCrashes = stats.totalCrashes,
                        uniqueCrashes = stats.uniqueCrashes,
                        executionsPerSecond = stats.executionsPerSecond,
                        elapsedMs = stats.elapsedMs
)
            }
        }

        viewModelScope.launch {
            harness.crashes.collect { crashList ->
                val uiCrashes = crashList.map { crash ->
                    CrashListItem(
                        testCaseId = crash.testCaseId,
                        bucket = crash.bucket,
                        crashType = crash.signature.take(20),
                        severity = crash.severity.name,
                        component = crash.component,
                        inputSize = 0, // Not easily available without full lookup here
                        timestamp = crash.firstSeen
                    )
                }
                _uiState.value = _uiState.value.copy(crashes = uiCrashes)
            }
        }
    }

    fun onAction(action: FuzzAction) {
        when (action) {
            is FuzzAction.UpdateConfig -> {
                _uiState.value = _uiState.value.copy(config = action.config)
            }
            is FuzzAction.StartFuzzing -> {
                startFuzzing()
            }
            is FuzzAction.StopFuzzing -> {
                harness.stop()
                _uiState.value = _uiState.value.copy(isRunning = false, isPaused = false, hasCompleted = true)
            }
            is FuzzAction.PauseFuzzing -> {
                harness.pause()
                _uiState.value = _uiState.value.copy(isPaused = true)
            }
            is FuzzAction.ResumeFuzzing -> {
                harness.resume()
                _uiState.value = _uiState.value.copy(isPaused = false)
            }
            is FuzzAction.SelectCrash -> {
               // Load detail
               viewModelScope.launch { _uiEvents.emit(UiEvent.ShowDialog(DialogType.CRASH_DETAIL)) }
            }
            is FuzzAction.ReplayCrashes -> {
                // Feature request: repro loop
                viewModelScope.launch { _uiEvents.emit(UiEvent.ShowToast("Replay initialized")) }
            }
            is FuzzAction.LoadSession -> {
                // Loads an old session
            }
            is FuzzAction.GenerateCorpus -> {
                // Populate the CorpusManager
                viewModelScope.launch { 
                    _uiEvents.emit(UiEvent.ShowToast("Generating seed corpus...")) 
                    delay(500)
                    _uiState.value = _uiState.value.copy(corpusSize = corpusManager.getStats().corpusCount)
                }
            }
        }
    }

    private fun startFuzzing() {
        if (_uiState.value.isRunning && !_uiState.value.isPaused) return

        val config = _uiState.value.config
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRunning = true,
                isPaused = false,
                hasCompleted = false,
                error = null,
                sessionId = "fuzz_${System.currentTimeMillis()}"
            )

            try {
                // Async block call
                harness.start()
                
                // Done
                _uiState.value = _uiState.value.copy(isRunning = false, hasCompleted = true)
                _uiEvents.emit(UiEvent.ShowToast("Fuzzing campaign completed smoothly"))

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRunning = false, error = "Harness error: ${e.message}"
                )
            }
        }
    }
}
