package com.deepeye.otg

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class OtgActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button
    private lateinit var partitionList: ListView
    private lateinit var dumpButton: Button
    private lateinit var flashButton: Button
    private lateinit var usbHostManager: UsbHostManager
    private var nativeHandle: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otg)

        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        partitionList = findViewById(R.id.partitionList)
        dumpButton = findViewById(R.id.dumpButton)
        flashButton = findViewById(R.id.flashButton)
        
        usbHostManager = UsbHostManager(this)

        connectButton.setOnClickListener {
            statusText.text = "Searching for OTG Target..."
            // Search for common service mode VID/PIDs (e.g., MTK 0E8D:0003)
            usbHostManager.findAndConnect(0x0E8D, 0x0003) { fd -> 
                initializeCore(fd)
            }
        }
    }

    private fun initializeCore(fd: Int) {
        nativeHandle = NativeBridge.initCore(fd, 0x0E8D, 0x0003)
        if (nativeHandle != 0L) {
            val identified = NativeBridge.identifyDevice(nativeHandle)
            if (identified) {
                statusText.text = "Connected: Protocol Identified"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                
                // Fetch and display partitions
                val partitions = NativeBridge.getPartitions(nativeHandle)
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, partitions)
                partitionList.adapter = adapter
                
                dumpButton.isEnabled = true
                flashButton.isEnabled = true
            } else {
                statusText.text = "Device detected but no protocol response."
            }
        } else {
            statusText.text = "Native Engine Initialization Failed (Check libusb logs)."
        }
    }

    override fun onDestroy() {
        if (nativeHandle != 0L) {
            NativeBridge.closeCore(nativeHandle)
        }
        super.onDestroy()
    }
}
