package com.deepeye.otg

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import com.deepeye.otg.ui.RemoteShareScreen
import java.util.UUID

class RemoteShareActivity : AppCompatActivity() {

    private lateinit var usbManager: UsbManager
    private val ACTION_USB_PERMISSION = "com.deepeye.otg.USB_PERMISSION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        setContent {
            var status by remember { mutableStateOf("WAITING FOR DEVICE") }
            var subStatus by remember { mutableStateOf("Connect USB device via OTG...") }
            var usbDevice by remember { mutableStateOf<UsbDevice?>(null) }
            var sessionCode by remember { mutableStateOf<String?>(null) }
            var isDetected by remember { mutableStateOf(false) }

            // Initial check
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
                        if (usbDevice != null) {
                            if (usbDevice != null && usbManager.hasPermission(usbDevice)) {
                                sessionCode = UUID.randomUUID().toString().substring(0, 8).uppercase()
                                status = "SHARING ACTIVE"
                                subStatus = "Relay Tunnel Established via TCP"
                            } else {
                                requestUsbPermission(usbDevice!!)
                            }
                        } else {
                            Toast.makeText(this@RemoteShareActivity, "No USB Device Connected!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        sessionCode = null
                        status = "DEVICE DETECTED"
                        subStatus = "Sharing stopped"
                    }
                },
                onConnectRemote = { id ->
                    if (id.isNotEmpty()) {
                        val intent = Intent(this@RemoteShareActivity, com.deepeye.otg.ui.OtgActivity::class.java)
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
        // FLAG_MUTABLE required on Android 12+ so the system can fill in EXTRA_DEVICE / EXTRA_PERMISSION_GRANTED
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_MUTABLE else 0
        val intent = Intent(ACTION_USB_PERMISSION).apply {
            setPackage(packageName)
        }
        val permissionIntent = PendingIntent.getBroadcast(this, 0, intent, flags)
        usbManager.requestPermission(device, permissionIntent)
    }
}
