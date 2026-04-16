package com.deepeye.otg.ui.gsmg

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import timber.log.Timber
import com.deepeye.otg.data.ProtocolFamily           // Technical protocol (V6, EDL etc)
import com.deepeye.otg.data.UniversalProtocolDetector // The detector
import com.deepeye.otg.data.gsmg.*               // DeviceState, BypassFeature, etc.
import com.deepeye.otg.usb.UsbLifecycleManager
import com.deepeye.otg.usb.UsbLifecycleState
import dagger.hilt.android.lifecycle.HiltViewModel
import com.deepeye.otg.usb.UsbSessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Bypass UI State — holds the connection context and filtered features.
 */
data class BypassUiState(
    val device:             DeviceState?          = null,
    val displayedFeatures:  List<BypassFeature>   = emptyList(),
    val selectedPlatform:   DevicePlatform        = DevicePlatform.UNKNOWN,
    val filters:            FeatureFilters        = FeatureFilters(),
    val isExecuting:        Boolean               = false,
    val errorMessage:       String?               = null,
    val successMessage:     String?               = null,
    val latestEvent:        BypassEvent?          = null,
    val activeFeatureId:    String?               = null,
    val activePlan:         ExecutionPlan?        = null,
    val showPlanDialog:     Boolean               = false,
    val freeCount:          Int                   = 0,
    val signalCount:        Int                   = 0,
    val totalAvailable:     Int                   = 0,
    val recommendation:     RecommendationResult? = null,

    // Python-powered IMEI validation
    val imei: String = "",
    val imeiValid: Boolean = false,
    val imeiManufacturer: String = "",
    val imeiTac: String = "",

    // DA validation status
    val daStatus: String = "",

    // iOS activation payload
    val iosActivationPayload: String = "",
)

