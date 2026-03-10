package com.deepeye.otg.service

import android.hardware.usb.UsbDevice
import android.util.Log
import com.deepeye.otg.usb.TransferResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import com.deepeye.otg.usb.UsbLifecycleState
import com.deepeye.otg.domain.models.DeviceMode
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
     * Provider Mode: Share the entire forensic fleet to the cloud for expert collaboration.
     */
    fun startFleetSharing(sessions: Map<String, UsbLifecycleState>) {
        _status.value = TunnelStatus.CONNECTING
        val request = Request.Builder().url(RELAY_ENDPOINT).build()
        
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _status.value = TunnelStatus.ACTIVE
                Log.i(TAG, "[TUNNEL] Fleet broadcast active.")
                
                val fleetStatus = JSONObject().apply {
                    put("type", "FLEET_HANDSHAKE")
                    put("nodeCount", sessions.size)
                    val nodes = org.json.JSONArray()
                    sessions.forEach { (key, state) ->
                        if (state is UsbLifecycleState.Connected) {
                            nodes.put(JSONObject().apply {
                                put("key", key)
                                put("name", state.deviceName)
                                put("chipset", state.chipset)
                                put("mode", state.detectedDeviceMode.name)
                            })
                        }
                    }
                    put("nodes", nodes)
                }
                webSocket.send(fleetStatus.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "[TUNNEL] Fleet sync failure: ${t.message}")
                _status.value = TunnelStatus.FAILED
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _status.value = TunnelStatus.IDLE
            }
        })
    }

    /**
     * Legacy Provider Mode: Share a local USB device to the cloud.
     */
    /**
     * Legacy Provider Mode: Share a local USB device to the cloud.
     */
    fun startSharing(device: UsbDevice) {
        // Implementation for single device sharing
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
        val transport = lm.getTransport() ?: run {
            Log.w(TAG, "[TUNNEL] Ignoring REMOTE_USB_REQ: no active USB transport")
            return
        }
        
        val reqId = json.optString("reqId", "")
        val op = json.optString("op", "").uppercase() // "READ" or "WRITE"
        
        tunnelScope.launch {
            val response = JSONObject().apply {
                put("type", "REMOTE_USB_RES")
                put("reqId", reqId)
            }

            try {
                when (op) {
                    "WRITE" -> {
                        val dataHex = json.optString("data", "")
                        val data = parseHexPayload(dataHex)
                        when (val result = transport.write(data, 5_000)) {
                            is TransferResult.Success -> {
                                response.put("success", true)
                                response.put("bytes", result.bytes)
                            }
                            is TransferResult.Partial -> {
                                response.put("success", false)
                                response.put("bytes", result.bytes)
                                response.put("expected", result.expected)
                                response.put("error", "Partial transfer")
                            }
                            else -> {
                                response.put("success", false)
                                response.put("error", result.toErrorMessage())
                            }
                        }
                    }

                    "READ" -> {
                        val length = json.optInt("length", 512).coerceAtLeast(1)
                        when (val result = transport.read(length, 5_000)) {
                            is TransferResult.Success -> {
                                response.put("success", true)
                                response.put("bytes", result.bytes)
                                response.put("data", result.data?.toHexString() ?: "")
                            }
                            is TransferResult.Partial -> {
                                response.put("success", false)
                                response.put("bytes", result.bytes)
                                response.put("expected", result.expected)
                                response.put("data", result.data.toHexString())
                                response.put("error", "Partial transfer")
                            }
                            else -> {
                                response.put("success", false)
                                response.put("error", result.toErrorMessage())
                            }
                        }
                    }

                    else -> {
                        response.put("success", false)
                        response.put("error", "Unsupported op: $op")
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

    private fun parseHexPayload(hex: String): ByteArray {
        val cleaned = hex.filterNot { it.isWhitespace() }
        if (cleaned.isEmpty()) return ByteArray(0)
        val normalized = if (cleaned.length % 2 == 0) cleaned else "0$cleaned"
        return normalized.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun ByteArray.toHexString(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte) }

    private fun TransferResult.toErrorMessage(): String = when (this) {
        TransferResult.Timeout -> "USB transfer timeout"
        TransferResult.DeviceGone -> "USB device disconnected"
        TransferResult.Stall -> "USB endpoint stalled"
        is TransferResult.IOError -> msg
        is TransferResult.ProtocolError -> msg
        is TransferResult.Success -> ""
        is TransferResult.Partial -> "Partial transfer"
        else -> "Unknown transfer error"
    }

    fun stopSharing() {
        socket?.close(1000, "User manually stopped sharing")
        socket = null
        _sessionCode.value = null
        _status.value = TunnelStatus.IDLE
    }
}
