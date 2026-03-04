package com.deepeye.otg.ui

import android.content.Context
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.deepeye.otg.ConnState
import com.deepeye.otg.DeviceModel
import com.deepeye.otg.NativeBridge
import com.deepeye.otg.RemoteShareActivity
import com.deepeye.otg.UsbConnectionController
import com.deepeye.otg.UsbSessionState
import com.deepeye.otg.auth.LicenseManager
import com.deepeye.otg.usb.DeepEyeOperation
import com.deepeye.otg.usb.SessionState
import com.deepeye.otg.usb.UsbBroadcastReceiver
import com.deepeye.otg.usb.UsbSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OtgActivity : AppCompatActivity() {
    
    // Logic Variables
    private var selectedBrand = "Xiaomi"
    private var selectedModelName = "Auto-Detect"
    private var nativeHandle: Long = 0
    private var deviceDatabase: MutableMap<String, List<DeviceModel>> = mutableMapOf()
    private var allModels: List<DeviceModel> = emptyList()

    private val controller by lazy { UsbConnectionController(this, lifecycleScope) }
    private var latestSession: UsbSessionState = UsbSessionState()
    private var lastInitializedDeviceKey: String? = null

    // Queue & Wait session manager
    private lateinit var sessionManager: UsbSessionManager
    private lateinit var usbReceiver: UsbBroadcastReceiver

    // Hidden license dialog: triple-tap counter
    private var tapCount = 0
    private var lastTapTime = 0L

    // Log State for Compose
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    data class LogEntry(val message: String, val type: String, val timestamp: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup Global Crash Handler
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            runOnUiThread {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("App Crash")
                    .setMessage(throwable.stackTraceToString())
                    .setPositiveButton("OK") { _, _ -> finish() }
                    .show()
            }
        }

        try {
            // initialize license/auth system
            LicenseManager.init(this)

            controller.register()
            lifecycleScope.launch {
                controller.state.collect { session ->
                    latestSession = session
                    updateUiFromSession(session)
                }
            }

            // Queue & Wait session manager
            sessionManager = UsbSessionManager(this)
            usbReceiver = UsbBroadcastReceiver(sessionManager)
            val usbFilter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction(UsbSessionManager.ACTION_USB_PERMISSION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(usbReceiver, usbFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(usbReceiver, usbFilter)
            }

            // Load Data
            loadDeviceDatabase()

            // Attach Compose UI Layers
            attachComposeMainUi()
            attachQueueWaitOverlay()

            // Observe license role changes
            lifecycleScope.launch {
                LicenseManager.role.collect { role ->
                    log("[AUTH] Role: ${role.label} (level=${role.level})", "INFO")
                }
            }
            
            log("DeepEye Unlocker v5.6.1 Ready - ${allModels.size} models loaded. [${LicenseManager.currentRole.label}]", "SUCCESS")
            
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
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _logs.value += LogEntry(message, type, timestamp)
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
            log("Selected: ${model.name} (${model.chipset})", "INFO")
        }
        builder.show()
    }

    private fun initializeCore(fd: Int, vid: Int, pid: Int, protocolLabel: String) {
        log("[OTG-NATIVE] initCore(fd=$fd, $vid:$pid, proto=$protocolLabel)...", "INFO")
        
        Thread {
            try {
                nativeHandle = NativeBridge.initCore(fd, vid, pid)
                if (nativeHandle != 0L) {
                    runOnUiThread { log("Native Handshake: Identifying device...", "INFO") }
                    val identified = try {
                        NativeBridge.identifyDevice(nativeHandle)
                    } catch (e: Exception) {
                        runOnUiThread { log("[OTG-NATIVE] identifyDevice threw: ${e.message}","ERROR") }
                        false
                    }
                    
                    runOnUiThread {
                        if (identified) {
                            log("Connected: Handshake OK ($protocolLabel)", "SUCCESS")
                        } else {
                            NativeBridge.closeCore(nativeHandle)
                            nativeHandle = 0L
                            log("Handshake failed. Device rejected protocol $protocolLabel.", "ERROR")
                        }
                    }
                } else {
                    runOnUiThread { log("Native Init Failed (Handle=0). USB Config Error?", "ERROR") }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    log("Native Exception: ${e.message}", "ERROR")
                    e.printStackTrace()
                }
            }
        }.start()
    }

    private fun runUsbDiagnostic() {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = usbManager.deviceList
        val sb = StringBuilder()
        sb.appendLine("=== DeepEye USB Diagnostic ===")
        sb.appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
        sb.appendLine("Android SDK: ${Build.VERSION.SDK_INT}")
        sb.appendLine("Devices found: ${devices.size}")
        sb.appendLine()

        if (devices.isEmpty()) {
            sb.appendLine("No USB devices detected.")
        }

        for ((name, device) in devices) {
            sb.appendLine("─── Device: $name ───")
            sb.appendLine("  VID: 0x${"%04X".format(device.vendorId)}, PID: 0x${"%04X".format(device.productId)}")
            sb.appendLine("  Name: ${device.productName ?: "N/A"}")
            sb.appendLine("  hasPermission: ${usbManager.hasPermission(device)}")
            sb.appendLine()
        }

        log(sb.toString(), "INFO")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("USB Diagnostic")
            .setMessage(sb.toString())
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
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

    private fun showLicenseDialog() {
        val current = LicenseManager.currentRole
        val input = EditText(this).apply {
            hint = "Enter license token..."
            setPadding(48, 24, 48, 24)
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🔑 License Manager")
            .setView(input)
            .setPositiveButton("Activate") { _, _ ->
                val token = input.text.toString().trim()
                if (token.isNotEmpty()) {
                    lifecycleScope.launch {
                        val result = LicenseManager.activateFromBackend(this@OtgActivity, token)
                        if (result.isSuccess) {
                            log("[AUTH] License activated", "SUCCESS")
                        } else {
                            log("[AUTH] Activation failed", "ERROR")
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)

        if (current == com.deepeye.otg.policy.UserRole.DEV || !LicenseManager.isLicensed) {
            builder.setNeutralButton("Dev Mode") { _, _ ->
                LicenseManager.setRole(com.deepeye.otg.policy.UserRole.DEV)
                log("[AUTH] Dev mode activated", "SUCCESS")
            }
        }
        builder.show()
    }

    override fun onDestroy() {
        if (nativeHandle != 0L) NativeBridge.closeCore(nativeHandle)
        controller.unregister()
        try { unregisterReceiver(usbReceiver) } catch (_: Exception) {}
        sessionManager.destroy()
        super.onDestroy()
    }

    private fun updateUiFromSession(session: UsbSessionState) {
        log("[STATE] ${latestSession.state} → ${session.state}", "INFO")
        when (session.state) {
            ConnState.CONNECTED_READY -> {
                val fd = session.connectionFd ?: -1
                val vid = session.vid ?: 0
                val pid = session.pid ?: 0
                val deviceKey = session.deviceKey
                if (deviceKey != null && deviceKey != lastInitializedDeviceKey && fd > 0) {
                    lastInitializedDeviceKey = deviceKey
                    initializeCore(fd, vid, pid, session.protocol.name)
                }
            }
            ConnState.ERROR -> {
                session.lastError?.let { log("[ERROR] $it", "ERROR") }
            }
            else -> {}
        }
    }

    private fun attachComposeMainUi() {
        val root = findViewById<FrameLayout>(android.R.id.content)
        val compose = ComposeView(this).apply {
            setContent {
                val session by controller.state.collectAsState()
                val queueSession by sessionManager.state.collectAsState()
                val logList by logs.collectAsState()
                MainScreen(
                    sessionState = session,
                    queueState = queueSession,
                    logs = logList,
                    onSelectModel = { showModelSelectionDialog() },
                    onRemoteUnlock = {
                        hapticFeedback()
                        val intent = android.content.Intent(this@OtgActivity, RemoteShareActivity::class.java)
                        startActivity(intent)
                    },
                    onOperationSelected = { op ->
                        hapticFeedback()
                        log("[QUEUE] ${op.label} queued", "INFO")
                        sessionManager.queueOperation(op)
                    }
                )
            }
        }
        root.addView(compose, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    private fun attachQueueWaitOverlay() {
        val root = findViewById<FrameLayout>(android.R.id.content)
        val overlay = ComposeView(this).apply {
            setContent {
                val session by sessionManager.state.collectAsState()
                QueueWaitOverlay(
                    session = session,
                    onCancel = { sessionManager.cancelQueue() },
                    onDismiss = { sessionManager.reset() },
                    onRetry = {
                        val op = (session as? SessionState.PermissionDenied)?.queuedOp
                            ?: (session as? SessionState.Error)?.queuedOp
                        if (op != null) sessionManager.queueOperation(op)
                        else sessionManager.reset()
                    }
                )
            }
        }
        root.addView(overlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }
}
