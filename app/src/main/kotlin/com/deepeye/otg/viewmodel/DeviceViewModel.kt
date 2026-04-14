package com.deepeye.otg.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import androidx.lifecycle.*
import com.deepeye.otg.device.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed class UiEvent {
    object PickDaFile : UiEvent()
    object PickProgrammerFile : UiEvent()
    object PickFlashImage : UiEvent()
}
data class ProtocolLog(
    val time:    String = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                         .format(java.util.Date()),
    val level:   String = "INFO",   // INFO / SUCCESS / ERROR / WARN
    val message: String,
)

class DeviceViewModel(app: Application) : AndroidViewModel(app) {

    // ── State ─────────────────────────────────────────────────
    private val _devices    = MutableStateFlow<List<DetectedDevice>>(emptyList())
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

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _flashStep = MutableStateFlow("Flashing...")
    val flashStep: StateFlow<String> = _flashStep.asStateFlow()

    val uiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val context = app.applicationContext

    // ── USB Hotplug Receiver ──────────────────────────────────
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    log("USB device attached — scanning...")
                    scanDevices()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    log("USB device detached")
                    _activeDevice.value = null
                    _deviceInfo.value   = null
                    _chipInfo.value     = null
                    scanDevices()
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(usbReceiver, filter)
        scanDevices()
        startPolling()
    }

    override fun onCleared() {
        super.onCleared()
        context.unregisterReceiver(usbReceiver)
    }

    // ── Scan USB devices ──────────────────────────────────────
    fun scanDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            val found = DeviceDetector.scanDevices(context)
            _devices.emit(found)
            // Auto-select first interesting device
            val priority = found.firstOrNull {
                it.mode in listOf(DeviceMode.BROM, DeviceMode.EDL,
                                  DeviceMode.ADB,  DeviceMode.FASTBOOT)
            }
            if (priority != null && priority != _activeDevice.value) {
                _activeDevice.emit(priority)
                log("Device detected: ${priority.mode} — ${priority.productName ?: priority.vid.toString(16)}")
                autoFetchInfo(priority)
            }
        }
    }

    // USB device attach (called from MainActivity after permission granted)
    fun onUsbDeviceAttached(usbDevice: UsbDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            val detected = DeviceDetector.fromUsbDevice(usbDevice)
            if (detected != null) {
                val current = _devices.value.toMutableList()
                current.removeAll { it.deviceName == detected.deviceName }
                current.add(detected)
                _devices.emit(current)
                log("✓ Connected: ${detected.mode.name} — VID:${"%04X".format(detected.vid)} PID:${"%04X".format(detected.pid)}")
                
                // Auto-fetch ADB info if ADB device
                if (detected.mode == DeviceMode.ADB) {
                    delay(800) // wait for ADB daemon
                    autoFetchInfo(detected)
                }
            }
        }
    }

    fun onUsbDeviceDetached(usbDevice: UsbDevice) {
        val current = _devices.value.toMutableList()
        current.removeAll { it.deviceName == usbDevice.deviceName }
        _devices.value = current
        if (current.isEmpty()) _deviceInfo.value = null
        log("⚠ Disconnected: ${usbDevice.deviceName}")
    }

    private fun startPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(1500)
                val found = DeviceDetector.scanDevices(context)
                if (found.map { it.pid } != _devices.value.map { it.pid }) {
                    _devices.emit(found)
                }
            }
        }
    }

    private fun autoFetchInfo(device: DetectedDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            when (device.mode) {
                DeviceMode.ADB -> {
                    device.serial?.let { serial ->
                        AdbEngine.getFullInfo(serial)
                            .onSuccess { _deviceInfo.emit(it); log("ADB info fetched ✓") }
                            .onFailure { log("ADB info failed: ${it.message}", "WARN") }
                    }
                }
                DeviceMode.FASTBOOT -> {
                    device.serial?.let { serial ->
                        val info = FastbootEngine.getFullInfo(serial).getOrDefault(emptyMap())
                        log("Fastboot: ${info.entries.joinToString { "${it.key}=${it.value}" }}")
                    }
                }
                DeviceMode.BROM -> connectBrom()
                DeviceMode.EDL  -> log("EDL device ready — load programmer to proceed")
                else -> {}
            }
        }
    }

    // ── MTK BROM ──────────────────────────────────────────────
    fun connectBrom() {
        viewModelScope.launch(Dispatchers.IO) {
            _isConnecting.emit(true)
            log("Connecting to MTK BROM...")
            val usbDevice = DeviceDetector.findUsbDevice(context, 0x0E8D, 0x0003)
                ?: run { logError("No BROM device found"); _isConnecting.emit(false); return@launch }
            val session = MtkBromSession(context, usbDevice)
            try {
                session.open().onFailure {
                    logError(it.message)
                    logBromRecoveryHints()
                    return@launch
                }
                session.handshake()
                    .onSuccess { log(it, "SUCCESS") }
                    .onFailure {
                        logError(it.message)
                        logBromRecoveryHints()
                        return@launch
                    }
                session.getHwCode()
                    .onSuccess {
                        _chipInfo.emit(it)
                        log("Chip: ${it.chipName} (${it.arch}) HW=0x${it.hwCode.toString(16).uppercase()}", "SUCCESS")
                    }
                    .onFailure {
                        logError(it.message)
                        logBromRecoveryHints()
                    }
            } finally {
                session.close()
                _isConnecting.emit(false)
            }
        }
    }

    fun sendDa(daPath: String, daAddr: Long = 0x201000L) {
        viewModelScope.launch(Dispatchers.IO) {
            _isConnecting.emit(true)
            log("Loading DA from: $daPath")
            val daBytes = java.io.File(daPath).readBytes()
            val usbDevice = DeviceDetector.findUsbDevice(context, 0x0E8D, 0x0003)
                ?: run { logError("No BROM device"); _isConnecting.emit(false); return@launch }
            val session = MtkBromSession(context, usbDevice)
            try {
                session.open().onFailure {
                    logError(it.message)
                    logBromRecoveryHints()
                    return@launch
                }
                session.handshake().onFailure {
                    logError(it.message)
                    logBromRecoveryHints()
                    return@launch
                }
                session.disableWatchdog()
                log("Sending DA (${daBytes.size / 1024}KB)...")
                session.sendDa(daBytes, daAddr)
                    .onSuccess { log("DA sent ✓", "SUCCESS") }
                    .onFailure {
                        logError(it.message)
                        logBromRecoveryHints()
                        return@launch
                    }
                session.jumpDa(daAddr)
                    .onSuccess { log("Jumped to DA — waiting for DA protocol...", "SUCCESS") }
                    .onFailure { logError(it.message) }
            } finally {
                session.close()
                _isConnecting.emit(false)
            }
        }
    }

    // ── Qualcomm EDL ──────────────────────────────────────────
    fun connectEdl() {
        viewModelScope.launch(Dispatchers.IO) {
            _isConnecting.emit(true)
            log("Connecting to Qualcomm EDL (Sahara)...")
            val usbDevice = DeviceDetector.findUsbDevice(context, 0x05C6, 0x9008)
                ?: run { logError("No EDL device (VID:05C6 PID:9008)"); _isConnecting.emit(false); return@launch }
            val session = QcomSaharaSession(context, usbDevice)
            session.open().onFailure { logError(it.message); _isConnecting.emit(false); return@launch }
            session.hello()
                .onSuccess { log("Sahara v$it connected ✓", "SUCCESS") }
                .onFailure { logError(it.message) }
            _isConnecting.emit(false)
        }
    }

    fun sendProgrammer(programmerPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isConnecting.emit(true)
            log("Sending programmer: $programmerPath")
            val prog = java.io.File(programmerPath).readBytes()
            val usbDevice = DeviceDetector.findUsbDevice(context, 0x05C6, 0x9008)
                ?: run { logError("No EDL device"); _isConnecting.emit(false); return@launch }
            val session = QcomSaharaSession(context, usbDevice)
            session.open().onFailure { logError(it.message); _isConnecting.emit(false); return@launch }
            session.hello().onFailure { logError(it.message); _isConnecting.emit(false); return@launch }
            session.sendProgrammer(prog) { pct ->
                viewModelScope.launch { log("Programmer upload: $pct%") }
            }
            .onSuccess { log("Programmer loaded ✓ — Firehose ready", "SUCCESS") }
            .onFailure { logError(it.message) }
            _isConnecting.emit(false)
        }
    }

    // ── ADB Operations ────────────────────────────────────────
    fun adbShell(serial: String, cmd: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            AdbEngine.shell(serial, cmd)
                .onSuccess { onResult(it); log("shell> $cmd\n$it") }
                .onFailure { logError(it.message) }
        }
    }

    fun adbReboot(serial: String, mode: String) {
        viewModelScope.launch {
            log("Rebooting to $mode...")
            AdbEngine.reboot(serial, mode)
                .onSuccess { log("Reboot to $mode sent ✓", "SUCCESS") }
                .onFailure { logError(it.message) }
        }
    }

    // ── Fastboot ─────────────────────────────────────────────
    fun fastbootFlash(serial: String, partition: String, imagePath: String) {
        viewModelScope.launch {
            log("Flashing $partition via fastboot...")
            FastbootEngine.flash(serial, partition, imagePath)
                .onSuccess { log("Flash $partition done ✓\n$it", "SUCCESS") }
                .onFailure { logError(it.message) }
        }
    }

    fun fastbootInfo(device: DetectedDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                device.serial?.let { serial ->
                    val info = FastbootEngine.getFullInfo(serial).getOrDefault(emptyMap())
                    log("FASTBOOT INFO:\n${info.entries.joinToString("\n") { "${it.key}=${it.value}" }}")
                }
            }.onFailure { logError(it.message) }
        }
    }

    fun fastbootErase(device: DetectedDevice?, partition: String) {
        if (device == null) { logError("ERROR: No fastboot device connected"); return }
        viewModelScope.launch(Dispatchers.IO) {
            log("Erasing partition: $partition")
            runCatching {
                device.serial?.let { serial ->
                    FastbootEngine.erase(serial, partition)
                    log("✓ Erased: $partition")
                }
            }.onFailure { logError(it.message) }
        }
    }

    fun fastbootOemUnlock(device: DetectedDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            log("⚠ OEM Unlock initiated — device may wipe!")
            runCatching {
                device.serial?.let { serial ->
                    FastbootEngine.oemUnlock(serial)
                    log("✓ OEM Unlock command sent")
                }
            }.onFailure { logError(it.message) }
        }
    }

    fun fastbootReboot(device: DetectedDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                device.serial?.let { serial ->
                    FastbootEngine.reboot(serial)
                    log("✓ Reboot command sent")
                }
            }.onFailure { logError(it.message) }
        }
    }

    fun firehoseFlash(partition: String, imagePath: String) {
        log("Flashing $partition with $imagePath...")
    }

    // ── Testpoint ────────────────────────────────────────────
    fun getTestpointGuide(model: String, chipset: String): TestpointGuide =
        TestpointDb.getGuide(model, chipset)

    // ── Utility ──────────────────────────────────────────────
    fun clearLogs() { _logs.value = emptyList() }
    fun dismissError() { _error.value = null }

    // ── Logging ───────────────────────────────────────────────
    private fun log(msg: String, level: String = "INFO") {
        viewModelScope.launch {
            val entry = ProtocolLog(level = level, message = msg)
            _logs.emit((_logs.value + entry).takeLast(200))
        }
    }
    private fun logError(msg: String?) { log(msg ?: "Unknown error", "ERROR"); _error.value = msg }

    private fun logBromRecoveryHints() {
        log("Use original/high-quality USB cable", "WARN")
        log("Connect directly to PC USB 2.0 port", "WARN")
        log("Hold Vol- while connecting USB", "WARN")
        log("Power device OFF before BROM connect", "WARN")
    }
}
