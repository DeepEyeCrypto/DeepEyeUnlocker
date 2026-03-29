package com.deepeye.otg.usecase

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import com.deepeye.otg.usb.DeviceMatrix
import com.deepeye.otg.usb.LgLafExecutor
import com.deepeye.otg.usb.MtkBromExecutor
import com.deepeye.otg.usb.MtkMetaExecutor
import com.deepeye.otg.usb.OdinExecutor
import com.deepeye.otg.usb.SpdFdlExecutor
import com.deepeye.otg.util.detectHydraProtocol
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydraProtocolUseCase @Inject constructor(
    private val odinExecutor: OdinExecutor,
    private val mtkBromExecutor: MtkBromExecutor,
    private val mtkMetaExecutor: MtkMetaExecutor,
    private val spdFdlExecutor: SpdFdlExecutor,
    private val lgLafExecutor: LgLafExecutor,
) {

    fun detectProtocol(device: UsbDevice): DeviceMatrix.HydraProtocol? = device.detectHydraProtocol()

    suspend fun runSamsungOdin(
        connection: UsbDeviceConnection,
        outEp: UsbEndpoint,
        inEp: UsbEndpoint,
        fileBytes: ByteArray,
        sessionId: String,
    ) = odinExecutor.flashOdin(connection, outEp, inEp, fileBytes, sessionId)

    suspend fun runMtkBromDa(
        connection: UsbDeviceConnection,
        outEp: UsbEndpoint,
        inEp: UsbEndpoint,
        daBytes: ByteArray,
        sessionId: String,
    ) = mtkBromExecutor.loadDa(connection, outEp, inEp, daBytes, sessionId)

    fun buildMtkMetaImeiWrite(imei: String): String = mtkMetaExecutor.buildWriteImeiCommand(imei)

    suspend fun runSpdFdl(
        connection: UsbDeviceConnection,
        outEp: UsbEndpoint,
        inEp: UsbEndpoint,
        fdl1: ByteArray,
        fdl2: ByteArray,
        sessionId: String,
    ) = spdFdlExecutor.sendFdlStages(connection, outEp, inEp, fdl1, fdl2, sessionId)

    suspend fun runLgLafCommand(
        connection: UsbDeviceConnection,
        outEp: UsbEndpoint,
        inEp: UsbEndpoint,
        command: String,
        arg1: Int,
        arg2: Int,
        data: ByteArray,
        sessionId: String,
    ) = lgLafExecutor.sendCommand(connection, outEp, inEp, command, arg1, arg2, data, sessionId)
}

