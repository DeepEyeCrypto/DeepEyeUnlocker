package com.deepeye.otg.viewmodel

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.usecase.FrpResult
import com.deepeye.otg.usecase.FrpUseCase
import com.deepeye.otg.usb.UsbPermissionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    @ApplicationContext private val context: Context  // ✅ Added for permission management
) : ViewModel() {

    private val _uiState = MutableStateFlow(FrpUiState())
    val uiState: StateFlow<FrpUiState> = _uiState.asStateFlow()
    
    // ✅ Track permission state
    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()
    
    // ✅ Track current device for permission polling
    private var currentDevice: UsbDevice? = null
    private var isPollingActive = true
    
    // ✅ FIX: Poll permission state to detect broadcast receiver changes
    init {
        viewModelScope.launch {
            while (isPollingActive) {
                kotlinx.coroutines.delay(1000)
                currentDevice?.let { device ->
                    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                    val hasPermission = usbManager.hasPermission(device)
                    
                    // Update state if changed
                    if (_permissionGranted.value != hasPermission) {
                        _permissionGranted.value = hasPermission
                        
                        if (hasPermission) {
                            _uiState.value = _uiState.value.copy(
                                statusMessage = "USB Permission Granted - Ready to start",
                                logs = _uiState.value.logs + "[INFO] USB permission detected (polling)"
                            )
                        }
                    }
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        isPollingActive = false  // Stop polling when ViewModel is destroyed
    }

    // ✅ FIX: Check permission before starting bypass
    fun startBypass(device: UsbDevice, androidVersion: Int) {
        currentDevice = device  // ✅ Track device for polling
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        
        // Check permission first
        if (!usbManager.hasPermission(device)) {
            _uiState.value = _uiState.value.copy(
                isRunning = false,
                statusMessage = "USB Permission Required",
                error = "USB permission not granted.\n\nPlease accept the USB permission dialog and try again.",
                logs = _uiState.value.logs + "[ERROR] USB permission not granted"
            )
            return
        }
        
        _permissionGranted.value = true
        
        val sessionId = UUID.randomUUID().toString()
        viewModelScope.launch {
            _uiState.value = FrpUiState(
                isRunning = true,
                statusMessage = "Initializing...",
                logs = listOf("[INFO] Starting FRP bypass session: $sessionId")
            )
            
            try {
                frpUseCase.executeBypass(device, androidVersion, sessionId).collect { result ->
                    when (result) {
                        is FrpResult.Progress -> {
                            _uiState.value = _uiState.value.copy(
                                progress = result.percentage,
                                statusMessage = result.message,
                                logs = _uiState.value.logs + "[INFO] ${result.message}"
                            )
                        }
                        is FrpResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isRunning = false,
                                progress = 100,
                                statusMessage = "Bypass Successful",
                                success = result.message,
                                logs = _uiState.value.logs + "[SUCCESS] ${result.message}"
                            )
                        }
                        is FrpResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isRunning = false,
                                statusMessage = "Bypass Failed",
                                error = result.message,
                                logs = _uiState.value.logs + "[ERROR] ${result.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    statusMessage = "Unexpected Error",
                    error = "Unexpected error: ${e.message}",
                    logs = _uiState.value.logs + "[ERROR] ${e.message}"
                )
            }
        }
    }
    
    // ✅ FIX: Request USB permission
    fun requestUsbPermission(device: UsbDevice) {
        currentDevice = device  // ✅ Track device for polling
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        
        if (usbManager.hasPermission(device)) {
            _permissionGranted.value = true
            _uiState.value = _uiState.value.copy(
                statusMessage = "USB Permission Granted",
                logs = _uiState.value.logs + "[INFO] USB permission already granted"
            )
            return
        }
        
        // Request permission using UsbPermissionGuard
        UsbPermissionGuard.requestPermission(
            context = context,
            usbManager = usbManager,
            device = device,
            actionPermission = UsbPermissionGuard.ACTION_USB_PERMISSION
        )
        
        _uiState.value = _uiState.value.copy(
            statusMessage = "Waiting for USB permission...",
            logs = _uiState.value.logs + "[INFO] USB permission dialog shown"
        )
    }
    
    // ✅ FIX: Update permission state from broadcast receiver
    fun onPermissionResult(granted: Boolean, device: UsbDevice) {
        _permissionGranted.value = granted
        
        if (granted) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "USB Permission Granted - Ready to start",
                logs = _uiState.value.logs + "[INFO] USB permission granted by user"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                statusMessage = "USB Permission Denied",
                error = "USB permission was denied. Cannot proceed with FRP bypass.",
                logs = _uiState.value.logs + "[ERROR] USB permission denied by user"
            )
        }
    }

    fun clearState() {
        _uiState.value = FrpUiState()
        _permissionGranted.value = false
        currentDevice = null  // ✅ Clear tracked device
    }
}
