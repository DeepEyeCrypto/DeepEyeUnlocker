package com.deepeye.otg.usb

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-level session for ADB operations (Stage 7.2).
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
    private var localId = 1
    private var remoteId = 0

    /**
     * Perform the ADB connection handshake.
     */
    suspend fun connect(systemIdentity: String = "host::DeepEyeOTG"): Boolean {
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

        // Wait for response
        val readRes = transport.read(24) // Header only first
        if (readRes !is TransferResult.Success || readRes.data == null) {
            Log.e(TAG, "Failed to read CNXN response")
            return false
        }

        val response = receiveMessage() ?: return false
        
        when (response.command) {
            AdbProtocol.A_CNXN -> {
                Log.i(TAG, "ADB handshake successful")
                _isConnected.value = true
                return true
            }
            AdbProtocol.A_AUTH -> {
                Log.w(TAG, "ADB AUTH required")
                return handleAuth(response.data)
            }
            else -> {
                Log.e(TAG, "Unexpected ADB command: ${response.command}")
                return false
            }
        }
    }

    private suspend fun receiveMessage(): AdbMessage? {
        val headerRes = transport.read(24)
        if (headerRes !is TransferResult.Success || headerRes.data == null) return null
        
        val header = headerRes.data
        val dataLen = java.nio.ByteBuffer.wrap(header).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt(12)
        
        val data = if (dataLen > 0) {
            val dataRes = transport.read(dataLen)
            if (dataRes is TransferResult.Success) dataRes.data else null
        } else null
        
        return AdbMessage.parse(header, data)
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

        // Read response to signature
        val readRes = transport.read(24)
        if (readRes !is TransferResult.Success || readRes.data == null) return false
        
        val resp = AdbMessage.parse(readRes.data, null)
        if (resp.command == AdbProtocol.A_CNXN) {
            _isConnected.value = true
            return true
        }

        // If signature failed, try sending public key
        if (resp.command == AdbProtocol.A_AUTH && resp.arg0 == AdbProtocol.AUTH_TOKEN) {
            val pubKey = AdbCrypto.getAdbPublicKeyPayload(keyPair.public as java.security.interfaces.RSAPublicKey)
            val pubKeyMsg = AdbMessage(
                AdbProtocol.A_AUTH,
                AdbProtocol.AUTH_RSAPUBLICKEY,
                0,
                pubKey + byteArrayOf(0) // Null terminated
            )
            transport.write(pubKeyMsg.serialize())

            val finalRead = transport.read(24)
            if (finalRead is TransferResult.Success && finalRead.data != null) {
                val finalResp = AdbMessage.parse(finalRead.data, null)
                if (finalResp.command == AdbProtocol.A_CNXN) {
                    _isConnected.value = true
                    return true
                }
            }
        }

        return false
    }

    suspend fun open(destination: String): Int? {
        if (!_isConnected.value) return null
        
        val id = localId++
        val openMsg = AdbMessage(
            AdbProtocol.A_OPEN,
            id,
            0,
            (destination + "\u0000").toByteArray()
        )

        transport.write(openMsg.serialize())
        
        // Wait for OKAY
        val resp = receiveMessage()
        if (resp != null && resp.command == AdbProtocol.A_OKAY) {
            remoteId = resp.arg0
            return remoteId
        }
        return null
    }

    suspend fun readString(): String? {
        val msg = receiveMessage() ?: return null
        if (msg.command == AdbProtocol.A_WRTE && msg.data != null) {
            // Acknowledge WRITE
            val okay = AdbMessage(AdbProtocol.A_OKAY, localId - 1, remoteId, null)
            transport.write(okay.serialize())
            return String(msg.data)
        }
        return null
    }
}
