package com.deepeye.otg.viewmodel

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.usb.DeviceMatrix
import com.deepeye.otg.usecase.HydraProtocolUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HydraState {
    object Idle : HydraState()
    data class Detected(val protocol: DeviceMatrix.HydraProtocol) : HydraState()
    data class Error(val reason: String) : HydraState()
}

@HiltViewModel
class HydraViewModel @Inject constructor(
    private val hydraProtocolUseCase: HydraProtocolUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<HydraState>(HydraState.Idle)
    val state: StateFlow<HydraState> = _state.asStateFlow()

    fun detect(device: UsbDevice) = viewModelScope.launch {
        val protocol = hydraProtocolUseCase.detectProtocol(device)
        if (protocol == null) {
            _state.emit(HydraState.Error("Unknown protocol for ${device.vendorId}:${device.productId}"))
        } else {
            _state.emit(HydraState.Detected(protocol))
        }
    }
}

