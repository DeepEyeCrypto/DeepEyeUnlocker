package com.deepeye.otg

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import android.os.Build
import android.widget.Toast

class OtgActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var statusSubtext: TextView
    private lateinit var connectButton: Button
    private lateinit var partitionList: RecyclerView
    private lateinit var dumpButton: Button
    private lateinit var flashButton: Button
    private lateinit var frpButton: Button
    private lateinit var connectionGlow: android.view.View
    private lateinit var connectionStatus: TextView
    private lateinit var globalProgress: android.widget.ProgressBar
    private lateinit var partitionCount: TextView
    private lateinit var usbHostManager: UsbHostManager
    private lateinit var partitionAdapter: PartitionAdapter
    private var nativeHandle: Long = 0

    private fun triggerHapticSuccess() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun triggerHapticError() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 100, 50, 100)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otg)

        // Initialize views
        statusText = findViewById(R.id.statusText)
        statusSubtext = findViewById(R.id.statusSubtext)
        connectButton = findViewById(R.id.connectButton)
        partitionList = findViewById(R.id.partitionList)
        dumpButton = findViewById(R.id.dumpButton)
        flashButton = findViewById(R.id.flashButton)
        frpButton = findViewById(R.id.frpButton)
        connectionGlow = findViewById(R.id.connectionGlow)
        connectionStatus = findViewById(R.id.connectionStatus)
        globalProgress = findViewById(R.id.globalProgress)
        partitionCount = findViewById(R.id.partitionCount)
        
        // Setup RecyclerView
        partitionAdapter = PartitionAdapter { partition ->
            Toast.makeText(this, "Selected: ${partition.name}", Toast.LENGTH_SHORT).show()
        }
        partitionList.apply {
            layoutManager = LinearLayoutManager(this@OtgActivity)
            adapter = partitionAdapter
        }
        
        // Start pulse animation on status and glow
        val pulse = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.pulse)
        statusText.startAnimation(pulse)
        connectionGlow.startAnimation(pulse)

        usbHostManager = UsbHostManager(this, object : UsbHostManager.HotplugListener {
            override fun onDeviceAttached(device: android.hardware.usb.UsbDevice) {
                runOnUiThread {
                    statusText.text = "DEVICE DETECTED: ${device.productName ?: "Unknown"}"
                    statusSubtext.text = "VID: ${device.vendorId.toString(16).uppercase()} | PID: ${device.productId.toString(16).uppercase()}"
                }
            }

            override fun onDeviceReady(fd: Int) {
                runOnUiThread {
                    initializeCore(fd)
                }
            }
        })

        connectButton.setOnClickListener {
            statusText.text = "SCANNING FOR OTG TARGET..."
            statusSubtext.text = "Probing USB host interface"
            statusText.setTextColor(ContextCompat.getColor(this, R.color.deepeye_cyan))
            globalProgress.isIndeterminate = true
            
            // Try common MTK VID/PIDs
            usbHostManager.findAndConnect(0x0E8D, 0x0003) // MTK Preloader
        }
        
        dumpButton.setOnClickListener {
            Toast.makeText(this, "Backup feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        flashButton.setOnClickListener {
            Toast.makeText(this, "Flash feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        frpButton.setOnClickListener {
            Toast.makeText(this, "FRP Bypass Engine initializing...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeCore(fd: Int) {
        nativeHandle = NativeBridge.initCore(fd, 0x0E8D, 0x0003)
        if (nativeHandle != 0L) {
            val identified = NativeBridge.identifyDevice(nativeHandle)
            if (identified) {
                triggerHapticSuccess()
                onDeviceConnected()
            } else {
                triggerHapticError()
                onConnectionFailed("HANDSHAKE FAILED", "Device unresponsive to DA injection")
            }
        } else {
            triggerHapticError()
            onConnectionFailed("ENGINE FAILURE", "Native core initialization failed")
        }
    }
    
    private fun onDeviceConnected() {
        statusText.text = "DEVICE SECURED"
        statusSubtext.text = "Protocol: MTK BROM | Chipset: MT6765"
        statusText.setTextColor(ContextCompat.getColor(this, R.color.deepeye_white))
        statusText.clearAnimation()
        connectionGlow.clearAnimation()
        
        connectionStatus.text = "ACTIVE"
        connectionStatus.setTextColor(ContextCompat.getColor(this, R.color.deepeye_success))
        
        globalProgress.isIndeterminate = false
        globalProgress.progress = 100
        
        // Show device info card
        val deviceInfoCard = findViewById<android.widget.LinearLayout>(R.id.deviceInfoCard)
        deviceInfoCard.visibility = android.view.View.VISIBLE
        val slideIn = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_in_up)
        deviceInfoCard.startAnimation(slideIn)
        
        findViewById<TextView>(R.id.deviceProtocol).text = "MTK BROM"
        findViewById<TextView>(R.id.deviceChipset).text = "MT6765"

        // Fetch and display partitions
        val partitionsRaw = NativeBridge.getPartitions(nativeHandle)
        val partitions = partitionsRaw.map { raw ->
            val parts = raw.split("(", ")")
            PartitionInfo(
                name = parts.getOrElse(0) { raw }.trim(),
                size = parts.getOrElse(1) { "Unknown" }.trim(),
                type = "System Partition"
            )
        }
        partitionAdapter.updatePartitions(partitions)
        partitionCount.text = "${partitions.size} partitions"
        
        // Enable buttons
        dumpButton.isEnabled = true
        flashButton.isEnabled = true
        frpButton.isEnabled = true
    }
    
    private fun onConnectionFailed(title: String, subtitle: String) {
        statusText.text = title
        statusSubtext.text = subtitle
        statusText.setTextColor(ContextCompat.getColor(this, R.color.deepeye_error))
        globalProgress.isIndeterminate = false
        globalProgress.progress = 0
        connectionStatus.text = "ERROR"
        connectionStatus.setTextColor(ContextCompat.getColor(this, R.color.deepeye_error))
    }

    override fun onDestroy() {
        if (nativeHandle != 0L) {
            NativeBridge.closeCore(nativeHandle)
        }
        usbHostManager.unregister()
        super.onDestroy()
    }
}
