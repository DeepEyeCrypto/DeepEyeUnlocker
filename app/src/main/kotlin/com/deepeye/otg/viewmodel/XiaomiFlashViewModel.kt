package com.deepeye.otg.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.data.model.FlashStatus
import com.deepeye.otg.data.model.XiaomiDeviceInfo
import com.deepeye.otg.data.model.XiaomiFlashTask
import com.deepeye.otg.data.model.XiaomiPartition
import com.deepeye.otg.engine.XiaomiFlashEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class XiaomiFlashViewModel @Inject constructor(
    private val flashEngine: XiaomiFlashEngine
) : ViewModel() {

    data class UiState(
        val deviceInfo: XiaomiDeviceInfo? = null,
        val isDetecting: Boolean = false,
        val flashTasks: List<XiaomiFlashTask> = emptyList(),
        val isFlashing: Boolean = false,
        val currentTask: XiaomiFlashTask? = null,
        val logs: List<String> = emptyList(),
        val errorMessage: String? = null,
        val selectedPartition: XiaomiPartition = XiaomiPartition.BOOT,
        val selectedImagePath: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun detectDevice() {
        viewModelScope.launch {
            _state.update { it.copy(isDetecting = true) }
            try {
                val info = flashEngine.detectDevice()
                _state.update { it.copy(deviceInfo = info, isDetecting = false) }
                addLog("✅ Device: ${info.model} (${info.codename})")
                addLog("📱 Mode: ${info.flashMode.name}")
                addLog("🔓 Bootloader: ${info.bootloaderStatus}")
            } catch (e: Exception) {
                _state.update { it.copy(isDetecting = false, errorMessage = e.message) }
            }
        }
    }

    fun addFlashTask(partition: XiaomiPartition, imagePath: String, size: Long) {
        val task = XiaomiFlashTask(
            partition = partition, 
            imagePath = imagePath,
            imageSize = size
        )
        _state.update { it.copy(flashTasks = it.flashTasks + task) }
    }

    fun removeTask(taskId: String) {
        _state.update { it.copy(
            flashTasks = it.flashTasks.filter { t -> t.id != taskId }
        )}
    }

    fun startFlashing() {
        viewModelScope.launch {
            _state.update { it.copy(isFlashing = true) }
            val tasks = _state.value.flashTasks
            tasks.forEachIndexed { index, task ->
                _state.update { it.copy(currentTask = task) }
                addLog("⚡ Flashing [${index+1}/${tasks.size}]: ${task.partition.label}")
                val success = flashEngine.flashPartition(task) { progress, log ->
                    updateTaskProgress(task.id, progress, log)
                    addLog(log)
                }
                updateTaskStatus(task.id, if (success) FlashStatus.SUCCESS else FlashStatus.FAILED)
            }
            _state.update { it.copy(isFlashing = false, currentTask = null) }
            addLog(if (tasks.all { it.status == FlashStatus.SUCCESS }) 
                "🎉 All partitions flashed successfully!" 
                else "⚠️ Some partitions failed. Check logs.")
        }
    }

    fun unlockBootloader() {
        viewModelScope.launch {
            flashEngine.unlockBootloader().collect { log -> addLog(log) }
        }
    }

    fun rebootToFastboot() { viewModelScope.launch { flashEngine.rebootToFastboot() } }
    fun rebootToRecovery() { viewModelScope.launch { flashEngine.rebootToRecovery() } }
    fun rebootToSystem() { viewModelScope.launch { flashEngine.rebootToSystem() } }
    fun rebootToEDL() { viewModelScope.launch { flashEngine.rebootToEDL() } }
    fun wipeData() { 
        viewModelScope.launch { 
            val success = flashEngine.wipeData()
            addLog(if (success) "🗑️ Data wiped!" else "❌ Wipe failed!")
        } 
    }

    fun selectPartition(partition: XiaomiPartition) {
        _state.update { it.copy(selectedPartition = partition) }
    }

    fun selectImage(path: String) {
        _state.update { it.copy(selectedImagePath = path) }
    }

    fun clearTasks() {
        _state.update { it.copy(flashTasks = emptyList()) }
        addLog("🗑️ All tasks cleared")
    }

    private fun addLog(msg: String) {
        _state.update { it.copy(logs = it.logs + "[${System.currentTimeMillis()}] $msg") }
    }
    
    private fun updateTaskProgress(id: String, progress: Float, log: String) {
        _state.update { s -> s.copy(flashTasks = s.flashTasks.map { 
            if (it.id == id) it.copy(progress = progress, logOutput = log, status = FlashStatus.FLASHING) 
            else it 
        })}
    }
    
    private fun updateTaskStatus(id: String, status: FlashStatus) {
        _state.update { s -> s.copy(flashTasks = s.flashTasks.map {
            if (it.id == id) it.copy(status = status) else it
        })}
    }
    
    fun clearError() { _state.update { it.copy(errorMessage = null) } }
}
