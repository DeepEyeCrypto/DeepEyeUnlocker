package com.deepeye.otg.service

import android.hardware.usb.UsbDevice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Stage H — DeepEye Tunnel Bridge.
 * Manages the relay of USB packets over a WebSocket tunnel.
 */
object TunnelManager {
    private const val TAG = "DeepEye-Tunnel"
    private const val RELAY_ENDPOINT = "wss://relay.deepeye.cloud/v1/tunnel"

    enum class TunnelStatus {
        IDLE,
        CONNECTING,
        ACTIVE,
        FAILED
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite for WS
        .build()

    private var socket: WebSocket? = null
    
    private val _status = MutableStateFlow(TunnelStatus.IDLE)
    val status: StateFlow<TunnelStatus> = _status

    private val _sessionCode = MutableStateFlow<String?>(null)
    val sessionCode: StateFlow<String?> = _sessionCode

    /**
     * Provider Mode: Share a local USB device to the cloud.
     */
    fun startSharing(device: UsbDevice) {
        _status.value = TunnelStatus.CONNECTING
        val request = Request.Builder().url(RELAY_ENDPOINT).build()
        
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _status.value = TunnelStatus.ACTIVE
                Log.i(TAG, "[TUNNEL] Pipe established.")
                
                // 1. Send Handshake with Device Info
                val handshake = JSONObject().apply {
                    put("type", "PROVIDER_HANDSHAKE")
                    put("deviceName", device.productName ?: "Generic Device")
                    put("vid", device.vendorId)
                    put("pid", device.productId)
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
                Log.i(TAG, "[TUNNEL] Closing: $reason")
                _status.value = TunnelStatus.IDLE
            }
        })
    }

    /**
     * Operator Mode: Connect to a shared device.
     */
    fun joinSession(code: String) {
        _status.value = TunnelStatus.CONNECTING
        val request = Request.Builder().url(RELAY_ENDPOINT).build()
        
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _status.value = TunnelStatus.ACTIVE
                val joinReq = JSONObject().apply {
                    put("type", "OPERATOR_JOIN")
                    put("sessionCode", code)
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

    private var lifecycleManager: com.deepeye.otg.usb.UsbLifecycleManager? = null

    fun initialize(lm: com.deepeye.otg.usb.UsbLifecycleManager) {
        this.lifecycleManager = lm
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.getString("type")) {
                "SESSION_CREATED" -> {
                    val code = json.getString("sessionCode")
                    _sessionCode.value = code
                }
                "REMOTE_USB_REQ" -> {
                    handleRemoteRequest(json)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[TUNNEL] Message parse error: ${e.message}")
        }
    }

    private val tunnelScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    private fun handleRemoteRequest(json: JSONObject) {
        val lm = lifecycleManager ?: return
        val tq = lm.getTransferQueue() ?: return
        
        val reqId = json.getString("reqId")
        val op = json.getString("op") // "READ" or "WRITE"
        
        tunnelScope.launch {
            val response = JSONObject().apply {
                put("type", "REMOTE_USB_RES")
                put("reqId", reqId)
            }

            try {
                if (op == "WRITE") {
                    val dataHex = json.getString("data")
                    val data = dataHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                    val result = tq.write(data, 5000)
                    response.put("success", result.success)
                } else if (op == "READ") {
                    val length = json.getInt("length")
                    val result = tq.read(length, 5000)
                    if (result.success) {
                        response.put("success", true)
                        response.put("data", result.data?.joinToString("") { "%02x".format(it) } ?: "")
                    } else {
                        response.put("success", false)
                    }
                }
                socket?.send(response.toString())
            } catch (e: Exception) {
                response.put("success", false)
                response.put("error", e.message)
                socket?.send(response.toString())
            }
        }
    }

    fun stopSharing() {
        socket?.close(1000, "User manually stopped sharing")
        socket = null
        _sessionCode.value = null
        _status.value = TunnelStatus.IDLE
    }
}
