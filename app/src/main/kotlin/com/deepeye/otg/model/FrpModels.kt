package com.deepeye.otg.model

import com.deepeye.otg.usb.DeviceMatrix

data class DetectedDevice(
    val vid: Int,
    val pid: Int,
    val brand: String,
    val model: String,
    val serial: String,
    val androidVersion: Int,
)

sealed class FrpResult {
    object Idle : FrpResult()
    data class PathSelected(val path: DeviceMatrix.FrpPath) : FrpResult()
    data class Progress(val percent: Float, val step: String) : FrpResult()
    data class Success(val message: String) : FrpResult()
    data class Error(val reason: String) : FrpResult()
    data class Unsupported(val device: String) : FrpResult()
}

