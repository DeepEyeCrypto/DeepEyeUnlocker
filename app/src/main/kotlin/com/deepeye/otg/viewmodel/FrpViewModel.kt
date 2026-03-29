package com.deepeye.otg.viewmodel

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.usecase.FrpResult
import com.deepeye.otg.usecase.FrpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val frpUseCase: FrpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FrpUiState())
    val uiState: StateFlow<FrpUiState> = _uiState.asStateFlow()

    fun startBypass(device: UsbDevice, androidVersion: Int) {
        val sessionId = UUID.randomUUID().toString()
        viewModelScope.launch {
            _uiState.value = FrpUiState(isRunning = true, statusMessage = "Initializing...")
            
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
        }
    }

    fun clearState() {
        _uiState.value = FrpUiState()
    }
}
