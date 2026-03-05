package com.deepeye.otg

import android.app.Application
import android.util.Log

/**
 * Global application class — installs crash handler for diagnostics.
 *
 * Registered in AndroidManifest.xml via android:name=".DeepEyeApplication"
 */
class DeepEyeApplication : Application() {

    companion object {
        private const val TAG = "DeepEye"
    }

    override fun onCreate() {
        super.onCreate()
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
}
