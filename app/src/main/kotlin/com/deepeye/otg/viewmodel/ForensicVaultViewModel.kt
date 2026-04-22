package com.deepeye.otg.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.data.db.entities.DeviceEntity
import com.deepeye.otg.data.db.entities.OperationLogEntity
import com.deepeye.otg.data.db.entities.SessionEntity
import com.deepeye.otg.data.repository.ForensicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ForensicVaultViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ForensicRepository
) : ViewModel() {

    private var statusClearJob: Job? = null

    val allDevices: StateFlow<List<DeviceEntity>> = repository.getAllDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDeviceKey = MutableStateFlow<String?>(null)
    val selectedDeviceKey = _selectedDeviceKey.asStateFlow()

    private val _selectedSessionId = MutableStateFlow<Long?>(null)
    val selectedSessionId = _selectedSessionId.asStateFlow()

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus = _exportStatus.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val deviceSessions: StateFlow<List<SessionEntity>> = _selectedDeviceKey
        .flatMapLatest { key ->
            if (key != null) repository.getSessions(key)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sessionLogs: StateFlow<List<OperationLogEntity>> = _selectedSessionId
        .flatMapLatest { id ->
            if (id != null) repository.getLogs(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDevice(key: String?) {
        _selectedDeviceKey.value = key
        _selectedSessionId.value = null
    }
    
    fun selectSession(id: Long?) {
        _selectedSessionId.value = id
    }

    fun exportAuditLog(sessionId: Long) {
        viewModelScope.launch {
            _exportStatus.value = "Generating Audit Report..."
            val logs = repository.getLogs(sessionId).first()
            if (logs.isEmpty()) {
                setStatusWithDelay("Error: No logs to export")
                return@launch
            }

            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val exportDir = File(context.getExternalFilesDir(null), "vault/exports")
                if (!exportDir.exists()) exportDir.mkdirs()
                
                val file = File(exportDir, "ForensicAudit_Session_${sessionId}_$timestamp.txt")
                
                val content = StringBuilder()
                content.append("========================================\n")
                content.append("DEEPEYE UNLOCKER - FORENSIC AUDIT LOG\n")
                content.append("Session ID: $sessionId\n")
                content.append("Export Time: ${Date()}\n")
                content.append("========================================\n\n")
                
                logs.forEach { log ->
                    val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp))
                    content.append("[$time] ${log.operationType.padEnd(12)} | [${log.result}] | ${log.details}\n")
                    if (log.artifactPath != null) {
                        content.append("       -> Artifact: ${log.artifactPath}\n")
                    }
                }
                
                content.append("\n-- END OF AUDIT --")
                
                file.writeText(content.toString())
                setStatusWithDelay("Report exported to: ${file.name}")
            } catch (e: Exception) {
                setStatusWithDelay("Export Failed: ${e.message}")
            }
        }
    }

    private fun setStatusWithDelay(message: String, delayMs: Long = 4000) {
        statusClearJob?.cancel()
        _exportStatus.value = message
        statusClearJob = viewModelScope.launch {
            delay(delayMs)
            _exportStatus.value = null
        }
    }
}
