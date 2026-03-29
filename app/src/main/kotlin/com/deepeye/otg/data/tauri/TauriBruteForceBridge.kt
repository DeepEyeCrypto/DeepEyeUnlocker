package com.deepeye.otg.data.tauri

import com.deepeye.otg.exploit.BruteForcePayloads
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridge to synchronize PIN brute-force sequences between Android OTG and Tauri Rust backend.
 */
@Singleton
class TauriBruteForceBridge @Inject constructor(
    private val tauriBridge: TauriBridge
) {
    /**
     * Synchronizes a PIN sequence to the Rust backend for high-speed execution.
     */
    suspend fun syncAndExecute(pins: List<String>, delayMs: Long = 2000): Result<String> {
        val sessionId = UUID.randomUUID().toString()
        Timber.d("[TauriBruteForceBridge] Syncing ${pins.size} PINs sessionId=$sessionId")
        
        return try {
            // Map Kotlin list to JSON-compatible format for Tauri
            val args = mapOf(
                "pins" to pins,
                "delay_ms" to delayMs
            )
            
            val response = tauriBridge.runCommand("run_pin_bruteforce", args)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "[TauriBruteForceBridge] Sync failed sessionId=$sessionId")
            Result.failure(e)
        }
    }
}
