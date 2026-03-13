package com.deepeye.otg.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.ui.viewmodel.LogEntry
import com.deepeye.otg.usb.AdbManager
import com.deepeye.otg.usb.HardwareManager
import com.deepeye.otg.usb.UsbLifecycleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val adbManager: AdbManager,
    private val hardwareManager: HardwareManager,
    private val lifecycleManager: UsbLifecycleManager
) : ViewModel() {

    private val _logs = MutableStateFlow<List<LogEntry>>(listOf(
        LogEntry("DeepEye Forensic Terminal [v1.0.0]", "INFO", "00:00:00"),
        LogEntry("Type 'help' for available commands or 'clear' to reset.", "SYSTEM", "00:00:00")
    ))
    val logs = _logs.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<String>>(emptyList())
    val commandHistory = _commandHistory.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    fun executeCommand(command: String) {
        if (command.isBlank()) return
        
        // Add to history
        _commandHistory.update { (listOf(command) + it).take(50) }
        
        // Log the command itself
        addLog("> $command", "COMMAND")

        when (command.lowercase().trim()) {
            "help" -> showHelp()
            "clear" -> clearLogs()
            "lsusb" -> runLsusb()
            "id" -> runIdentification()
            else -> {
                if (command.startsWith("adb ")) {
                    runAdbCommand(command.removePrefix("adb ").trim())
                } else {
                    addLog("Unknown command: $command. Use 'adb <cmd>' for shell access.", "ERROR")
                }
            }
        }
    }

    private fun showHelp() {
        addLog("Available Commands:", "INFO")
        addLog("  help       - Show this menu", "INFO")
        addLog("  clear      - Clear terminal screen", "INFO")
        addLog("  lsusb      - List connected USB devices", "INFO")
        addLog("  id         - Identify connected mobile hardware", "INFO")
        addLog("  adb <cmd>  - Run ADB shell command (e.g., adb getprop)", "INFO")
    }

    private fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun runLsusb() {
        viewModelScope.launch {
            val devices = lifecycleManager.getDiscoveredDevices()
            if (devices.isEmpty()) {
                addLog("No USB devices detected.", "WARNING")
            } else {
                devices.forEach { dev ->
                    addLog("Device: VID=0x%04X PID=0x%04X [ID: ${dev.deviceId}]".format(dev.vendorId, dev.productId), "SUCCESS")
                }
            }
        }
    }

    private fun runIdentification() {
        _isProcessing.value = true
        hardwareManager.performMtkIdentification { result ->
            addLog(result, if (result.contains("MTK")) "SUCCESS" else "ERROR")
            _isProcessing.value = false
        }
    }

    private fun runAdbCommand(shellCmd: String) {
        _isProcessing.value = true
        adbManager.runShellCommand(shellCmd) { result ->
            result.lines().forEach { line ->
                if (line.isNotBlank()) addLog(line, "OUTPUT")
            }
            _isProcessing.value = false
        }
    }

    private fun addLog(message: String, type: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _logs.update { it + LogEntry(message, type, timestamp) }
    }
}
