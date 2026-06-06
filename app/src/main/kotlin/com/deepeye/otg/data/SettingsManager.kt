package com.deepeye.otg.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "deepeye_settings")

class SettingsManager @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    // Scope for background disk I/O
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Preference Keys
    private val KEY_PERF_MODE = booleanPreferencesKey("perf_mode")
    private val KEY_ADB_SIG_REQ = booleanPreferencesKey("adb_sig_req")
    private val KEY_DEBOUNCE_ATTACH = booleanPreferencesKey("debounce_attach")
    private val KEY_PERM_TIMEOUT = intPreferencesKey("perm_timeout")
    private val KEY_SHOW_DEBUG = booleanPreferencesKey("show_debug")
    private val KEY_SHOW_REASON = booleanPreferencesKey("show_reason")
    private val KEY_MONO_HEX = booleanPreferencesKey("mono_hex")
    private val KEY_FORCE_RECLASSIFY = booleanPreferencesKey("force_reclassify")
    private val KEY_LOG_USB_FILE = booleanPreferencesKey("log_usb_file")
    
    // Performance Mode
    val performanceMode: StateFlow<Boolean> = context.dataStore.data
        .map { it[KEY_PERF_MODE] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)
    
    // Detection Settings
    val adbSignatureRequired: StateFlow<Boolean> = context.dataStore.data
        .map { it[KEY_ADB_SIG_REQ] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)
    
    val debounceAttach: StateFlow<Boolean> = context.dataStore.data
        .map { it[KEY_DEBOUNCE_ATTACH] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)
    
    val permissionTimeout: StateFlow<Int> = context.dataStore.data
        .map { it[KEY_PERM_TIMEOUT] ?: 10 }
        .stateIn(scope, SharingStarted.Eagerly, 10)
    
    // Display Settings
    val showDebugPanel: StateFlow<Boolean> = context.dataStore.data
        .map { it[KEY_SHOW_DEBUG] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)
    
    val showDetectionReason: StateFlow<Boolean> = context.dataStore.data
        .map { it[KEY_SHOW_REASON] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)
    
    val monospaceHex: StateFlow<Boolean> = context.dataStore.data
        .map { it[KEY_MONO_HEX] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)
    
    // Advanced Settings
    val forceReclassify: StateFlow<Boolean> = context.dataStore.data
        .map { it[KEY_FORCE_RECLASSIFY] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)
    
    val logUsbToFile: StateFlow<Boolean> = context.dataStore.data
        .map { it[KEY_LOG_USB_FILE] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)
    
    // Toggles & Setters
    fun togglePerformanceMode() = toggle(KEY_PERF_MODE, performanceMode)
    fun toggleAdbSignature() = toggle(KEY_ADB_SIG_REQ, adbSignatureRequired)
    fun toggleDebounceAttach() = toggle(KEY_DEBOUNCE_ATTACH, debounceAttach)
    
    fun setPermissionTimeout(seconds: Int) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_PERM_TIMEOUT] = seconds
            }
        }
    }
    
    fun toggleShowDebugPanel() = toggle(KEY_SHOW_DEBUG, showDebugPanel)
    fun toggleShowDetectionReason() = toggle(KEY_SHOW_REASON, showDetectionReason)
    fun toggleMonospaceHex() = toggle(KEY_MONO_HEX, monospaceHex)
    
    fun toggleForceReclassify() = toggle(KEY_FORCE_RECLASSIFY, forceReclassify)
    fun toggleLogUsbToFile() = toggle(KEY_LOG_USB_FILE, logUsbToFile)

    private fun toggle(key: Preferences.Key<Boolean>, flow: StateFlow<Boolean>) {
        val newVal = !flow.value
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[key] = newVal
            }
        }
    }
}
