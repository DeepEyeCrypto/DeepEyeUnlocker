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
import java.util.UUID

class RemoteShareActivity : AppCompatActivity() {

    private lateinit var usbManager: UsbManager
    private val actionUsbPermission = "com.deepeye.otg.USB_PERMISSION"
    private var pendingPermissionDeviceId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        setContent {
            var status by remember { mutableStateOf("WAITING FOR DEVICE") }
            var subStatus by remember { mutableStateOf("Connect USB device via OTG...") }
            var usbDevice by remember { mutableStateOf<UsbDevice?>(null) }
            var sessionCode by remember { mutableStateOf<String?>(null) }
            var isDetected by remember { mutableStateOf(false) }

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
                sessionCode = sessionCode,
                isDeviceDetected = isDetected,
                onStartSharing = {
                    if (sessionCode == null) {
                        val device = usbDevice
                        if (device == null) {
                            Toast.makeText(
                                this@RemoteShareActivity,
                                "No USB Device Connected!",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@RemoteShareScreen
                        }

                        if (usbManager.hasPermission(device)) {
                            sessionCode = UUID.randomUUID().toString()
                                .substring(0, 8)
                                .uppercase()
                            status = "SHARING ACTIVE"
                            subStatus = "Relay Tunnel Established via TCP"
                        } else {
                            requestUsbPermission(device)
                            status = "WAITING PERMISSION"
                            subStatus = "Approve USB permission dialog"
                        }
                    } else {
                        sessionCode = null
                        status = "DEVICE DETECTED"
                        subStatus = "Sharing stopped"
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