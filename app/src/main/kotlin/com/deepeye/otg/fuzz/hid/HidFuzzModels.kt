package com.deepeye.otg.fuzz.hid

sealed class FuzzResult {
    data class Success(val durationMs: Long) : FuzzResult()
    data class Crash(val type: CrashType, val signature: String) : FuzzResult()
    object Timeout : FuzzResult()
}

enum class CrashType {
    USB_DISCONNECT,
    KERNEL_PANIC,
    HANG,
    UNKNOWN
}

data class FuzzCase(
    val id: String,
    val name: String,
    val payload: ByteArray,
    val mutationType: String? = null
)
