package com.deepeye.otg.usb

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionHealth {
    HEALTHY,      // Last ping successful
    DEGRADED,     // 1-2 ping failures
    DEAD,         // 3+ failures → Trigger automatic session recovery
    PAUSED        // Watchdog suspended during critical flash ops
}

/**
 * Monitors the physical connection health via non-blocking USB GET_STATUS pings.
 * Useful for catching silent disconnects that don't trigger BroadcastReceivers.
 */
class UsbConnectionWatchdog(
    private val scope: CoroutineScope,
    private val pingProvider: suspend () -> Boolean,
    private val disconnectHandler: suspend () -> Unit,
    private val pingIntervalMs: Long = 5000L,
    private val maxMissedPings: Int = 3
) {
    private val TAG = "DeepEye-Watchdog"

    private val _health = MutableStateFlow(ConnectionHealth.HEALTHY)
    val health: StateFlow<ConnectionHealth> = _health.asStateFlow()

    private var missedPings = 0
    private var watchdogJob: Job? = null
    private var isPaused = false

    fun start() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch(Dispatchers.IO) {
            Log.i(TAG, "USB Watchdog Started (Interval: ${pingIntervalMs}ms)")
            while (isActive) {
                delay(pingIntervalMs)

                if (isPaused) continue

                val alive = pingProvider()

                if (alive) {
                    missedPings = 0
                    _health.value = ConnectionHealth.HEALTHY
                } else {
                    missedPings++
                    Log.w(TAG, "USB Watchdog missed $missedPings/$maxMissedPings pings.")

                    _health.value = when {
                        missedPings >= maxMissedPings -> ConnectionHealth.DEAD
                        else -> ConnectionHealth.DEGRADED
                    }

                    if (missedPings >= maxMissedPings) {
                        Log.e(TAG, "Connection DEAD. Forcing disconnect/cleanup.")
                        _health.value = ConnectionHealth.HEALTHY
                        missedPings = 0
                        disconnectHandler()
                    }
                }
            }
        }
    }

    /**
     * Pause watchdog during time-critical operations like flashing or full dumps.
     */
    fun pause() {
        isPaused = true
        _health.value = ConnectionHealth.PAUSED
        Log.d(TAG, "Watchdog PAUSED (Session protected)")
    }

    fun resume() {
        isPaused = false
        missedPings = 0
        _health.value = ConnectionHealth.HEALTHY
        Log.d(TAG, "Watchdog RESUMED")
    }

    fun stop() {
        watchdogJob?.cancel()
        watchdogJob = null
        Log.i(TAG, "Watchdog Stopped")
    }
}
