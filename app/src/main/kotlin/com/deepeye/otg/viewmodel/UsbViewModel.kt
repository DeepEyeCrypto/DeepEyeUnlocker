package com.deepeye.otg.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.usb.*
import com.deepeye.otg.usb.SessionState
import com.deepeye.otg.service.LicenseManager
import com.deepeye.otg.service.UpdateManager
import com.deepeye.otg.domain.models.LicenseStatus
import com.deepeye.otg.ui.UsbUiState
import com.deepeye.otg.ui.toUiState
import com.deepeye.otg.ui.viewmodel.LogEntry
import com.deepeye.otg.data.*
import com.deepeye.otg.domain.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class UsbViewModel(
    private val context: Context,
    private val lifecycleManager: UsbLifecycleManager,
    private val settings: com.deepeye.otg.data.SettingsManager,
    val usbState: StateFlow<SessionState>,
    val logs: StateFlow<List<LogEntry>>
) : ViewModel() {

    private val defaultSessionState = com.deepeye.otg.domain.models.SessionState()

    private val _statusMsg = MutableStateFlow("Disconnected")
    val statusMsg: StateFlow<String> = _statusMsg.asStateFlow()

    private val _otgResult = MutableStateFlow<OtgCapabilityResult?>(null)
    val otgResult: StateFlow<OtgCapabilityResult?> = _otgResult.asStateFlow()

    private val _diagnosticSteps = MutableStateFlow<Map<Int, DiagnosticStatus>>(emptyMap())
    val diagnosticSteps: StateFlow<Map<Int, DiagnosticStatus>> = _diagnosticSteps.asStateFlow()

    val licenseStatus = LicenseManager.licenseState
    val currentLicense = LicenseManager.currentLicense

    private val _showActivation = MutableStateFlow(false)
    val showActivation = _showActivation.asStateFlow()

    fun setActivationVisibility(visible: Boolean) {
        _showActivation.value = visible
    }

    fun activateLicense(key: String) {
        viewModelScope.launch {
            LicenseManager.activate(key)
        }
    }

    sealed class DiagnosticStatus {
        object Idle : DiagnosticStatus()
        object Loading : DiagnosticStatus()
        data class Pass(val msg: String) : DiagnosticStatus()
        data class Fail(val msg: String) : DiagnosticStatus()
    }

    val performanceMode: StateFlow<Boolean> = settings.performanceMode

    val updateState = UpdateManager.updateState

    init {
        viewModelScope.launch {
            // Check for updates delayed so as not to block initial JNI load
            delay(3000)
            UpdateManager.checkForUpdates()
        }
    }

    fun launchUpdate() {
        val url = updateState.value?.downloadUrl ?: return
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
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

    val currentUserPolicyTier: StateFlow<PolicyTier> = LicenseManager.currentLicense
        .map { it?.tier ?: PolicyTier.SAFE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), PolicyTier.SAFE)

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

    private val _activeProtocolFamily = MutableStateFlow(ProtocolFamily.UNKNOWN)
    val activeProtocolFamily: StateFlow<ProtocolFamily> = _activeProtocolFamily.asStateFlow()

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
                updateDiagnosticStep(
                    4,
                    DiagnosticStatus.Pass(
                        "Mode: ${state.detectedDeviceMode.name} (${state.protocolFamily})"
                    )
                )

                _activeProtocolFamily.value = state.protocolFamily
                
                _domainSessionState.value = _domainSessionState.value.copy(
                    connected = true,
                    deviceName = state.device.productName ?: state.device.deviceName ?: "Unknown",
                    protocolFamily = state.protocolFamily,
                    deviceMode = state.detectedDeviceMode,
                    statusMessage = "Detected ${state.detectedDeviceMode}: ${state.detectionReason}"
                )
            }
            is UsbLifecycleState.Connected -> {
                _usbStateValue.value = SessionState.ConnectedReady(state.deviceName)
                updateDiagnosticStep(5, DiagnosticStatus.Pass("Permission granted"))
                updateDiagnosticStep(6, DiagnosticStatus.Pass("Interface claimed"))
                updateDiagnosticStep(
                    7,
                    DiagnosticStatus.Pass(
                        "Endpoints resolved (${state.confidence}% ${state.protocolFamily})"
                    )
                )
                _statusMsg.value = "Connected: ${state.deviceName}"

                _activeProtocolFamily.value = state.protocolFamily
                
                _domainSessionState.value = _domainSessionState.value.copy(
                    connected = true,
                    deviceName = state.deviceName,
                    protocolFamily = state.protocolFamily,
                    hasPermission = true,
                    deviceMode = state.detectedDeviceMode,
                    statusMessage = "Detected ${state.detectedDeviceMode}: ${state.detectionReason}"
                )
            }
            is UsbLifecycleState.PermissionDenied -> {
                _usbStateValue.value = SessionState.Error("Permission denied")
                updateDiagnosticStep(5, DiagnosticStatus.Fail("Permission denied"))
                val current = _queueStatus.value
                if (current is SessionState.PermissionPending) {
                    _queueStatus.value = SessionState.PermissionDenied(current.queuedOp)
                }
                _domainSessionState.value = _domainSessionState.value.copy(
                    hasPermission = false,
                    statusMessage = "Permission denied"
                )
            }
            is UsbLifecycleState.Dead -> {
                _usbStateValue.value = SessionState.Error(state.reason)
                updateDiagnosticStep(6, DiagnosticStatus.Fail(state.reason))
                _activeProtocolFamily.value = ProtocolFamily.UNKNOWN
                _domainSessionState.value = defaultSessionState.copy(
                    statusMessage = "Disconnected: ${state.reason}",
                    currentError = state.reason
                )
            }
            is UsbLifecycleState.Idle -> {
                _usbStateValue.value = SessionState.Idle
                resetDiagnostics()
                _statusMsg.value = "Disconnected"
                _activeProtocolFamily.value = ProtocolFamily.UNKNOWN
                _domainSessionState.value = defaultSessionState.copy(statusMessage = "No device connected")
            }
            is UsbLifecycleState.Error -> {
                _usbStateValue.value = SessionState.Error(state.message)
                _statusMsg.value = "Error: ${state.message}"
                _domainSessionState.value = _domainSessionState.value.copy(
                    currentError = state.message,
                    statusMessage = state.message
                )
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
        if (featureId == "op_test_harness") {
            enterTestHarness()
            return
        }
        val feature = activeBrandFeatures.value.groups.flatMap { it.features }.find { it.id == featureId } ?: return
        val op = DeepEyeOperation.values().find { it.id.equals(feature.id, ignoreCase = true) } 
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

    fun enterTestHarness() {
        _queueStatus.value = SessionState.TestHarness
    }

    fun exitTestHarness() {
        _queueStatus.value = SessionState.Idle
    }

    private fun startOperation(op: DeepEyeOperation) {
        viewModelScope.launch {
            _queueStatus.value = SessionState.ExecutingOperation(op, 0, "Initializing Engine...")
            
            // Re-check connect state
            val activeState = _usbStateValue.value
            if (activeState !is SessionState.ConnectedReady) {
                _queueStatus.value = SessionState.Error("Device not ready for operation")
                return@launch
            }

            val connection = lifecycleManager.getActiveConnection()
            val device = lifecycleManager.getActiveDevice()
            
            if (connection == null || device == null) {
                _queueStatus.value = SessionState.Error("Lost USB connection")
                return@launch
            }
            
            try {
                val fd = connection.fileDescriptor
                val protocolFamily = when (val detected = _activeProtocolFamily.value) {
                    ProtocolFamily.PRELOADER -> ProtocolFamily.BROM
                    ProtocolFamily.MTK -> ProtocolFamily.BROM
                    ProtocolFamily.SAMSUNG -> ProtocolFamily.ODIN
                    ProtocolFamily.QC -> ProtocolFamily.EDL
                    ProtocolFamily.UNKNOWN -> _domainSessionState.value.protocolFamily
                    else -> detected
                }

                if (protocolFamily == ProtocolFamily.UNKNOWN) {
                    _queueStatus.value = SessionState.Error("Cannot execute: protocol family is UNKNOWN")
                    addLog("ERROR", "Blocked operation: UNKNOWN protocol family")
                    return@launch
                }

                val result = com.deepeye.otg.engine.EngineDispatcher.execute(
                    op = op,
                    device = device,
                    protocol = protocolFamily,
                    fd = fd,
                    onProgress = { pct, msg ->
                        if (_queueStatus.value is SessionState.ExecutingOperation) {
                            _queueStatus.value = SessionState.ExecutingOperation(op, pct, msg)
                            addLog("INFO", "[$pct%] $msg")
                        }
                    }
                )

                if (result.success) {
                    _queueStatus.value = SessionState.OperationComplete(op, true, result.message)
                    addLog("SUCCESS", result.message)
                } else {
                    _queueStatus.value = SessionState.Error(result.message)
                    addLog("ERROR", result.message)
                }
            } catch (e: Exception) {
                _queueStatus.value = SessionState.Error("Execution failed: ${e.message}")
                addLog("ERROR", "Execution failed: ${e.message}")
            }
        }
    }

    private fun addLog(type: String, msg: String) {
        val list = _logLines.value.toMutableList()
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        list.add(LogEntry(ts, type, msg))
        _logLines.value = list
    }

    override fun onCleared() {
        super.onCleared()
        lifecycleManager.destroy()
    }
}
