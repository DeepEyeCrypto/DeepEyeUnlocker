package com.deepeye.otg.ui

import android.content.Context
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val sessionManager by lazy { UsbSessionManager(this) }
    private val usbReceiver by lazy { UsbBroadcastReceiver(sessionManager) }

    // Log State for Compose
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    // ── Engine loading state ────────────────────────────────────
    private val _engineLoaded = MutableStateFlow(false)

    data class LogEntry(val message: String, val type: String, val timestamp: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // ┌──────────────────────────────────────────────────────┐
        // │  RULE: onCreate MUST return in < 100ms               │
        // │  setContent is the ONLY heavy thing here.            │
        // │  Everything else → IO thread AFTER UI is up.         │
        // └──────────────────────────────────────────────────────┘

        // 1. Show UI immediately — loading screen if engine not ready
        setContent {
            val engineReady by _engineLoaded.collectAsState()

            if (!engineReady) {
                // Show loading screen while native lib loads
                DeepEyeLoadingScreen()
            } else {
                val session by controller.state.collectAsState()
                val queueSession by sessionManager.state.collectAsState()
                val logList by logs.collectAsState()

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
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

                    QueueWaitOverlay(
                        session = queueSession,
                        onCancel = { sessionManager.cancelQueue() },
                        onDismiss = { sessionManager.reset() },
                        onRetry = {
                            val op = (queueSession as? SessionState.PermissionDenied)?.queuedOp
                                ?: (queueSession as? SessionState.Error)?.queuedOp
                            if (op != null) sessionManager.queueOperation(op)
                            else sessionManager.reset()
                        }
                    )
                }
            }
        }

        // 2. Register receivers/controllers after first frame setup
        registerUsbReceiver()
        controller.register(scanExistingDevices = false)
        lifecycleScope.launch {
            controller.state.collect { session ->
                updateUiFromSession(session)
                latestSession = session
            }
        }

        // 3. Deferred init — runs on IO thread AFTER setContent returns
        lifecycleScope.launch(Dispatchers.IO) {
            // Wait for native lib (Application already started loading it)
            NativeBridge.loadAsync()

            // USB manager warmup (can be slow on first call)
            sessionManager.initAsync()

            // Existing attached-device bootstrap off main thread
            controller.bootstrapAttachedDevicesAsync()

            // Init license system (lightweight, but off main thread to be safe)
            withContext(Dispatchers.Main) {
                LicenseManager.init(this@OtgActivity)
            }

            // Load device database from assets
            loadDeviceDatabase()

            // Finish lightweight startup state
            withContext(Dispatchers.Main) {
                // Observe license role changes
                lifecycleScope.launch {
                    LicenseManager.role.collect { role ->
                        log("[AUTH] Role: ${role.label} (level=${role.level})", "INFO")
                    }
                }

                log("DeepEye Unlocker v${com.deepeye.otg.BuildConfig.VERSION_NAME} Ready - ${allModels.size} models loaded. [${LicenseManager.currentRole.label}]", "SUCCESS")

                // ── Signal engine ready — switches from loading screen to main UI
                _engineLoaded.value = true
            }
        }
    }

    private fun registerUsbReceiver() {
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
    }

    // --- HELPER FUNCTIONS ---

    private fun hapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
    
    private fun log(message: String, type: String = "INFO") {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _logs.value = (_logs.value + LogEntry(message, type, timestamp)).takeLast(500)
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
        if (!NativeBridge.isLoaded()) {
            log("[OTG-NATIVE] Cannot init — native lib not loaded yet", "ERROR")
            return
        }

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

    private fun loadDeviceDatabase() {
        try {
            val jsonString = assets.open("models.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            val models = (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                DeviceModel(obj.getString("name"), obj.getString("chipset"), obj.getString("brand"))
            }
            deviceDatabase = models.groupBy { it.brand }.mapValues { it.value.toMutableList() }.toMutableMap()
            allModels = models
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
        if (nativeHandle != 0L && NativeBridge.isLoaded()) NativeBridge.closeCore(nativeHandle)
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

}
