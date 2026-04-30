package com.deepeye.otg.data.gsmg

// =============================================================================
// ProtocolResult.kt — Real execution result types
// Every operation returns typed success/failure — never silently fakes
// =============================================================================

sealed interface ProtocolResult {

    // ── Success states ────────────────────────────────────────────────────────

    data class FrpErased(
        val method:    String,   // "BROM" | "META" | "EDL" | "ADB"
        val partition: String,   // e.g. "frp", "misc"
        val sessionId: String,
    ) : ProtocolResult

    data class DeviceInfoRead(
        val imei:      String?,
        val imei2:     String?,
        val serial:    String?,
        val ecid:      String?,
        val chipName:  String,
        val iosVersion:String?,
        val btMac:     String?,
        val wifiMac:   String?,
        val sessionId: String,
    ) : ProtocolResult

    data class ImeiWritten(
        val slot:      Int,      // 0 or 1
        val imei:      String,
        val method:    String,
        val sessionId: String,
    ) : ProtocolResult

    data class FirmwareFlashed(
        val firmware:  String,
        val sizeBytes: Long,
        val method:    String,
        val sessionId: String,
    ) : ProtocolResult

    data class PartitionErased(
        val name:      String,
        val sessionId: String,
    ) : ProtocolResult

    data class ActivationBypassed(
        val method:       String,
        val signalEnabled:Boolean,
        val untethered:   Boolean,
        val sessionId:    String,
    ) : ProtocolResult

    data class BootloaderUnlocked(
        val method:    String,
        val sessionId: String,
    ) : ProtocolResult

    data class AccountRemoved(
        val accountType: String,
        val sessionId:   String,
    ) : ProtocolResult

    data class ServerBypassComplete(
        val token:     String,
        val sessionId: String,
    ) : ProtocolResult

    data class AdbCommandComplete(
        val command: String,
        val output:  String,
        val sessionId: String,
    ) : ProtocolResult

    data class EfsBackedUp(
        val path:      String,
        val sizeBytes: Long,
        val sessionId: String,
    ) : ProtocolResult

    data class GenericSuccess(
        val operation: String,
        val sessionId: String,
    ) : ProtocolResult

    // ── Failure states ────────────────────────────────────────────────────────

    sealed class Failure : Exception(), ProtocolResult {
        abstract val reason:    String
        abstract val layer:     String
        abstract val sessionId: String
        abstract val retryable: Boolean
        override val message: String? get() = "[$layer] $reason (session: $sessionId)"
    }

    data class GenericFailure(
        override val reason:    String,
        override val layer:     String     = "GENERIC",
        override val sessionId: String,
        override val retryable: Boolean    = false,
    ) : Failure()

    data class UsbTransportError(
        override val reason:    String,
        override val layer:     String     = "USB_TRANSPORT",
        override val sessionId: String,
        override val retryable: Boolean    = true,
        val transferred:        Int        = -1,
    ) : Failure()

    data class ProtocolHandshakeFailed(
        override val reason:    String,
        override val layer:     String     = "HANDSHAKE",
        override val sessionId: String,
        override val retryable: Boolean    = true,
        val sentByte:           Int        = -1,
        val receivedByte:       Int        = -1,
    ) : Failure()

    data class AuthenticationFailed(
        override val reason:    String,
        override val layer:     String     = "AUTH",
        override val sessionId: String,
        override val retryable: Boolean    = false,
        val authType:           String     = "DA",
    ) : Failure()

    data class DaChecksumMismatch(
        override val reason:    String     = "DA checksum mismatch",
        override val layer:     String     = "DA_UPLOAD",
        override val sessionId: String,
        override val retryable: Boolean    = false,
        val expected:           Int,
        val actual:             Int,
    ) : Failure()

    data class PartitionNotFound(
        override val reason:    String,
        override val layer:     String     = "PARTITION",
        override val sessionId: String,
        override val retryable: Boolean    = false,
        val partitionName:      String,
    ) : Failure()

    data class ServerError(
        override val reason:    String,
        override val layer:     String     = "SERVER",
        override val sessionId: String,
        override val retryable: Boolean    = true,
        val httpCode:           Int        = -1,
    ) : Failure()

    data class AdbNotAvailable(
        override val reason:    String     = "ADB not available or unauthorized",
        override val layer:     String     = "ADB",
        override val sessionId: String,
        override val retryable: Boolean    = false,
    ) : Failure()

    data class EdlNotDetected(
        override val reason:    String     = "EDL mode not detected (need 9008)",
        override val layer:     String     = "EDL",
        override val sessionId: String,
        override val retryable: Boolean    = false,
    ) : Failure()

    data class NotImplementedYet(
        override val reason:    String,
        override val layer:     String     = "NOT_IMPLEMENTED",
        override val sessionId: String,
        override val retryable: Boolean    = false,
        val mechanism:          String,
        val trackerNote:        String     = "See v2026.31.0 Stage implementation",
    ) : Failure()
}
