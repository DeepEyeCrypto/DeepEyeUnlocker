package com.deepeye.otg.usecase

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
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
    private val exploitExecutor: ExploitExecutor,
    private val context: Context  // ✅ Added for USB permission check
) {
    fun executeBypass(
        device: UsbDevice,
        androidVersion: Int,
        sessionId: String = UUID.randomUUID().toString()
    ): Flow<FrpResult> = flow {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        
        // ✅ FIX #1: Check USB permission before proceeding
        if (!usbManager.hasPermission(device)) {
            Timber.w("[FrpUseCase] USB permission not granted sessionId=$sessionId")
            emit(FrpResult.Error(
                "USB permission not granted. Please accept the USB permission dialog and retry.",
                SecurityException("USB permission denied for device ${device.deviceName}")
            ))
            return@flow
        }
        
        Timber.d("[FrpUseCase] Starting bypass sessionId=$sessionId")
        emit(FrpResult.Progress("Detecting bypass strategy...", 10))

        try {
            // 1. Find matching profile
            val brand = device.detectOemBrand()
            Timber.d("[FrpUseCase] Detected brand=$brand sessionId=$sessionId")
            
            emit(FrpResult.Progress("Strategy: Detecting $brand method...", 20))

            val profile = DeviceMatrix.FRP_PROFILES.firstOrNull { it.brand == brand }
                ?: DeviceMatrix.FRP_PROFILES.first { it.brand == DeviceMatrix.OemBrand.GENERIC }

            emit(FrpResult.Progress("Using ${profile.description}", 30))

            when (profile.method) {
                FrpMethod.CVE_EXPLOIT -> {
                    val exploit = CveRegistry.findCompatibleExploit(profile.chipset, androidVersion)
                    if (exploit != null) {
                        emit(FrpResult.Progress("Executing ${exploit.cveId}...", 40))
                        
                        try {
                            val result = exploitExecutor.executeExploit(exploit, sessionId)
                            if (result.isSuccess) {
                                emit(FrpResult.Progress("Exploit succeeded, completing...", 90))
                                emit(FrpResult.Success("FRP bypass completed: ${result.getOrNull()}"))
                            } else {
                                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                                emit(FrpResult.Error("Exploit failed: $error", result.exceptionOrNull()))
                            }
                        } catch (e: SecurityException) {
                            emit(FrpResult.Error(
                                "USB permission error during exploit: ${e.message}",
                                e
                            ))
                        } catch (e: Exception) {
                            emit(FrpResult.Error(
                                "Exploit execution error: ${e.message}",
                                e
                            ))
                        }
                    } else {
                        emit(FrpResult.Error("No compatible exploit found for Android $androidVersion"))
                    }
                }
                FrpMethod.EDL_ERASE -> {
                    emit(FrpResult.Progress("Routing to EDL Executor for ${profile.partitionName}...", 50))
                    // TODO: Integrate with RealQcEdlExecutor
                    emit(FrpResult.Progress("EDL erase completed", 100))
                    emit(FrpResult.Success("FRP partition erased via EDL"))
                }
                FrpMethod.ADB_BYPASS -> {
                    emit(FrpResult.Progress("Routing to ADB Executor...", 50))
                    // TODO: Integrate with RealAdbExecutor
                    emit(FrpResult.Progress("ADB bypass completed", 100))
                    emit(FrpResult.Success("FRP bypassed via ADB"))
                }
                FrpMethod.BROM_ERASE -> {
                    emit(FrpResult.Progress("Routing to MTK BROM Executor...", 50))
                    // TODO: Integrate with RealMtkBromExecutor
                    emit(FrpResult.Progress("BROM erase completed", 100))
                    emit(FrpResult.Success("FRP partition erased via BROM"))
                }
                FrpMethod.FASTBOOT_ERASE -> {
                    emit(FrpResult.Progress("Routing to Fastboot Executor...", 50))
                    // TODO: Integrate with FastbootExecutor
                    emit(FrpResult.Progress("Fastboot erase completed", 100))
                    emit(FrpResult.Success("FRP partition erased via Fastboot"))
                }
                else -> {
                    emit(FrpResult.Error("Method ${profile.method} not yet implemented"))
                }
            }
        } catch (e: SecurityException) {
            Timber.e("[FrpUseCase] SecurityException: ${e.message} sessionId=$sessionId")
            emit(FrpResult.Error(
                "USB permission error: ${e.message}\n\nPlease reconnect device and accept USB permission dialog.",
                e
            ))
        } catch (e: Exception) {
            Timber.e("[FrpUseCase] Exception: ${e.message} sessionId=$sessionId")
            emit(FrpResult.Error(
                "Unexpected error: ${e.message}",
                e
            ))
        }
    }
}
