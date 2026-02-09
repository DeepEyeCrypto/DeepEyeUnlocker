package com.deepeye.otg

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

class RemoteShareActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var txtStatus: TextView
    private lateinit var txtSubStatus: TextView
    private lateinit var txtSessionCode: TextView
    private lateinit var sessionBox: LinearLayout
    private lateinit var iconStatus: ImageView

    private lateinit var usbManager: UsbManager
    private var usbDevice: UsbDevice? = null
    private val ACTION_USB_PERMISSION = "com.deepeye.otg.USB_PERMISSION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote_share)

        btnStart = findViewById(R.id.btnStartShare)
        txtStatus = findViewById(R.id.txtStatus)
        txtSubStatus = findViewById(R.id.txtSubStatus)
        txtSessionCode = findViewById(R.id.txtSessionCode)
        sessionBox = findViewById(R.id.sessionBox)
        iconStatus = findViewById(R.id.iconStatus)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        // Check for connected devices immediately
        checkConnectedDevices()

        btnStart.setOnClickListener {
            if (usbDevice != null) {
                if (usbManager.hasPermission(usbDevice)) {
                    startSharing()
                } else {
                    requestUsbPermission()
                }
            } else {
                Toast.makeText(this, "No USB Device Connected!", Toast.LENGTH_SHORT).show()
                checkConnectedDevices()
            }
        }
    }

    private fun checkConnectedDevices() {
        val deviceList = usbManager.deviceList
        if (deviceList.isNotEmpty()) {
            usbDevice = deviceList.values.first() // Take first device
            txtStatus.text = "DEVICE DETECTED"
            txtSubStatus.text = "${usbDevice?.productName ?: "Unknown Device"} (ID: ${usbDevice?.deviceId})"
            btnStart.isEnabled = true
            iconStatus.setColorFilter(getColor(android.R.color.holo_green_light))
        } else {
            usbDevice = null
            txtStatus.text = "WAITING FOR DEVICE"
            txtSubStatus.text = "Connect USB device via OTG..."
            btnStart.isEnabled = false
            iconStatus.setColorFilter(getColor(android.R.color.darker_gray))
        }
    }

    private fun requestUsbPermission() {
        val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
        usbManager.requestPermission(usbDevice, permissionIntent)
    }

    private fun startSharing() {
        // Here we would start the TCP Server / UsbSharer logic
        // For now, we simulate a session code
        
        val code = UUID.randomUUID().toString().substring(0, 8).uppercase()
        
        txtStatus.text = "SHARING ACTIVE"
        txtStatus.setTextColor(getColor(android.R.color.holo_purple))
        txtSubStatus.text = "Relay Tunnel Established via TCP"
        
        sessionBox.visibility = android.view.View.VISIBLE
        txtSessionCode.text = code
        btnStart.text = "STOP SHARING"
        btnStart.setBackgroundColor(getColor(android.R.color.holo_red_dark))
        
        // TODO: Call Native Bridge for USB Redirection
        Toast.makeText(this, "Remote Tunnel Opened: $code", Toast.LENGTH_LONG).show()
    }
}
