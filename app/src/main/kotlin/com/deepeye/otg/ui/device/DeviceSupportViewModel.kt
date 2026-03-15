package com.deepeye.otg.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.data.device.DeviceDatabase
import com.deepeye.otg.data.device.DeviceEntry
import com.deepeye.otg.data.device.DeviceProtocol
import com.deepeye.otg.data.device.ProtocolRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceSupportUiState(
    val brands:          List<String>         = emptyList(),
    val selectedBrand:   String?              = null,
    val models:          List<String>         = emptyList(),
    val selectedModel:   String?              = null,
    val routingResult:   ProtocolRouter.RoutingResult? = null,
    val searchQuery:     String               = "",
    val searchResults:   List<DeviceEntry>    = emptyList(),
    val totalDevices:    Int                  = 0,
    val protocolCounts:  Map<DeviceProtocol, Int> = emptyMap(),
    val isLoading:       Boolean              = false,
)

@HiltViewModel
class DeviceSupportViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceSupportUiState())
    val uiState: StateFlow<DeviceSupportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(
                brands         = DeviceDatabase.allBrands(),
                totalDevices   = DeviceDatabase.total(),
                protocolCounts = DeviceDatabase.countByProtocol(),
            )}
        }
    }

    fun onBrandSelected(brand: String) {
        val models = DeviceDatabase.modelsForBrand(brand)
        _uiState.update { it.copy(
            selectedBrand = brand,
            models        = models,
            selectedModel = null,
            routingResult = null,
        )}
    }

    fun onModelSelected(model: String) {
        val brand = _uiState.value.selectedBrand ?: return
        val entry = DeviceDatabase.findByBrandModel(brand, model)
        val result = ProtocolRouter.route(
            vid   = entry?.vid ?: 0,
            pid   = 0,
            brand = brand,
            model = model,
        )
        _uiState.update { it.copy(
            selectedModel = model,
            routingResult = result,
        )}
    }

    fun onSearch(query: String) {
        val results = if (query.length >= 2) {
            val q = query.lowercase()
            DeviceDatabase.getAllEntries()
                .filter {
                    it.brand.lowercase().contains(q) ||
                    it.model.lowercase().contains(q) ||
                    it.series.lowercase().contains(q)
                }
                .take(50)
        } else emptyList()
        _uiState.update { it.copy(searchQuery = query, searchResults = results) }
    }

    fun onVidDetected(vid: Int, pid: Int) {
        val result = ProtocolRouter.route(vid = vid, pid = pid)
        _uiState.update { it.copy(routingResult = result) }
    }
}
