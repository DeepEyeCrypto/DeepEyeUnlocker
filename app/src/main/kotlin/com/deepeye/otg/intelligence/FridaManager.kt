package com.deepeye.otg.intelligence

import android.content.Context
import com.deepeye.otg.usb.AdbExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * FridaManager handles the deployment and management of Frida hooks on the device.
 * Capability: SSL Pinning, Root Detection, and Biometric bypasses.
 */
class FridaManager(
    private val context: Context,
    private val adbExecutor: AdbExecutor
) {

    companion object {
        private const val FRIDA_SERVER_REMOTE = "/data/local/tmp/frida-server"
        private const val FRIDA_SCRIPT_REMOTE = "/data/local/tmp/frida_script.js"
    }

    /**
     * Lists all available hook scripts.
     */
    fun listAvailableHooks(): List<String> {
        return try {
            context.assets.list("frida/hooks")?.toList() ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "[FRIDA] Failed to list hooks")
            emptyList()
        }
    }

    /**
     * Deploys a set of hooks to a target package.
     */
    suspend fun deployHooks(
        packageName: String,
        hookNames: List<String>,
        onProgress: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onProgress("Deploying ${hookNames.size} hooks to $packageName...")
            
            // 1. Push Frida server to device
            onProgress("Pushing Frida server to device...")
            pushFridaServer()
            
            // 2. Build combined hook script
            onProgress("Building hook script...")
            val combinedScript = buildCombinedScript(hookNames)
            
            // 3. Push script to device
            onProgress("Uploading hook script...")
            pushScriptToDevice(combinedScript)
            
            // 4. Start Frida server
            onProgress("Starting Frida server...")
            startFridaServer()
            
            // 5. Inject into target process
            onProgress("Injecting hooks into $packageName...")
            injectIntoProcess(packageName)
            
            Timber.i("[FRIDA] Deployed ${hookNames.size} hooks to $packageName")
            onProgress("Frida hooks active for $packageName")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[FRIDA] Deployment failed for $packageName")
            Result.failure(e)
        }
    }

    /**
     * Pushes Frida server binary to the device.
     */
    private suspend fun pushFridaServer() {
        try {
            // Copy frida-server from assets to cache
            val fridaServerFile = File(context.cacheDir, "frida-server")
            if (!fridaServerFile.exists()) {
                context.assets.open("frida/frida-server").use { input ->
                    fridaServerFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            // Push to device
            adbExecutor.push(fridaServerFile, FRIDA_SERVER_REMOTE)
            adbExecutor.shell("chmod 755 $FRIDA_SERVER_REMOTE")
            
            Timber.d("[FRIDA] Frida server pushed successfully")
        } catch (e: Exception) {
            Timber.e(e, "[FRIDA] Failed to push Frida server")
            throw e
        }
    }

    /**
     * Builds combined JavaScript hook script.
     */
    private fun buildCombinedScript(hookNames: List<String>): String {
        val combinedScript = StringBuilder()
        combinedScript.append("// DeepEye Frida Hooks\n")
        combinedScript.append("// Generated: ${System.currentTimeMillis()}\n\n")
        
        hookNames.forEach { name ->
            try {
                val content = context.assets.open("frida/hooks/$name").bufferedReader().use { it.readText() }
                combinedScript.append("// --- Hook: $name ---\n")
                combinedScript.append(content)
                combinedScript.append("\n\n")
            } catch (e: Exception) {
                Timber.w(e, "[FRIDA] Failed to load hook: $name")
            }
        }
        
        return combinedScript.toString()
    }

    /**
     * Pushes hook script to device.
     */
    private suspend fun pushScriptToDevice(script: String) {
        try {
            val scriptFile = File(context.cacheDir, "frida_script.js")
            scriptFile.writeText(script)
            
            adbExecutor.push(scriptFile, FRIDA_SCRIPT_REMOTE)
            Timber.d("[FRIDA] Script pushed successfully")
        } catch (e: Exception) {
            Timber.e(e, "[FRIDA] Failed to push script")
            throw e
        }
    }

    /**
     * Starts Frida server on the device.
     */
    private suspend fun startFridaServer() {
        try {
            // Check if already running
            val checkResult = adbExecutor.shell("ps | grep frida-server")
            if (checkResult.contains("frida-server")) {
                Timber.d("[FRIDA] Frida server already running")
                return
            }
            
            // Start in background
            adbExecutor.shell("$FRIDA_SERVER_REMOTE &")
            
            // Wait for server to start
            kotlinx.coroutines.delay(2000)
            Timber.d("[FRIDA] Frida server started")
        } catch (e: Exception) {
            Timber.e(e, "[FRIDA] Failed to start Frida server")
            throw e
        }
    }

    /**
     * Injects hooks into target process.
     */
    private suspend fun injectIntoProcess(packageName: String) {
        try {
            // Use Frida to inject script into target package
            val command = "frida -U -f $packageName -l $FRIDA_SCRIPT_REMOTE --no-pause"
            val result = adbExecutor.shell(command)
            
            Timber.d("[FRIDA] Injection result: $result")
        } catch (e: Exception) {
            Timber.e(e, "[FRIDA] Failed to inject into $packageName")
            throw e
        }
    }

    /**
     * Stops Frida server and cleans up.
     */
    suspend fun cleanup(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            adbExecutor.shell("pkill -f frida-server")
            adbExecutor.shell("rm -f $FRIDA_SERVER_REMOTE")
            adbExecutor.shell("rm -f $FRIDA_SCRIPT_REMOTE")
            
            Timber.i("[FRIDA] Cleanup completed")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[FRIDA] Cleanup failed")
            Result.failure(e)
        }
    }
}
