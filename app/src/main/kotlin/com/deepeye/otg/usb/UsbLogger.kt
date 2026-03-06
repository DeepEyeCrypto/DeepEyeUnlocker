package com.deepeye.otg.usb

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Structured circular logging for USB operations and terminal display.
 */
object UsbLogger {
    private const val MAX_LOGS = 500
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    private val _logBuffer = MutableStateFlow<List<String>>(emptyList())
    val logBuffer: StateFlow<List<String>> = _logBuffer.asStateFlow()

    fun debug(tag: String, msg: String) = append(Log.DEBUG, tag, msg)
    fun info(tag: String, msg: String) = append(Log.INFO, tag, msg)
    fun warn(tag: String, msg: String) = append(Log.WARN, tag, msg)
    fun error(tag: String, msg: String, err: Throwable? = null) = append(Log.ERROR, tag, "$msg ${err?.message ?: ""}")

    private fun append(priority: Int, tag: String, msg: String) {
        val timestamp = timeFormat.format(Date())
        val priorityChar = when (priority) {
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            else -> "V"
        }
        
        val line = "[$timestamp] $priorityChar/$tag: $msg"
        Log.println(priority, tag, msg)

        // Atomic update for terminal flow to avoid race conditions
        val currentLogs = _logBuffer.value.toMutableList()
        currentLogs.add(line)
        if (currentLogs.size > MAX_LOGS) {
            currentLogs.removeAt(0)
        }
        _logBuffer.value = currentLogs
    }

    fun clear() {
        _logBuffer.value = emptyList()
    }

    fun dump(): String = _logBuffer.value.joinToString("\n")
}
