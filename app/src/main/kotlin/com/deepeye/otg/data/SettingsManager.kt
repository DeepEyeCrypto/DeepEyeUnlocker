package com.deepeye.otg.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("deepeye_settings", Context.MODE_PRIVATE)
    
    // Performance Mode
    private val _performanceMode = MutableStateFlow(prefs.getBoolean("perf_mode", false))
    val performanceMode: StateFlow<Boolean> = _performanceMode.asStateFlow()
    
    // Detection Settings
    private val _adbSignatureRequired = MutableStateFlow(prefs.getBoolean("adb_sig_req", true))
    val adbSignatureRequired: StateFlow<Boolean> = _adbSignatureRequired.asStateFlow()
    
    private val _debounceAttach = MutableStateFlow(prefs.getBoolean("debounce_attach", true))
    val debounceAttach: StateFlow<Boolean> = _debounceAttach.asStateFlow()
    
    private val _permissionTimeout = MutableStateFlow(prefs.getInt("perm_timeout", 10)) // Seconds
    val permissionTimeout: StateFlow<Int> = _permissionTimeout.asStateFlow()
    
    // Display Settings
    private val _showDebugPanel = MutableStateFlow(prefs.getBoolean("show_debug", false))
    val showDebugPanel: StateFlow<Boolean> = _showDebugPanel.asStateFlow()
    
    private val _showDetectionReason = MutableStateFlow(prefs.getBoolean("show_reason", true))
    val showDetectionReason: StateFlow<Boolean> = _showDetectionReason.asStateFlow()
    
    private val _monospaceHex = MutableStateFlow(prefs.getBoolean("mono_hex", true))
    val monospaceHex: StateFlow<Boolean> = _monospaceHex.asStateFlow()
    
    // Advanced Settings
    private val _forceReclassify = MutableStateFlow(prefs.getBoolean("force_reclassify", true))
    val forceReclassify: StateFlow<Boolean> = _forceReclassify.asStateFlow()
    
    private val _logUsbToFile = MutableStateFlow(prefs.getBoolean("log_usb_file", false))
    val logUsbToFile: StateFlow<Boolean> = _logUsbToFile.asStateFlow()
    
    // Toggles & Setters
    fun togglePerformanceMode() = toggle("perf_mode", _performanceMode)
    fun toggleAdbSignature() = toggle("adb_sig_req", _adbSignatureRequired)
    fun toggleDebounceAttach() = toggle("debounce_attach", _debounceAttach)
    fun setPermissionTimeout(seconds: Int) {
        prefs.edit().putInt("perm_timeout", seconds).apply()
        _permissionTimeout.value = seconds
    }
    
    fun toggleShowDebugPanel() = toggle("show_debug", _showDebugPanel)
    fun toggleShowDetectionReason() = toggle("show_reason", _showDetectionReason)
    fun toggleMonospaceHex() = toggle("mono_hex", _monospaceHex)
    
    fun toggleForceReclassify() = toggle("force_reclassify", _forceReclassify)
    fun toggleLogUsbToFile() = toggle("log_usb_file", _logUsbToFile)

    private fun toggle(key: String, flow: MutableStateFlow<Boolean>) {
        val newVal = !flow.value
        prefs.edit().putBoolean(key, newVal).apply()
        flow.value = newVal
    }
}
