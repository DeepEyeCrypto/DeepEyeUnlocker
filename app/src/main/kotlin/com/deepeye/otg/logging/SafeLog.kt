package com.deepeye.otg.logging

import android.util.Log

/**
 * Logging shim that tolerates JVM unit tests where android.util.Log may be absent
 * or configured to throw "not mocked" exceptions.
 */
object SafeLog {
    fun d(tag: String, msg: String): Int = call { Log.d(tag, msg) }

    fun i(tag: String, msg: String): Int = call { Log.i(tag, msg) }

    fun w(tag: String, msg: String): Int = call { Log.w(tag, msg) }

    fun e(tag: String, msg: String, tr: Throwable? = null): Int = call {
        if (tr != null) Log.e(tag, msg, tr) else Log.e(tag, msg)
    }

    private inline fun call(block: () -> Int): Int {
        return try {
            block()
        } catch (_: Throwable) {
            0
        }
    }
}