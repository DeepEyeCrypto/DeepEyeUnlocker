package com.deepeye.otg.viewmodel

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.core.usb.UsbDeviceDetector
import com.deepeye.otg.usecase.FrpResult
import com.deepeye.otg.usecase.FrpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class FrpUiState(
    val isRunning: Boolean = false,
    val progress: Int = 0,
    val statusMessage: String = "Ready",
    val logs: List<String> = emptyList(),
    val error: String? = null,
    val success: String? = null
)

@HiltViewModel
class FrpViewModel @Inject constructor(
    private val frpUseCase: FrpUseCase,
    private val deviceDetector: UsbDeviceDetector
) : ViewModel() {

    private val _uiState = MutableStateFlow(FrpUiState())
    val uiState: StateFlow<FrpUiState> = _uiState.asStateFlow()
    
    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()
    
    private var currentDevicePath: String? = null
    
    init {
        // Reactive monitoring of USB devices and their permissions
        viewModelScope.launch {
            deviceDetector.observeDevices().collect { devices ->
                val target = devices.find { it.name == currentDevicePath }
                if (target != null) {
                    if (target.hasPermission && !_permissionGranted.value) {
                        _permissionGranted.value = true
                        addLog("[INFO] USB permission detected reactively")
                        _uiState.update { it.copy(statusMessage = "Permission Granted - Ready") }
                    } else if (!target.hasPermission && _permissionGranted.value) {
                        _permissionGranted.value = false
                        addLog("[WARN] USB permission lost")
                    }
                }
            }
        }
    }

    fun startBypass(device: UsbDevice, androidVersion: Int) {
        currentDevicePath = device.deviceName
        
        if (!deviceDetector.hasPermission(device)) {
            _uiState.update { it.copy(
                isRunning = false,
                statusMessage = "USB Permission Required",
                error = "Please grant USB permission first."
            )}
            addLog("[ERROR] Attempted start without permission")
            return
        }
        
        _permissionGranted.value = true
        val sessionId = UUID.randomUUID().toString()
        
        viewModelScope.launch {
            _uiState.update { it.copy(
                isRunning = true,
                statusMessage = "Initializing...",
                logs = it.logs + "[INFO] Starting session: $sessionId"
            )}
            
            try {
                frpUseCase.executeBypass(device, androidVersion, sessionId).collect { result ->
                    when (result) {
                        is FrpResult.Progress -> {
                            _uiState.update { it.copy(
                                progress = result.percentage,
                                statusMessage = result.message
                            )}
                            addLog("[INFO] ${result.message}")
                        }
                        is FrpResult.Success -> {
                            _uiState.update { it.copy(
                                isRunning = false,
                                progress = 100,
                                statusMessage = "Bypass Successful",
                                success = result.message
                            )}
                            addLog("[SUCCESS] ${result.message}")
                        }
                        is FrpResult.Error -> {
                            _uiState.update { it.copy(
                                isRunning = false,
                                statusMessage = "Bypass Failed",
                                error = result.message
                            )}
                            addLog("[ERROR] ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isRunning = false,
                    statusMessage = "Unexpected Error",
                    error = e.message
                )}
                addLog("[ERROR] Exception: ${e.message}")
            }
        }
    }
    
    fun requestUsbPermission(device: UsbDevice) {
        currentDevicePath = device.deviceName
        if (deviceDetector.hasPermission(device)) {
            _permissionGranted.value = true
            _uiState.update { it.copy(statusMessage = "USB Permission Already Granted") }
            return
        }
        
        deviceDetector.requestPermission(device)
        addLog("[INFO] Requesting USB permission dynamically...")
    }
    
    private fun addLog(message: String) {
        _uiState.update { it.copy(logs = it.logs + message) }
    }

    fun clearState() {
        _uiState.value = FrpUiState()
        _permissionGranted.value = false
        currentDevicePath = null
    }
}
