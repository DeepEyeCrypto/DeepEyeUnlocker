package com.deepeye.otg.usb

import android.hardware.usb.UsbDeviceConnection
import com.deepeye.otg.model.FrpResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbExecutor @Inject constructor(
    private val adbSession: AdbSession
) {

    suspend fun shell(command: String): String {
        return adbSession.shell(command).getOrNull() ?: "error"
    }

    suspend fun push(localFile: java.io.File, remotePath: String) {
        val bytes = localFile.readBytes()
        val streamId = adbSession.open("shell:cat > $remotePath") 
            ?: throw Exception("Failed to open push stream")
        
        try {
            val chunkSize = 16384
            var offset = 0
            while (offset < bytes.size) {
                val end = (offset + chunkSize).coerceAtMost(bytes.size)
                val chunk = bytes.sliceArray(offset until end)
                adbSession.write(streamId, chunk)
                offset = end
            }
        } finally {
            adbSession.close(streamId)
        }
    }

    suspend fun adbFrpUnlock(
        connection: UsbDeviceConnection,
        sessionId: String
    ): Flow<FrpResult> = flow {
        emit(FrpResult.Progress(0.1f, "ADB connecting..."))

        val cmd = "content delete --uri content://settings/secure --where \"name='user_setup_complete'\""
        val result = shell(cmd)
        
        if (result.contains("Exception", true) || result.contains("error", true)) {
            emit(FrpResult.Error("ADB FRP command rejected: $result"))
            return@flow
        }

        val cmd2 = "am broadcast -a com.google.android.gsf.LOGIN_ACCOUNTS_CHANGED"
        shell(cmd2)
        
        emit(FrpResult.Success("ADB FRP unlock sent. Reboot device to verify."))
    }
}

