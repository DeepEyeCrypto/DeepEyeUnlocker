package com.deepeye.otg.viewmodel

import androidx.lifecycle.ViewModel
import com.deepeye.otg.UsbSessionState
import com.deepeye.otg.data.FeatureItem
import com.deepeye.otg.usb.DeepEyeOperation
import com.deepeye.otg.usb.SessionState
import com.deepeye.otg.usb.UsbSessionManager
import com.deepeye.otg.ui.LogEntry
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.data.*
import kotlinx.coroutines.flow.*

class UsbViewModel(
    private val sessionManager: UsbSessionManager,
    private val settings: com.deepeye.otg.data.SettingsManager,
    val usbState: StateFlow<UsbSessionState>,
    val logs: StateFlow<List<LogEntry>>
) : ViewModel() {

    val queueState: StateFlow<SessionState> = sessionManager.state
    val performanceMode: StateFlow<Boolean> = settings.performanceMode
    val selectedBrand = MutableStateFlow(0)
    
    private val _selectedMode = MutableStateFlow(ConnectionMode.ADB)
    val selectedMode: StateFlow<ConnectionMode> = _selectedMode.asStateFlow()

    // Dynamic feature set for current brand
    val activeBrandFeatures: StateFlow<BrandFeatureSet> = selectedBrand
        .map { FeatureData.forBrand(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FeatureData.forBrand(0))

    // List of feature IDs that are supported by the current ConnectionMode
    val availableFeatureIds: StateFlow<List<String>> = combine(activeBrandFeatures, selectedMode) { brandSet, mode ->
        val supportedMode = SupportedMode.valueOf(mode.name)
        brandSet.groups.flatMap { it.features }
            .filter { it.modes.contains(supportedMode) }
            .map { it.id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onModeSelected(mode: ConnectionMode) {
        _selectedMode.value = mode
    }
    
    fun togglePerformance() {
        settings.togglePerformanceMode()
    }

    fun queueOperation(feature: FeatureItem) {
        // Map feature label to DeepEyeOperation if matching, or handle by ID
        val op = DeepEyeOperation.entries.find { it.label == feature.label }
        if (op != null) {
            sessionManager.queueOperation(op)
        } else {
            // For now, if no mapping exists, simulate it or log it
            // In a real app, this would trigger the actual shell command
            sessionManager.queueOperation(DeepEyeOperation.DEEP_DEVICE_INFO) 
        }
    }

    fun queueOperation(op: DeepEyeOperation) {
        sessionManager.queueOperation(op)
    }

    fun cancelWaiting() {
        sessionManager.cancelQueue()
    }

    fun resetToIdle() {
        sessionManager.reset()
    }
}
