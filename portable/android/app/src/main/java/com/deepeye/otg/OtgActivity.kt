package com.deepeye.otg

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONObject
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import android.os.Build

class OtgActivity : AppCompatActivity() {
    
    // UI Elements
    private lateinit var btnSelectModel: Button
    private lateinit var logOverlay: View
    private lateinit var terminalText: TextView
    private lateinit var logScrollView: ScrollView
    private lateinit var btnCloseLog: View
    private lateinit var connectionIndicator: TextView
    private lateinit var btnRemote: Button
    private lateinit var mtpBanner: View
    private lateinit var mtpBannerDismiss: View
    private lateinit var mtpBannerBody: TextView
    
    // Logic Variables
    private lateinit var usbHostManager: UsbHostManager
    private var selectedBrand = "Xiaomi"
    private var selectedModelName = "Auto-Detect"
    private var nativeHandle: Long = 0
    private var deviceDatabase: MutableMap<String, List<DeviceModel>> = mutableMapOf()
    private var allModels: List<DeviceModel> = emptyList()
    
    // Connection State Machine (FIX FOR "NATIVE CORE OFFLINE")
    @Volatile private var connectionState: ConnectionState = ConnectionState.DISCONNECTED
    private val stateLock = Any()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup Global Crash Handler to show Dialog instead of closing
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runOnUiThread {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("App Crash")
                    .setMessage(throwable.stackTraceToString())
                    .setPositiveButton("OK") { _, _ -> finish() }
                    .show()
            }
        }

        try {
            setContentView(R.layout.activity_otg)
            
            // Bind UI
            btnSelectModel = findViewById(R.id.btnSelectModel)
            logOverlay = findViewById(R.id.logOverlay)
            terminalText = findViewById(R.id.terminalText)
            logScrollView = findViewById(R.id.logScrollView)
            btnCloseLog = findViewById(R.id.btnCloseLog)
            connectionIndicator = findViewById(R.id.connectionIndicator)
            btnRemote = findViewById(R.id.btnRemoteUnlock)
            mtpBanner = findViewById(R.id.mtpBanner)
            mtpBannerDismiss = findViewById(R.id.mtpBannerDismiss)
            mtpBannerBody = findViewById(R.id.mtpBannerBody)
    
            // Setup Listeners
            btnSelectModel.setOnClickListener { showModelSelectionDialog() }
            btnCloseLog.setOnClickListener { logOverlay.visibility = View.GONE }
            
            btnRemote.setOnClickListener {
                hapticFeedback()
                val intent = android.content.Intent(this, RemoteShareActivity::class.java)
                startActivity(intent)
            }
            mtpBannerDismiss.setOnClickListener { mtpBanner.visibility = View.GONE }
            
            // Load Data & Setup
            loadDeviceDatabase()
            setupBrandTabs()
            setupOperationButtons()
            
            // USB Manager
            usbHostManager = UsbHostManager(this, object : UsbHostManager.HotplugListener {
                override fun onDeviceAttached(device: android.hardware.usb.UsbDevice) {
                    runOnUiThread {
                        updateConnectionState(ConnectionState.DEVICE_FOUND, "Device detected: ${device.productName} (${device.vendorId}:${device.productId})")
                    }
                }

                override fun onDeviceReady(fd: Int, vid: Int, pid: Int, protocol: DetectedProtocol, ifaceDump: String) {
                    runOnUiThread {
                        if (protocol == DetectedProtocol.UNKNOWN) {
                            // Show Smart Mode Guidance
                            val currentModel = allModels.find { it.name == selectedModelName && it.brand == selectedBrand }
                            val soc = currentModel?.chipset ?: "Generic"
                            
                            val guidance = ModeHelper.getGuidance(selectedBrand, selectedModelName, soc)
                            
                            updateConnectionState(ConnectionState.ERROR, "Wrong Mode: Expected ${guidance.requiredMode}")
                            
                            // Show detailed dialog
                            androidx.appcompat.app.AlertDialog.Builder(this@OtgActivity)
                                .setTitle("Wrong USB Mode")
                                .setMessage(buildString {
                                    append("Device connected in UNKNOWN mode (likely MTP/Charging).\n\n")
                                    append("Required Mode: ${guidance.requiredMode}\n")
                                    append("Chipset: $soc\n\n")
                                    append("Instructions:\n")
                                    guidance.steps.forEach { append("$it\n") }
                                    
                                    guidance.alternativeSteps?.let {
                                        append("\nAlternatives:\n")
                                        it.forEach { alt -> append("- $alt\n") }
                                    }
                                    
                                    guidance.safetyNotes?.let {
                                        append("\nNOTE: ${it.joinToString("\n")}")
                                    }
                                })
                                .setPositiveButton("I Understand") { d, _ -> d.dismiss() }
                                .setCancelable(false)
                                .show()
                                
                            log("Device Connected but protocol UNKNOWN. Is it MTP/Charging?", "ERROR")
                            log("Guidance shown for $selectedBrand $selectedModelName ($soc)", "INFO")
                            log("[PROTO] Dump:\n$ifaceDump", "DEBUG")
                        } else if (protocol == DetectedProtocol.MTP_ONLY) {
                            updateConnectionState(ConnectionState.CONNECTED_MTP_ONLY, "MTP-only interface detected. Switch USB mode for DeepEye operations.")
                            showMtpBanner()
                            log("[PROTO] Classified as MTP_ONLY. Interfaces:\n$ifaceDump", "WARNING")
                        } else {
                            updateConnectionState(ConnectionState.CONNECTED_PROTOCOL_DETECT, "Mode: $protocol (FD=$fd)")
                            initializeCore(fd, vid, pid, protocol)
                        }
                    }
                }

                override fun onDeviceError(message: String) {
                    runOnUiThread {
                        val next = if (message.contains("detached", ignoreCase = true)) ConnectionState.DISCONNECTED else ConnectionState.ERROR
                        updateConnectionState(next, "USB Error: $message")
                    }
                }

                override fun onStatusUpdate(message: String) {
                    runOnUiThread {
                        log(message, "INFO")
                    }
                }

                override fun onPermissionStateChanged(state: UsbPermissionManager.PermissionState, message: String) {
                    runOnUiThread {
                        when (state) {
                            UsbPermissionManager.PermissionState.NONE -> {
                                // No action needed
                            }
                            UsbPermissionManager.PermissionState.REQUESTING -> {
                                updateConnectionState(ConnectionState.PERMISSION_PENDING, message)
                            }
                            UsbPermissionManager.PermissionState.GRANTED -> {
                                // Permission granted - will trigger onDeviceReady next
                                log("[PERM] $message", "SUCCESS")
                            }
                            UsbPermissionManager.PermissionState.DENIED -> {
                                updateConnectionState(ConnectionState.PERMISSION_DENIED, message)
                            }
                        }
                    }
                }
            })
            
            log("DeepEye Unlocker v5.2.4 Ready - ${allModels.size} models loaded.", "SUCCESS")
            
        } catch (e: Exception) {
            Toast.makeText(this, "Init Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    // --- HELPER FUNCTIONS ---

    private fun hapticFeedback() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }
    
    private fun log(message: String, type: String = "INFO") {
        val colorHex = when(type) {
            "ERROR" -> "#FF5252"
            "SUCCESS" -> "#00E676"
            "WARNING" -> "#FFB300"
            else -> "#00F2FF"
        }
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formattedMsg = "<font color='#888888'>$timestamp</font> <font color='$colorHex'>$message</font><br/>"
        
        runOnUiThread {
            if (logOverlay.visibility != View.VISIBLE) logOverlay.visibility = View.VISIBLE
            terminalText.append(android.text.Html.fromHtml(formattedMsg, android.text.Html.FROM_HTML_MODE_LEGACY))
            logScrollView.post { logScrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun showMtpBanner() {
        mtpBannerBody.text = buildString {
            append("USB mode not ready for DeepEye.\n")
            append("On the attached phone:\n")
            append("• Open Quick Settings → USB → choose File transfer/MTP.\n")
            append("• Enable USB debugging in Developer Options.\n")
            append("• For some brands (e.g., Xiaomi), enable ‘USB debugging (security)’.\n")
        }
        mtpBanner.visibility = View.VISIBLE
    }

    private fun showModelSelectionDialog() {
        val brands = deviceDatabase.keys.toList().sorted()
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Select Brand")
        builder.setItems(brands.toTypedArray()) { _, which ->
            val brand = brands[which]
            showModelListDialog(brand)
        }
        builder.show()
    }
    
    private fun showModelListDialog(brand: String) {
        val models = deviceDatabase[brand] ?: emptyList()
        val modelNames = models.map { "${it.name} (${it.chipset})" }.toTypedArray()
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("$brand Models")
        builder.setItems(modelNames) { _, which ->
            val model = models[which]
            selectedBrand = brand
            selectedModelName = model.name
            btnSelectModel.text = "📱 ${model.name}"
            log("Selected: ${model.name} (${model.chipset})", "INFO")
        }
        builder.show()
    }

    private fun setupBrandTabs() {
        // Simple click listener for horizontal scroll tabs
        val brandMap = mapOf(
            R.id.brandXiaomi to "Xiaomi",
            R.id.brandSamsung to "Samsung",
            R.id.brandHuawei to "Huawei",
            R.id.brandOppo to "OPPO",
            R.id.brandVivo to "Vivo",
            R.id.brandRealme to "Realme",
            R.id.brandOnePlus to "OnePlus"
        )
        
        brandMap.forEach { (viewId, brand) ->
            findViewById<TextView>(viewId)?.setOnClickListener { view ->
                hapticFeedback()
                selectedBrand = brand
                showModelListDialog(brand) // Direct shortcut
                
                // Update Visuals
                brandMap.keys.forEach { id -> findViewById<TextView>(id)?.setBackgroundResource(R.drawable.brand_tab_unselected) }
                (view as TextView).setBackgroundResource(R.drawable.brand_tab_selected)
            }
        }
    }

    private fun setupOperationButtons() {
        val operations = mapOf(
            R.id.btnUnlockBl to "Unlock Bootloader",
            R.id.btnRelockBl to "Relock Bootloader",
            R.id.btnEraseFrp to "Erase FRP",
            R.id.btnFactoryReset to "Factory Reset",
            R.id.btnRemovePin to "Remove Screen Lock",
            R.id.btnRemoveMiAccount to "Remove MI Account",
            R.id.btnBypassAuth to "Bypass Auth",
            R.id.btnReadInfo to "Read Device Info",
            R.id.btnReadImei to "Read IMEI"
        )
        
        operations.forEach { (viewId, opName) ->
            findViewById<Button>(viewId)?.setOnClickListener {
                hapticFeedback()
                
                synchronized(stateLock) {
                    if (!connectionState.canExecuteOperations()) {
                        val stateMsg = when (connectionState) {
                            ConnectionState.DISCONNECTED -> "No device connected. Plug in OTG cable."
                            ConnectionState.DEVICE_FOUND -> "Waiting for USB permission..."
                            ConnectionState.PERMISSION_PENDING -> "Permission pending, please approve."
                            ConnectionState.PERMISSION_DENIED -> "USB permission denied. Re-plug and approve."
                            ConnectionState.CONNECTED_MTP_ONLY -> "Device is in MTP-only mode. Switch USB mode to allow operations."
                            ConnectionState.USB_OPEN, ConnectionState.NATIVE_INITIALIZING -> "Core is initializing, wait..."
                            ConnectionState.ERROR -> "Connection error. Try re-plugging device."
                            else -> "Native Core Offline (State: $connectionState)"
                        }
                        log("Cannot execute: $stateMsg", "ERROR")
                        Toast.makeText(this, stateMsg, Toast.LENGTH_SHORT).show()
                    } else {
                        log("[OTG-OP] Executing: $opName...", "INFO")
                        executeOperation(opName)
                    }
                }
            }
        }
    }

    private fun initializeCore(fd: Int, vid: Int, pid: Int, protocol: DetectedProtocol) {
        updateConnectionState(ConnectionState.NATIVE_INITIALIZING, "[OTG-NATIVE] initCore(fd=$fd, $vid:$pid, proto=$protocol)...")
        
        Thread {
            try {
                // Future: Pass protocolId to native init if needed
                log("Native Bridge Init: FD=$fd, VID=$vid, PID=$pid", "DEBUG")
                nativeHandle = NativeBridge.initCore(fd, vid, pid)
                
                if (nativeHandle != 0L) {
                    runOnUiThread {
                        log("Native Handshake: Identifying device...", "INFO")
                    }
                    
                    // Attempt device identification handshake
                    val identified = try {
                        NativeBridge.identifyDevice(nativeHandle)
                    } catch (e: Exception) {
                        runOnUiThread {
                            log("[OTG-NATIVE] identifyDevice threw: ${e.message}","ERROR")
                        }
                        false
                    }
                    
                    runOnUiThread {
                        if (identified) {
                            updateConnectionState(ConnectionState.CONNECTED, "Connected: Handshake OK ($protocol)")
                        } else {
                            NativeBridge.closeCore(nativeHandle)
                            nativeHandle = 0L
                            updateConnectionState(ConnectionState.ERROR, "Handshake failed. Device rejected protocol $protocol.")
                        }
                    }
                } else {
                    runOnUiThread {
                        updateConnectionState(ConnectionState.ERROR, "Native Init Failed (Handle=0). USB Config Error?")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    updateConnectionState(ConnectionState.ERROR, "Native Exception: ${e.message}")
                    e.printStackTrace()
                }
            }
        }.start()
    }
    
    private fun updateConnectionState(newState: ConnectionState, logMessage: String) {
        synchronized(stateLock) {
            val oldState = connectionState
            connectionState = newState
            
            // Update UI
            connectionIndicator.text = newState.getBadgeText()
            connectionIndicator.setTextColor(ContextCompat.getColor(this, newState.getBadgeColorRes()))
            
            // Log
            val logType = when (newState) {
                ConnectionState.CONNECTED -> "SUCCESS"
                ConnectionState.CONNECTED_MTP_ONLY -> "WARNING"
                ConnectionState.ERROR -> "ERROR"
                ConnectionState.DISCONNECTED -> "WARNING"
                ConnectionState.PERMISSION_DENIED -> "ERROR"
                else -> "INFO"
            }
            log("[STATE] $oldState → $newState: $logMessage", logType)
        }
    }
    
    private fun executeOperation(opName: String) {
        // Placeholder for actual native operation calls
        log("[EXEC] $opName - Native call would happen here with handle=$nativeHandle", "INFO")
        Toast.makeText(this, "Operation: $opName (not yet implemented)", Toast.LENGTH_SHORT).show()
    }
    
    private fun loadDeviceDatabase() {
        try {
            val jsonString = assets.open("models.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            deviceDatabase.clear()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val brand = obj.getString("brand")
                val model = DeviceModel(obj.getString("name"), obj.getString("chipset"), brand)
                if (!deviceDatabase.containsKey(brand)) deviceDatabase[brand] = mutableListOf()
                (deviceDatabase[brand] as MutableList).add(model)
            }
            allModels = deviceDatabase.values.flatten()
        } catch (e: Exception) {
            log("DB Load Error: ${e.message}", "ERROR")
        }
    }

    override fun onDestroy() {
        if (nativeHandle != 0L) NativeBridge.closeCore(nativeHandle)
        usbHostManager.unregister()
        super.onDestroy()
    }
}
