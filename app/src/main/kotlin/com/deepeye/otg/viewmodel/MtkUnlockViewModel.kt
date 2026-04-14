package com.deepeye.otg.viewmodel

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.data.model.*
import com.deepeye.otg.engine.MtkUnlockEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MtkUnlockViewModel @Inject constructor(
    private val mtkEngine: MtkUnlockEngine
) : ViewModel() {

    data class UiState(
        val deviceInfo: MtkDeviceInfo? = null,
        val isDetecting: Boolean = false,
        val isExecuting: Boolean = false,
        val currentOperation: MtkUnlockOperation? = null,
        val operations: List<MtkFlashTask> = emptyList(),
        val logs: List<String> = emptyList(),
        val errorMessage: String? = null,
        val selectedOperation: MtkUnlockOperation = MtkUnlockOperation.READ_INFO,
        val selectedImagePath: String? = null,
        val outputDir: String = "/sdcard/Download",
        val successMessage: String? = null,
        val partitions: List<MtkPartitionInfo> = emptyList(),
        val isReadingPartitions: Boolean = false,
        val nvramData: ByteArray? = null,
        val selectedPartition: MtkPartitionInfo? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun detectDevice(usbDevice: UsbDevice) {
        viewModelScope.launch {
            _state.update { it.copy(isDetecting = true, errorMessage = null, successMessage = null) }
            try {
                val info = mtkEngine.detectDevice(usbDevice)
                applyDeviceDetectionResult(info)
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isDetecting = false, 
                        successMessage = null,
                        errorMessage = "Detection failed: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun detectDeviceAdb() {
        viewModelScope.launch {
            _state.update { it.copy(isDetecting = true, errorMessage = null) }
            try {
                val info = mtkEngine.readDeviceInfo { log -> addLog(log) }
                _state.update { it.copy(deviceInfo = info, isDetecting = false) }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isDetecting = false, 
                        errorMessage = "Detection failed: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun retryBromIdentification() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    deviceInfo = null,
                    isDetecting = false,
                    isExecuting = false,
                    currentOperation = null,
                    errorMessage = null,
                    successMessage = null
                )
            }
            addLog("♻️ Resetting USB state before BROM re-scan...")
            delay(1000)
            _state.update { it.copy(isDetecting = true) }

            try {
                val info = mtkEngine.retryBromIdentification()
                applyDeviceDetectionResult(
                    info = info,
                    successMessage = "✅ BROM identification retry completed"
                )
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isDetecting = false,
                        successMessage = null,
                        errorMessage = "Retry failed: ${e.message}"
                    )
                }
                addLog("❌ Retry failed: ${e.message}")
            }
        }
    }

    fun executeOperation(usbDevice: UsbDevice?) {
        viewModelScope.launch {
            val operation = _state.value.selectedOperation
            _state.update { 
                it.copy(
                    isExecuting = true, 
                    currentOperation = operation,
                    errorMessage = null,
                    successMessage = null
                ) 
            }
            
            addLog("⚡ Starting: ${operation.name}")
            
            try {
                val success = when (operation) {
                    MtkUnlockOperation.READ_INFO -> {
                        val info = mtkEngine.readDeviceInfo { log -> addLog(log) }
                        _state.update { it.copy(deviceInfo = info) }
                        true
                    }
                    MtkUnlockOperation.REMOVE_FRP -> {
                        mtkEngine.removeFrp { log -> addLog(log) }
                    }
                    MtkUnlockOperation.UNLOCK_BOOTLOADER -> {
                        if (usbDevice != null) {
                            mtkEngine.unlockBootloaderBrom(usbDevice) { log -> addLog(log) }
                        } else {
                            addLog("❌ USB device required for BROM unlock")
                            false
                        }
                    }
                    MtkUnlockOperation.FORMAT_USERDATA -> {
                        mtkEngine.formatUserdata { log -> addLog(log) }
                    }
                    MtkUnlockOperation.READ_NVRAM -> {
                        val path = mtkEngine.readNvram(_state.value.outputDir) { log -> addLog(log) }
                        path != null
                    }
                    MtkUnlockOperation.DA_AUTH_BYPASS -> {
                        if (usbDevice != null) {
                            mtkEngine.bypassDaAuth(usbDevice) { log -> addLog(log) }
                        } else {
                            addLog("❌ USB device required for DA bypass")
                            false
                        }
                    }
                    MtkUnlockOperation.DISABLE_VERITY -> {
                        mtkEngine.disableVerity { log -> addLog(log) }
                    }
                    MtkUnlockOperation.WRITE_NVRAM,
                    MtkUnlockOperation.READ_PRELOADER,
                    MtkUnlockOperation.WRITE_PRELOADER,
                    MtkUnlockOperation.SLA_AUTH_BYPASS,
                    MtkUnlockOperation.PATCH_BOOT -> {
                        addLog("⚠️ Operation not yet implemented: ${operation.name}")
                        false
                    }
                    MtkUnlockOperation.REMOVE_MI_ACCOUNT -> {
                        mtkEngine.removeMiAccount { log -> addLog(log) }
                    }
                    MtkUnlockOperation.READ_PARTITIONS -> {
                        // This will be handled separately
                        true
                    }
                }
                
                if (success) {
                    _state.update { 
                        it.copy(
                            successMessage = "✅ ${operation.name} completed successfully!"
                        ) 
                    }
                    addLog("🎉 Operation successful!")
                } else {
                    _state.update { 
                        it.copy(
                            errorMessage = "❌ ${operation.name} failed"
                        ) 
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        errorMessage = "Exception: ${e.message}"
                    ) 
                }
                addLog("❌ Error: ${e.message}")
            } finally {
                _state.update { 
                    it.copy(isExecuting = false, currentOperation = null) 
                }
            }
        }
    }

    fun addOperation(operation: MtkUnlockOperation, partition: String = "", imagePath: String = "") {
        val task = MtkFlashTask(
            operation = operation,
            partition = partition,
            imagePath = imagePath
        )
        _state.update { it.copy(operations = it.operations + task) }
        addLog("➕ Added operation: ${operation.name}")
    }

    fun removeOperation(operationId: String) {
        _state.update { 
            it.copy(operations = it.operations.filter { op -> op.id != operationId }) 
        }
    }

    fun executeAllOperations(usbDevice: UsbDevice?) {
        viewModelScope.launch {
            val operations = _state.value.operations
            if (operations.isEmpty()) {
                _state.update { it.copy(errorMessage = "No operations in queue") }
                return@launch
            }
            
            _state.update { it.copy(isExecuting = true) }
            
            operations.forEachIndexed { index, task ->
                _state.update { it.copy(currentOperation = task.operation) }
                addLog("⚡ Executing [${index+1}/${operations.size}]: ${task.operation.name}")
                
                val success = when (task.operation) {
                    MtkUnlockOperation.READ_INFO -> {
                        val info = mtkEngine.readDeviceInfo { log -> addLog(log) }
                        _state.update { it.copy(deviceInfo = info) }
                        true
                    }
                    MtkUnlockOperation.REMOVE_FRP -> {
                        mtkEngine.removeFrp { log -> addLog(log) }
                    }
                    MtkUnlockOperation.UNLOCK_BOOTLOADER -> {
                        if (usbDevice != null) {
                            mtkEngine.unlockBootloaderBrom(usbDevice) { log -> addLog(log) }
                        } else false
                    }
                    MtkUnlockOperation.FORMAT_USERDATA -> {
                        mtkEngine.formatUserdata { log -> addLog(log) }
                    }
                    else -> {
                        addLog("⚠️ Skipped: ${task.operation.name}")
                        true
                    }
                }
                
                updateOperationStatus(task.id, if (success) MtkTaskStatus.SUCCESS else MtkTaskStatus.FAILED)
            }
            
            _state.update { it.copy(isExecuting = false, currentOperation = null) }
            addLog("🎉 All operations completed!")
        }
    }

    fun selectOperation(operation: MtkUnlockOperation) {
        _state.update { it.copy(selectedOperation = operation) }
    }

    fun selectImage(path: String) {
        _state.update { it.copy(selectedImagePath = path) }
    }

    fun setOutputDir(path: String) {
        _state.update { it.copy(outputDir = path) }
    }

    fun clearOperations() {
        _state.update { it.copy(operations = emptyList()) }
        addLog("🗑️ All operations cleared")
    }

    private fun addLog(msg: String) {
        _state.update { 
            it.copy(logs = it.logs + "[${System.currentTimeMillis()}] $msg") 
        }
    }

    private fun applyDeviceDetectionResult(
        info: MtkDeviceInfo,
        successMessage: String? = null
    ) {
        if (isBromIdentificationFailure(info)) {
            val failureMessage = info.securityConfig.ifBlank {
                "❌ Device Identification failed on BROM"
            }

            _state.update {
                it.copy(
                    deviceInfo = null,
                    isDetecting = false,
                    successMessage = null,
                    errorMessage = failureMessage
                )
            }

            failureMessage.lineSequence()
                .filter { line -> line.isNotBlank() }
                .forEach { line -> addLog(line) }
            return
        }

        _state.update {
            it.copy(
                deviceInfo = info,
                isDetecting = false,
                errorMessage = null,
                successMessage = successMessage
            )
        }

        addLog("✅ Device detected: ${info.chip.chipName}")
        addLog("📱 Mode: ${info.connectMode.name}")
        if (info.brand.isNotEmpty()) {
            addLog("📱 Brand: ${info.brand} ${info.model}")
            addLog("🤖 Android: ${info.androidVer}")
        }
        if (info.daAuthRequired) {
            addLog("⚠️ DA Auth Required: Yes")
        }
    }

    private fun isBromIdentificationFailure(info: MtkDeviceInfo): Boolean {
        return info.connectMode == MtkConnectionMode.BROM && info.hwCode.isBlank()
    }
    
    private fun updateOperationStatus(id: String, status: MtkTaskStatus) {
        _state.update { s -> 
            s.copy(operations = s.operations.map { 
                if (it.id == id) it.copy(status = status) else it 
            })
        }
    }
    
    fun clearError() { 
        _state.update { it.copy(errorMessage = null) } 
    }
    
    fun clearSuccess() { 
        _state.update { it.copy(successMessage = null) } 
    }
    
    fun readPartitions(usbDevice: UsbDevice) {
        viewModelScope.launch {
            _state.update { it.copy(isReadingPartitions = true, errorMessage = null) }
            try {
                val partitions = mtkEngine.readPartitionTable(usbDevice) { log -> addLog(log) }
                _state.update { 
                    it.copy(
                        partitions = partitions,
                        isReadingPartitions = false
                    ) 
                }
                addLog("✅ Found ${partitions.size} partitions")
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isReadingPartitions = false,
                        errorMessage = "Failed to read partitions: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun writeNvram(usbDevice: UsbDevice, data: ByteArray) {
        viewModelScope.launch {
            _state.update { it.copy(isExecuting = true, errorMessage = null) }
            try {
                val success = mtkEngine.writeNvram(usbDevice, data) { log -> addLog(log) }
                if (success) {
                    _state.update { it.copy(successMessage = "✅ NVRAM written successfully!") }
                } else {
                    _state.update { it.copy(errorMessage = "❌ NVRAM write failed") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Error: ${e.message}") }
            } finally {
                _state.update { it.copy(isExecuting = false) }
            }
        }
    }
    
    fun slaBypass(usbDevice: UsbDevice) {
        viewModelScope.launch {
            _state.update { it.copy(isExecuting = true, errorMessage = null) }
            try {
                val success = mtkEngine.slaBypass(usbDevice) { log -> addLog(log) }
                if (success) {
                    _state.update { it.copy(successMessage = "✅ SLA bypass successful!") }
                } else {
                    _state.update { it.copy(errorMessage = "❌ SLA bypass failed") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Error: ${e.message}") }
            } finally {
                _state.update { it.copy(isExecuting = false) }
            }
        }
    }
    
    fun readPreloader(usbDevice: UsbDevice) {
        viewModelScope.launch {
            _state.update { it.copy(isExecuting = true, errorMessage = null) }
            try {
                val path = mtkEngine.readPreloader(usbDevice, _state.value.outputDir) { log -> addLog(log) }
                if (path != null) {
                    _state.update { it.copy(successMessage = "✅ Preloader saved to: $path") }
                } else {
                    _state.update { it.copy(errorMessage = "❌ Preloader read failed") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Error: ${e.message}") }
            } finally {
                _state.update { it.copy(isExecuting = false) }
            }
        }
    }
    
    fun writePreloader(usbDevice: UsbDevice, path: String) {
        viewModelScope.launch {
            _state.update { it.copy(isExecuting = true, errorMessage = null) }
            try {
                val success = mtkEngine.writePreloader(usbDevice, path) { log -> addLog(log) }
                if (success) {
                    _state.update { it.copy(successMessage = "✅ Preloader written! Reboot device.") }
                } else {
                    _state.update { it.copy(errorMessage = "❌ Preloader write failed") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Error: ${e.message}") }
            } finally {
                _state.update { it.copy(isExecuting = false) }
            }
        }
    }
    
    fun selectPartition(partition: MtkPartitionInfo) {
        _state.update { it.copy(selectedPartition = partition) }
        addLog("📌 Selected partition: ${partition.name} (${partition.sizeMb} MB)")
    }
}
