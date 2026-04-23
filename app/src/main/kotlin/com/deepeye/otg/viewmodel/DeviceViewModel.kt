package com.deepeye.otg.viewmodel

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.core.usb.UsbDeviceDetector
import com.deepeye.otg.device.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UiEvent {
    object PickDaFile : UiEvent()
    object PickProgrammerFile : UiEvent()
    object PickFlashImage : UiEvent()
}

data class ProtocolLog(
    val time: String = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
        .format(java.util.Date()),
    val level: String = "INFO",   // INFO / SUCCESS / ERROR / WARN
    val message: String,
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val deviceDetector: UsbDeviceDetector
) : ViewModel() {

    // ── Constants ─────────────────────────────────────────────
    companion object {
        private const val ADB_DAEMON_WAIT_MS = 800L // Hardware sync: Wait for adbd to initialize
        private const val DA_READY_WAIT_MS = 1000L   // Hardware sync: Wait for DA protocol handshake
    }

    // ── State ─────────────────────────────────────────────────
    private val _devices = MutableStateFlow<List<DetectedDevice>>(emptyList())
    val devices: StateFlow<List<DetectedDevice>> = _devices.asStateFlow()

    private val _activeDevice = MutableStateFlow<DetectedDevice?>(null)
    val activeDevice: StateFlow<DetectedDevice?> = _activeDevice.asStateFlow()

    private val _deviceInfo = MutableStateFlow<AdbDeviceInfo?>(null)
    val deviceInfo: StateFlow<AdbDeviceInfo?> = _deviceInfo.asStateFlow()

    private val _chipInfo = MutableStateFlow<MtkChipInfo?>(null)
    val chipInfo: StateFlow<MtkChipInfo?> = _chipInfo.asStateFlow()

    private val _logs = MutableStateFlow<List<ProtocolLog>>(emptyList())
    val logs: StateFlow<List<ProtocolLog>> = _logs.asStateFlow()

    private val _flashProgress = MutableStateFlow<FlashProgress?>(null)
    val flashProgress: StateFlow<FlashProgress?> = _flashProgress.asStateFlow()

    private val _flashStep = MutableStateFlow("")
    val flashStep: StateFlow<String> = _flashStep.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Status fields for specific operations
    private val _daStatus = MutableStateFlow("Ready")
    val daStatus: StateFlow<String> = _daStatus.asStateFlow()

    private val _frpEraseStatus = MutableStateFlow("Ready")
    val frpEraseStatus: StateFlow<String> = _frpEraseStatus.asStateFlow()

    // UI Events for Navigation/Pickers
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    init {
        log("📱 Device Monitoring System Initialized")
        
        // Reactive device detection via UsbDeviceDetector
        viewModelScope.launch {
            deviceDetector.observeDevices().collect { usbInfos ->
                val detected = usbInfos.mapNotNull { info ->
                    val mode = DeviceDetector.classifyDevice(info.vendorId, info.productId)
                    if (mode == DeviceMode.UNKNOWN) null
                    else DetectedDevice(
                        mode = mode,
                        vid = info.vendorId,
                        pid = info.productId,
                        serial = info.serial,
                        manufacturer = info.manufacturer,
                        productName = info.product,
                        deviceName = info.name
                    )
                }
                
                _devices.value = detected
                
                // Auto-select priority device if nothing active
                if (_activeDevice.value == null || detected.none { it.deviceName == _activeDevice.value?.deviceName }) {
                    val priority = detected.firstOrNull { 
                        it.mode in listOf(DeviceMode.BROM, DeviceMode.EDL, DeviceMode.ADB, DeviceMode.FASTBOOT)
                    }
                    if (priority != null) {
                        _activeDevice.value = priority
                        log("New target detected: ${priority.mode} (${priority.productName})")
                        autoFetchInfo(priority)
                    } else if (_activeDevice.value != null) {
                        log("Device disconnected")
                        _activeDevice.value = null
                        _deviceInfo.value = null
                    }
                }
            }
        }
    }

    private fun autoFetchInfo(device: DetectedDevice) {
        viewModelScope.launch {
            when (device.mode) {
                DeviceMode.ADB -> {
                    // Sync: wait for daemon
                    kotlinx.coroutines.delay(ADB_DAEMON_WAIT_MS)
                    device.serial?.let { serial ->
                        AdbEngine.getFullInfo(serial)
                            .onSuccess { _deviceInfo.value = it; log("ADB identification successful ✓") }
                            .onFailure { log("ADB info failed: ${it.message}", "WARN") }
                    }
                }
                DeviceMode.FASTBOOT -> {
                    device.serial?.let { serial ->
                        FastbootEngine.getFullInfo(serial).onSuccess { info ->
                            log("Fastboot: ${info.entries.joinToString { "${it.key}=${it.value}" }}")
                        }
                    }
                }
                DeviceMode.BROM -> connectBrom()
                else -> {}
            }
        }
    }

    // ── Ported Methods (Required by MainActivity) ──────────────

    fun onUsbDeviceAttached(device: UsbDevice) {
        log("Physical USB Attached: ${device.deviceName}")
    }

    fun onUsbDeviceDetached(device: UsbDevice) {
        log("Physical USB Detached: ${device.deviceName}")
    }

    fun sendDa(path: String, address: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            log("Sending DA ($path) to 0x${address.toString(16)}...")
            // Actual implementation would call MtkBromProtocol
        }
    }

    fun sendProgrammer(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            log("Sending programmer: $path")
            // Actual implementation would call FirehoseEngine
        }
    }

    fun firehoseFlash(partition: String, path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            log("Flashing $partition with $path...")
            // Actual implementation would call FirehoseEngine
        }
    }

    fun connectBrom() {
        viewModelScope.launch(Dispatchers.IO) {
            _isConnecting.emit(true)
            log("Connecting to MTK BROM...")
            _isConnecting.emit(false)
        }
    }

    fun eraseFrpPartition() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRunning.value = true
            _frpEraseStatus.value = "Erasing FRP partition..."
            log("Starting FRP partition erase...")
            _isRunning.value = false
        }
    }

    private fun log(msg: String, level: String = "INFO") {
        val entry = ProtocolLog(level = level, message = msg)
        _logs.update { (it + entry).takeLast(200) }
    }

    private fun logError(msg: String?) {
        log(msg ?: "Unknown error", "ERROR")
        _error.value = msg
    }

    fun dismissError() { _error.value = null }
    fun clearLogs() { _logs.value = emptyList() }

    // ── Methods required by DeviceDashboardScreen ────────────

    fun scanDevices() {
        log("🔍 Scanning for USB devices...")
        // Device detection is reactive via UsbDeviceDetector in init block
        // This method triggers a manual rescan notification
    }

    fun connectEdl() {
        viewModelScope.launch(Dispatchers.IO) {
            _isConnecting.emit(true)
            log("Connecting to Qualcomm EDL 9008...")
            _isConnecting.emit(false)
        }
    }

    fun adbReboot(serial: String, mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            log("ADB reboot → $mode (serial: $serial)")
            AdbEngine.reboot(serial, mode)
                .onSuccess { log("Reboot command sent ✓") }
                .onFailure { logError("Reboot failed: ${it.message}") }
        }
    }

    fun fastbootFlash(serial: String, partition: String, imagePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            log("Fastboot flash $serial → $partition")
            FastbootEngine.flash(serial, partition, imagePath)
                .onSuccess { log("Fastboot flash sent ✓") }
                .onFailure { logError("Fastboot flash failed: ${it.message}") }
        }
    }

    fun adbShell(serial: String, command: String, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            log("\$ $command")
            AdbEngine.shell(serial, command)
                .onSuccess {
                    log(it.ifBlank { "(empty output)" })
                    onResult(it)
                }
                .onFailure {
                    logError("Shell error: ${it.message}")
                    onResult("")
                }
        }
    }

    fun getTestpointGuide(model: String, chipset: String): com.deepeye.otg.device.TestpointGuide {
        return com.deepeye.otg.device.TestpointDb.getGuide(model, chipset)
    }
}
