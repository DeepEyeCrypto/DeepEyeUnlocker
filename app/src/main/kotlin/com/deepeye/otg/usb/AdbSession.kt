package com.deepeye.otg.usb

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * High-level session for ADB operations (Stage 7.2).
 * Hardened for Pro cycle (v2026.27): concurrent safety and window management stubs.
 */
class AdbSession(
    private val transport: UsbTransport
) {
    companion object {
        private const val TAG = "DeepEye-ADB"
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
        
        Log.i(TAG, "Initiating ADB handshake...")

        val connectMsg = AdbMessage(
            AdbProtocol.A_CNXN,
            AdbProtocol.CONNECT_VERSION,
            AdbProtocol.CONNECT_MAXDATA,
            systemIdentity.toByteArray()
        )

        val writeRes = transport.write(connectMsg.serialize())
        if (!writeRes.isSuccess) {
            Log.e(TAG, "Failed to write CNXN packet")
            return false
        }

        val response = receiveMessage() ?: return false
        
        return when (response.command) {
            AdbProtocol.A_CNXN -> {
                this.maxData = response.arg1
                Log.i(TAG, "ADB handshake successful (maxData=$maxData)")
                _isConnected.value = true
                true
            }
            AdbProtocol.A_AUTH -> {
                Log.w(TAG, "ADB AUTH required")
                handleAuth(response.data)
            }
            else -> {
                Log.e(TAG, "Unexpected ADB command: ${response.command}")
                false
            }
        }
    }

    private suspend fun receiveMessage(): AdbMessage? {
        val headerRes = transport.read(24)
        if (headerRes !is TransferResult.Success || headerRes.data == null) return null
        
        val msg = AdbMessage.parseHeader(headerRes.data)
        val dataLen = java.nio.ByteBuffer.wrap(headerRes.data).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt(12)
        
        val data = if (dataLen > 0) {
            val dataRes = transport.read(dataLen)
            if (dataRes is TransferResult.Success) dataRes.data else null
        } else null
        
        return msg.copy(data = data)
    }

    private suspend fun handleAuth(token: ByteArray?): Boolean {
        if (token == null) return false
        Log.i(TAG, "Handling ADB AUTH...")

        val keyPair = AdbCrypto.generateKeyPair()
        val signature = AdbCrypto.signToken(token, keyPair)

        val authMsg = AdbMessage(
            AdbProtocol.A_AUTH,
            AdbProtocol.AUTH_SIGNATURE,
            0,
            signature
        )

        transport.write(authMsg.serialize())

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
            transport.write(pubKeyMsg.serialize())

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

        transport.write(openMsg.serialize())
        
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
                transport.write(okay.serialize())
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
}
