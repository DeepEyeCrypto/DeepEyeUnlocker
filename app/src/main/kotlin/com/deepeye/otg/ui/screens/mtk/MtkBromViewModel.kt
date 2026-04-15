package com.deepeye.otg.ui.screens.mtk

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
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@Immutable
data class MtkBromUiState(
    val vidInput: String = "",
    val pidInput: String = "",
    val chipIdInput: String = "",
    val scatterInput: String = "",
    val spFlashXmlInput: String = "",
    val isLoading: Boolean = false,
    val isBromDetected: Boolean = false,
    val chipLabel: String = "",
    val deviceReport: String = "",
    val scatterPartitions: List<ScatterPartition> = emptyList(),
    val spFlashResult: String = "",
    val spFlashValid: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MtkBromViewModel @Inject constructor(
    private val pythonBridge: PythonBridge
) : ViewModel() {

    private val _uiState = MutableStateFlow(MtkBromUiState())
    val uiState: StateFlow<MtkBromUiState> = _uiState.asStateFlow()

    fun onVidChanged(value: String) {
        _uiState.update { it.copy(vidInput = sanitizeHex(value, 6), error = null) }
    }

    fun onPidChanged(value: String) {
        _uiState.update { it.copy(pidInput = sanitizeHex(value, 6), error = null) }
    }

    fun onChipIdChanged(value: String) {
        _uiState.update { it.copy(chipIdInput = sanitizeHex(value, 10), error = null) }
    }

    fun onScatterChanged(value: String) {
        _uiState.update { it.copy(scatterInput = value, error = null) }
    }

    fun onSpFlashXmlChanged(value: String) {
        _uiState.update { it.copy(spFlashXmlInput = value, error = null) }
    }

    fun identifyDevice() {
        val state = _uiState.value
        val vid = parseHexValue(state.vidInput)
        val pid = parseHexValue(state.pidInput)

        if (vid == null || pid == null) {
            _uiState.update {
                it.copy(error = "Enter valid hexadecimal VID and PID values.")
            }
            return
        }

        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            _uiState.update { it.copy(isLoading = true, error = null) }
            Timber.d("[MtkBromVM] identify vid=${state.vidInput} pid=${state.pidInput} sid=$sessionId")

            runCatching {
                val report = pythonBridge.generateMtkReport(
                    vid = vid,
                    pid = pid,
                    chipIdHex = state.chipIdInput.trim(),
                    scatterText = state.scatterInput,
                    sessionId = sessionId
                )
                val usbInfo = pythonBridge.detectMtkFromUsb(vid, pid, sessionId)
                val chipInfo = state.chipIdInput.trim()
                    .takeIf { it.isNotBlank() }
                    ?.let { pythonBridge.identifyMtkChip(it, sessionId) }
                Triple(report, usbInfo, chipInfo)
            }.onSuccess { (report, usbInfo, chipInfo) ->
                val chipLabel = chipInfo?.let(::buildChipLabel).orEmpty()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isBromDetected = usbInfo.optBoolean("is_brom"),
                        chipLabel = chipLabel,
                        deviceReport = report,
                        error = firstNotBlank(
                            usbInfo.optString("error"),
                            chipInfo?.optString("error")
                        )
                    )
                }
                Timber.d("[MtkBromVM] done isBrom=${usbInfo.optBoolean("is_brom")} sid=$sessionId")
            }.onFailure { throwable ->
                Timber.e("[MtkBromVM] identify: ${throwable.message} sid=$sessionId")
                _uiState.update {
                    it.copy(isLoading = false, error = throwable.message ?: "Failed to identify MTK device")
                }
            }
        }
    }

    fun parseScatter() {
        val scatterText = _uiState.value.scatterInput
        if (scatterText.isBlank()) {
            _uiState.update { it.copy(error = "Paste scatter.txt content before parsing.") }
            return
        }

        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            _uiState.update { it.copy(isLoading = true, error = null) }
            Timber.d("[MtkBromVM] parseScatter sid=$sessionId")

            runCatching {
                pythonBridge.parseScatterFile(scatterText, sessionId)
            }.onSuccess { result ->
                val parts = parseScatterPartitions(result)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        scatterPartitions = parts,
                        error = result.optString("error").takeIf(String::isNotBlank)
                    )
                }
                Timber.d("[MtkBromVM] scatter partitions=${parts.size} sid=$sessionId")
            }.onFailure { throwable ->
                Timber.e("[MtkBromVM] parseScatter: ${throwable.message} sid=$sessionId")
                _uiState.update {
                    it.copy(isLoading = false, error = throwable.message ?: "Failed to parse scatter file")
                }
            }
        }
    }

    fun validateSpFlashXml() {
        val xmlContent = _uiState.value.spFlashXmlInput
        if (xmlContent.isBlank()) {
            _uiState.update { it.copy(error = "Paste download.xml content before validation.") }
            return
        }

        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            _uiState.update { it.copy(isLoading = true, error = null) }
            Timber.d("[MtkBromVM] spFlashXml sid=$sessionId")

            runCatching {
                pythonBridge.validateSpFlashXml(xmlContent, sessionId)
            }.onSuccess { result ->
                val valid = result.optBoolean("valid")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        spFlashValid = valid,
                        spFlashResult = result.toString(2),
                        error = result.optString("error").takeIf(String::isNotBlank)
                    )
                }
            }.onFailure { throwable ->
                Timber.e("[MtkBromVM] spFlashXml: ${throwable.message} sid=$sessionId")
                _uiState.update {
                    it.copy(isLoading = false, error = throwable.message ?: "Failed to validate SP Flash XML")
                }
            }
        }
    }

    private fun sanitizeHex(value: String, maxLength: Int): String {
        return value.filter {
            it.isDigit() || it.lowercaseChar() in 'a'..'f' || it == 'x' || it == 'X'
        }.take(maxLength)
    }

    private fun parseHexValue(value: String): Int? {
        val normalized = value.trim().removePrefix("0x").removePrefix("0X")
        if (normalized.isBlank()) {
            return null
        }
        return normalized.toIntOrNull(16)
    }

    private fun parseScatterPartitions(result: JSONObject): List<ScatterPartition> {
        val partitions = result.optJSONArray("partitions") ?: return emptyList()
        return buildList {
            for (index in 0 until partitions.length()) {
                val item = partitions.optJSONObject(index) ?: continue
                add(
                    ScatterPartition(
                        name = item.optString("name"),
                        startAddr = item.optString("start_addr", "0x0"),
                        sizeMb = item.optDouble("size_mb", 0.0).toFloat(),
                        type = item.optString("type", "raw"),
                        file = item.optString("file", "")
                    )
                )
            }
        }
    }

    private fun buildChipLabel(chipInfo: JSONObject): String {
        return buildList {
            chipInfo.optString("name").takeIf { it.isNotBlank() }?.let(::add)
            chipInfo.optString("arch").takeIf { it.isNotBlank() }?.let(::add)
            chipInfo.optInt("cores").takeIf { it > 0 }?.let { add("$it cores") }
            chipInfo.optString("process").takeIf { it.isNotBlank() }?.let(::add)
        }.joinToString("  •  ").ifBlank {
            chipInfo.optString("note", "")
        }
    }

    private fun firstNotBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }
}
