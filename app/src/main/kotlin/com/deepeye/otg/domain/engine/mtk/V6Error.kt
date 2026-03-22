package com.deepeye.otg.domain.engine.mtk

sealed class V6Error(message: String) : Exception(message) {
    // From MtkV6Session
    data class KeyExchangeFailed(val reason: String) : V6Error(reason)
    data class DaNotFound(val hwCode: Int) : V6Error("DA not found for hwCode: 0x${hwCode.toString(16)}")
    data class DaUploadFailed(val transferred: Int) : V6Error("DA upload failed after $transferred bytes")
    data class DaChecksumMismatch(val expected: Int, val actual: Int) : V6Error("DA checksum mismatch: expected 0x${expected.toString(16)}, got 0x${actual.toString(16)}")
    data class PartitionNotFound(val name: String) : V6Error("Partition '$name' not found")
    data class ErasePartitionFailed(val name: String, val code: Int) : V6Error("Failed to erase partition '$name', error code: $code")
    object KeyExchangeTimeout : V6Error("Key exchange timeout")
    
    // From MtkCdcSession
    object InterfaceClaimFailed : V6Error("Failed to claim CDC control/data interfaces")
    object EndpointDiscoveryFailed : V6Error("Failed to resolve CDC bulk endpoints")
    object CdcSetupFailed : V6Error("CDC-ACM setup failed")
    object SyncAttemptedBeforeSetup : V6Error("CDC setup must complete before V6 sync")
    object SyncTransferFailed : V6Error("Failed to send V6 sync bytes")
    object HelloReadFailed : V6Error("Failed to read V6 hello packet")
}