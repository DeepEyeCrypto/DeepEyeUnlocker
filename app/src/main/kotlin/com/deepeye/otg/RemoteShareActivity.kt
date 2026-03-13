package com.deepeye.otg

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.deepeye.otg.ui.RemoteShareScreen
import androidx.compose.runtime.collectAsState
import java.util.UUID

@dagger.hilt.android.AndroidEntryPoint
class RemoteShareActivity : AppCompatActivity() {

    private lateinit var usbManager: UsbManager
    private val actionUsbPermission = com.deepeye.otg.usb.UsbPermissionGuard.ACTION_USB_PERMISSION
    private var pendingPermissionDeviceId: Int? = null

    @javax.inject.Inject
    lateinit var tunnel: com.deepeye.otg.service.TunnelManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        
        setContent {
            val tunnelStatus by tunnel.status.collectAsState()
            val tunnelCode by tunnel.sessionCode.collectAsState()
            
            var status by remember { mutableStateOf("WAITING FOR DEVICE") }
            var subStatus by remember { mutableStateOf("Connect USB device via OTG...") }
            var usbDevice by remember { mutableStateOf<UsbDevice?>(null) }
            var isDetected by remember { mutableStateOf(false) }

            // Sync UI with Tunnel State
            LaunchedEffect(tunnelStatus, tunnelCode) {
                when (tunnelStatus) {
                    com.deepeye.otg.service.TunnelManager.TunnelStatus.ACTIVE -> {
                        status = "SHARING ACTIVE"
                        subStatus = "Relay Tunnel Established via WebSocket"
                    }
                    com.deepeye.otg.service.TunnelManager.TunnelStatus.CONNECTING -> {
                        status = "CONNECTING..."
                        subStatus = "Establishing secure handshake..."
                    }
                    com.deepeye.otg.service.TunnelManager.TunnelStatus.FAILED -> {
                        status = "TUNNEL FAILED"
                        subStatus = "Relay server unreachable."
                    }
                    else -> if (isDetected) {
                        status = "DEVICE DETECTED"
                        subStatus = "Ready to share."
                    }
                }
            }

            LaunchedEffect(Unit) {
                val deviceList = usbManager.deviceList
                if (deviceList.isNotEmpty()) {
                    val dev = deviceList.values.first()
                    usbDevice = dev
                    isDetected = true
                    status = "DEVICE DETECTED"
                    subStatus = "${dev.productName ?: "Unknown Device"} (ID: ${dev.deviceId})"
                }
            }

            RemoteShareScreen(
                status = status,
                subStatus = subStatus,
                sessionCode = tunnelCode,
                isDeviceDetected = isDetected,
                onStartSharing = {
                    if (tunnelCode == null) {
                        tunnel.startFleetSharing()
                    } else {
                        tunnel.stopSharing()
                    }
                },
                onConnectRemote = { id ->
                    if (id.isNotEmpty()) {
                        val intent = Intent(
                            this@RemoteShareActivity,
                            com.deepeye.otg.ui.OtgActivity::class.java
                        )
                        intent.putExtra("REMOTE_SESSION", id)
                        startActivity(intent)
                        finish()
                    }
                },
                onBack = { finish() }
            )
        }
    }

    private fun requestUsbPermission(device: UsbDevice) {
        pendingPermissionDeviceId = device.deviceId
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }

        val intent = Intent(actionUsbPermission).apply {
            setPackage(packageName)
        }
        val permissionIntent = PendingIntent.getBroadcast(this, 0, intent, flags)
        runCatching { usbManager.requestPermission(device, permissionIntent) }
    }

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != actionUsbPermission) return

            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            } ?: return

            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            if (pendingPermissionDeviceId != null && pendingPermissionDeviceId != device.deviceId) {
                return
            }
            pendingPermissionDeviceId = null

            val message = if (granted) "USB permission granted" else "USB permission denied"
            Toast.makeText(this@RemoteShareActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(actionUsbPermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(permissionReceiver, filter)
        }
    }

    override fun onStop() {
        runCatching { unregisterReceiver(permissionReceiver) }
        super.onStop()
    }
}