package com.deepeye.otg.viewmodel

import android.hardware.usb.UsbManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.data.BypassItem
import com.deepeye.otg.data.BypassRepository
import com.deepeye.otg.bypass.BypassExecutor
import com.deepeye.otg.usb.UsbSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BypassResult {
    data class Success(val id: String) : BypassResult()
    data class Failed(val id: String, val message: String) : BypassResult()
}

import com.deepeye.otg.data.DeepEyeDatabase
import com.deepeye.otg.data.BypassHistoryEntry
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first

@HiltViewModel
class BypassViewModel @Inject constructor(
    private val usbManager: UsbManager,
    private val usbSessionManager: UsbSessionManager,
    private val db: DeepEyeDatabase
) : ViewModel() {

    val bypassHistory = db.historyDao().getAll()

    private val _bypasses = MutableStateFlow<List<BypassItem>>(emptyList())
    val bypasses: StateFlow<List<BypassItem>> = _bypasses.asStateFlow()

    private val _runningId = MutableStateFlow<String?>(null)
    val runningId: StateFlow<String?> = _runningId.asStateFlow()

    private val _lastResult = MutableStateFlow<BypassResult?>(null)
    val lastResult: StateFlow<BypassResult?> = _lastResult.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    init {
        // Load real bypass data
        _bypasses.value = BypassRepository.allBypasses
        
        // Sort by priority descending
        _bypasses.value = _bypasses.value.sortedByDescending { it.priority }
    }

    fun runBypass(item: BypassItem) {
        viewModelScope.launch {
            _runningId.value = item.id
            _lastResult.value = null
            _logs.value = emptyList() // clear logs for new run
            
            val success = BypassExecutor.execute(
                item       = item,
                usbManager = usbManager,
                device     = usbSessionManager.currentUsbDevice, 
                onLog      = { log -> addLog(log) }
            )
            
            _lastResult.value = if (success)
                BypassResult.Success(item.id)
            else
                BypassResult.Failed(item.id, "Execution failed")

            // SAVE TO HISTORY
            db.historyDao().insert(
                BypassHistoryEntry(
                    bypassId = item.id,
                    carrier = item.carrier,
                    method = item.method.name,
                    success = success,
                    deviceModel = usbSessionManager.currentUsbDevice?.productName ?: "Unknown Device",
                    logs = _logs.value.joinToString("\n")
                )
            )
                
            _runningId.value = null
        }
    }

    fun exportHistory() {
        viewModelScope.launch {
            val entries = db.historyDao().getAll().first()
            val sb = StringBuilder()
            sb.appendLine("=== DeepEye Bypass History ===")
            sb.appendLine("Exported: ${java.util.Date()}")
            sb.appendLine()
            entries.forEach { e ->
                sb.appendLine("[${if (e.success) "SUCCESS" else "FAILED "}] " +
                    "${e.carrier} | ${e.method} | ${e.deviceModel}")
                sb.appendLine("  Time: ${java.util.Date(e.timestamp)}")
                if (e.logs.isNotBlank()) {
                    sb.appendLine("  Logs: ${e.logs.take(200)}")
                }
                sb.appendLine()
            }

            // Save to Downloads
            try {
                val filename = "deepeye_history_${System.currentTimeMillis()}.txt"
                val downloadsDir = android.os.Environment
                    .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                java.io.File(downloadsDir, filename).writeText(sb.toString())
                addLog("✓ History exported to Downloads/$filename")
            } catch (e: Exception) {
                addLog("✗ Export failed: ${e.message}")
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { db.historyDao().clearAll() }
    }

    private fun addLog(log: String) {
        _logs.value = _logs.value + log
    }
    
    fun clearLogs() {
        _logs.value = emptyList()
    }
    
    fun clearResult() {
        _lastResult.value = null
    }
}
