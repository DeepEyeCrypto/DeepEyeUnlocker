package com.deepeye.otg.intelligence.vulndb

import android.util.Log

/**
 * Thread-safe logger that avoids android.util.Log for JVM-only zones
 * if needed, but defaults to Android Log for the OTG app.
 */
object LogSafe {
    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }
    fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        Log.e(tag, msg, tr)
    }
    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }
}
