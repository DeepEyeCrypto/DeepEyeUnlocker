package com.deepeye.otg

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import android.os.Build

class OtgActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button
    private lateinit var partitionList: ListView
    private lateinit var dumpButton: Button
    private lateinit var flashButton: Button
    private lateinit var connectionGlow: android.view.View
    private lateinit var globalProgress: android.widget.ProgressBar
    private lateinit var usbHostManager: UsbHostManager
    private var nativeHandle: Long = 0

    private fun triggerHapticSuccess() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(50)
        }
    }

    private fun triggerHapticError() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 100, 50, 100)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            vibrator.vibrate(pattern, -1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otg)

        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        partitionList = findViewById(R.id.partitionList)
        dumpButton = findViewById(R.id.dumpButton)
        flashButton = findViewById(R.id.flashButton)
        connectionGlow = findViewById(R.id.connectionGlow)
        globalProgress = findViewById(R.id.globalProgress)
        
        // Start pulse animation on status and glow
        val pulse = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.pulse)
        statusText.startAnimation(pulse)
        connectionGlow.startAnimation(pulse)

        usbHostManager = UsbHostManager(this, object : UsbHostManager.HotplugListener {
            override fun onDeviceAttached(device: android.hardware.usb.UsbDevice) {
                runOnUiThread {
                    statusText.text = "PROACTIVE DISCOVERY: ${device.productName}"
                }
            }

            override fun onDeviceReady(fd: Int) {
                runOnUiThread {
                    initializeCore(fd)
                }
            }
        })

        connectButton.setOnClickListener {
            statusText.text = "SEARCHING FOR OTG TARGET..."
            statusText.setTextColor(ContextCompat.getColor(this, R.color.deepeye_cyan))
            // Manual trigger for known MTK VID/PID
            usbHostManager.findAndConnect(0x0E8D, 0x0003) 
        }
    }

    private fun initializeCore(fd: Int) {
        nativeHandle = NativeBridge.initCore(fd, 0x0E8D, 0x0003)
        if (nativeHandle != 0L) {
            val identified = NativeBridge.identifyDevice(nativeHandle)
            if (identified) {
                triggerHapticSuccess()
                statusText.text = "CONNECTED: PROTOCOL IDENTIFIED"
                statusText.setTextColor(ContextCompat.getColor(this, R.color.deepeye_cyan))
                statusText.clearAnimation() 
                connectionGlow.clearAnimation()
                globalProgress.isIndeterminate = false
                globalProgress.progress = 100
                
                // Fetch and display partitions
                val partitions = NativeBridge.getPartitions(nativeHandle)
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, partitions)
                partitionList.adapter = adapter
                
                dumpButton.isEnabled = true
                flashButton.isEnabled = true
            } else {
                triggerHapticError()
                statusText.text = "ERROR: NO PROTOCOL RESPONSE"
                statusText.setTextColor(ContextCompat.getColor(this, R.color.deepeye_orange))
            }
        } else {
            triggerHapticError()
            statusText.text = "ERROR: NATIVE ENGINE FAILED"
            statusText.setTextColor(ContextCompat.getColor(this, R.color.deepeye_orange))
        }
    }

    override fun onDestroy() {
        if (nativeHandle != 0L) {
            NativeBridge.closeCore(nativeHandle)
        }
        usbHostManager.unregister()
        super.onDestroy()
    }
}
