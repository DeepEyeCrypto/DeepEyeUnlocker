package com.deepeye.otg.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.NativeBridge
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
import org.json.JSONObject

class UsbViewModel(
    private val appContext: Context,
    private val lifecycleManager: UsbLifecycleManager,
    private val settings: com.deepeye.otg.data.SettingsManager
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

    val performanceMode = settings.performanceMode
    val adbSignatureRequired = settings.adbSignatureRequired
    val debounceAttach = settings.debounceAttach
    val permissionTimeout = settings.permissionTimeout
    val showDebugPanel = settings.showDebugPanel
    val showDetectionReason = settings.showDetectionReason
    val monospaceHex = settings.monospaceHex
    val forceReclassify = settings.forceReclassify
    val logUsbToFile = settings.logUsbToFile

    fun togglePerformance() = settings.togglePerformanceMode()
    fun toggleAdbSignature() = settings.toggleAdbSignature()
    fun toggleDebounceAttach() = settings.toggleDebounceAttach()
    fun toggleDebugPanel() = settings.toggleShowDebugPanel()
    fun toggleShowDetectionReason() = settings.toggleShowDetectionReason()
    fun toggleMonospaceHex() = settings.toggleMonospaceHex()
    fun toggleForceReclassify() = settings.toggleForceReclassify()
    fun toggleLogUsbToFile() = settings.toggleLogUsbToFile()
    fun setPermissionTimeout(seconds: Int) = settings.setPermissionTimeout(seconds)

    private val _currentNav = MutableStateFlow(com.deepeye.otg.ui.screens.NavTarget.HOME)
    val currentNav = _currentNav.asStateFlow()
    fun setNav(target: com.deepeye.otg.ui.screens.NavTarget) { _currentNav.value = target }

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
        appContext.startActivity(intent)
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

    private val _deviceMetadata = MutableStateFlow<String?>(null)
    val deviceMetadata: StateFlow<String?> = _deviceMetadata.asStateFlow()

    init {
        // Collect lifecycle state to trigger deep scan on connect
        viewModelScope.launch {
            lifecycleState.collect { state ->
                if (state is UsbLifecycleState.Connected) {
                    performDeepIdentification(state)
                } else {
                    _deviceMetadata.value = null
                }
            }
        }
    }

    private fun performDeepIdentification(state: UsbLifecycleState.Connected) {
        viewModelScope.launch(Dispatchers.IO) {
            val conn = lifecycleManager.getActiveConnection() ?: return@launch
            val handle = NativeBridge.initCore(conn.fileDescriptor, state.vendorId, state.productId)
            if (handle != 0L) {
                try {
                    val info = NativeBridge.getDeviceInfo(handle)
                    _deviceMetadata.value = info
                    
                    try {
                        val json = JSONObject(info)
                        val modelName = json.optString("model", "Unknown")
                        addLog("INFO", "Deep Scan: Identified $modelName")
                    } catch (e: Exception) {}
                } catch (e: Exception) {
                    Log.e("UsbViewModel", "Deep identification failed: ${e.message}")
                } finally {
                    NativeBridge.closeCore(handle)
                }
            }
        }
    }

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


    private val _logLines = MutableStateFlow<List<LogEntry>>(emptyList())
    val logLines: StateFlow<List<LogEntry>> = _logLines.asStateFlow()
    val logs: StateFlow<List<LogEntry>> = _logLines.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

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
            val result = OtgCapabilityChecker.check(appContext)
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

    private fun ConnectionMode.toSupportedMode(): SupportedMode = try {
        SupportedMode.valueOf(this.name)
    } catch (e: Exception) {
        SupportedMode.UNKNOWN
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

    fun exportSessionReport() {
        val file = com.deepeye.otg.service.ReportManager.generateFinalJson(appContext)
        _queueStatus.value = SessionState.Reporting(file)
    }

    fun shareReport(file: java.io.File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            appContext,
            "com.deepeye.otg.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(intent, "Share Forensic Audit Report")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(chooser)
    }

    fun dismissReport() {
        _queueStatus.value = SessionState.Idle
    }

    fun runHardenValidation() {
        // Disabled: OtgTestHelper missing in this environment
        UsbLogger.info("DeepEye-Test", "Validation skipped")
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
                    context = appContext,
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
                    if (op == DeepEyeOperation.PARTITION_MANAGER) {
                        val csv = result.data["partitions"] ?: ""
                        val items = csv.split("|").filter { it.isNotBlank() }.map { s ->
                            val name = s.substringBefore(" (")
                            val size = s.substringAfter(" (", "").substringBefore(")")
                            com.deepeye.otg.domain.models.PartitionItem(s, name, size)
                        }
                        _queueStatus.value = SessionState.PartitionPreview(items)
                        addLog("SUCCESS", "Partition table loaded: ${items.size} entries")
                    } else {
                        _queueStatus.value = SessionState.OperationComplete(op, true, result.message)
                        addLog("SUCCESS", result.message)
                    }
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

    private val _hexPeekData = MutableStateFlow<String?>(null)
    val hexPeekData: StateFlow<String?> = _hexPeekData.asStateFlow()

    fun peekPartition(partitionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val conn = lifecycleManager.getActiveConnection() ?: return@launch
            val fd = conn.fileDescriptor
            val handle = NativeBridge.initCore(fd, 0, 0) // Basic transport
            if (handle != 0L) {
                try {
                    val hex = NativeBridge.peekPartition(handle, partitionName, 512)
                    _hexPeekData.value = hex
                    addLog("INFO", "Forensic Peek: $partitionName (512 bytes)")
                } catch (e: Exception) {
                    Log.e("UsbViewModel", "Hex peek failed: ${e.message}")
                } finally {
                    NativeBridge.closeCore(handle)
                }
            }
        }
    }

    fun closeHexPeek() { _hexPeekData.value = null }

    private fun addLog(type: String, msg: String) {
        val list = _logLines.value.toMutableList()
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        list.add(LogEntry(message = msg, type = type, timestamp = ts))
        _logLines.value = list
    }

    override fun onCleared() {
        super.onCleared()
    }
}
