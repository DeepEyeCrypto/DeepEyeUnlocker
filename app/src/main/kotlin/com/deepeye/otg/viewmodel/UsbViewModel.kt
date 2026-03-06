package com.deepeye.otg.viewmodel

import androidx.lifecycle.ViewModel
import com.deepeye.otg.UsbSessionState
import com.deepeye.otg.data.FeatureItem
import com.deepeye.otg.usb.DeepEyeOperation
import com.deepeye.otg.usb.SessionState
import com.deepeye.otg.usb.UsbSessionManager
import com.deepeye.otg.ui.LogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UsbViewModel(
    private val sessionManager: UsbSessionManager,
    private val settings: com.deepeye.otg.data.SettingsManager,
    val usbState: StateFlow<UsbSessionState>,
    val logs: StateFlow<List<LogEntry>>
) : ViewModel() {

    val queueState: StateFlow<SessionState> = sessionManager.state
    val performanceMode: StateFlow<Boolean> = settings.performanceMode
    val selectedBrand = MutableStateFlow(0)
    
    fun togglePerformance() {
        settings.togglePerformanceMode()
    }

    fun queueOperation(feature: FeatureItem) {
        val op = DeepEyeOperation.entries.find { it.label == feature.label }
        if (op != null) {
            sessionManager.queueOperation(op)
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
