package com.deepeye.otg.intelligence.vulndb

/**
 * Lightweight logger that is JVM-test friendly (no android.util.Log dependency).
 * Falls back to stdout when android.util.Log is unavailable (unit tests).
 */
internal object LogSafe {
    private const val TAG_PREFIX = "[vulndb]"

    // Reflection lookup for android.util.Log to avoid hard dependency in unit tests.
    private val androidLogClass: Class<*>? = runCatching {
        Class.forName("android.util.Log")
    }.getOrNull()

    private val methodI = androidLogClass?.methods?.find { it.name == "i" && it.parameterTypes.size >= 2 }
    private val methodW = androidLogClass?.methods?.find { it.name == "w" && it.parameterTypes.size >= 2 }
    private val methodE = androidLogClass?.methods?.find { it.name == "e" && it.parameterTypes.size >= 2 }

    fun i(tag: String, msg: String) {
        if (!tryAndroidLog(methodI, tag, msg)) println("$TAG_PREFIX I/$tag: $msg")
    }

    fun w(tag: String, msg: String) {
        if (!tryAndroidLog(methodW, tag, msg)) println("$TAG_PREFIX W/$tag: $msg")
    }

    fun e(tag: String, msg: String) {
        if (!tryAndroidLog(methodE, tag, msg)) println("$TAG_PREFIX E/$tag: $msg")
    }

    private fun tryAndroidLog(method: java.lang.reflect.Method?, tag: String, msg: String): Boolean {
        return try {
            if (method == null) return false
            method.invoke(null, tag, msg)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
