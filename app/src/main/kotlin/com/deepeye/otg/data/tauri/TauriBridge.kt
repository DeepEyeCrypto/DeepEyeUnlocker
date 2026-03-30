package com.deepeye.otg.data.tauri

import com.deepeye.otg.usb.DeviceMatrix

/**
 * Bridge to Tauri backend for Apple device operations.
 * Implemented by the desktop layer (Tauri) and injected via Hilt.
 */
interface TauriBridge {
    /**
     * Get Apple device info via ideviceinfo.
     * Returns JSON string with device information.
     */
    suspend fun appleDeviceInfo(): String

    /**
     * Send a raw iRecovery command to an Apple device in Recovery/DFU mode.
     * @param cmd The iRecovery command (e.g., "getenv", "reboot")
     * @return Response from iRecovery
     */
    suspend fun appleIrecoveryCmd(cmd: String): String

    /**
     * Exit recovery mode (send "setenv auto-boot true" + "saveenv" + "reboot").
     * @return Success message
     */
    suspend fun appleExitRecovery(): String

    /**
     * Enter DFU mode (send "go" command).
     * @return Success message
     */
    suspend fun appleEnterDfu(): String

    /**
     * Get detected Apple mode from USB device.
     * This is a convenience method that uses the local USB detection.
     */
    fun getDetectedAppleMode(): DeviceMatrix.AppleMode?

    suspend fun runPalera1n(flags: List<String>): String

    suspend fun verifyPwnedDfu(): Boolean

    suspend fun bypassIcloudActivation(method: String): String

    suspend fun appleCheckActivation(): String

    suspend fun appleDnsActivation(serverHost: String): String

    suspend fun appleMdmBypass(profilePath: String): String

    suspend fun appleRestoreActivationRecord(recordPath: String): String

    /**
     * Generic command runner for Tauri backend.
     */
    suspend fun runCommand(command: String, args: Map<String, Any>): String
}
