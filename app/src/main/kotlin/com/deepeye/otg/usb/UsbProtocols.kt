package com.deepeye.otg.usb

import com.deepeye.otg.domain.models.DeviceMode
import com.deepeye.otg.domain.models.ProtocolFamily
import com.deepeye.otg.domain.models.DeepEyeOperation

/**
 * High-level operation states for UI flow and queuing.
 */
sealed class SessionState {
    object Idle : SessionState()
    data class WaitingForDevice(val queuedOp: DeepEyeOperation) : SessionState()
    data class DeviceFound(val queuedOp: DeepEyeOperation) : SessionState()
    data class PermissionPending(val queuedOp: DeepEyeOperation) : SessionState()
    data class ProtocolDetect(val queuedOp: DeepEyeOperation) : SessionState()
    data class ReenumerationWait(val queuedOp: DeepEyeOperation) : SessionState()
    data class ConnectedReady(
        val deviceName: String,
        val brand: String = "Unknown",
        val chipset: String = "Generic",
        val secureBoot: String = "UNKNOWN"
    ) : SessionState()
    data class ExecutingOperation(
        val op: DeepEyeOperation,
        val progress: Int,
        val statusMsg: String,
        val deviceName: String = "Unknown"
    ) : SessionState()
    data class OperationComplete(
        val op: DeepEyeOperation,
        val success: Boolean,
        val message: String,
        val reportPath: String? = null
    ) : SessionState()
    data class Error(val message: String, val queuedOp: DeepEyeOperation? = null) : SessionState()
    data class PermissionDenied(val queuedOp: DeepEyeOperation? = null) : SessionState()
    data class ConnectedMtpOnly(val deviceName: String) : SessionState()
    object TestHarness : SessionState()
    data class Reporting(val reportFile: java.io.File?) : SessionState()
    data class PartitionPreview(val partitions: List<com.deepeye.otg.domain.models.PartitionItem>) : SessionState()
}

