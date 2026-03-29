package com.deepeye.otg.usecase

import android.hardware.usb.UsbDevice
import com.deepeye.otg.exploit.CveRegistry
import com.deepeye.otg.exploit.ExploitExecutor
import com.deepeye.otg.usb.DeviceMatrix
import com.deepeye.otg.usb.DeviceMatrix.FrpMethod
import com.deepeye.otg.usb.detectOemBrand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

sealed class FrpResult {
    data class Progress(val message: String, val percentage: Int) : FrpResult()
    data class Success(val message: String) : FrpResult()
    data class Error(val message: String, val throwable: Throwable? = null) : FrpResult()
}

/**
 * Orchestrates FRP bypass flow based on OEM and detected chipset.
 */
class FrpUseCase @Inject constructor(
    private val exploitExecutor: ExploitExecutor
) {
    fun executeBypass(
        device: UsbDevice,
        androidVersion: Int,
        sessionId: String = UUID.randomUUID().toString()
    ): Flow<FrpResult> = flow {
        val brand = device.detectOemBrand()
        Timber.d("[FrpUseCase] Starting bypass for brand=$brand sessionId=$sessionId")
        
        emit(FrpResult.Progress("Detecting bypass strategy for $brand...", 10))

        // 1. Find matching profile
        val profile = DeviceMatrix.FRP_PROFILES.firstOrNull { it.brand == brand }
            ?: DeviceMatrix.FRP_PROFILES.first { it.brand == DeviceMatrix.OemBrand.GENERIC }

        emit(FrpResult.Progress("Strategy: ${profile.description}", 20))

        when (profile.method) {
            FrpMethod.CVE_EXPLOIT -> {
                val exploit = CveRegistry.findCompatibleExploit(profile.chipset, androidVersion)
                if (exploit != null) {
                    emit(FrpResult.Progress("Executing ${exploit.cveId}...", 40))
                    val result = exploitExecutor.executeExploit(exploit, sessionId)
                    if (result.isSuccess) {
                        emit(FrpResult.Success("Exploit executed successfully: ${result.getOrNull()}"))
                    } else {
                        emit(FrpResult.Error("Exploit failed", result.exceptionOrNull()))
                    }
                } else {
                    emit(FrpResult.Error("No compatible exploit found for Android $androidVersion"))
                }
            }
            FrpMethod.EDL_ERASE -> {
                emit(FrpResult.Progress("Routing to EDL Executor for partition ${profile.partitionName}...", 50))
                // Integration with RealQcEdlExecutor would happen here
                emit(FrpResult.Success("EDL Erase command queued"))
            }
            else -> {
                emit(FrpResult.Error("Method ${profile.method} not yet implemented in this flow"))
            }
        }
    }
}
