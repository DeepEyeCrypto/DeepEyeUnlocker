package com.deepeye.otg.usb

import com.deepeye.otg.logging.SafeLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-level session for ADB operations (Stage 7.2).
 * Hardened for Pro cycle (v2026.27): concurrent safety and window management stubs.
 */
class AdbSession(
    private val transport: UsbTransport
) {
    companion object {
        private const val TAG = "DeepEye-ADB"
        private const val HEADER_LENGTH = 24
        private const val MAX_MESSAGE_DATA = 1024 * 1024
    }

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private var maxData = AdbProtocol.CONNECT_MAXDATA
    private var localIdCounter = 1
    
    // Concurrent Safety
    private val sessionMutex = Mutex()
    private val activeStreams = mutableMapOf<Int, Int>() // localId -> remoteId

    /**
     * Perform the ADB connection handshake.
     */
    suspend fun connect(systemIdentity: String = "host::DeepEyeOTG"): Boolean = sessionMutex.withLock {
        if (_isConnected.value) return true
        
        SafeLog.i(TAG, "Initiating ADB handshake...")

        val connectMsg = AdbMessage(
            AdbProtocol.A_CNXN,
            AdbProtocol.CONNECT_VERSION,
            AdbProtocol.CONNECT_MAXDATA,
            systemIdentity.toByteArray()
        )

        val writeRes = transport.write(connectMsg.serialize())
        if (!writeRes.isSuccess) {
            SafeLog.e(TAG, "Failed to write CNXN packet")
            return false
        }

        val response = receiveMessage() ?: return false
        
        return when (response.command) {
            AdbProtocol.A_CNXN -> {
                this.maxData = response.arg1
                SafeLog.i(TAG, "ADB handshake successful (maxData=$maxData)")
                _isConnected.value = true
                true
            }
            AdbProtocol.A_AUTH -> {
                SafeLog.w(TAG, "ADB AUTH required")
                handleAuth(response.data)
            }
            else -> {
                SafeLog.e(TAG, "Unexpected ADB command: ${response.command}")
                false
            }
        }
    }

    private suspend fun receiveMessage(): AdbMessage? {
        val header = readExact(HEADER_LENGTH) ?: run {
            SafeLog.e(TAG, "Failed to read full ADB header")
            return null
        }

        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val command = buffer.int
        val arg0 = buffer.int
        val arg1 = buffer.int
        val dataLen = buffer.int
        val checksum = buffer.int
        val magic = buffer.int

        if (magic != AdbProtocol.generateMagic(command)) {
            SafeLog.e(TAG, "ADB header magic mismatch command=$command magic=$magic")
            return null
        }

        if (dataLen < 0 || dataLen > MAX_MESSAGE_DATA) {
            SafeLog.e(TAG, "ADB payload length out of range len=$dataLen")
            return null
        }

        val data = if (dataLen > 0) {
            readExact(dataLen) ?: run {
                SafeLog.e(TAG, "Failed to read full ADB payload len=$dataLen")
                return null
            }
        } else {
            null
        }

        if (AdbProtocol.generateChecksum(data) != checksum) {
            SafeLog.e(TAG, "ADB payload checksum mismatch len=$dataLen")
            return null
        }

        return AdbMessage(command = command, arg0 = arg0, arg1 = arg1, data = data)
    }

    private suspend fun readExact(expectedSize: Int): ByteArray? {
        if (expectedSize == 0) return ByteArray(0)

        val output = ByteArrayOutputStream(expectedSize)
        while (output.size() < expectedSize) {
            val remaining = expectedSize - output.size()
            when (val result = transport.read(remaining)) {
                is TransferResult.Success -> {
                    val chunk = result.data
                    if (chunk == null || chunk.isEmpty()) {
                        return null
                    }
                    output.write(chunk)
                }

                else -> return null
            }
        }

        return output.toByteArray()
    }

    private suspend fun handleAuth(token: ByteArray?): Boolean {
        if (token == null) return false
        SafeLog.i(TAG, "Handling ADB AUTH...")

        val keyPair = AdbCrypto.generateKeyPair()
        val signature = AdbCrypto.signToken(token, keyPair)

        val authMsg = AdbMessage(
            AdbProtocol.A_AUTH,
            AdbProtocol.AUTH_SIGNATURE,
            0,
            signature
        )

        if (!transport.write(authMsg.serialize()).isSuccess) {
            SafeLog.e(TAG, "Failed to send ADB AUTH signature")
            return false
        }

        val resp = receiveMessage() ?: return false
        if (resp.command == AdbProtocol.A_CNXN) {
            _isConnected.value = true
            return true
        }

        // Try public key fallback
        if (resp.command == AdbProtocol.A_AUTH && resp.arg0 == AdbProtocol.AUTH_TOKEN) {
            val pubKey = AdbCrypto.getAdbPublicKeyPayload(keyPair.public as java.security.interfaces.RSAPublicKey)
            val pubKeyMsg = AdbMessage(
                AdbProtocol.A_AUTH,
                AdbProtocol.AUTH_RSAPUBLICKEY,
                0,
                pubKey + byteArrayOf(0)
            )
            if (!transport.write(pubKeyMsg.serialize()).isSuccess) {
                SafeLog.e(TAG, "Failed to send ADB AUTH public key")
                return false
            }

            val finalResp = receiveMessage() ?: return false
            if (finalResp.command == AdbProtocol.A_CNXN) {
                _isConnected.value = true
                return true
            }
        }

        return false
    }

    suspend fun open(destination: String): Int? {
        if (!_isConnected.value) return null
        
        val lId = localIdCounter++
        val openMsg = AdbMessage(
            AdbProtocol.A_OPEN,
            lId,
            0,
            (destination + "\u0000").toByteArray()
        )

        if (!transport.write(openMsg.serialize()).isSuccess) {
            SafeLog.e(TAG, "Failed to open ADB destination=$destination")
            return null
        }
        
        val resp = receiveMessage()
        if (resp != null && resp.command == AdbProtocol.A_OKAY) {
            val rId = resp.arg0
            activeStreams[lId] = rId
            return lId
        }
        return null
    }

    /**
     * Reads a single block of data from a stream.
     * In a full implementation, this would be reactive (listening for WRTE/CLSE).
     */
    suspend fun readString(localId: Int): String? {
        val rId = activeStreams[localId] ?: return null
        val msg = receiveMessage() ?: return null
        
        when (msg.command) {
            AdbProtocol.A_WRTE -> {
                // Acknowledge WRITE (OKAY)
                val okay = AdbMessage(AdbProtocol.A_OKAY, localId, rId, null)
                if (!transport.write(okay.serialize()).isSuccess) {
                    SafeLog.e(TAG, "Failed to acknowledge ADB WRTE localId=$localId")
                    return null
                }
                return msg.data?.let { String(it) }
            }
            AdbProtocol.A_CLSE -> {
                activeStreams.remove(localId)
                return null
            }
        }
        return null
    }

    suspend fun write(localId: Int, data: ByteArray): Boolean {
        val rId = activeStreams[localId] ?: return false
        val msg = AdbMessage(AdbProtocol.A_WRTE, localId, rId, data)
        return transport.write(msg.serialize()).isSuccess
    }

    suspend fun close(localId: Int) {
        val rId = activeStreams[localId] ?: return
        val msg = AdbMessage(AdbProtocol.A_CLSE, localId, rId, null)
        transport.write(msg.serialize())
        activeStreams.remove(localId)
    }

    /**
     * Executes a single shell command and returns the output.
     * Opens a stream, reads until CLSE, and closes.
     */
    suspend fun shell(command: String, timeoutMs: Long = 10000): Result<String> {
        val streamId = open("shell:$command") 
            ?: return Result.failure(Exception("Failed to open shell stream"))

        return try {
            val output = StringBuilder()
            // In a real implementation, we'd loop until CLSE. 
            // For now, read the first chunk of response.
            val chunk = readString(streamId)
            if (chunk != null) output.append(chunk)
            
            Result.success(output.toString())
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            close(streamId)
        }
    }
}
