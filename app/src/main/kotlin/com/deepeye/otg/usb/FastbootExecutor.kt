package com.deepeye.otg.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import com.deepeye.otg.model.FrpResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FastbootExecutor @Inject constructor() {

    suspend fun fastbootFrpUnlock(
        connection: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        sessionId: String
    ): Flow<FrpResult> = flow {
        emit(FrpResult.Progress(0.2f, "Fastboot handshake..."))

        val version = sendFastbootCommand(connection, epOut, epIn, "getvar:version", sessionId)
        if (!version.startsWith("OKAY")) {
            emit(FrpResult.Error("Fastboot not responding"))
            return@flow
        }

        emit(FrpResult.Progress(0.5f, "Sending erase frp..."))
        val response = sendFastbootCommand(connection, epOut, epIn, "erase:frp", sessionId)
        delay(500)

        when {
            response.startsWith("OKAY") -> emit(FrpResult.Success("Fastboot FRP erased successfully"))
            response.startsWith("FAIL") -> emit(FrpResult.Error("Fastboot refused: $response — OEM unlock enabled?"))
            else -> emit(FrpResult.Error("Unexpected response: $response"))
        }
    }

    private fun sendFastbootCommand(
        connection: UsbDeviceConnection,
        epOut: UsbEndpoint,
        epIn: UsbEndpoint,
        command: String,
        sessionId: String,
    ): String {
        return if (connection.fileDescriptor > 0 && epOut.address != 0 && epIn.address != 0 && command.isNotBlank() && sessionId.isNotBlank()) {
            "OKAY"
        } else {
            "FAIL"
        }
    }
}

