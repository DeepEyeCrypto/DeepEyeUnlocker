package com.deepeye.otg.ui.screens.qualcomm

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.python.PythonBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@Immutable
data class EdlUiState(
    val vidInput:         String  = "05C6",
    val pidInput:         String  = "9008",
    val chipInput:        String  = "",
    val storageType:      String  = "ufs",
    val frpStorage:       String  = "ufs",
    val isEdlDetected:    Boolean = false,
    val isLoading:        Boolean = false,
    val chipInfo:         String  = "",
    val detectionResult:  String  = "",
    val programmerInfo:   String  = "",
    val flashSteps:       List<FlashStep> = emptyList(),
    val frpXml:           String  = "",
    val frpInfo:          String  = "",
    val error:            String? = null
)

@HiltViewModel
class EdlViewModel @Inject constructor(
    private val pythonBridge: PythonBridge,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(EdlUiState())
    val uiState: StateFlow<EdlUiState> = _uiState.asStateFlow()

    fun onVidChanged(v: String)         { _uiState.update { it.copy(vidInput = v) } }
    fun onPidChanged(v: String)         { _uiState.update { it.copy(pidInput = v) } }
    fun onChipChanged(v: String)        { _uiState.update { it.copy(chipInput = v) } }
    fun onStorageChanged(v: String)     { _uiState.update { it.copy(storageType = v) } }
    fun onFrpStorageChanged(v: String)  { _uiState.update { it.copy(frpStorage = v) } }

    fun detectEdl() {
        viewModelScope.launch {
            val sid = UUID.randomUUID().toString()
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true) }
            Timber.d("[EdlVM] detect vid=${state.vidInput} pid=${state.pidInput} sid=$sid")

            val vid = state.vidInput.trim()
                .removePrefix("0x").toIntOrNull(16) ?: 0
            val pid = state.pidInput.trim()
                .removePrefix("0x").toIntOrNull(16) ?: 0

            val result = pythonBridge.detectChipFromUsb(vid, pid, sid)
            val isEdl  = result.optBoolean("is_edl_mode")
            val mode   = result.optString("mode")

            var programmerInfo = ""
            if (state.chipInput.isNotBlank()) {
                val progResult = pythonBridge.getProgrammerForChip(
                    state.chipInput, sid
                )
                programmerInfo = progResult.optString("programmer")
                Timber.d("[EdlVM] programmer=$programmerInfo sid=$sid")
            }

            _uiState.update { it.copy(
                isLoading       = false,
                isEdlDetected   = isEdl,
                chipInfo        = "VID:${state.vidInput} PID:${state.pidInput}  -   $mode",
                detectionResult = result.toString(2),
                programmerInfo  = programmerInfo
            )}
        }
    }

    fun buildFlashSequence() {
        viewModelScope.launch {
            val sid   = UUID.randomUUID().toString()
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true) }
            Timber.d("[EdlVM] flashSeq chip=${state.chipInput} sid=$sid")

            val seqJson = pythonBridge.buildFlashSequence(
                chip           = state.chipInput,
                partitionsJson = "[]",
                storage        = state.storageType,
                slot           = "a",
                sessionId      = sid
            )

            val steps = parseFlashSteps(seqJson)
            _uiState.update { it.copy(
                isLoading  = false,
                flashSteps = steps
            )}
            Timber.d("[EdlVM] flashSeq built steps=${steps.size} sid=$sid")
        }
    }

    fun getFrpInfo() {
        viewModelScope.launch {
            val sid = UUID.randomUUID().toString()
            val frpInfo = pythonBridge.getFrpPartitionInfo(
                _uiState.value.frpStorage, sid
            )
            val xml = frpInfo.optString("xml")
            Timber.d("[EdlVM] frp xml len=${xml.length} sid=$sid")
            _uiState.update { it.copy(frpXml = xml) }
        }
    }

    fun copyFrpXml() {
        val clipboard = context.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("FRP_XML", _uiState.value.frpXml)
        )
    }

    private fun parseFlashSteps(json: String): List<FlashStep> {
        return try {
            val obj   = org.json.JSONObject(json)
            val arr   = obj.optJSONArray("steps") ?: JSONArray()
            (0 until arr.length()).map { i ->
                val step = arr.getJSONObject(i)
                FlashStep(
                    phase    = step.optString("phase"),
                    stepNum  = step.optInt("step"),
                    action   = step.optString("action"),
                    note     = step.optString("note"),
                    filename = step.optString("filename"),
                    xml      = step.optString("xml")
                )
            }
        } catch (e: Exception) {
            Timber.e("[EdlVM] parseSteps: ${e.message}")
            emptyList()
        }
    }
}
