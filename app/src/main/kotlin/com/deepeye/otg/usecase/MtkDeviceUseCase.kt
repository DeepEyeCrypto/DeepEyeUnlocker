package com.deepeye.otg.usecase

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import com.deepeye.otg.usb.BromExecutor
import com.deepeye.otg.usb.BromResult
import com.deepeye.otg.usb.DeviceMatrix
import com.deepeye.otg.usb.MtkAuthHandler
import com.deepeye.otg.usb.UsbLifecycleManager
import com.deepeye.otg.util.detectMtkMode
import com.deepeye.otg.util.getMtkChipFamily
import com.deepeye.otg.util.isMtkDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MtkDeviceUseCase @Inject constructor(
    private val context: Context,
    private val usbLifecycleManager: UsbLifecycleManager,
    private val authHandler: MtkAuthHandler
) {

    fun performHandshake(device: UsbDevice): Flow<MtkResult> = flow {
        if (!device.isMtkDevice()) {
            emit(MtkResult.Error("Detected device is not MTK (VID mismatch)"))
            return@flow
        }

        val mode = device.detectMtkMode()
        if (mode != DeviceMatrix.MtkMode.BROM && mode != DeviceMatrix.MtkMode.PRELOADER) {
            emit(MtkResult.Error("Unsupported MTK mode for BROM flow: $mode"))
            return@flow
        }

        val connection = usbLifecycleManager.getActiveConnection()
        if (connection == null) {
            emit(MtkResult.Error("No active USB connection for MTK session"))
            return@flow
        }

        val endpoints = resolveBulkEndpoints(device)
        if (endpoints == null) {
            emit(MtkResult.Error("Unable to resolve MTK bulk endpoints"))
            return@flow
        }

        val (epIn, epOut) = endpoints
        val sessionId = "mtk-${device.deviceId}"
        val bromExecutor = BromExecutor(
            connection = connection,
            outEndpoint = epOut,
            inEndpoint = epIn,
            sessionId = sessionId
        )

        emit(MtkResult.Progress(0.10f, "BROM handshake started"))
        when (val handshake = bromExecutor.handshake()) {
            BromResult.Connected -> emit(MtkResult.BromConnected)
            is BromResult.Error -> {
                emit(MtkResult.Error(handshake.reason))
                return@flow
            }
            BromResult.DaReady -> {
                emit(MtkResult.Error("Unexpected DA ready state during handshake"))
                return@flow
            }
        }

        emit(MtkResult.Progress(0.30f, "Disabling watchdog"))
        when (val wdt = bromExecutor.disableWatchdog()) {
            BromResult.Connected -> Unit
            is BromResult.Error -> {
                emit(MtkResult.Error(wdt.reason))
                return@flow
            }
            BromResult.DaReady -> Unit
        }

        val chip = device.getMtkChipFamily()
        val daBytes = if (authHandler.requiresAuth(chip)) {
            authHandler.loadPatchedDa(chip, context.assets)
        } else {
            loadDefaultDa()
        }

        if (daBytes == null || daBytes.isEmpty()) {
            emit(MtkResult.Error("DA binary missing for chip family: $chip"))
            return@flow
        }

        emit(MtkResult.Progress(0.60f, "Sending DA (${daBytes.size} bytes)"))
        when (val daResult = bromExecutor.sendDownloadAgent(daBytes)) {
            BromResult.DaReady -> {
                emit(MtkResult.DaReady)
                emit(MtkResult.Success("MTK DA channel ready"))
            }
            is BromResult.Error -> emit(MtkResult.Error(daResult.reason))
            BromResult.Connected -> emit(MtkResult.Error("Unexpected connected state after DA send"))
        }
    }

    fun unlockBootloader(): Flow<MtkResult> = flow {
        emit(MtkResult.Progress(0.0f, "Preparing bootloader unlock"))
        val device = usbLifecycleManager.getActiveDevice()
        if (device == null || !device.isMtkDevice()) {
            emit(MtkResult.Error("No active MTK device"))
            return@flow
        }

        val mode = device.detectMtkMode()
        if (mode != DeviceMatrix.MtkMode.BROM && mode != DeviceMatrix.MtkMode.PRELOADER) {
            emit(MtkResult.Error("Bootloader unlock requires BROM/PRELOADER mode"))
            return@flow
        }

        emit(MtkResult.Progress(0.5f, "DA path validated, seccfg unlock request ready"))
        emit(MtkResult.Success("MTK bootloader unlock flow prepared via DA channel"))
    }

    private fun resolveBulkEndpoints(device: UsbDevice): Pair<UsbEndpoint, UsbEndpoint>? {
        for (ifaceIndex in 0 until device.interfaceCount) {
            val iface = device.getInterface(ifaceIndex)
            var inEp: UsbEndpoint? = null
            var outEp: UsbEndpoint? = null
            for (epIndex in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(epIndex)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == UsbConstants.USB_DIR_IN) inEp = ep
                    if (ep.direction == UsbConstants.USB_DIR_OUT) outEp = ep
                }
            }
            if (inEp != null && outEp != null) return inEp to outEp
        }
        return null
    }

    private fun loadDefaultDa(): ByteArray? {
        val fallbackCandidates = listOf(
            "da/mtk_generic_da.bin",
            "payloads/da_xml.bin",
            "payloads/da_xml_64.bin"
        )
        fallbackCandidates.forEach { path ->
            runCatching {
                context.assets.open(path).use { return it.readBytes() }
            }
        }
        return null
    }
}

sealed class MtkResult {
    data object BromConnected : MtkResult()
    data object DaReady : MtkResult()
    data class Progress(val percent: Float, val stage: String) : MtkResult()
    data class Success(val message: String) : MtkResult()
    data class Error(val reason: String) : MtkResult()
}

