package com.deepeye.otg.engine

import android.content.Context
import com.deepeye.otg.data.model.FlashStatus
import com.deepeye.otg.data.model.XiaomiDeviceInfo
import com.deepeye.otg.data.model.XiaomiFlashMode
import com.deepeye.otg.data.model.XiaomiFlashTask
import com.deepeye.otg.data.model.XiaomiPartition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XiaomiFlashEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Step 1: Detect Xiaomi device via fastboot/ADB
    suspend fun detectDevice(): XiaomiDeviceInfo {
        return withContext(Dispatchers.IO) {
            val codename = runFastboot("getvar product") 
                ?: runAdb("shell getprop ro.product.device") 
                ?: "unknown"
            val model = runAdb("shell getprop ro.product.model") ?: ""
            val miui = runAdb("shell getprop ro.miui.ui.version.name") ?: ""
            val android = runAdb("shell getprop ro.build.version.release") ?: ""
            val bootloader = runFastboot("getvar unlocked") ?: "unknown"
            val arb = runFastboot("getvar anti") ?: "0"
            val serial = runFastboot("getvar serialno") ?: ""
            
            XiaomiDeviceInfo(
                codename = codename,
                model = model,
                miuiVersion = miui,
                androidVersion = android,
                bootloaderStatus = if (bootloader == "yes") "unlocked" else "locked",
                antiRollback = arb,
                serialNo = serial,
                flashMode = detectFlashMode()
            )
        }
    }

    // Step 2: Detect mode (fastboot/EDL/ADB)
    private fun detectFlashMode(): XiaomiFlashMode {
        val fastbootDevices = runCommand("fastboot devices")
        val adbDevices = runCommand("adb devices")
        val edlDevices = runCommand("lsusb").orEmpty()
        
        return when {
            edlDevices.contains("05c6:9008") -> XiaomiFlashMode.EDL
            fastbootDevices?.isNotBlank() == true -> XiaomiFlashMode.FASTBOOT
            adbDevices?.contains("device") == true -> XiaomiFlashMode.TWRP_SIDELOAD
            else -> XiaomiFlashMode.FASTBOOT
        }
    }

    // Step 3: Flash single partition
    suspend fun flashPartition(
        task: XiaomiFlashTask,
        onProgress: (Float, String) -> Unit
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                onProgress(0.1f, "Preparing ${task.partition.label}...")
                
                val cmd = when (task.partition) {
                    XiaomiPartition.SUPER -> {
                        // Super partition needs special handling
                        onProgress(0.2f, "Flashing super (this may take 5-10 min)...")
                        "fastboot flash super \"${task.imagePath}\""
                    }
                    else -> "fastboot flash ${task.partition.fastbootCmd} \"${task.imagePath}\""
                }
                
                onProgress(0.3f, "Running: $cmd")
                val result = runCommandWithOutput(cmd) { line ->
                    val prog = when {
                        line.contains("Sending") -> 0.5f
                        line.contains("Writing") -> 0.75f
                        line.contains("Finished") -> 0.95f
                        else -> null
                    }
                    prog?.let { onProgress(it, line) }
                }
                
                val success = result?.contains("OKAY") == true || 
                              result?.contains("Finished") == true
                onProgress(1f, if (success) "✅ Done!" else "❌ Failed")
                success
            } catch (e: Exception) {
                onProgress(1f, "❌ Error: ${e.message}")
                false
            }
        }
    }

    // Step 4: OEM unlock bootloader
    suspend fun unlockBootloader(): Flow<String> = flow {
        emit("Sending OEM unlock command...")
        runFastboot("oem unlock")?.let { emit(it) }
        emit("⚠️ Device will reboot and factory reset!")
        emit("Confirm on device screen within 30 seconds...")
        delay(2000)
        runFastboot("flashing unlock")?.let { emit(it) }
        emit("Done! Check device screen.")
    }.flowOn(Dispatchers.IO)

    // Step 5: Reboot commands
    suspend fun rebootToFastboot() = runFastboot("reboot bootloader")
    suspend fun rebootToRecovery() = runFastboot("reboot recovery")  
    suspend fun rebootToSystem() = runFastboot("reboot")
    suspend fun rebootToEDL() = runAdb("reboot edl")

    // Step 6: Wipe partitions
    suspend fun wipeData(): Boolean {
        runFastboot("erase userdata") ?: return false
        runFastboot("erase cache")
        return true
    }

    private fun runFastboot(cmd: String): String? = 
        runCommand("fastboot $cmd")
    
    private fun runAdb(cmd: String): String? = 
        runCommand("adb $cmd")

    private fun runCommand(cmd: String): String? = try {
        val proc = Runtime.getRuntime().exec(cmd.split(" ").toTypedArray())
        proc.waitFor(10, TimeUnit.SECONDS)
        proc.inputStream.bufferedReader().readText().trim().ifEmpty { null }
    } catch (e: Exception) { null }

    private fun runCommandWithOutput(
        cmd: String, 
        onLine: (String) -> Unit
    ): String? = try {
        val proc = Runtime.getRuntime().exec(cmd.split(" ").toTypedArray())
        val output = StringBuilder()
        proc.errorStream.bufferedReader().forEachLine { line ->
            output.appendLine(line)
            onLine(line)
        }
        proc.waitFor(300, TimeUnit.SECONDS)
        output.toString()
    } catch (e: Exception) { null }
}
