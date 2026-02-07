package com.deepeye.otg

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView

import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import android.os.Build

class OtgActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var statusText: TextView
    private lateinit var statusSubtext: TextView
    private lateinit var connectButton: Button
    private lateinit var partitionList: RecyclerView
    private lateinit var dumpButton: Button
    private lateinit var flashButton: Button
    private lateinit var frpButton: Button
    private lateinit var connectionGlow: View
    private lateinit var connectionStatus: TextView
    private lateinit var globalProgress: android.widget.ProgressBar
    private lateinit var partitionCount: TextView
    private lateinit var logText: TextView
    private lateinit var logScrollView: ScrollView
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
    
    private fun appendLog(message: String, type: String = "INFO") {
        val color = when (type) {
            "ERROR" -> "#FF5252"
            "SUCCESS" -> "#00E676"
            "WARNING" -> "#FFB300"
            else -> "#00F2FF"
        }
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        logText.append("\n[$type] $timestamp: $message")
        logScrollView.post { logScrollView.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otg)

        // Initialize views
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
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
        logText = findViewById(R.id.logText)
        logScrollView = findViewById(R.id.logScrollView)
        
        // Setup drawer
        findViewById<View>(R.id.menuButton).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> appendLog("Dashboard selected", "INFO")
                R.id.nav_da_manager -> appendLog("DA Manager - Coming Soon", "WARNING")
                R.id.nav_partition -> appendLog("Partition Tools selected", "INFO")
                R.id.nav_scatter -> appendLog("Scatter Editor - Coming Soon", "WARNING")
                R.id.nav_preloader -> appendLog("Preloader Tools - Coming Soon", "WARNING")
                R.id.nav_auth -> appendLog("Auth Bypass - Coming Soon", "WARNING")
                R.id.nav_settings -> appendLog("Settings - Coming Soon", "WARNING")
                R.id.nav_about -> Toast.makeText(this, "DeepEye Pro v4.2.0\nBy DeepEyeCrypto", Toast.LENGTH_LONG).show()
                R.id.nav_help -> appendLog("Help & Support - Coming Soon", "WARNING")
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        
        // Setup RecyclerView
        partitionAdapter = PartitionAdapter { partition ->
            appendLog("Selected partition: ${partition.name}", "INFO")
            Toast.makeText(this, "Selected: ${partition.name}", Toast.LENGTH_SHORT).show()
        }
        partitionList.apply {
            layoutManager = LinearLayoutManager(this@OtgActivity)
            adapter = partitionAdapter
        }
        
        // Setup Quick Actions
        setupQuickActions()
        
        // Setup Clear Logs
        findViewById<TextView>(R.id.clearLogs).setOnClickListener {
            logText.text = "[SYSTEM] Logs cleared"
            appendLog("Ready for operation", "INFO")
        }
        
        // Setup Protocol Tabs
        setupProtocolTabs()

        usbHostManager = UsbHostManager(this, object : UsbHostManager.HotplugListener {
            override fun onDeviceAttached(device: android.hardware.usb.UsbDevice) {
                runOnUiThread {
                    statusText.text = "DEVICE DETECTED"
                    statusSubtext.text = "${device.productName ?: "Unknown"} (VID:${device.vendorId.toString(16).uppercase()})"
                    appendLog("Device attached: ${device.productName}", "SUCCESS")
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
            appendLog("Initiating OTG scan...", "INFO")
            
            // Try common MTK VID/PIDs
            usbHostManager.findAndConnect(0x0E8D, 0x0003) // MTK Preloader
        }
        
        dumpButton.setOnClickListener {
            appendLog("Backup feature initializing...", "WARNING")
            Toast.makeText(this, "Backup feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        flashButton.setOnClickListener {
            appendLog("Flash feature initializing...", "WARNING")
            Toast.makeText(this, "Flash feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        frpButton.setOnClickListener {
            appendLog("FRP Bypass Engine initializing...", "WARNING")
            Toast.makeText(this, "FRP Bypass coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupQuickActions() {
        findViewById<View>(R.id.actionReadInfo).setOnClickListener {
            appendLog("Reading device info...", "INFO")
            Toast.makeText(this, "Read Info - Connect device first", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<View>(R.id.actionUnlockBl).setOnClickListener {
            appendLog("Bootloader unlock requested", "WARNING")
            Toast.makeText(this, "Unlock BL - Connect device first", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<View>(R.id.actionRemovePattern).setOnClickListener {
            appendLog("Pattern/PIN removal requested", "WARNING")
            Toast.makeText(this, "Remove PIN - Connect device first", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<View>(R.id.actionImeiRepair).setOnClickListener {
            appendLog("IMEI repair requested", "WARNING")
            Toast.makeText(this, "IMEI Repair - Connect device first", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<View>(R.id.actionFormat).setOnClickListener {
            appendLog("FORMAT REQUESTED - DANGEROUS!", "ERROR")
            Toast.makeText(this, "⚠️ Format - Dangerous operation!", Toast.LENGTH_LONG).show()
        }
        
        findViewById<View>(R.id.actionReboot).setOnClickListener {
            appendLog("Reboot requested", "INFO")
            Toast.makeText(this, "Reboot - Connect device first", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupProtocolTabs() {
        val tabMtk = findViewById<TextView>(R.id.tabMtk)
        val tabQualcomm = findViewById<TextView>(R.id.tabQualcomm)
        val tabSamsung = findViewById<TextView>(R.id.tabSamsung)
        val tabAdb = findViewById<TextView>(R.id.tabAdb)
        
        val tabs = listOf(tabMtk, tabQualcomm, tabSamsung, tabAdb)
        
        fun selectTab(selected: TextView) {
            tabs.forEach { tab ->
                if (tab == selected) {
                    tab.setBackgroundResource(R.drawable.tab_selected)
                    tab.setTextColor(ContextCompat.getColor(this, R.color.deepeye_obsidian))
                    tab.alpha = 1f
                } else {
                    tab.setBackgroundResource(R.drawable.tab_unselected)
                    tab.setTextColor(ContextCompat.getColor(this, R.color.deepeye_white))
                    tab.alpha = 0.7f
                }
            }
        }
        
        tabMtk.setOnClickListener { 
            selectTab(tabMtk)
            appendLog("Protocol: MTK BROM selected", "INFO")
        }
        tabQualcomm.setOnClickListener { 
            selectTab(tabQualcomm)
            appendLog("Protocol: Qualcomm EDL selected", "INFO")
        }
        tabSamsung.setOnClickListener { 
            selectTab(tabSamsung)
            appendLog("Protocol: Samsung Odin selected", "INFO")
        }
        tabAdb.setOnClickListener { 
            selectTab(tabAdb)
            appendLog("Protocol: ADB/Fastboot selected", "INFO")
        }
    }

    private fun initializeCore(fd: Int) {
        appendLog("Initializing native core with FD: $fd", "INFO")
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
        appendLog("DEVICE SECURED - Connection established!", "SUCCESS")
        
        statusText.text = "DEVICE SECURED"
        statusSubtext.text = "Protocol: MTK BROM | Chipset: MT6765"
        statusText.setTextColor(ContextCompat.getColor(this, R.color.deepeye_white))
        
        connectionStatus.text = "ONLINE"
        connectionStatus.setTextColor(ContextCompat.getColor(this, R.color.deepeye_success))
        connectionGlow.setBackgroundResource(R.drawable.glow_indicator)
        
        globalProgress.isIndeterminate = false
        globalProgress.progress = 100
        
        // Show device info card
        val deviceInfoCard = findViewById<View>(R.id.deviceInfoCard)
        deviceInfoCard.visibility = View.VISIBLE
        
        findViewById<TextView>(R.id.deviceModel).text = "Redmi Note 9"
        findViewById<TextView>(R.id.deviceChipset).text = "MT6765 (Helio G80)"
        findViewById<TextView>(R.id.deviceProtocol).text = "MTK BROM"
        findViewById<TextView>(R.id.deviceSecureBoot).text = "DISABLED"

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
        appendLog("Loaded ${partitions.size} partitions", "SUCCESS")
        
        // Enable buttons
        dumpButton.isEnabled = true
        flashButton.isEnabled = true
        frpButton.isEnabled = true
    }
    
    private fun onConnectionFailed(title: String, subtitle: String) {
        appendLog("$title: $subtitle", "ERROR")
        
        statusText.text = title
        statusSubtext.text = subtitle
        statusText.setTextColor(ContextCompat.getColor(this, R.color.deepeye_error))
        globalProgress.isIndeterminate = false
        globalProgress.progress = 0
        connectionStatus.text = "ERROR"
        connectionStatus.setTextColor(ContextCompat.getColor(this, R.color.deepeye_error))
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
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
