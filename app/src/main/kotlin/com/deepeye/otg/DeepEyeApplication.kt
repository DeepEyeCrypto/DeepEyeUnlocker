package com.deepeye.otg

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.deepeye.otg.usb.UsbLifecycleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Global application class — loads native lib on IO thread + crash handler.
 *
 * Registered in AndroidManifest.xml via android:name=".DeepEyeApplication"
 */
class DeepEyeApplication : Application() {

    private val appJob = SupervisorJob()
    private val appExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Application scope uncaught coroutine", throwable)
    }

    val appScope = CoroutineScope(appJob + Dispatchers.Default + appExceptionHandler)

    val usbManager: UsbManager by lazy {
        getSystemService(Context.USB_SERVICE) as UsbManager
    }

    /**
     * Process-wide USB lifecycle manager.
     * This survives Activity recreation and prevents lifecycle coupling bugs.
     */
    val usbLifecycleManager: UsbLifecycleManager by lazy {
        UsbLifecycleManager(
            context = applicationContext,
            usbManager = usbManager,
            scope = appScope
        )
    }

    companion object {
        private const val TAG = "DeepEye"
    }

    override fun onCreate() {
        super.onCreate()

        // ── Load native lib on IO thread — NEVER on main ────────
        appScope.launch(Dispatchers.IO) {
            NativeBridge.loadAsync()
        }

        // ── Initialize Secure Licensing (Stage C) ───────────────
        com.deepeye.otg.service.LicenseManager.initialize(this)

        // ── Bootstrap already attached devices (cold start + process restart) ──
        bootstrapAttachedDevices()

        // ── Crash handler for diagnostics ───────────────────────
        setupCrashHandler()

        Log.i(TAG, "DeepEye Unlocker v${BuildConfig.VERSION_NAME} initialized")
    }

    /**
     * Installs an uncaught exception handler that logs crashes to a file
     * in the app's external files directory before delegating to the
     * default system handler (which shows the ANR/crash dialog).
     */
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "UNCAUGHT EXCEPTION on ${thread.name}", throwable)

            // Write crash log to external files dir for later analysis
            runCatching {
                val logFile = getExternalFilesDir(null)
                    ?.resolve("crash_${System.currentTimeMillis()}.txt")
                logFile?.writeText(buildString {
                    appendLine("═══ DeepEye Crash Report ═══")
                    appendLine("Time:      ${java.util.Date()}")
                    appendLine("Thread:    ${thread.name}")
                    appendLine("Exception: ${throwable.javaClass.name}")
                    appendLine("Message:   ${throwable.message}")
                    appendLine("Version:   ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("═══ Stack Trace ═══")
                    appendLine(throwable.stackTraceToString())
                })
            }

            // Delegate to system handler (shows crash dialog, writes tombstone)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun bootstrapAttachedDevices() {
        appScope.launch(Dispatchers.IO) {
            val attached = runCatching { usbManager.deviceList.values.toList() }
                .getOrDefault(emptyList())
            attached.forEach { device ->
                usbLifecycleManager.onDeviceAttached(device)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        usbLifecycleManager.destroy()
        appScope.cancel()
    }
}
