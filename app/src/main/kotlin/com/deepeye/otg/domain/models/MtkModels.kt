package com.deepeye.otg.domain.models

data class MtkDeviceInfo(
    val sessionId: String,
    val vid: Int,          // 0x22D9
    val pid: Int,          // 0x6
    val hwCode: Int,       // 0x1209
    val hwSub: Int,
    val hwVer: Int,
    val swVer: Int,
    val chipName: String,
    val manufacturer: String,
    val secureBoot: Boolean,
    val slaEnabled: Boolean,
    val daaEnabled: Boolean,
    val mode: MtkMode
)

enum class MtkMode {
    BROM,         // Handshake complete, full access
    META,         // CDC_SERIAL but not BROM
    ADB_DIAG,     // ADB diagnostic interface
    UNKNOWN
}

data class PartitionEntry(
    val name: String,
    val startLba: Long,
    val endLba: Long,
    val sizeMb: Float,
    val attributes: Long
)

sealed class MtkSessionError {
    object HandshakeFailed : MtkSessionError()
    object InterfaceClaimFail : MtkSessionError()
    object EndpointNotFound : MtkSessionError()
    data class BulkReadFailed(val code: Int) : MtkSessionError()
    data class BulkWriteFailed(val code: Int) : MtkSessionError()
    data class Timeout(val op: String) : MtkSessionError()
    object SecureBootBlocked : MtkSessionError()
}
