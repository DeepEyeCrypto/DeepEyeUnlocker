package com.deepeye.otg.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.usb.*
import com.deepeye.otg.usb.SessionState
import com.deepeye.otg.ui.UsbUiState
import com.deepeye.otg.ui.toUiState
import com.deepeye.otg.ui.viewmodel.LogEntry
import com.deepeye.otg.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class UsbViewModel(
    private val context: Context,
    private val lifecycleManager: UsbLifecycleManager,
    private val settings: com.deepeye.otg.data.SettingsManager,
    val usbState: StateFlow<SessionState>,
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
    private val _selectedBrand = MutableStateFlow(0)
    val selectedBrand: StateFlow<Int> = _selectedBrand.asStateFlow()
    
    private val _selectedMode = MutableStateFlow(ConnectionMode.ADB)
    val selectedMode: StateFlow<ConnectionMode> = _selectedMode.asStateFlow()

    val lifecycleState: StateFlow<UsbLifecycleState> = lifecycleManager.state
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val usbUiState: StateFlow<UsbUiState> = lifecycleState
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UsbLifecycleState.Idle.toUiState())

    private val _domainSessionState = MutableStateFlow(com.deepeye.otg.domain.models.SessionState())
    val domainSessionState: StateFlow<com.deepeye.otg.domain.models.SessionState> = _domainSessionState.asStateFlow()

    // Deprecated legacy feature properties
    val activeBrandFeatures: StateFlow<BrandFeatureSet> = _selectedBrand
        .map { FeatureData.forBrand(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FeatureData.forBrand(0))

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

    private val _queueStatus = MutableStateFlow<SessionState>(SessionState.Idle)
    val queueState: StateFlow<SessionState> = _queueStatus.asStateFlow()

    // Internal mutable state for compatibility with foreground service
    private val _usbStateValue = MutableStateFlow<SessionState>(SessionState.Idle) 
    val activeUsbState: StateFlow<SessionState> = _usbStateValue.asStateFlow()

    init {
        checkOtgCapability()
        
        viewModelScope.launch {
            lifecycleState.collect { state ->
                handleLifecycleState(state)
                syncQueueState(state)
            }
        }
    }

    private fun syncQueueState(ls: UsbLifecycleState) {
        val current = _queueStatus.value
        when (ls) {
            is UsbLifecycleState.Idle -> {
                if (current !is SessionState.Error && current !is SessionState.OperationComplete) {
                     _queueStatus.value = SessionState.Idle
                }
            }
            is UsbLifecycleState.Connected -> {
                if (current is SessionState.WaitingForDevice || 
                    current is SessionState.DeviceFound || 
                    current is SessionState.PermissionPending) {
                    // Auto-start operation if we were waiting
                    val op = when(current) {
                        is SessionState.WaitingForDevice -> current.queuedOp
                        is SessionState.DeviceFound -> current.queuedOp
                        is SessionState.PermissionPending -> current.queuedOp
                        else -> null
                    }
                    if (op != null) {
                        startOperation(op)
                    } else {
                        _queueStatus.value = SessionState.ConnectedReady(ls.deviceName)
                    }
                } else {
                    _queueStatus.value = SessionState.ConnectedReady(ls.deviceName)
                }
            }
            is UsbLifecycleState.DeviceDetected -> {
                if (current is SessionState.WaitingForDevice) {
                    _queueStatus.value = SessionState.DeviceFound(current.queuedOp)
                }
            }
            is UsbLifecycleState.PermissionPending -> {
                if (current is SessionState.DeviceFound) {
                    _queueStatus.value = SessionState.PermissionPending(current.queuedOp)
                }
            }
            is UsbLifecycleState.Dead -> {
                 _queueStatus.value = SessionState.Error(ls.reason)
            }
            else -> Unit
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
                _usbStateValue.value = SessionState.ConnectedReady(state.deviceName)
                updateDiagnosticStep(5, DiagnosticStatus.Pass("Permission granted"))
                updateDiagnosticStep(6, DiagnosticStatus.Pass("Interface claimed"))
                updateDiagnosticStep(7, DiagnosticStatus.Pass("Endpoints resolved"))
                _statusMsg.value = "Connected: ${state.deviceName}"
            }
            is UsbLifecycleState.PermissionDenied -> {
                _usbStateValue.value = SessionState.Error("Permission denied")
                updateDiagnosticStep(5, DiagnosticStatus.Fail("Permission denied"))
                val current = _queueStatus.value
                if (current is SessionState.PermissionPending) {
                    _queueStatus.value = SessionState.PermissionDenied(current.queuedOp)
                }
            }
            is UsbLifecycleState.Dead -> {
                _usbStateValue.value = SessionState.Error(state.reason)
                updateDiagnosticStep(6, DiagnosticStatus.Fail(state.reason))
            }
            is UsbLifecycleState.Idle -> {
                _usbStateValue.value = SessionState.Idle
                resetDiagnostics()
                _statusMsg.value = "Disconnected"
            }
            is UsbLifecycleState.Error -> {
                _usbStateValue.value = SessionState.Error(state.message)
                _statusMsg.value = "Error: ${state.message}"
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
        _selectedBrand.value = index
    }

    private fun ConnectionMode.toSupportedMode(): SupportedMode = SupportedMode.valueOf(this.name)
    
    fun togglePerformance() {
        settings.togglePerformanceMode()
    }

    fun resetToIdle() {
        _queueStatus.value = SessionState.Idle
    }

    fun cancelWaiting() {
        _queueStatus.value = SessionState.Idle
    }

    fun queueOperation(featureId: String) {
        val feature = activeBrandFeatures.value.groups.flatMap { it.features }.find { it.id == featureId } ?: return
        val op = DeepEyeOperation.values().find { it.name.equals(feature.id, ignoreCase = true) } 
            ?: DeepEyeOperation.values().find { it.label.equals(feature.label, ignoreCase = true) }
            ?: DeepEyeOperation.DEEP_DEVICE_INFO // Fallback
            
        queueOperation(op)
    }

    fun queueOperation(op: DeepEyeOperation) {
        if (lifecycleState.value is UsbLifecycleState.Connected) {
            startOperation(op)
        } else {
            _queueStatus.value = SessionState.WaitingForDevice(op)
        }
    }

    private fun startOperation(op: DeepEyeOperation) {
        viewModelScope.launch {
            _queueStatus.value = SessionState.ExecutingOperation(op, 0, "Starting...")
            
            // Simulation of operation for now
            for (p in 10..100 step 10) {
                delay(300)
                if (_queueStatus.value is SessionState.ExecutingOperation) {
                    _queueStatus.value = SessionState.ExecutingOperation(op, p, "Processing ${op.label}...")
                } else {
                    return@launch // Cancelled or reset
                }
            }
            
            _queueStatus.value = SessionState.OperationComplete(op, true, "${op.label} completed successfully")
        }
    }

    override fun onCleared() {
        super.onCleared()
        lifecycleManager.destroy()
    }
}
