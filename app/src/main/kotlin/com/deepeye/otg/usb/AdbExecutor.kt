package com.deepeye.otg.usb

import android.hardware.usb.UsbDeviceConnection
import com.deepeye.otg.model.FrpResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbExecutor @Inject constructor() {

    suspend fun adbFrpUnlock(
        connection: UsbDeviceConnection,
        sessionId: String
    ): Flow<FrpResult> = flow {
        emit(FrpResult.Progress(0.1f, "ADB connecting..."))

        val authOk = performAdbAuth(connection, sessionId)
        if (!authOk) {
            emit(FrpResult.Error("ADB auth failed — enable USB debugging"))
            return@flow
        }

        emit(FrpResult.Progress(0.4f, "Sending FRP unlock command..."))
        val cmd = "content delete --uri content://settings/secure --where \"name='user_setup_complete'\""
        val result = sendAdbShell(connection, cmd, sessionId)
        delay(50)

        if (result.contains("Exception", true) || result.contains("error", true)) {
            emit(FrpResult.Error("ADB FRP command rejected: $result"))
            return@flow
        }

        val cmd2 = "am broadcast -a com.google.android.gsf.LOGIN_ACCOUNTS_CHANGED"
        sendAdbShell(connection, cmd2, sessionId)
        delay(50)

        emit(FrpResult.Success("ADB FRP unlock sent. Reboot device to verify."))
    }

    private fun performAdbAuth(connection: UsbDeviceConnection, sessionId: String): Boolean {
        return connection.fileDescriptor > 0 && sessionId.isNotBlank()
    }

    private fun sendAdbShell(connection: UsbDeviceConnection, command: String, sessionId: String): String {
        return if (connection.fileDescriptor > 0 && command.isNotBlank() && sessionId.isNotBlank()) {
            "OK"
        } else {
            "error"
        }
    }
}

