package com.deepeye.otg.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("deepeye_settings", Context.MODE_PRIVATE)
    
    private val _performanceMode = MutableStateFlow(prefs.getBoolean("perf_mode", false))
    val performanceMode: StateFlow<Boolean> = _performanceMode
    
    fun togglePerformanceMode() {
        val newVal = !_performanceMode.value
        prefs.edit().putBoolean("perf_mode", newVal).apply()
        _performanceMode.value = newVal
    }
}
