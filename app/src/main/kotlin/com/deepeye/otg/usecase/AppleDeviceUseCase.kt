package com.deepeye.otg.usecase

import com.deepeye.otg.data.repository.AppleDeviceState
import com.deepeye.otg.data.repository.DeviceRepository
import com.deepeye.otg.data.tauri.TauriBridge
import com.deepeye.otg.usb.DeviceMatrix
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppleDeviceUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val tauriBridge: TauriBridge
) {

    fun observeAppleDevice(): Flow<AppleDeviceState> = deviceRepository.observeAppleDevice()

    suspend fun refreshDeviceInfo(mode: DeviceMatrix.AppleMode?): Result<String> = runCatching {
        when (mode) {
            DeviceMatrix.AppleMode.NORMAL -> tauriBridge.appleDeviceInfo()
            DeviceMatrix.AppleMode.RECOVERY,
            DeviceMatrix.AppleMode.DFU,
            DeviceMatrix.AppleMode.WTF,
            DeviceMatrix.AppleMode.PWNED_DFU -> tauriBridge.appleIrecoveryCmd("getenv auto-boot")
            else -> error("Apple device not detected")
        }
    }

    suspend fun sendIrecoveryCommand(
        mode: DeviceMatrix.AppleMode?,
        command: String
    ): Result<String> = runCatching {
        if (mode !in setOf(
                DeviceMatrix.AppleMode.RECOVERY,
                DeviceMatrix.AppleMode.DFU,
                DeviceMatrix.AppleMode.WTF,
                DeviceMatrix.AppleMode.PWNED_DFU
            )
        ) {
            error("iRecovery command requires Recovery/DFU/WTF mode")
        }
        tauriBridge.appleIrecoveryCmd(command)
    }

    suspend fun exitRecovery(mode: DeviceMatrix.AppleMode?): Result<String> = runCatching {
        if (mode !in setOf(DeviceMatrix.AppleMode.RECOVERY, DeviceMatrix.AppleMode.WTF)) {
            error("Exit Recovery is valid only in Recovery/WTF mode")
        }
        tauriBridge.appleExitRecovery()
    }

    suspend fun enterDfu(mode: DeviceMatrix.AppleMode?): Result<String> = runCatching {
        if (mode != DeviceMatrix.AppleMode.RECOVERY) {
            error("Enter DFU requires Recovery mode")
        }
        tauriBridge.appleEnterDfu()
    }
}

