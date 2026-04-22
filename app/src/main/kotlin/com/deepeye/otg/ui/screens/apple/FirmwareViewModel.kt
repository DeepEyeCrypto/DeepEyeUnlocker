package com.deepeye.otg.ui.screens.apple

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.python.PythonBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID
import javax.inject.Inject

data class FirmwareUiState(
    val model: String = "",
    val isLoading: Boolean = false,
    val firmwareList: List<FirmwareEntry> = emptyList(),
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadEta: String = ""
)

@HiltViewModel
class FirmwareViewModel @Inject constructor(
    private val pythonBridge: PythonBridge
) : ViewModel() {

    private val _uiState = MutableStateFlow(FirmwareUiState())
    val uiState: StateFlow<FirmwareUiState> = _uiState.asStateFlow()

    fun onModelChanged(model: String) {
        _uiState.update { it.copy(model = model, isLoading = true) }
        if (model.isNotBlank()) {
            fetchFirmware(model)
        } else {
            _uiState.update { it.copy(firmwareList = emptyList(), isLoading = false) }
        }
    }

    private fun fetchFirmware(model: String) {
        viewModelScope.launch {
            val sid = UUID.randomUUID().toString()
            val resultStr = pythonBridge.getFirmwareForModel(model, sid)
            val list = mutableListOf<FirmwareEntry>()
            try {
                if (resultStr.startsWith("[")) {
                    val arr = JSONArray(resultStr)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            FirmwareEntry(
                                iosVersion = obj.optString("ios_version"),
                                buildId = obj.optString("build_id"),
                                sizeGb = obj.optDouble("size_gb"),
                                signed = obj.optBoolean("signed", false),
                                modelName = obj.optString("model_name"),
                                url = obj.optString("url")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e("Error parsing firmware list: ${e.message}")
            }
            _uiState.update { it.copy(firmwareList = list, isLoading = false) }
        }
    }

    fun onDownloadSelected(fw: FirmwareEntry, targetFile: File) {
        viewModelScope.launch {
            val sid = UUID.randomUUID().toString()
            val etaRet = pythonBridge.estimateDownloadTime(fw.sizeGb, 15.0, sid)
            _uiState.update {
                it.copy(
                    isDownloading = true,
                    downloadProgress = 0f,
                    downloadEta = etaRet.optString("display", "Unknown")
                )
            }

            try {
                withContext(Dispatchers.IO) {
                    val url = URL(fw.url)
                    val connection = url.openConnection()
                    connection.connect()
                    
                    val fileLength = connection.contentLength
                    val input = connection.getInputStream()
                    val output = FileOutputStream(targetFile)
                    
                    val data = ByteArray(4096)
                    var total: Long = 0
                    var count: Int
                    
                    while (input.read(data).also { count = it } != -1) {
                        total += count.toLong()
                        if (fileLength > 0) {
                            val progress = (total * 100 / fileLength).toFloat() / 100f
                            _uiState.update { it.copy(downloadProgress = progress) }
                        }
                        output.write(data, 0, count)
                    }
                    output.flush()
                    output.close()
                    input.close()
                }
            } catch (e: Exception) {
                Timber.e("Firmware download error: \${e.message}")
            }
            
            _uiState.update { it.copy(isDownloading = false, downloadProgress = 1f, downloadEta = "Complete") }
        }
    }
}
