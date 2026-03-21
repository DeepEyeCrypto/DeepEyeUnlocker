package com.deepeye.otg.intelligence.vulndb

import com.deepeye.otg.logging.SafeLog

/**
 * Thread-safe logger that avoids android.util.Log for JVM-only zones
 * if needed, but defaults to Android Log for the OTG app.
 */
object LogSafe {
    fun i(tag: String, msg: String) {
        SafeLog.i(tag, msg)
    }
    fun w(tag: String, msg: String) {
        SafeLog.w(tag, msg)
    }
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        SafeLog.e(tag, msg, tr)
    }
    fun d(tag: String, msg: String) {
        SafeLog.d(tag, msg)
    }
}
