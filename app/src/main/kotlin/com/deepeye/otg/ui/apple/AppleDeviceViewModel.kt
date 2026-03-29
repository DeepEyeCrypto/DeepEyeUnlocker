package com.deepeye.otg.ui.apple

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.data.repository.AppleDeviceState
import com.deepeye.otg.usecase.AppleDeviceUseCase
import com.deepeye.otg.usb.DeviceMatrix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for Apple device screen.
 */
data class AppleDeviceUiState(
    val appleDeviceState: AppleDeviceState = AppleDeviceState.Idle,
    val detectedMode: DeviceMatrix.AppleMode? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val irecoveryOutput: String? = null
)

/**
 * ViewModel for Apple device operations.
 * Observes USB attachments via DeviceRepository and provides functions to interact with Apple devices via Tauri.
 */
@HiltViewModel
class AppleDeviceViewModel @Inject constructor(
    private val appleDeviceUseCase: AppleDeviceUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AppleDeviceUiState())
    val state: StateFlow<AppleDeviceUiState> = _state.asStateFlow()

    init {
        observeAppleDevice()
    }

    /**
     * Start observing Apple device attachments.
     */
    private fun observeAppleDevice() {
        viewModelScope.launch {
            appleDeviceUseCase.observeAppleDevice()
                .onEach { deviceState ->
                    _state.value = _state.value.copy(
                        appleDeviceState = deviceState,
                        detectedMode = when (deviceState) {
                            is AppleDeviceState.Detected -> deviceState.mode
                            else -> null
                        }
                    )
                }
                .collect()
        }
    }

    /**
     * Refresh Apple device info by calling Tauri backend.
     */
    fun refreshAppleDevice() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, errorMessage = null)
            val mode = _state.value.detectedMode
            appleDeviceUseCase.refreshDeviceInfo(mode)
                .onSuccess { info ->
                Timber.d("Apple device info: $info")
                _state.value = _state.value.copy(
                    successMessage = "Device info retrieved",
                    irecoveryOutput = info,
                    isRefreshing = false
                )
                }
                .onFailure { e ->
                Timber.e(e, "Failed to get Apple device info")
                _state.value = _state.value.copy(
                    errorMessage = "Failed to get device info: ${e.message}",
                    isRefreshing = false
                )
                }
        }
    }

    /**
     * Send an iRecovery command to the connected Apple device.
     */
    fun sendIrecoveryCommand(cmd: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, errorMessage = null)
            val mode = _state.value.detectedMode
            appleDeviceUseCase.sendIrecoveryCommand(mode, cmd)
                .onSuccess { output ->
                Timber.d("iRecovery command '$cmd' output: $output")
                _state.value = _state.value.copy(
                    successMessage = "Command executed",
                    irecoveryOutput = output,
                    isRefreshing = false
                )
                }
                .onFailure { e ->
                Timber.e(e, "iRecovery command failed")
                _state.value = _state.value.copy(
                    errorMessage = "Command failed: ${e.message}",
                    isRefreshing = false
                )
                }
        }
    }

    /**
     * Exit recovery mode (send auto-boot true and reboot).
     */
    fun exitRecovery() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, errorMessage = null)
            val mode = _state.value.detectedMode
            appleDeviceUseCase.exitRecovery(mode)
                .onSuccess { output ->
                Timber.d("Exit recovery output: $output")
                _state.value = _state.value.copy(
                    successMessage = "Device exiting recovery",
                    irecoveryOutput = output,
                    isRefreshing = false
                )
                }
                .onFailure { e ->
                Timber.e(e, "Exit recovery failed")
                _state.value = _state.value.copy(
                    errorMessage = "Exit recovery failed: ${e.message}",
                    isRefreshing = false
                )
                }
        }
    }

    /**
     * Enter DFU mode (send go command).
     */
    fun enterDfu() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, errorMessage = null)
            val mode = _state.value.detectedMode
            appleDeviceUseCase.enterDfu(mode)
                .onSuccess { output ->
                Timber.d("Enter DFU output: $output")
                _state.value = _state.value.copy(
                    successMessage = "Device entering DFU",
                    irecoveryOutput = output,
                    isRefreshing = false
                )
                }
                .onFailure { e ->
                Timber.e(e, "Enter DFU failed")
                _state.value = _state.value.copy(
                    errorMessage = "Enter DFU failed: ${e.message}",
                    isRefreshing = false
                )
                }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    /**
     * Clear success message.
     */
    fun clearSuccess() {
        _state.value = _state.value.copy(successMessage = null)
    }
}
