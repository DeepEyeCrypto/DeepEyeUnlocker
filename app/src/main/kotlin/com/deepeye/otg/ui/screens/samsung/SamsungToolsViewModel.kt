package com.deepeye.otg.ui.screens.samsung

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.python.PythonBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@Immutable
data class SamsungUsbDetection(
    val vendor: String = "Unknown",
    val vid: String = "",
    val pid: String = "",
    val mode: String = "Unknown",
    val isDownload: Boolean = false,
    val isSamsung: Boolean = false,
    val action: String = "",
)

@Immutable
data class SamsungPitEntry(
    val id: Int,
    val name: String,
    val filename: String,
    val type: String,
    val sizeMb: Float,
    val storage: String,
)

@Immutable
data class SamsungOdinValidation(
    val valid: Boolean = false,
    val flashType: String = "UNKNOWN",
    val totalFiles: Int = 0,
    val hasPit: Boolean = false,
    val filledSlots: Map<String, List<String>> = emptyMap(),
    val missingSlots: List<String> = emptyList(),
    val unrecognized: List<String> = emptyList(),
)

@Immutable
data class SamsungToolsUiState(
    val vidInput: String = "04E8",
    val pidInput: String = "6860",
    val modelInput: String = "SM-S928",
    val storage: String = "ufs",
    val odinFilesInput: String = "",
    val isLoading: Boolean = false,
    val usbDetection: SamsungUsbDetection? = null,
    val pitEntries: List<SamsungPitEntry> = emptyList(),
    val odinValidation: SamsungOdinValidation? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class SamsungToolsViewModel @Inject constructor(
    private val pythonBridge: PythonBridge,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SamsungToolsUiState())
    val uiState: StateFlow<SamsungToolsUiState> = _uiState.asStateFlow()

    fun onVidChanged(value: String) {
        _uiState.update { it.copy(vidInput = sanitizeHex(value), errorMessage = null) }
    }

    fun onPidChanged(value: String) {
        _uiState.update { it.copy(pidInput = sanitizeHex(value), errorMessage = null) }
    }

    fun onModelChanged(value: String) {
        _uiState.update { it.copy(modelInput = value.take(32), errorMessage = null) }
    }

    fun onStorageChanged(value: String) {
        _uiState.update { it.copy(storage = value, errorMessage = null) }
    }

    fun onOdinFilesChanged(value: String) {
        _uiState.update { it.copy(odinFilesInput = value, errorMessage = null) }
    }

    fun detectUsb() {
        val state = _uiState.value
        val vid = parseHexValue(state.vidInput)
        val pid = parseHexValue(state.pidInput)

        if (vid == null || pid == null) {
            _uiState.update {
                it.copy(errorMessage = "Enter valid hexadecimal VID and PID values.")
            }
            return
        }

        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            Timber.d("[SamsungToolsVM] detectUsb vid=0x${vid.toString(16)} pid=0x${pid.toString(16)} sid=$sessionId")

            runCatching {
                pythonBridge.detectSamsungFromUsb(vid, pid, sessionId)
            }.onSuccess { result ->
                val detection = SamsungUsbDetection(
                    vendor = result.optString("vendor", "Unknown"),
                    vid = result.optString("vid", ""),
                    pid = result.optString("pid", ""),
                    mode = result.optString("mode", "Unknown"),
                    isDownload = result.optBoolean("is_download"),
                    isSamsung = result.optBoolean("is_samsung"),
                    action = result.optString("action", ""),
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        usbDetection = detection,
                        errorMessage = result.optString("error").takeIf(String::isNotBlank),
                    )
                }
            }.onFailure { throwable ->
                Timber.e("[SamsungToolsVM] detectUsb failed: ${throwable.message} sid=$sessionId")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = throwable.message ?: "USB detection failed")
                }
            }
        }
    }

    fun loadPitTable() {
        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            Timber.d("[SamsungToolsVM] loadPit model=${state.modelInput} storage=${state.storage} sid=$sessionId")

            runCatching {
                val payload = pythonBridge.generatePitTable(state.storage, state.modelInput, sessionId)
                val array = JSONArray(payload)
                (0 until array.length()).map { index ->
                    val item = array.getJSONObject(index)
                    SamsungPitEntry(
                        id = item.optInt("id"),
                        name = item.optString("name"),
                        filename = item.optString("file"),
                        type = item.optString("type"),
                        sizeMb = item.optDouble("size_mb", 0.0).toFloat(),
                        storage = item.optString("storage", state.storage),
                    )
                }
            }.onSuccess { entries ->
                _uiState.update { it.copy(isLoading = false, pitEntries = entries) }
            }.onFailure { throwable ->
                Timber.e("[SamsungToolsVM] loadPit failed: ${throwable.message} sid=$sessionId")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = throwable.message ?: "Failed to load PIT table")
                }
            }
        }
    }

    fun validateOdinPackage() {
        val files = parsePackageFiles(_uiState.value.odinFilesInput)
        if (files.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Enter one or more Odin package filenames to validate.")
            }
            return
        }

        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            Timber.d("[SamsungToolsVM] validateOdin files=${files.size} sid=$sessionId")

            runCatching {
                val request = JSONArray(files).toString()
                val result = pythonBridge.validateOdinTar(request, sessionId)
                SamsungOdinValidation(
                    valid = result.optBoolean("valid"),
                    flashType = result.optString("flash_type", "UNKNOWN"),
                    totalFiles = result.optInt("total_files"),
                    hasPit = result.optBoolean("has_pit"),
                    filledSlots = parseSlots(result.optJSONObject("slots")),
                    missingSlots = jsonArrayToList(result.optJSONArray("missing_slots")),
                    unrecognized = jsonArrayToList(result.optJSONArray("unrecognized")),
                )
            }.onSuccess { validation ->
                _uiState.update { it.copy(isLoading = false, odinValidation = validation) }
            }.onFailure { throwable ->
                Timber.e("[SamsungToolsVM] validateOdin failed: ${throwable.message} sid=$sessionId")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = throwable.message ?: "Odin validation failed")
                }
            }
        }
    }

    private fun sanitizeHex(value: String): String {
        return value.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it == 'x' || it == 'X' }
            .take(6)
    }

    private fun parseHexValue(value: String): Int? {
        val normalized = value.trim().removePrefix("0x").removePrefix("0X")
        return normalized.toIntOrNull(16)
    }

    private fun parsePackageFiles(input: String): List<String> {
        return input.split('\n', ',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun parseSlots(slots: JSONObject?): Map<String, List<String>> {
        if (slots == null) return emptyMap()
        return listOf("BL", "AP", "CP", "CSC")
            .associateWith { key -> jsonArrayToList(slots.optJSONArray(key)) }
            .filterValues { it.isNotEmpty() }
    }

    private fun jsonArrayToList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { index -> array.optString(index) }
    }
}
