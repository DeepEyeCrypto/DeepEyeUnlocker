package com.deepeye.otg.data.tauri

import com.deepeye.otg.usb.DeviceMatrix
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpTauriBridge @Inject constructor() : TauriBridge {
    override suspend fun appleDeviceInfo(): String {
        throw IllegalStateException("Tauri bridge not available in Android runtime")
    }

    override suspend fun appleIrecoveryCmd(cmd: String): String {
        throw IllegalStateException("Tauri bridge not available in Android runtime")
    }

    override suspend fun appleExitRecovery(): String {
        throw IllegalStateException("Tauri bridge not available in Android runtime")
    }

    override suspend fun appleEnterDfu(): String {
        throw IllegalStateException("Tauri bridge not available in Android runtime")
    }

    override fun getDetectedAppleMode(): DeviceMatrix.AppleMode? = null

    override suspend fun runPalera1n(flags: List<String>): String {
        throw IllegalStateException("Tauri bridge not available in Android runtime")
    }

    override suspend fun verifyPwnedDfu(): Boolean {
        throw IllegalStateException("Tauri bridge not available in Android runtime")
    }

    override suspend fun bypassIcloudActivation(method: String): String {
        throw IllegalStateException("Tauri bridge not available in Android runtime")
    }
}
