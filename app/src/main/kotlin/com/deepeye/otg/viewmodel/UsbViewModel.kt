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
import com.deepeye.otg.domain.models.ProtocolFamily
import com.deepeye.otg.fuzz.hid.HidFuzzCoordinator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import timber.log.Timber
import com.deepeye.otg.intelligence.vulndb.*
import com.deepeye.otg.data.db.dao.FuzzDao

@dagger.hilt.android.lifecycle.HiltViewModel
class UsbViewModel @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: Context,
    private val lifecycleManager: UsbLifecycleManager,
    private val settings: com.deepeye.otg.data.SettingsManager,
    private val adbManager: AdbManager,
    private val hardwareManager: HardwareManager,
    private val repository: com.deepeye.otg.data.repository.ForensicRepository,
    private val forensicEngine: com.deepeye.otg.engine.ForensicEngine,
    private val aiAssistant: com.deepeye.otg.engine.ForensicAiAssistant,
    private val massExtractor: com.deepeye.otg.service.MassExtractor,
    private val sessionCoordinator: SessionCoordinator,
    private val hidFuzzer: com.deepeye.otg.fuzz.hid.HidFuzzCoordinator,
    private val cloudSyncService: com.deepeye.otg.service.CloudSyncService,
    private val licenseManager: com.deepeye.otg.service.LicenseManager,
    private val tunnelManager: com.deepeye.otg.service.TunnelManager,
    private val fuzzDao: FuzzDao,
    private val cveDao: com.deepeye.otg.intelligence.vulndb.CveDao,
    private val exploitOrchestrator: com.deepeye.otg.exploit.UniversalExploitOrchestrator,
    private val activationEngine: com.deepeye.otg.engine.ActivationEngine,
    private val mtkEngine: com.deepeye.otg.engine.MtkEngine,
    private val vaultManager: com.deepeye.otg.engine.CloudVaultManager
) : ViewModel() {

    val tunnelStatus = tunnelManager.status
    val tunnelCode = tunnelManager.sessionCode

    // Tunnel/Relay control methods
    fun startFleetSharing() {
        tunnelManager.startFleetSharing()
        Timber.i("[UsbViewModel] Fleet sharing started")
    }

    fun joinRemoteSession(code: String) {
        tunnelManager.joinSession(code)
        Timber.i("[UsbViewModel] Joining remote session: $code")
    }

    fun stopSharing() {
        tunnelManager.stopSharing()
        Timber.i("[UsbViewModel] Sharing stopped")
    }

    init {
        startHeartbeatLoop()
    }

    private fun startHeartbeatLoop() {
        viewModelScope.launch {
            // Wait for initial stability
            delay(10_000)
            while (isActive) {
                if (licenseManager.licenseState.value == com.deepeye.otg.domain.models.LicenseStatus.ACTIVE) {
                    licenseManager.performHeartbeat()
                }
                delay(30 * 60 * 1000L) // Every 30 minutes
            }
        }
    }

    private val defaultSessionState = com.deepeye.otg.domain.models.SessionState()

    private val _statusMsg = MutableStateFlow("Disconnected")
    val statusMsg: StateFlow<String> = sessionCoordinator.state.map { state ->
        when (state) {
            is ConnectionState.Idle -> "Disconnected"
            is ConnectionState.DeviceDetected -> "Device Detected"
            is ConnectionState.PermissionPending -> "Permission Pending"
            is ConnectionState.PermissionDenied -> "Permission Denied"
            is ConnectionState.Opening -> "Opening..."
            is ConnectionState.Open -> "Connected"
            is ConnectionState.Ready -> "Ready"
            is ConnectionState.Busy -> "Busy"
            is ConnectionState.Recovering -> "Recovering..."
            is ConnectionState.Disconnected -> "Disconnected"
            is ConnectionState.Failed -> "Error: ${state.errorCode}"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "Disconnected")

    val connectionState = sessionCoordinator.state

    val fuzzingActive = hidFuzzer.isFuzzing
    val fuzzingStats = hidFuzzer.fuzzStats

    fun startHidFuzzing(deviceKey: String) = hidFuzzer.startFuzzing(deviceKey)
    fun stopHidFuzzing() = hidFuzzer.stopFuzzing()

    val fuzzFindings = fuzzDao.getAllFindings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val exploitState = exploitOrchestrator.state

    fun runExploitChain(deviceKey: String) {
        viewModelScope.launch {
            val transport = lifecycleManager.getTransport(deviceKey) ?: return@launch
            exploitOrchestrator.autoExploit(transport, _exposureReport.value ?: return@launch)
        }
    }

    val aiAnalysis = aiAssistant.analysis
    val aiIsProcessing = aiAssistant.isProcessing
    val aiConfidence = aiAssistant.confidence

    private val _forensicStatus = forensicEngine.status
    val forensicStatus = _forensicStatus
    
    private val _forensicProgress = forensicEngine.progress
    val forensicProgress = _forensicProgress

    val hardwareStatus = hardwareManager.status

    private val _otgResult = MutableStateFlow<OtgCapabilityResult?>(null)
    val otgResult: StateFlow<OtgCapabilityResult?> = _otgResult.asStateFlow()

    private val _diagnosticSteps = MutableStateFlow<Map<Int, DiagnosticStatus>>(emptyMap())
    val diagnosticSteps: StateFlow<Map<Int, DiagnosticStatus>> = _diagnosticSteps.asStateFlow()

    val licenseStatus = licenseManager.licenseState
    val currentLicense = licenseManager.currentLicense

    private val _showActivation = MutableStateFlow(false)
    val showActivation = _showActivation.asStateFlow()

    val activationStatus = activationEngine.status
    val isActivationProcessing = activationEngine.isProcessing

    fun setActivationVisibility(visible: Boolean) {
        _showActivation.value = visible
    }

    fun activateLicense(key: String) {
        viewModelScope.launch {
            licenseManager.activate(key)
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
    val showDebugPanel: StateFlow<Boolean> = settings.showDebugPanel
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

    private val _currentNav = MutableStateFlow(com.deepeye.otg.ui.screens.NavTarget.DASHBOARD)
    val currentNav: StateFlow<com.deepeye.otg.ui.screens.NavTarget> = _currentNav.asStateFlow()
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
    val vaultStatus = vaultManager.syncStatus
    val sessions: StateFlow<Map<String, UsbLifecycleState>> = lifecycleManager.sessions
    
    private val _selectedDeviceKey = MutableStateFlow<String?>(null)
    val selectedDeviceKey = _selectedDeviceKey.asStateFlow()

    fun selectDevice(key: String) {
        _selectedDeviceKey.value = key
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _fileContentHex = MutableStateFlow<String?>(null)
    val fileContentHex = _fileContentHex.asStateFlow()

    val usbUiState: StateFlow<UsbUiState> = lifecycleState
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UsbLifecycleState.Idle.toUiState())

    private val _domainSessionState = MutableStateFlow(com.deepeye.otg.domain.models.SessionState())
    val domainSessionState: StateFlow<com.deepeye.otg.domain.models.SessionState> = _domainSessionState.asStateFlow()

    val currentUserPolicyTier: StateFlow<PolicyTier> = licenseManager.currentLicense
        .map { lic: DeepEyeLicense? -> lic?.tier ?: PolicyTier.SAFE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), PolicyTier.SAFE)

    private val _deviceMetadata = MutableStateFlow<String?>(null)
    val deviceMetadata: StateFlow<String?> = _deviceMetadata.asStateFlow()

    private val _globalInsights = MutableStateFlow<String>("")
    val globalInsights = _globalInsights.asStateFlow()

    private val _batchSelectedKeys = MutableStateFlow<Set<String>>(emptySet())
    val batchSelectedKeys = _batchSelectedKeys.asStateFlow()

    private val _exposureReport = MutableStateFlow<com.deepeye.otg.intelligence.vulndb.DevicePatchReport?>(null)
    val exposureReport = _exposureReport.asStateFlow()


    private var currentSessionId: Long? = null

    fun toggleBatchSelection(key: String) {
        val current = _batchSelectedKeys.value.toMutableSet()
        if (current.contains(key)) current.remove(key) else current.add(key)
        _batchSelectedKeys.value = current
    }

    fun clearBatchSelection() { _batchSelectedKeys.value = emptySet() }

    fun selectAllBatch() {
        _batchSelectedKeys.value = sessions.value.keys
    }

    private val _fleetHealth = MutableStateFlow<Map<String, String>>(emptyMap())
    val fleetHealth = _fleetHealth.asStateFlow()

    /**
     * Stage J - Environment Pre-flight.
     * Checks NDK availability, hardware descriptor integrity, and socket health.
     */
    fun runFleetHealthCheck() {
        viewModelScope.launch {
            val results = mutableMapOf<String, String>()
            results["JVM"] = System.getProperty("java.version") ?: "UNKNOWN"
            results["NDK"] = if (NativeBridge.calculateFileHash("") != "ERROR") "READY" else "FAULT"
            results["TUNNEL"] = if (tunnelManager.status.value != com.deepeye.otg.service.TunnelManager.TunnelStatus.FAILED) "NOMINAL" else "ERROR"
            
            _fleetHealth.value = results
            addLog("SECURITY", "Environmental Pre-flight complete: ${results["NDK"]}")
        }
    }

    fun performBatchOperation(op: String) {
        val targets = _batchSelectedKeys.value
        if (targets.isEmpty()) return
        
        addLog("BATCH", "Executing $op on ${targets.size} devices...")
        targets.forEach { key ->
            viewModelScope.launch {
                when (op) {
                    "IDENTIFY" -> hardwareManager.performMtkIdentification(key) { addLog("BATCH-$key", it) }
                    "SAHARA" -> hardwareManager.performQcomHandshake(key) { success -> 
                        addLog("BATCH-$key", "Sahara: ${if(success) "OK" else "FAIL"}") 
                    }
                    "EXTRACT" -> {
                        addLog("BATCH-$key", "Starting Massive Decrypted Pull...")
                        massExtractor.extractFromFleet(
                            setOf(key), 
                            listOf("/data/media/0/DCIM", "/data/system/users/0")
                        ) { node, msg -> addLog("BATCH-$node", msg) }
                        addLog("BATCH-$key", "Forensic extraction finished.")
                    }
                }
            }
        }
    }

    init {
        // Collect lifecycle state to trigger deep scan on connect
        viewModelScope.launch {
            lifecycleState.collect { state ->
                if (state is UsbLifecycleState.Connected) {
                    if (_selectedDeviceKey.value == null) {
                        _selectedDeviceKey.value = state.deviceKey
                    }
                    performDeepIdentification(state)
                } else if (state is UsbLifecycleState.Idle) {
                    _selectedDeviceKey.value = null
                    _deviceMetadata.value = null
                }
            }
        }

        // Global Situation Analysis (Stage 500.2)
        viewModelScope.launch {
            sessions.collect { currentSessions ->
                if (currentSessions.size > 1) {
                    val devices = currentSessions.values.map { 
                        (it as? UsbLifecycleState.Connected)?.deviceName ?: "Unknown" 
                    }
                    val protocols = currentSessions.values.map { 
                        (it as? UsbLifecycleState.Connected)?.protocolFamily ?: com.deepeye.otg.domain.models.ProtocolFamily.UNKNOWN 
                    }
                    aiAssistant.analyzeGlobalSituation(devices, protocols)
                }
            }
        }
    }

    val adbBusy = adbManager.isBusy
    val adbLog = adbManager.lastLog

    fun runAdbCommand(command: String) {
        adbManager.runShellCommand(command) { result ->
            addLog("ADB", result)
            viewModelScope.launch {
                currentSessionId?.let { id ->
                    repository.logOperation(id, "ADB_SHELL", command, "SUCCESS")
                }
            }
        }
    }

    fun performMtkIdentification() {
        hardwareManager.performMtkIdentification(_selectedDeviceKey.value) { result ->
            addLog("HW-MTK", result)
            viewModelScope.launch {
                currentSessionId?.let { id ->
                    repository.logOperation(id, "MTK_IDENT", "Identification request", if (result.contains("ERROR")) "FAILED" else "SUCCESS")
                }
            }
        }
    }

    fun performMtkDaInjection() {
        hardwareManager.performMtkDaInjection(_selectedDeviceKey.value) { success, msg ->
            addLog("HW-MTK-DA", msg)
        }
    }

    fun performQcomHandshake() {
        hardwareManager.performQcomHandshake(_selectedDeviceKey.value) { success ->
            val msg = if (success) "Sahara Handshake Success" else "Sahara Handshake Failed"
            addLog("HW-QC", msg)
            viewModelScope.launch {
                currentSessionId?.let { id ->
                    repository.logOperation(id, "QC_SAHARA", "Handshake attempt", if (success) "SUCCESS" else "FAILED")
                }
            }
        }
    }

    fun performFastbootUnlock() {
        hardwareManager.performFastbootUnlock(_selectedDeviceKey.value) { success ->
            addLog("HW-FASTBOOT", if (success) "Bootloader Unlock sequence accepted" else "Unlock sequence rejected")
        }
    }

    fun performAppleDfuHandshake() {
        hardwareManager.performAppleDfuHandshake(_selectedDeviceKey.value) { success ->
            addLog("HW-APPLE", if (success) "Apple DFU Handshake Success" else "Apple DFU Handshake Failed")
        }
    }

    private val _currentPath = MutableStateFlow("/")
    val currentPath = _currentPath.asStateFlow()

    private val _directoryFiles = MutableStateFlow<String>("[]")
    val directoryFiles = _directoryFiles.asStateFlow()

    fun browsePath(path: String) {
        viewModelScope.launch {
            val key = _selectedDeviceKey.value ?: return@launch
            val conn = lifecycleManager.getActiveConnection(key) ?: return@launch
            val dev = lifecycleManager.getActiveDevice(key) ?: return@launch

            _currentPath.value = path
            val handle = NativeBridge.initCore(conn.fileDescriptor, dev.vendorId, dev.productId)
            if (handle != 0L) {
                try {
                    val json = com.deepeye.otg.service.MtkFsDecryptor.listFolder(handle, path)
                    _directoryFiles.value = json
                } catch (e: Exception) {
                    addLog("FS", "Failed to list $path: ${e.message}")
                } finally {
                    NativeBridge.closeCore(handle)
                }
            }
        }
    }

    fun openFile(path: String) {
        viewModelScope.launch {
            val key = _selectedDeviceKey.value ?: return@launch
            val conn = lifecycleManager.getActiveConnection(key) ?: return@launch
            val device = lifecycleManager.getActiveDevice(key) ?: return@launch
            
            addLog("FS", "Opening Sector: $path")
            val handle = NativeBridge.initCore(conn.fileDescriptor, device.vendorId, device.productId)
            if (handle != 0L) {
                try {
                    val bytes = NativeBridge.fsReadFile(handle, "userdata", path)
                    if (bytes.isNotEmpty()) {
                        // Limit to first 4KB for preview
                        val preview = bytes.take(4096).toByteArray()
                        _fileContentHex.value = preview.joinToString(" ") { "%02X".format(it) }
                            .chunked(48).joinToString("\n")
                    }
                } finally {
                    NativeBridge.closeCore(handle)
                }
            }
        }
    }

    fun closeFilePreview() {
        _fileContentHex.value = null
    }

    fun performMtkFsDecryption() {
        viewModelScope.launch {
            val key = _selectedDeviceKey.value ?: return@launch
            val conn = lifecycleManager.getActiveConnection(key) ?: return@launch
            val device = lifecycleManager.getActiveDevice(key) ?: return@launch

            _statusMsg.value = "Decrypting MTK FS..."
            val handle = NativeBridge.initCore(conn.fileDescriptor, device.vendorId, device.productId)
            if (handle != 0L) {
                try {
                    val success = com.deepeye.otg.service.MtkFsDecryptor.decryptUserdata(handle)
                    if (success) {
                        addLog("DECRYPT", "MTK Real-time Decryption layer ACTIVE")
                        _statusMsg.value = "Userdata Accessible"
                    } else {
                        addLog("DECRYPT", "Failed to initialize MTK Decryption")
                    }
                } finally {
                    NativeBridge.closeCore(handle)
                }
            }
        }
    }

    fun performForensicAcquisition(partition: String) {
        viewModelScope.launch {
            val key = _selectedDeviceKey.value ?: return@launch
            val device = lifecycleManager.getActiveDevice(key) ?: return@launch
            val conn = lifecycleManager.getActiveConnection(key) ?: return@launch
            
            _queueStatus.value = SessionState.ExecutingOperation(
                DeepEyeOperation.FORENSIC_ACQUISITION, 
                0, 
                "Initializing Acquisition...", 
                device.productName ?: "Unknown"
            )

            val file = java.io.File(appContext.filesDir, "forensics/${partition}_${System.currentTimeMillis()}.bin")
            file.parentFile?.mkdirs()

            val handle = NativeBridge.initCore(conn.fileDescriptor, device.vendorId, device.productId)
            if (handle != 0L) {
                try {
                    val result = forensicEngine.acquirePartition(handle, partition, file) { p ->
                        _progress.value = (p * 100).toInt()
                    }
                    _queueStatus.value = SessionState.OperationComplete(DeepEyeOperation.FORENSIC_ACQUISITION, result.success, result.message)
                    addLog("FORENSIC", "Acquired $partition: ${result.sha256}")
                } finally {
                    NativeBridge.closeCore(handle)
                }
            } else {
                _queueStatus.value = SessionState.Error("Failed to init core for forensics")
            }
        }
    }

    fun performDeletedDataCarving(partition: String, types: List<String>) {
        viewModelScope.launch {
            val key = _selectedDeviceKey.value ?: return@launch
            val device = lifecycleManager.getActiveDevice(key) ?: return@launch
            val conn = lifecycleManager.getActiveConnection(key) ?: return@launch
            
            _queueStatus.value = SessionState.ExecutingOperation(
                DeepEyeOperation.DELETED_DATA_CARVING, 
                0, 
                "Scanning for signatures...", 
                device.productName ?: "Unknown"
            )

            val handle = NativeBridge.initCore(conn.fileDescriptor, device.vendorId, device.productId)
            if (handle != 0L) {
                try {
                    val json = forensicEngine.carveData(handle, partition, types)
                    _queueStatus.value = SessionState.OperationComplete(DeepEyeOperation.DELETED_DATA_CARVING, true, "Carving complete")
                    addLog("CARVE", "Results available in report")
                } finally {
                    NativeBridge.closeCore(handle)
                }
            }
        }
    }

    fun triggerAiAnalysis() {
        val chipset = (lifecycleState.value as? UsbLifecycleState.Connected)?.chipset ?: "Generic"
        val protocol = _activeProtocolFamily.value
        val metadata = _deviceMetadata.value
        
        viewModelScope.launch {
            aiAssistant.analyzeSession(chipset, protocol, metadata)
        }
    }

    fun performImeiRepair(imei1: String, imei2: String) {
        hardwareManager.performIdentityRepair(imei1, imei2, _selectedDeviceKey.value) { success, msg ->
            if (success) {
                addLog("REPAIR", msg)
                _queueStatus.value = SessionState.OperationComplete(DeepEyeOperation.IMEI_RESTORE, true, msg)
            } else {
                addLog("ERROR", "Repair failed: $msg")
                _queueStatus.value = SessionState.Error(msg)
            }
        }
    }

    private val _currentImei1 = MutableStateFlow("N/A")
    val currentImei1 = _currentImei1.asStateFlow()
    private val _currentImei2 = MutableStateFlow("N/A")
    val currentImei2 = _currentImei2.asStateFlow()

    private val _splitViewActive = MutableStateFlow(false)
    val splitViewActive = _splitViewActive.asStateFlow()

    fun toggleSplitView() {
        _splitViewActive.value = !_splitViewActive.value
    }

    fun performCloudSync() {
        viewModelScope.launch {
            val report = com.deepeye.otg.service.ReportManager.generateFleetReport(appContext, fuzzFindings.value)
            if (report != null) {
                val license = licenseManager.currentLicense.value
                if (license != null) {
                    addLog("SYNC", "Uploading audit trail to Cloud...")
                    val result = cloudSyncService.uploadVault(report, license.key) { px ->
                        _cloudSyncStatus.value = _cloudSyncStatus.value.copy(syncing = true, progress = px, fileName = report.name)
                    }
                    if (result.first) {
                        _cloudSyncStatus.value = _cloudSyncStatus.value.copy(syncing = false, result = "Sync Success", isError = false)
                        addLog("SYNC", "Audit trail synchronized successfully.")
                    } else {
                        _cloudSyncStatus.value = _cloudSyncStatus.value.copy(syncing = false, result = result.second, isError = true)
                        addLog("ERROR", "Cloud Sync failed: ${result.second}")
                    }
                } else {
                    addLog("ERROR", "No active license for cloud sync")
                }
            } else {
                addLog("ERROR", "No report available for sync")
            }
        }
    }

    fun generateForensicPdf() {
        viewModelScope.launch {
            val report = com.deepeye.otg.service.ReportManager.generateFleetReport(appContext, fuzzFindings.value)
            if (report != null) {
                addLog("REPORT", "Generating official Fleet Forensic documentation...")
                val pdf = com.deepeye.otg.service.ForensicReportGenerator.generatePdfReport(appContext, report.absolutePath)
                if (pdf != null) {
                    addLog("SUCCESS", "Forensic Audit Trail saved: ${pdf.name}")
                    _queueStatus.value = SessionState.Reporting(pdf)
                } else {
                    addLog("ERROR", "PDF rendering failed.")
                }
            } else {
                addLog("ERROR", "No fleet data available for report.")
            }
        }
    }

    fun readImei() {
        viewModelScope.launch {
            val key = _selectedDeviceKey.value ?: return@launch
            val conn = lifecycleManager.getActiveConnection(key) ?: return@launch
            val device = lifecycleManager.getActiveDevice(key) ?: return@launch
            addLog("REPAIR", "Reading persistent identity...")
            
            withContext(Dispatchers.IO) {
                val handle = com.deepeye.otg.NativeBridge.initCore(conn.fileDescriptor, device.vendorId, device.productId)
                if (handle != 0L) {
                    try {
                        val jsonStr = com.deepeye.otg.repair.NvBridge.readMtkImei(handle)
                        withContext(Dispatchers.Main) {
                            val json = JSONObject(jsonStr)
                            _currentImei1.value = json.optString("imei1", "N/A")
                            _currentImei2.value = json.optString("imei2", "N/A")
                            addLog("SUCCESS", "Identity synced from NVRAM")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            addLog("ERROR", "Failed to parse identity: ${e.message}")
                        }
                    } finally {
                        com.deepeye.otg.NativeBridge.closeCore(handle)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        addLog("ERROR", "Native core initialization failed.")
                    }
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
                        
                        // Automatically trigger AI analysis after deep identification
                        aiAssistant.analyzeSession(state.chipset, _activeProtocolFamily.value, info)

                        // Trigger Forensic Vulnerability Analysis
                        performVulnerabilityAnalysis(json)

                    } catch (e: Exception) {}
                } catch (e: Exception) {
                    Log.e("UsbViewModel", "Deep identification failed: ${e.message}")
                } finally {
                    NativeBridge.closeCore(handle)
                }
            }
        }
    }

    private fun performVulnerabilityAnalysis(json: JSONObject) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Extract telemetry
                val androidSpl = json.optString("android_spl", "")
                val qtiSpl = json.optString("qti_spl", "")
                val mtkSpl = json.optString("mtk_spl", "")
                val kernel = json.optString("kernel_version", "")
                val brand = json.optString("brand", "Unknown")
                val model = json.optString("model", "Unknown")

                // Fetch intelligence base
                val cves = cveDao.getAllSync()
                
                // Initialize forensic analyzer
                val analyzer = com.deepeye.otg.intelligence.vulndb.PatchStateAnalyzer(cveDao)
                
                // Execute assessment
                val observation = DeviceObservation(
                    brand = brand,
                    model = model,
                    androidSpl = androidSpl,
                    qtiSpl = qtiSpl.ifEmpty { null },
                    mtkSpl = mtkSpl.ifEmpty { null },
                    kernelVersion = kernel
                )
                val report = analyzer.analyze(observation)

                _exposureReport.value = report
                
                Timber.i("[INTEL] Vulnerability analysis complete. Risk: ${report.overallRiskLevel}")
                if (report.exposedCves.isNotEmpty()) {
                    addLog("SECURITY", "WARN: ${report.exposedCves.size} potential exposures detected. Risk: ${report.overallRiskLevel}")
                }
            } catch (e: Exception) {
                Timber.e(e, "[INTEL] Vulnerability analysis failed")
            }
        }
    }

    private fun triggerAutoExploit() {
        val currentReport = _exposureReport.value ?: return
        val currentKey = _selectedDeviceKey.value ?: return
        val session = lifecycleManager.getTransport(currentKey) ?: return
        
        viewModelScope.launch {
            addLog("SECURITY", "WARN: Starting automated compromise chain based on VulnIntel-AI...")
            val transport = session
            exploitOrchestrator.autoExploit(transport, currentReport)
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
    val queueStatus: StateFlow<SessionState> = _queueStatus.asStateFlow()
    val queueState: StateFlow<SessionState> = queueStatus

    private val _cloudSyncStatus = MutableStateFlow(CloudSyncStatus())
    val cloudSyncStatus = _cloudSyncStatus.asStateFlow()

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
                        when (op) {
                            DeepEyeOperation.CLEAR_LOGS -> {
                                _logLines.value = emptyList()
                            }
                            DeepEyeOperation.AUTO_EXPLOIT -> {
                                triggerAutoExploit()
                            }
                            else -> {
                                startOperation(op)
                            }
                        }
                    } else {
                        _queueStatus.value = SessionState.ConnectedReady(
                            deviceName = ls.deviceName,
                            brand = ls.brand,
                            chipset = ls.chipset,
                            secureBoot = ls.secureBootStatus
                        )
                    }
                } else {
                    _queueStatus.value = SessionState.ConnectedReady(
                        deviceName = ls.deviceName,
                        brand = ls.brand,
                        chipset = ls.chipset,
                        secureBoot = ls.secureBootStatus
                    )
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
                _usbStateValue.value = SessionState.ConnectedReady(
                    deviceName = state.deviceName,
                    brand = state.brand,
                    chipset = state.chipset,
                    secureBoot = state.secureBootStatus
                )
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
                    selectedBrand = state.brand,
                    chipset = state.chipset,
                    secureBoot = state.secureBootStatus,
                    protocolFamily = state.protocolFamily,
                    hasPermission = true,
                    deviceMode = state.detectedDeviceMode,
                    statusMessage = "Detected ${state.detectedDeviceMode}: ${state.detectionReason}"
                )

                // Initialize Forensic Audit for this device
                val deviceJson = JSONObject().apply {
                    put("model", state.deviceName)
                    put("vid", state.vendorId)
                    put("pid", state.productId)
                    put("chipset", state.chipset)
                }.toString()
                com.deepeye.otg.service.ReportManager.initDevice(state.deviceKey, deviceJson)

                // Stage 21: Persistent Registry
                viewModelScope.launch {
                    repository.recordDevice(
                        com.deepeye.otg.data.db.entities.DeviceEntity(
                            deviceKey = state.deviceKey,
                            vendorId = state.vendorId,
                            productId = state.productId,
                            manufacturer = state.brand,
                            model = state.deviceName,
                            firstDetectedAt = System.currentTimeMillis(),
                            lastDetectedAt = System.currentTimeMillis()
                        )
                    )
                    currentSessionId = repository.startSession(state.deviceKey, state.mode.name)
                    repository.logOperation(currentSessionId!!, "CONNECT", "Device connected via ${state.mode}", "SUCCESS")
                }
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
        if (featureId.startsWith("act_") || featureId.startsWith("fmi_") || 
            featureId.startsWith("jb_") || featureId.startsWith("adv_") || 
            featureId.startsWith("tool_")) {
            viewModelScope.launch {
                activationEngine.executeActivation(featureId)
            }
            return
        }
        
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
        val file = com.deepeye.otg.service.ReportManager.generateFleetReport(appContext, fuzzFindings.value)
        _queueStatus.value = SessionState.Reporting(file)
    }

    fun shareReport(file: java.io.File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            appContext,
            "com.deepeye.otg.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = if (file.extension == "deepvault") "application/octet-stream" else "application/json"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(intent, "Share Forensic Audit Report")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(chooser)
    }

    fun encryptToVault(file: java.io.File, password: String) {
        viewModelScope.launch {
            val vaultFile = com.deepeye.otg.service.VaultManager.encryptReport(file, password)
            if (vaultFile != null) {
                _queueStatus.value = SessionState.Reporting(vaultFile)
                addLog("SUCCESS", "Report encrypted to vault: ${vaultFile.name}")
            } else {
                addLog("ERROR", "Encryption failed")
            }
        }
    }

    fun syncToCloud(file: java.io.File) {
        val license = licenseManager.currentLicense.value
        if (license == null) {
            addLog("ERROR", "No active license for cloud sync")
            return
        }

        viewModelScope.launch {
            _cloudSyncStatus.value = CloudSyncStatus(syncing = true, fileName = file.name)
            val result = cloudSyncService.uploadVault(file, license.key) { px ->
                _cloudSyncStatus.value = _cloudSyncStatus.value.copy(progress = px)
            }
            
            if (result.first) {
                _cloudSyncStatus.value = _cloudSyncStatus.value.copy(syncing = false, result = "Sync Success", isError = false)
                addLog("SUCCESS", "Vault synchronized to cloud")
            } else {
                _cloudSyncStatus.value = _cloudSyncStatus.value.copy(syncing = false, result = result.second, isError = true)
                addLog("ERROR", "Cloud sync failed: ${result.second}")
            }
        }
    }

    fun dismissReport() {
        _queueStatus.value = SessionState.Idle
    }

    fun startRemoteTunnel() {
        tunnelManager.startFleetSharing()
    }

    fun stopRemoteTunnel() {
        tunnelManager.stopSharing()
    }

    fun runHardenValidation() {
        // Disabled: OtgTestHelper missing in this environment
        UsbLogger.info("DeepEye-Test", "Validation skipped")
    }

    private fun startOperation(op: DeepEyeOperation) {
        viewModelScope.launch {
            val deviceName = (lifecycleState.value as? UsbLifecycleState.Connected)?.deviceName ?: "Unknown"
            _queueStatus.value = SessionState.ExecutingOperation(op, 0, "Initializing Engine...", deviceName)
            
            if (op == DeepEyeOperation.BROWSE_FS) {
                browsePath("/")
                setNav(com.deepeye.otg.ui.screens.NavTarget.FILE_EXPLORER)
                _queueStatus.value = SessionState.Idle
                return@launch
            }

            if (op == DeepEyeOperation.SAFE_DUMP) {
                hardwareManager.performSafeDump("userdata", _selectedDeviceKey.value) { success, msg ->
                    _queueStatus.value = if (success) SessionState.OperationComplete(op, true, msg) else SessionState.Error(msg)
                    addLog(if (success) "SUCCESS" else "ERROR", msg)
                }
                return@launch
            }

            if (op == DeepEyeOperation.RAM_IMAGING) {
                hardwareManager.performRamImaging(_selectedDeviceKey.value) { success, msg ->
                    _queueStatus.value = if (success) SessionState.OperationComplete(op, true, msg) else SessionState.Error(msg)
                    addLog(if (success) "SUCCESS" else "ERROR", msg)
                }
                return@launch
            }

            if (op == DeepEyeOperation.AUTO_EXPLOIT) {
                triggerAutoExploit()
                return@launch
            }

            if (op == DeepEyeOperation.CLEAR_LOGS) {
                _logLines.value = emptyList()
                _queueStatus.value = SessionState.Idle
                return@launch
            }

            if (op == DeepEyeOperation.DELETED_DATA_CARVING) {
                hardwareManager.performDeletedDataCarving("userdata", _selectedDeviceKey.value) { success, msg ->
                    _queueStatus.value = if (success) SessionState.OperationComplete(op, true, msg) else SessionState.Error(msg)
                    addLog(if (success) "SUCCESS" else "ERROR", msg)
                }
                return@launch
            }

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
                    deviceKey = _selectedDeviceKey.value,
                    onProgress = { pct, msg ->
                        if (_queueStatus.value is SessionState.ExecutingOperation) {
                            _queueStatus.value = SessionState.ExecutingOperation(op, pct, msg)
                            addLog("INFO", "[$pct%] $msg")
                        }
                    }
                )

                if (result.success) {
                    com.deepeye.otg.service.ReportManager.logOperation(
                        deviceKey = _selectedDeviceKey.value, 
                        op = op, 
                        success = true, 
                        message = result.message
                    )
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
                    com.deepeye.otg.service.ReportManager.logOperation(
                        deviceKey = _selectedDeviceKey.value, 
                        op = op, 
                        success = false, 
                        message = result.message
                    )
                    _queueStatus.value = SessionState.Error(result.message)
                    addLog("ERROR", result.message)
                }
            } catch (e: Exception) {
                com.deepeye.otg.service.ReportManager.logOperation(
                    deviceKey = _selectedDeviceKey.value, 
                    op = op, 
                    success = false, 
                    message = e.message ?: "Unknown Error"
                )
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