@HiltViewModel
class BypassViewModel @Inject constructor(
    private val bypassEngine:   UniversalBypassEngine,
    private val usbSessionMgr:  UsbSessionManager,
    private val usbLifecycleManager: UsbLifecycleManager,
    private val pythonBridge: com.deepeye.otg.python.PythonBridge,
) : ViewModel() {

    private val _state = MutableStateFlow(BypassUiState())
    val state: StateFlow<BypassUiState> = _state.asStateFlow()

    // High-assurance: persistent reference to active USB device
    private var _connectedUsbDevice: android.hardware.usb.UsbDevice? = null
    private var _lastSessionId: String? = null
    private var executionJob: Job? = null

    init {
        refreshFeatures()

        // THE BRIDGE: Automatically pick up device when UsbLifecycleManager connects
        viewModelScope.launch {
            usbLifecycleManager.sessions.collect { sessions ->
                val activeSession = sessions.values
                    .filterIsInstance<UsbLifecycleState.Connected>()
                    .firstOrNull()

                if (activeSession != null && activeSession.sessionId != _lastSessionId) {
                    _lastSessionId = activeSession.sessionId
                    val featureStr = activeSession.device?.productName?.takeIf { it.startsWith("hw_code:") }
                    onUsbDeviceConnected(activeSession.device!!, featureStr)
                }
            }
        }
    }

    /**
     * Called by UsbLifecycleManager / UsbPermissionHandler when device is ready.
     * This is the bridge that fixes [USB_REQUIRED] and [NOT_IMPLEMENTED] bugs.
     */
    fun onUsbDeviceConnected(
        usbDevice:  UsbDevice,
        featureStr: String? = null,
    ) {
        Timber.d("[BypassVM] onUsbDeviceConnected: ${usbDevice.deviceName} featureStr=$featureStr")
        _connectedUsbDevice = usbDevice

        // 1. Detect Protocol (TECHNICAL: V6, EDL, BROM etc)
        // Check interface for ADB/Fastboot detection if possible
        val iface = if (usbDevice.interfaceCount > 0) usbDevice.getInterface(0) else null
        val detection = UniversalProtocolDetector.detect(
            vid            = usbDevice.vendorId,
            pid            = usbDevice.productId,
            featureStr     = featureStr,
            ifaceClass     = iface?.interfaceClass ?: -1,
            ifaceSubclass  = iface?.interfaceSubclass ?: -1,
            ifaceProto     = iface?.interfaceProtocol ?: -1
        )

        // 2. Create DeviceState for UI
        val deviceState = DeviceState(
            sessionId    = java.util.UUID.randomUUID().toString(),
            ecid         = null, // read later
            imei         = null,
            serial       = usbDevice.serialNumber,
            chipName     = detection.chipEntry?.chipName ?: "Unknown Chip",
            chipRange    = detection.chipEntry?.protocol?.toChipRange() ?: ChipRange.ALL_ANDROID,
            iosVersion   = "0",
            buildNumber  = null,
            isJailbroken = false,
            fmiEnabled   = false,
            imeiPresent  = false,
            imeiValid    = false,
            isCdmaMeid   = false,
            activated    = false,
            mdmEnrolled  = false,
            dfuMode      = false,
            androidBrand = detection.brand,
            androidModel = usbDevice.productName,
            edlAvailable = detection.protocol == ProtocolFamily.QC_EDL,
        )

        // 3. Update State + Filter Catalog
        _state.update { it.copy(device = deviceState) }
        refreshFeatures(detection.protocol)
    }

    private fun refreshFeatures(protocol: ProtocolFamily? = null) {
        val all = UnifiedBypassRegistry.all
        Timber.d("[BypassVM] Total features in registry: ${all.size}")
        
        // If protocol is known, filter out incompatible crap (e.g. hide QC on MTK)
        val filtered = if (protocol != null) {
            filterFeaturesForDevice(all, protocol)
        } else {
            all
        }
        Timber.d("[BypassVM] After filtering: ${filtered.size} features (protocol=$protocol)")

        _state.update { it.copy(
            displayedFeatures = applyFilters(filtered, it.filters, it.selectedPlatform),
            totalAvailable    = filtered.size,
            freeCount         = filtered.count { feat -> feat.isFree },
            signalCount       = filtered.count { feat -> feat.signalAfter }
        )}
    }

    private fun filterFeaturesForDevice(
        all: List<BypassFeature>, 
        protocol: ProtocolFamily
    ): List<BypassFeature> {
        return all.filter { feature ->
            when (protocol) {
                ProtocolFamily.MTK_V6 -> {
                    // Only show MTK V6 or Generic MTK features
                    feature.chipRange == ChipRange.MTK_V6 || 
                    feature.chipRange == ChipRange.MTK_ALL
                }
                ProtocolFamily.QC_EDL -> {
                    // Only show Qualcomm features
                    feature.chipRange == ChipRange.QC_ALL
                }
                ProtocolFamily.IOS_DFU, ProtocolFamily.IOS_RECOVERY -> {
                    // Only show iOS features
                    feature.chipRange.name.startsWith("A")
                }
                else -> true // Fallback for ADB/Fastboot
            }
        }
    }

    // ── UI Actions ──────────────────────────────────────────

    fun onSelectPlatform(platform: DevicePlatform) {
        _state.update { it.copy(selectedPlatform = platform) }
        refreshFeatures()
    }

    fun onSearch(query: String) {
        _state.update { it.copy(filters = it.filters.copy(searchQuery = query)) }
        refreshFeatures()
    }

    fun onRequestExecute(feature: BypassFeature) {
        val deviceState = _state.value.device
        if (deviceState == null) {
            _state.update { it.copy(errorMessage = "Connect a device first — No USB target detected") }
            return
        }
        
        // Inject physical device handle into engine
        val plan = UnifiedBypassRegistry.buildPlan(
            feature   = feature,
            device    = deviceState,
            sessionId = java.util.UUID.randomUUID().toString()
        )
        _state.update { it.copy(activePlan = plan, showPlanDialog = true) }
    }

    fun onConfirmPlan() {
        val plan = _state.value.activePlan ?: return
        _state.update {
            it.copy(
                showPlanDialog = false,
                isExecuting = true,
                latestEvent = null,
                activeFeatureId = plan.feature.id,
                activePlan = null,
            )
        }
        
        executionJob?.cancel()
        executionJob = viewModelScope.launch {
            try {
                // THE FIX: Pass the actual UsbDevice object to the engine!
                val usbDevice = _connectedUsbDevice 
                    ?: usbSessionMgr.currentUsbDevice // fallback to static manager
                
                bypassEngine.execute(
                    feature = plan.feature,
                    device = plan.device,
                    usbDevice = usbDevice,
                    sessionId = plan.sessionId
                ).collect { event ->
                    _state.update { it.copy(latestEvent = event) }

                    when (event) {
                        is BypassEvent.Completed -> {
                            _state.update {
                                it.copy(
                                    isExecuting = false,
                                    successMessage = "Operation Successful",
                                    activeFeatureId = null,
                                )
                            }
                            delay(2500)
                            _state.update { it.copy(latestEvent = null) }
                        }

                        is BypassEvent.Failed -> {
                            _state.update {
                                it.copy(
                                    errorMessage = event.reason,
                                    isExecuting = false,
                                    activeFeatureId = null,
                                )
                            }
                            delay(2500)
                            _state.update { it.copy(latestEvent = null) }
                        }

                        else -> Unit
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        errorMessage = e.localizedMessage ?: "Unknown execution failure",
                        isExecuting = false,
                        activeFeatureId = null,
                    )
                }
            }
        }
    }

    fun cancelExecution() {
        executionJob?.cancel()
        _state.update {
            it.copy(
                isExecuting = false,
                latestEvent = null,
                activeFeatureId = null,
                activePlan = null,
            )
        }
    }

    // ── Helper: Protocol to ChipRange Mapper ──────────────────
    private fun ProtocolFamily.toChipRange(): ChipRange = when(this) {
        ProtocolFamily.MTK_V6           -> ChipRange.MTK_V6
        ProtocolFamily.MTK_BROM_CLASSIC -> ChipRange.MTK_CLASSIC
        ProtocolFamily.QC_EDL           -> ChipRange.QC_ALL
        ProtocolFamily.SAMSUNG_ODIN     -> ChipRange.SAMSUNG_ALL
        ProtocolFamily.SPD_UNISOC       -> ChipRange.SPD_ALL
        ProtocolFamily.IOS_DFU          -> ChipRange.A7_TO_A11
        else                            -> ChipRange.ALL_ANDROID
    }

    // ── Other UI state management (Clear errors, etc) ─────────
    fun onDismissPlan() = _state.update { it.copy(showPlanDialog = false, activePlan = null) }
    fun onClearError() = _state.update { it.copy(errorMessage = null) }
    fun onClearSuccess() = _state.update { it.copy(successMessage = null) }
    
    // ── Filter Toggles ────────────────────────────────────────
    fun onUpdateSearch(q: String) = _state.update { it.copy(filters = it.filters.copy(searchQuery = q)) }
    fun onToggleFreeOnly() = _state.update { it.copy(filters = it.filters.copy(freeOnly = !it.filters.freeOnly)) }
    fun onToggleSignalOnly() = _state.update { it.copy(filters = it.filters.copy(signalOnly = !it.filters.signalOnly)) }
    fun onToggleUntethered() = _state.update { it.copy(filters = it.filters.copy(isUntethered = !it.filters.isUntethered)) }
    fun onToggleOfflineOnly() = _state.update { it.copy(filters = it.filters.copy(offlineOnly = !it.filters.offlineOnly)) }
    fun onToggleNoDataLoss() = _state.update { it.copy(filters = it.filters.copy(noDataLoss = !it.filters.noDataLoss)) }
    fun onToggleNoJailbreak() = _state.update { it.copy(filters = it.filters.copy(noJailbreak = !it.filters.noJailbreak)) }

    fun onRefineRecommendation(signalOnly: Boolean, freeOnly: Boolean, untetheredOnly: Boolean) {
        _state.update { 
            it.copy(filters = it.filters.copy(
                signalOnly = signalOnly,
                freeOnly = freeOnly,
                isUntethered = untetheredOnly
            ))
        }
        refreshFeatures()
    }

    // ── Internal Filtering ────────────────────────────────────
    private fun applyFilters(
        features: List<BypassFeature>, 
        f: FeatureFilters, 
        p: DevicePlatform
    ): List<BypassFeature> {
        return features.filter { feature ->
            // Platform filter
            val platformMatch = when(p) {
                DevicePlatform.IOS     -> feature.chipRange.name.startsWith("A")
                DevicePlatform.ANDROID -> !feature.chipRange.name.startsWith("A")
                else -> true
            }
            if (!platformMatch) return@filter false

            // Text search
            if (f.searchQuery.isNotEmpty()) {
                val q = f.searchQuery.lowercase()
                if (!feature.displayName.lowercase().contains(q) && 
                    !feature.description.lowercase().contains(q)) return@filter false
            }

            // Boolean toggles
            if (f.freeOnly && !feature.isFree) return@filter false
            if (f.signalOnly && !feature.signalAfter) return@filter false
            if (f.offlineOnly && !feature.isOffline) return@filter false
            if (f.isUntethered && !feature.isUntethered) return@filter false
            if (f.noDataLoss && !feature.noDataLoss) return@filter false
            
            true
        }
    }

    fun onBrandFilter(brand: String) {
        _state.update { it.copy(filters = it.filters.copy(brandFilter = brand)) }
        refreshFeatures()
    }

    // ── Python-Powered UI Helpers ─────────────────────────────

    fun onImeiChanged(imei: String) {
        _state.update { it.copy(imei = imei) }
        if (imei.length == 15) {
            viewModelScope.launch {
                val sessionId = java.util.UUID.randomUUID().toString()
                val result = pythonBridge.validateImei(imei, sessionId)
                _state.update { state ->
                    state.copy(
                        imeiValid = result.isValid,
                        imeiManufacturer = if (result.isValid) result.manufacturer else "❌ Invalid IMEI",
                        imeiTac = result.tac
                    )
                }
            }
        } else {
            _state.update { it.copy(imeiValid = false, imeiManufacturer = "", imeiTac = "") }
        }
    }

    suspend fun validateDaBeforeFlash(daBytes: ByteArray, sessionId: String): Boolean {
        return when (val r = pythonBridge.validateDa(daBytes, sessionId)) {
            is com.deepeye.otg.python.DaValidationResult.Valid -> {
                Timber.d("[VM] DA valid sha=${r.sha256} sid=$sessionId")
                _state.update { it.copy(daStatus = "✅ DA Valid: ${r.sha256.take(16)}...") }
                true
            }
            is com.deepeye.otg.python.DaValidationResult.Invalid -> {
                Timber.e("[VM] DA INVALID: ${r.error} sid=$sessionId")
                _state.update { it.copy(daStatus = "❌ DA Invalid: ${r.error}") }
                false
            }
        }
    }

    fun onBuildIosPayload(udid: String, imei: String, serial: String, model: String, iosVersion: String) {
        viewModelScope.launch {
            val sessionId = java.util.UUID.randomUUID().toString()
            val payload = pythonBridge.buildIosActivationRequest(udid, imei, serial, model, iosVersion, sessionId)
            _state.update { it.copy(iosActivationPayload = payload) }
            Timber.d("[VM] iOS payload built sid=$sessionId")
        }
    }
}
