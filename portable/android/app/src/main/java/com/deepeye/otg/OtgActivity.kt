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

            // Hidden USB Diagnostic: long-press the connection indicator
            connectionIndicator.setOnLongClickListener {
                hapticFeedback()
                runUsbDiagnostic()
                true
            }
            
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
                        when (protocol) {
                            DetectedProtocol.UNKNOWN -> {
                                // Show Smart Mode Guidance
                                val currentModel = allModels.find { it.name == selectedModelName && it.brand == selectedBrand }
                                val soc = currentModel?.chipset ?: "Generic"
                                
                                val guidance = ModeHelper.getGuidance(selectedBrand, selectedModelName, soc)
                                
                                updateConnectionState(ConnectionState.ERROR, "Wrong Mode: Expected ${guidance.requiredMode}")
                                
                                // Show detailed dialog with interface dump for debugging
                                androidx.appcompat.app.AlertDialog.Builder(this@OtgActivity)
                                    .setTitle("Wrong USB Mode")
                                    .setMessage(buildString {
                                        append("Device connected but protocol could not be identified.\n")
                                        append("The phone may be in Charging-only or MTP mode.\n\n")
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
                                    
                                log("[PROTO] Protocol UNKNOWN. Device may need USB mode switch.", "ERROR")
                                log("[UX] Guidance shown for $selectedBrand $selectedModelName ($soc)", "INFO")
                                log("[PROTO] Interface dump:\n$ifaceDump", "WARNING")
                            }

                            DetectedProtocol.MTP_ONLY -> {
                                updateConnectionState(ConnectionState.CONNECTED_MTP_ONLY, "MTP-only interface detected. Switch USB mode for DeepEye operations.")
                                showMtpBanner()
                                log("[PROTO] Classified as MTP_ONLY. All interfaces are Still-Image/MSC class.", "WARNING")
                                log("[PROTO] Interface dump:\n$ifaceDump", "WARNING")
                            }

                            DetectedProtocol.ADB -> {
                                // ADB mode: connected but not in bootloader/diag mode needed for most operations
                                updateConnectionState(ConnectionState.CONNECTED_PROTOCOL_DETECT, "Mode: ADB (FD=$fd)")
                                log("[PROTO] Device is in ADB mode. Some operations require EDL/BROM/Download mode.", "WARNING")
                                initializeCore(fd, vid, pid, protocol)
                            }

                            else -> {
                                // Known protocol (EDL, BROM, Fastboot, Samsung Odin, MTK Preloader)
                                updateConnectionState(ConnectionState.CONNECTED_PROTOCOL_DETECT, "Mode: $protocol (FD=$fd)")
                                log("[PROTO] Matched: $protocol", "SUCCESS")
                                initializeCore(fd, vid, pid, protocol)
                            }
                        }
                    }
                }

                override fun onDeviceError(message: String) {
                    runOnUiThread {
                        val next = when {
                            message.contains("detached", ignoreCase = true) -> ConnectionState.DISCONNECTED
                            message.contains("re-enumerat", ignoreCase = true) ||
                            message.contains("System revoked", ignoreCase = true) -> {
                                // Don't spam ERROR on re-enumeration; the manager will re-request permission
                                log("[USB] $message", "WARNING")
                                return@runOnUiThread
                            }
                            else -> ConnectionState.ERROR
                        }
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
            
            log("DeepEye Unlocker v5.5.0 Ready - ${allModels.size} models loaded.", "SUCCESS")
            
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
            append("The attached phone is in MTP/File Transfer mode.\n")
            append("DeepEye needs a special boot mode to perform operations.\n\n")
            append("On the attached phone:\n")
            append("☐ Open Quick Settings → USB → choose 'File Transfer' or 'MTP'\n")
            append("☐ Enable USB debugging in Developer Options\n")
            append("☐ For Xiaomi: also enable 'USB debugging (security settings)'\n")
            append("☐ For Samsung: enable 'OEM unlock' in Developer Options\n\n")
            append("Then re-plug the cable or switch the phone to the required boot mode\n")
            append("(EDL / BROM / Download Mode) depending on your operation.")
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

            // Validate transition to prevent illegal state jumps (e.g. CONNECTED → PERMISSION_PENDING)
            if (!oldState.canTransitionTo(newState)) {
                Log.w("DeepEye-OTG", "[STATE] BLOCKED illegal transition: $oldState → $newState ($logMessage)")
                // Allow the transition to ERROR or DISCONNECTED anyway (universal escapes)
                if (newState != ConnectionState.ERROR && newState != ConnectionState.DISCONNECTED) {
                    log("[STATE] Transition blocked: $oldState → $newState (invalid)", "WARNING")
                    return
                }
            }

            connectionState = newState
            
            // Update UI
            connectionIndicator.text = newState.getBadgeText()
            connectionIndicator.setTextColor(ContextCompat.getColor(this, newState.getBadgeColorRes()))

            // Hide MTP banner on disconnect or new connection
            if (newState == ConnectionState.DISCONNECTED || newState == ConnectionState.DEVICE_FOUND) {
                mtpBanner.visibility = View.GONE
            }
            
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

    /**
     * USB Diagnostic self-test: enumerates all connected devices,
     * shows protocol classification and permission status for each.
     * Useful for debugging permission/detection issues on any Android version.
     */
    private fun runUsbDiagnostic() {
        val usbManager = getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
        val devices = usbManager.deviceList
        val sb = StringBuilder()
        sb.appendLine("=== DeepEye USB Diagnostic ===")
        sb.appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
        sb.appendLine("Android SDK: ${Build.VERSION.SDK_INT}")
        sb.appendLine("Devices found: ${devices.size}")
        sb.appendLine()

        if (devices.isEmpty()) {
            sb.appendLine("No USB devices detected.")
            sb.appendLine("• Ensure OTG adapter is connected.")
            sb.appendLine("• Check that 'OTG' is enabled in phone settings (some brands require this).")
        }

        for ((name, device) in devices) {
            sb.appendLine("─── Device: $name ───")
            sb.appendLine("  VID: 0x${"%04X".format(device.vendorId)}, PID: 0x${"%04X".format(device.productId)}")
            sb.appendLine("  Name: ${device.productName ?: "N/A"}")
            sb.appendLine("  DeviceId: ${device.deviceId}")
            sb.appendLine("  hasPermission: ${usbManager.hasPermission(device)}")
            sb.appendLine("  Interfaces: ${device.interfaceCount}")
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                sb.appendLine("    iface[$i]: class=0x${"%02X".format(iface.interfaceClass)}, sub=0x${"%02X".format(iface.interfaceSubclass)}, proto=0x${"%02X".format(iface.interfaceProtocol)}, eps=${iface.endpointCount}")
            }
            sb.appendLine()
        }

        sb.appendLine("Current connection state: $connectionState")
        log(sb.toString(), "INFO")

        // Also show in a dialog for easy reading
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("USB Diagnostic")
            .setMessage(sb.toString())
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .setNeutralButton("Copy") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("USB Diagnostic", sb.toString()))
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .show()
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
