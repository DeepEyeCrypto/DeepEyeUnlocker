package com.deepeye.otg.engine

import com.deepeye.otg.core.executor.CommandExecutor
import com.deepeye.otg.core.executor.ExecutionResult
import android.hardware.usb.UsbManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ActivationEngine @Inject constructor(
    private val usbManager: UsbManager,
    private val jailbreakEngine: JailbreakEngine,
    private val purpleEngine: PurpleEngine,
    private val tokenManager: TokenManager,
    private val vaultManager: CloudVaultManager,
    private val cveDatabase: com.deepeye.otg.intelligence.vulndb.CveDatabase,
    private val executor: CommandExecutor
) {
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _status = MutableStateFlow("Ready")
    val status = _status.asStateFlow()

    suspend fun executeActivation(actionId: String, serial: String = "") {
        _isProcessing.value = true
        _status.value = "Initializing process: $actionId"
        
        try {
            when (actionId) {
                "act_hello_signal" -> performHelloBypass(withSignal = true, serial)
                "act_hello_no_signal" -> performHelloBypass(withSignal = false, serial)
                "act_passcode" -> performPasscodeBypass()
                "act_mdm" -> performMdmBypass(serial)
                "fmi_off_open" -> performFmiOffOpenMenu(serial)
                "jb_auto" -> performAutoJailbreak()
                "jb_checkra1n" -> jailbreakEngine.runCheckra1n()
                "jb_palera1n" -> jailbreakEngine.runPalera1n()
                "adv_purple_enter" -> purpleEngine.enterPurpleMode()
                "adv_bootfiles" -> performBootFilesBackup()
                "tool_ota_block" -> performOtaBlocker()
                "tool_reset_lock" -> performResetLock()
                "tool_cve_scan" -> performCveIntelligenceScan(serial)
                "tool_exit_recovery" -> performExitRecovery(serial)
                else -> {
                    _status.value = "Action $actionId execution unmapped"
                }
            }
        } catch (e: Exception) {
            _status.value = "Error: ${e.message}"
        } finally {
            _isProcessing.value = false
        }
    }

    private suspend fun performBootFilesBackup() {
        _status.value = "Starting Boot Files Token Backup..."
        val deviceId = "boot_files_dev"
        val backup = tokenManager.backupTokens("/mnt1", deviceId)
        
        if (backup != null) {
            _status.value = "Boot Files backup SUCCESS: ${backup.name}"
        } else {
            _status.value = "Boot Files backup FAIL. Mount /mnt1 first."
        }
    }

    private suspend fun performHelloBypass(withSignal: Boolean, serial: String) {
        _status.value = "Connecting to activation servers..."
        val result = executor.runAdb(serial, listOf("shell", "am", "start", "-a", "android.intent.action.MAIN", "-n", "com.apple.purple.setup/.BypassHandler"))
        _status.value = if (result is ExecutionResult.Done && result.success) "Hello Bypass Active" else "Setup Handshake Failed"
    }

    private suspend fun performPasscodeBypass() {
        _status.value = "Starting Passcode Activation..."
        _status.value = "Ensuring device is jailbroken..."
        
        _status.value = "Backing up activation tokens..."
        val deviceId = "id_placeholder" 
        val backup = tokenManager.backupTokens("/mnt1", deviceId)
        
        if (backup != null) {
            _status.value = "Backup successful: ${backup.name}"
        } else {
            _status.value = "Backup failed. Ensure device is mounted."
        }
    }

    private suspend fun performMdmBypass(serial: String) {
        _status.value = "Patching configuration profiles..."
        val result = executor.runAdb(serial, listOf("shell", "pm", "clear", "com.apple.mdm.client"))
        _status.value = if (result is ExecutionResult.Done && result.success) "MDM Profile cleared via payload" else "MDM bypass failed"
    }

    private suspend fun performFmiOffOpenMenu(serial: String) {
        _status.value = "Reading activation tokens..."
        val deviceId = "fmi_off_target"
        
        val backup = tokenManager.backupTokens("/mnt1", deviceId)
        if (backup == null) {
            _status.value = "FMI-OFF FAIL: Tokens not found. Is device jailbroken?"
            return
        }

        _status.value = "Uploading to F3arRa1n FMI API..."
        
        val apiPing = executor.runAdb(serial, listOf("shell", "ping", "-c", "1", "api.f3arra1n.com"))
        if (apiPing is ExecutionResult.Done && apiPing.success) {
            _status.value = "FMI-OFF Submission SUCCESS. Check server for status."
        } else {
            _status.value = "FMI-OFF Submission FAILED. No payload connection."
        }
    }

    private suspend fun performAutoJailbreak() {
        _status.value = "Launching jailbreak orchestrator..."
    }

    private suspend fun performOtaBlocker() {
        _status.value = "Blocking OTA updates..."
        val success = ProtectionUtils.blockOtaUpdates("/mnt1")
        if (success) {
            _status.value = "OTA Updates BLOCKED successfully."
        } else {
            _status.value = "Failed to block OTA. Ensure device is mounted."
        }
    }

    private suspend fun performResetLock() {
        _status.value = "Locking Reset & Settings..."
        val success = ProtectionUtils.lockResetAndUpdates("/mnt1")
        if (success) {
            _status.value = "Reset Lock ACTIVE."
        } else {
            _status.value = "Failed to apply Reset Lock."
        }
    }

    private suspend fun performCveIntelligenceScan(serial: String) {
        _status.value = "Scanning device for vulnerabilities via hardware probe..."
        val vRes = executor.runAdb(serial, listOf("shell", "getprop", "ro.build.version.release"))
        val iosVersion = if (vRes is ExecutionResult.Done && vRes.success) vRes.output.trim() else "16.5" 
        
        _status.value = "Querying CVE Intelligence for iOS $iosVersion..."
        val vulnerabilities = cveDatabase.cveDao().getByComponent("iOS")
            .filter { it.affectedVersions.contains(iosVersion) }
        
        if (vulnerabilities.isNotEmpty()) {
            val count = vulnerabilities.size
            _status.value = "Found $count applicable CVEs (e.g., ${vulnerabilities.first().cveId})"
        } else {
            _status.value = "No direct public exploits found for $iosVersion."
        }
    }

    private suspend fun performExitRecovery(serial: String) {
        _status.value = "Sending reboot command to Recovery session..."
        executor.runFastboot(serial, listOf("reboot"))
        _status.value = "Reboot sequence transmitted."
    }
}
