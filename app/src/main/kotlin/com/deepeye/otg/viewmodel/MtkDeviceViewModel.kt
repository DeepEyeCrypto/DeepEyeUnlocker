package com.deepeye.otg.viewmodel

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.usecase.MtkDeviceUseCase
import com.deepeye.otg.usecase.MtkResult
import com.deepeye.otg.usb.DeviceMatrix
import com.deepeye.otg.util.detectMtkMode
import com.deepeye.otg.util.getMtkChipFamily
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MtkDeviceViewModel @Inject constructor(
    private val mtkUseCase: MtkDeviceUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<MtkOperationState>(MtkOperationState.Idle)
    val state: StateFlow<MtkOperationState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    fun onDeviceDetected(device: UsbDevice) = viewModelScope.launch {
        val mode = device.detectMtkMode()
        val chip = device.getMtkChipFamily()
        _state.emit(MtkOperationState.ModeDetected(mode, chip))
    }

    fun startBromHandshake(device: UsbDevice) = viewModelScope.launch {
        _state.emit(MtkOperationState.BromHandshaking)
        mtkUseCase.performHandshake(device)
            .onEach { result ->
                when (result) {
                    MtkResult.BromConnected -> _state.emit(MtkOperationState.SendingDa)
                    MtkResult.DaReady -> _state.emit(MtkOperationState.DaReady)
                    is MtkResult.Progress -> {
                        _progress.emit(result.percent)
                        _state.emit(MtkOperationState.Progress(result.percent, result.stage))
                    }
                    is MtkResult.Success -> _state.emit(MtkOperationState.Success(result.message))
                    is MtkResult.Error -> _state.emit(MtkOperationState.Error(result.reason))
                }
            }
            .launchIn(viewModelScope)
    }

    fun unlockBootloader() = viewModelScope.launch {
        _state.emit(MtkOperationState.Progress(0f, "Unlocking bootloader"))
        mtkUseCase.unlockBootloader()
            .onEach { result ->
                when (result) {
                    is MtkResult.Progress -> {
                        _progress.emit(result.percent)
                        _state.emit(MtkOperationState.Progress(result.percent, result.stage))
                    }
                    is MtkResult.Success -> _state.emit(MtkOperationState.Success(result.message))
                    is MtkResult.Error -> _state.emit(MtkOperationState.Error(result.reason))
                    else -> Unit
                }
            }
            .launchIn(viewModelScope)
    }
}

sealed class MtkOperationState {
    data object Idle : MtkOperationState()
    data class ModeDetected(
        val mode: DeviceMatrix.MtkMode,
        val chip: DeviceMatrix.MtkChipFamily
    ) : MtkOperationState()

    data object BromHandshaking : MtkOperationState()
    data object SendingDa : MtkOperationState()
    data object DaReady : MtkOperationState()
    data class Progress(val percent: Float, val stage: String) : MtkOperationState()
    data class Success(val message: String) : MtkOperationState()
    data class Error(val reason: String) : MtkOperationState()
}

