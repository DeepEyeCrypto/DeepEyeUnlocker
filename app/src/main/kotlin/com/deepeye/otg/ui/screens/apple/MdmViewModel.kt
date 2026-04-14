package com.deepeye.otg.ui.screens.apple

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.python.PythonBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

data class MdmUiState(
    val plistInput: String = "",
    val model: String = "",
    val chip: String = "",
    val isSupervised: Boolean = false,
    val parsedMdm: ParsedMdmInfo? = null,
    val bypassReport: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class MdmViewModel @Inject constructor(
    private val pythonBridge: PythonBridge
) : ViewModel() {

    private val _uiState = MutableStateFlow(MdmUiState())
    val uiState: StateFlow<MdmUiState> = _uiState.asStateFlow()

    fun onPlistChanged(plist: String) {
        _uiState.update { it.copy(plistInput = plist) }
    }

    fun onModelChanged(model: String) {
        _uiState.update { it.copy(model = model) }
    }

    fun onChipChanged(chip: String) {
        _uiState.update { it.copy(chip = chip) }
    }

    fun onSupervisedChanged(isSupervised: Boolean) {
        _uiState.update { it.copy(isSupervised = isSupervised) }
    }

    fun parseMdmProfile() {
        val state = _uiState.value
        if (state.plistInput.isBlank()) return
        
        viewModelScope.launch {
            val sid = UUID.randomUUID().toString()
            val result = pythonBridge.parseMdmPlist(state.plistInput, sid)
            
            if (!result.has("error")) {
                val info = ParsedMdmInfo(
                    orgName = result.optString("org_name", "Unknown"),
                    mdmType = result.optString("mdm_type", "Unknown"),
                    serverUrl = result.optString("server_url", "Unknown"),
                    isSupervised = result.optBoolean("is_supervised", false),
                    removable = result.optBoolean("removable", true)
                )
                _uiState.update { it.copy(parsedMdm = info, isSupervised = info.isSupervised) }
            }
        }
    }

    fun generateBypassPlan() {
        val state = _uiState.value
        if (state.model.isBlank() || state.chip.isBlank()) return
        
        viewModelScope.launch {
            val sid = UUID.randomUUID().toString()
            val mdmType = state.parsedMdm?.mdmType ?: "unknown"
            val report = pythonBridge.getMdmBypassReport(
                state.model,
                state.chip,
                mdmType,
                state.isSupervised,
                sid
            )
            _uiState.update { it.copy(bypassReport = report) }
        }
    }
}
