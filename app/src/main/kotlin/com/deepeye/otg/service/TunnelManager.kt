package com.deepeye.otg.service

import android.hardware.usb.UsbDevice
import android.util.Log
import com.deepeye.otg.usb.TransferResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import com.deepeye.otg.usb.UsbLifecycleState
import com.deepeye.otg.usb.UsbLifecycleManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stage 5 — DeepEye Remote Tunnel (Enhanced).
 * Manages the relay of USB packets and forensic logs over a pinned WebSocket tunnel.
 */
@Singleton
class TunnelManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val lifecycleManager: UsbLifecycleManager
) {
    companion object {
        private const val TAG = "DeepEye-Tunnel"
        private const val RELAY_ENDPOINT = "wss://relay.deepeye.security/v1/tunnel"
    }

    enum class TunnelStatus {
        IDLE,
        CONNECTING,
        ACTIVE,
        FAILED
    }

    private var socket: WebSocket? = null
    
    private val _status = MutableStateFlow(TunnelStatus.IDLE)
    val status: StateFlow<TunnelStatus> = _status.asStateFlow()

    private val _sessionCode = MutableStateFlow<String?>(null)
    val sessionCode: StateFlow<String?> = _sessionCode.asStateFlow()

    private val tunnelScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Fleet Sharing: Securely expose all connected hardware to the DeepEye Relay.
     */
    fun startFleetSharing() {
        if (_status.value == TunnelStatus.ACTIVE || _status.value == TunnelStatus.CONNECTING) return
        
        _status.value = TunnelStatus.CONNECTING
        val request = Request.Builder().url(RELAY_ENDPOINT).build()
        
        socket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _status.value = TunnelStatus.ACTIVE
                Log.i(TAG, "[TUNNEL] Fleet broadcast active.")
                
                // Initial Handshake
                val handshake = JSONObject().apply {
                    put("type", "FLEET_HANDSHAKE")
                    put("hwid", com.deepeye.otg.data.HWIDEngine.getHWID())
                    put("timestamp", System.currentTimeMillis())
                }
                webSocket.send(handshake.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "[TUNNEL] Critical failure: ${t.message}")
                _status.value = TunnelStatus.FAILED
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _status.value = TunnelStatus.IDLE
            }
        })
    }

    /**
     * Operator Mode: Attach to a remote forensic node via its session code.
     */
    fun joinSession(code: String) {
        if (_status.value != TunnelStatus.IDLE) stopSharing()
        
        _status.value = TunnelStatus.CONNECTING
        val request = Request.Builder().url(RELAY_ENDPOINT).build()
        
        socket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _status.value = TunnelStatus.ACTIVE
                val joinReq = JSONObject().apply {
                    put("type", "OPERATOR_JOIN")
                    put("sessionCode", code)
                    put("timestamp", System.currentTimeMillis())
                }
                webSocket.send(joinReq.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _status.value = TunnelStatus.FAILED
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "SESSION_CREATED" -> {
                    _sessionCode.value = json.optString("sessionCode")
                }
                "REMOTE_PEEK_REQ" -> {
                    // Stage 5: Remote 'Peek' Analysis
                    val logCount = json.optInt("count", 50).coerceIn(10, 500)
                    sendSystemSnapshot(logCount)
                }
                "REMOTE_USB_REQ" -> {
                    handleRemoteUsbRequest(json)
                }
                "PING" -> {
                    socket?.send(JSONObject().apply { put("type", "PONG") }.toString())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[TUNNEL] Protocol Error: ${e.message}")
        }
    }

    private fun sendSystemSnapshot(logCount: Int) {
        tunnelScope.launch {
            val logs = com.deepeye.otg.usb.UsbLogger.logBuffer.value.takeLast(logCount)
            val snapshot = JSONObject().apply {
                put("type", "SYSTEM_SNAPSHOT")
                put("status", _status.value.name)
                put("deviceMode", lifecycleManager.getTransport()?.let { "CONNECTED" } ?: "IDLE")
                val logsArray = org.json.JSONArray()
                logs.forEach { logsArray.put(it) }
                put("logs", logsArray)
                put("timestamp", System.currentTimeMillis())
            }
            socket?.send(snapshot.toString())
        }
    }

    private fun handleRemoteUsbRequest(json: JSONObject) {
        val transport = lifecycleManager.getTransport() ?: run {
            sendError(json.optString("reqId"), "No active USB transport")
            return
        }
        
        val reqId = json.optString("reqId", "")
        val op = json.optString("op", "").uppercase()
        
        tunnelScope.launch {
            val response = JSONObject().apply {
                put("type", "REMOTE_USB_RES")
                put("reqId", reqId)
            }

            try {
                when (op) {
                    "WRITE" -> {
                        val data = parseHex(json.optString("data", ""))
                        when (val result = transport.write(data, 5_000)) {
                            is TransferResult.Success -> {
                                response.put("success", true)
                                response.put("bytes", result.bytes)
                            }
                            else -> {
                                response.put("success", false)
                                response.put("error", result.toString())
                            }
                        }
                    }
                    "READ" -> {
                        val length = json.optInt("length", 512).coerceAtLeast(1)
                        when (val result = transport.read(length, 5_000)) {
                            is TransferResult.Success -> {
                                response.put("success", true)
                                response.put("data", result.data?.joinToString("") { "%02x".format(it) } ?: "")
                            }
                            else -> {
                                response.put("success", false)
                                response.put("error", result.toString())
                            }
                        }
                    }
                }
                socket?.send(response.toString())
            } catch (e: Exception) {
                sendError(reqId, e.message ?: "Execution error")
            }
        }
    }

    private fun sendError(reqId: String?, msg: String) {
        val err = JSONObject().apply {
            put("type", "REMOTE_ERROR")
            put("reqId", reqId)
            put("message", msg)
        }
        socket?.send(err.toString())
    }

    private fun parseHex(hex: String): ByteArray {
        val s = hex.filterNot { it.isWhitespace() }
        return s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    fun stopSharing() {
        socket?.close(1000, "Clean switch")
        socket = null
        _sessionCode.value = null
        _status.value = TunnelStatus.IDLE
    }
}
