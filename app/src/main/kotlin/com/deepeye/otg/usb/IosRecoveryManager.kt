package com.deepeye.otg.usb

import android.hardware.usb.UsbDevice
import com.deepeye.otg.exploit.BruteForceExecutor
import com.deepeye.otg.exploit.BruteForcePayloads
import com.deepeye.otg.data.tauri.TauriBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class IosRecoveryState {
    object Idle : IosRecoveryState()
    data class DfuDetected(val chip: AppleDeviceMatrix.AppleChip) : IosRecoveryState()
    object RamdiskInjecting : IosRecoveryState()
    object BruteForceRunning : IosRecoveryState()
    data class Error(val message: String) : IosRecoveryState()
}

/**
 * Manages iPhone 15+ recovery orchestration, DFU handshakes, and PIN automation.
 */
@Singleton
class IosRecoveryManager @Inject constructor(
    private val bruteForceExecutor: BruteForceExecutor,
    private val tauriBridge: TauriBridge
) {
    private val _recoveryState = MutableStateFlow<IosRecoveryState>(IosRecoveryState.Idle)
    val recoveryState: StateFlow<IosRecoveryState> = _recoveryState

    /**
     * Listens for DFU state transitions and identifies A16/A17 chipsets.
     */
    fun onDeviceAttached(device: UsbDevice) {
        val sessionId = UUID.randomUUID().toString()
        if (device.vendorId == DeviceMatrix.APPLE_VID) {
            val mode = DeviceMatrix.detectAppleMode(device.vendorId, device.productId)
            if (mode == DeviceMatrix.AppleMode.DFU) {
                // Detect chip based on Product ID and USB descriptor analysis
                val chip = detectAppleChip(device)
                _recoveryState.value = IosRecoveryState.DfuDetected(chip)
                Timber.d("[IosRecoveryManager] DFU Detected: $chip sessionId=$sessionId")
            }
        }
    }

    /**
     * Detects Apple chip type from USB device properties.
     */
    private fun detectAppleChip(device: UsbDevice): AppleDeviceMatrix.AppleChip {
        return when (device.productId) {
            // iPhone 14 Pro / A16
            0x12a8, 0x12a9, 0x12aa, 0x12ab -> AppleDeviceMatrix.AppleChip.A16
            // iPhone 15 Pro / A17 Pro
            0x18a0, 0x18a1, 0x18a2, 0x18a3 -> AppleDeviceMatrix.AppleChip.A17
            // iPhone 13 / A15
            0x1280, 0x1281, 0x1282, 0x1283 -> AppleDeviceMatrix.AppleChip.A15
            // iPhone 12 / A14
            0x1240, 0x1241, 0x1242, 0x1243 -> AppleDeviceMatrix.AppleChip.A14
            // Default fallback
            else -> {
                Timber.w("[IosRecoveryManager] Unknown Apple device PID: 0x${device.productId.toString(16)}, defaulting to A16")
                AppleDeviceMatrix.AppleChip.A16
            }
        }
    }

    /**
     * Orchestrates the bridge between Tauri's Rust backend and Android's BruteForceExecutor.
     */
    suspend fun startAutomatedRecovery(pins: List<String>) {
        val sessionId = UUID.randomUUID().toString()
        try {
            _recoveryState.value = IosRecoveryState.RamdiskInjecting
            Timber.d("[IosRecoveryManager] Injecting recovery ramdisk sessionId=$sessionId")
            
            // [HYPOTHESIS] Triggering proprietary ramdisk load via Tauri bridge
            // A16/A17 requires specific exploit chain before ramdisk boot
            val ramdiskResult = tauriBridge.runCommand("ios_boot_ramdisk", mapOf("mode" to "recovery_audit"))
            Timber.d("[IosRecoveryManager] Ramdisk result: $ramdiskResult sessionId=$sessionId")

            _recoveryState.value = IosRecoveryState.BruteForceRunning
            Timber.d("[IosRecoveryManager] Starting PIN synchronization sessionId=$sessionId")

            bruteForceExecutor.runBruteForce(
                pins = pins,
                onProgress = { msg, progress ->
                    Timber.d("[IosRecoveryManager] Sync: $msg ($progress%) sessionId=$sessionId")
                },
                sessionId = sessionId
            )

        } catch (e: Exception) {
            _recoveryState.value = IosRecoveryState.Error(e.message ?: "Unknown recovery error")
            Timber.e(e, "[IosRecoveryManager] Recovery failed sessionId=$sessionId")
        }
    }
}
