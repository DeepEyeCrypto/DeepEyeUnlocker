package com.deepeye.otg.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.UsbSessionState
import com.deepeye.otg.data.*
import com.deepeye.otg.ui.LogEntry
import com.deepeye.otg.usb.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UsbViewModel(
    private val context: Context,
    private val lifecycleManager: UsbLifecycleManager,
    private val settings: com.deepeye.otg.data.SettingsManager,
    val usbState: StateFlow<UsbSessionState>,
    val logs: StateFlow<List<LogEntry>>
) : ViewModel() {

    private val _statusMsg = MutableStateFlow("Disconnected")
    val statusMsg: StateFlow<String> = _statusMsg.asStateFlow()

    private val _otgResult = MutableStateFlow<OtgCapabilityResult?>(null)
    val otgResult: StateFlow<OtgCapabilityResult?> = _otgResult.asStateFlow()

    private val _diagnosticSteps = MutableStateFlow<Map<Int, DiagnosticStatus>>(emptyMap())
    val diagnosticSteps: StateFlow<Map<Int, DiagnosticStatus>> = _diagnosticSteps.asStateFlow()

    sealed class DiagnosticStatus {
        object Idle : DiagnosticStatus()
        object Loading : DiagnosticStatus()
        data class Pass(val msg: String) : DiagnosticStatus()
        data class Fail(val msg: String) : DiagnosticStatus()
    }

    val performanceMode: StateFlow<Boolean> = settings.performanceMode
    val selectedBrand = MutableStateFlow(0)
    
    private val _selectedMode = MutableStateFlow(ConnectionMode.ADB)
    val selectedMode: StateFlow<ConnectionMode> = _selectedMode.asStateFlow()

    val lifecycleState: StateFlow<UsbLifecycleState> = lifecycleManager.state
    val usbUiState: StateFlow<UsbUiState> = lifecycleState
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UsbLifecycleState.Idle.toUiState())

    // Dynamic feature set for current brand
    val activeBrandFeatures: StateFlow<BrandFeatureSet> = selectedBrand
        .map { FeatureData.forBrand(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FeatureData.forBrand(0))

    // List of feature IDs that are supported by the current ConnectionMode
    val availableFeatureIds: StateFlow<List<String>> = combine(activeBrandFeatures, selectedMode) { brandSet, mode ->
        val supportedMode = mode.toSupportedMode()
        brandSet.groups.flatMap { it.features }
            .filter { it.modes.contains(supportedMode) }
            .map { it.id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _logLines = MutableStateFlow<List<LogEntry>>(emptyList())
    val logLines: StateFlow<List<LogEntry>> = _logLines.asStateFlow()

    // Internal mutable state for compatibility with existing UI if needed
    private val _usbStateValue = MutableStateFlow<UsbSessionState>(UsbSessionState.Idle)
    val activeUsbState: StateFlow<UsbSessionState> = _usbStateValue.asStateFlow()

    init {
        checkOtgCapability()
        
        viewModelScope.launch {
            lifecycleState.collect { state ->
                handleLifecycleState(state)
            }
        }
    }

    private fun handleLifecycleState(state: UsbLifecycleState) {
        when (state) {
            is UsbLifecycleState.DeviceDetected -> {
                _selectedMode.value = state.detectedMode
                updateDiagnosticStep(3, DiagnosticStatus.Pass("Device recognized: ${state.brand}"))
                updateDiagnosticStep(4, DiagnosticStatus.Pass("Mode: ${state.detectedMode.name}"))
            }
            is UsbLifecycleState.Connected -> {
                _usbStateValue.value = UsbSessionState.ConnectedReady(state.deviceName)
                updateDiagnosticStep(5, DiagnosticStatus.Pass("Permission granted"))
                updateDiagnosticStep(6, DiagnosticStatus.Pass("Interface claimed"))
                updateDiagnosticStep(7, DiagnosticStatus.Pass("Endpoints resolved"))
                _statusMsg.value = "Connected: ${state.deviceName}"
            }
            is UsbLifecycleState.PermissionDenied -> {
                _usbStateValue.value = UsbSessionState.Error("Permission denied")
                updateDiagnosticStep(5, DiagnosticStatus.Fail("Permission denied"))
            }
            is UsbLifecycleState.Dead -> {
                _usbStateValue.value = UsbSessionState.Error(state.reason)
                updateDiagnosticStep(6, DiagnosticStatus.Fail(state.reason))
            }
            is UsbLifecycleState.Idle -> {
                _usbStateValue.value = UsbSessionState.Idle
                resetDiagnostics()
            }
            is UsbLifecycleState.Error -> {
                _usbStateValue.value = UsbSessionState.Error(state.message)
            }
            else -> Unit
        }
    }

    private fun checkOtgCapability() {
        viewModelScope.launch {
            val result = OtgCapabilityChecker.check(context)
            _otgResult.value = result
            if (result.hasOtgSupport) {
                updateDiagnosticStep(1, DiagnosticStatus.Pass("Host support OK"))
            } else {
                updateDiagnosticStep(1, DiagnosticStatus.Fail(result.recommendation))
            }
        }
    }

    private fun updateDiagnosticStep(step: Int, status: DiagnosticStatus) {
        val current = _diagnosticSteps.value.toMutableMap()
        current[step] = status
        _diagnosticSteps.value = current
    }

    private fun resetDiagnostics() {
        _diagnosticSteps.value = emptyMap()
        checkOtgCapability()
    }

    fun onModeSelected(mode: ConnectionMode) {
        _selectedMode.value = mode
    }

    fun onBrandSelected(index: Int) {
        selectedBrand.value = index
    }

    private fun ConnectionMode.toSupportedMode(): SupportedMode = SupportedMode.valueOf(this.name)
    
    fun togglePerformance() {
        settings.togglePerformanceMode()
    }

    fun queueOperation(feature: FeatureItem) {
        _statusMsg.value = "Running: ${feature.label}..."
        _progress.value = 10 
        viewModelScope.launch {
            for (p in 10..100 step 20) {
                delay(400)
                _progress.value = p
            }
            _statusMsg.value = "Complete: ${feature.label}"
        }
    }

    override fun onCleared() {
        super.onCleared()
        lifecycleManager.destroy()
    }
}
}
