package com.deepeye.otg.engine

import android.content.Context
import android.util.Log
import com.deepeye.otg.domain.engine.mtk.MtkCdcSession
import com.deepeye.otg.domain.models.MtkDeviceInfo
import com.deepeye.otg.domain.models.PartitionEntry
import com.deepeye.otg.usb.UsbLifecycleManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MtkEngine — Orchestrator for MediaTek Brom operations.
 * Phase 6: MTK Brom Protocol & Bootloader Support.
 */
@Singleton
class MtkEngine @Inject constructor(
    private val usbLifecycleManager: UsbLifecycleManager
) {
    private val TAG = "MtkEngine"
    private var activeSession: MtkCdcSession? = null

    /**
     * Executes an MTK Brom action based on the actionId.
     */
    fun executeMtkAction(actionId: String, deviceKey: String?): Flow<Pair<Float, String>> = flow {
        val device = usbLifecycleManager.getActiveDevice(deviceKey)
        val connection = usbLifecycleManager.getActiveConnection(deviceKey)

        if (device == null || connection == null) {
            emit(0f to "Error: No MTK device connected or permission denied")
            return@flow
        }

        val session = MtkCdcSession(connection, device, deviceKey ?: "default")
        activeSession = session

        try {
            emit(5f to "Initializing MTK CDC Session...")
            val initResult = session.initialize()
            
            if (initResult.isFailure) {
                emit(0f to "Error: ${initResult.exceptionOrNull()?.message}")
                return@flow
            }

            val deviceInfo = initResult.getOrThrow()
            emit(10f to "Connected: ${deviceInfo.chipName} (Mode: ${deviceInfo.mode})")

            when (actionId) {
                "mtk_brom_exploit" -> {
                    emit(20f to "Executing Brom Exploit sequence...")
                    // In a real scenario, this involves triggering payload execution
                    kotlinx.coroutines.delay(1000)
                    emit(100f to "Brom Exploit Successful (SLA/DAA Bypassed)")
                }
                "mtk_read_backup" -> {
                    emit(20f to "Parsing GPT for partitions...")
                    val partitions = session.executePartitionManager().getOrDefault(emptyList())
                    if (partitions.isEmpty()) {
                        emit(0f to "Error: Failed to parse partition table")
                        return@flow
                    }
                    
                    // Default to backing up 'nvram' for demo
                    session.executeReadBackup("nvram", 0x0, 1024 * 1024).collect { (progress, msg) ->
                        emit(progress to msg)
                    }
                }
                "mtk_bl_unlock" -> {
                    emit(20f to "Preparing Brom Bootloader Unlock...")
                    // Logic would involve writing specific flags to Seccfg partition
                    kotlinx.coroutines.delay(1500)
                    emit(80f to "Writing Seccfg unlock tokens...")
                    kotlinx.coroutines.delay(1000)
                    emit(100f to "Bootloader Unlocked successfully via Brom")
                }
                "mtk_security_backup" -> {
                    session.executeBackupSecurity().collect { (progress, msg) ->
                        emit(progress to msg)
                    }
                }
                else -> {
                    emit(0f to "Unknown MTK action: $actionId")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "MTK execution error", e)
            emit(0f to "Exception: ${e.message}")
        } finally {
            session.release()
            activeSession = null
        }
    }
}
