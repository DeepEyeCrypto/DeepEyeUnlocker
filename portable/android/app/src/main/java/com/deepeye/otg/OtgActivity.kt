package com.deepeye.otg

import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import android.hardware.usb.UsbManager
import android.view.Gravity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.deepeye.otg.ui.ExecutingOperationOverlay
import com.deepeye.otg.ui.OperationCompleteBanner
import com.deepeye.otg.ui.ReenumerationWaitBanner
import com.deepeye.otg.ui.WaitingForDeviceScreen
import com.deepeye.otg.auth.LicenseManager
import com.deepeye.otg.usb.DeepEyeOperation
import com.deepeye.otg.usb.SessionState
import com.deepeye.otg.usb.UsbBroadcastReceiver
import com.deepeye.otg.usb.UsbSessionManager
import kotlinx.coroutines.launch

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

            // Hidden License Dialog: triple-tap the connection indicator
            connectionIndicator.setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastTapTime > 800) tapCount = 0
                tapCount++
                lastTapTime = now
                if (tapCount >= 3) {
                    tapCount = 0
                    hapticFeedback()
                    showLicenseDialog()
                }
            }
            
            // Load Data & Setup
            loadDeviceDatabase()
            setupBrandTabs()
            setupOperationButtons()
            
            controller.register()
            lifecycleScope.launch {
                controller.state.collect { session ->
                    latestSession = session
                    updateUiFromSession(session)
                }
            }

            // Initialize license/auth system
            LicenseManager.init(this)

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

            attachComposeSessionPanel()
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

    // Maps layout button IDs to DeepEyeOperation for Queue & Wait
    private val opButtonMap: Map<Int, DeepEyeOperation> by lazy {
        mapOf(
            // Category A — Flashing & Firmware
            R.id.btnWriteFirmware  to DeepEyeOperation.WRITE_FIRMWARE,
            R.id.btnReadFirmware   to DeepEyeOperation.READ_FIRMWARE,
            R.id.btnBackupEfs      to DeepEyeOperation.BACKUP_EFS,
            R.id.btnRestoreEfs     to DeepEyeOperation.RESTORE_EFS,
            R.id.btnPartitionMgr   to DeepEyeOperation.PARTITION_MANAGER,

            // Category B — Reset & Cleanup
            R.id.btnFactoryReset   to DeepEyeOperation.FACTORY_RESET,
            R.id.btnDemoUnlock     to DeepEyeOperation.DEMO_UNLOCK,
            R.id.btnSafeWipe       to DeepEyeOperation.SAFE_WIPE,

            // Category C — FRP & Account
            R.id.btnEraseFrp       to DeepEyeOperation.ERASE_FRP,
            R.id.btnRemoveMiAccount to DeepEyeOperation.REMOVE_MI_CLOUD,
            R.id.btnEfrpMdm        to DeepEyeOperation.EFRP_MDM_HOOK,
            R.id.btnMtkMetaFrp     to DeepEyeOperation.MTK_METAMODE_FRP,

            // Category D — Locks & Security
            R.id.btnRemovePin      to DeepEyeOperation.REMOVE_SCREEN_LOCK,
            R.id.btnLockState      to DeepEyeOperation.LOCK_STATE_ANALYSIS,
            R.id.btnUnlockBl       to DeepEyeOperation.UNLOCK_BOOTLOADER,
            R.id.btnMdmRemove      to DeepEyeOperation.MDM_REMOVE,

            // Category E — IMEI & Network
            R.id.btnReadImei       to DeepEyeOperation.IMEI_CHECK,
            R.id.btnImeiRestore    to DeepEyeOperation.IMEI_RESTORE,
            R.id.btnModemRepair    to DeepEyeOperation.MODEM_REPAIR,
            R.id.btnNetworkUnlock  to DeepEyeOperation.NETWORK_UNLOCK,

            // Category F — Advanced & Diagnostics
            R.id.btnReadInfo       to DeepEyeOperation.DEEP_DEVICE_INFO,
            R.id.btnAdbEnable      to DeepEyeOperation.ADB_ENABLE,
            R.id.btnOneClickRoot   to DeepEyeOperation.ONE_CLICK_ROOT,
            R.id.btnAppManager     to DeepEyeOperation.APP_MANAGER,
        )
    }

    private fun setupOperationButtons() {
        opButtonMap.forEach { (viewId, op) ->
            findViewById<Button>(viewId)?.setOnClickListener {
                hapticFeedback()
                log("[QUEUE] ${op.label} queued — plug in device or auto-execute if connected", "INFO")
                sessionManager.queueOperation(op)
            }
        }
    }

    private fun initializeCore(fd: Int, vid: Int, pid: Int, protocolLabel: String) {
        log("[OTG-NATIVE] initCore(fd=$fd, $vid:$pid, proto=$protocolLabel)...", "INFO")
        
        Thread {
            try {
                // Future: Pass protocolId to native init if needed
                log("Native Bridge Init: FD=$fd, VID=$vid, PID=$pid", "DEBUG")
                nativeHandle = NativeBridge.initCore(fd, vid, pid)
                
                if (nativeHandle != 0L) {
                    runOnUiThread { log("Native Handshake: Identifying device...", "INFO") }
                    
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
                            log("Connected: Handshake OK ($protocolLabel)", "SUCCESS")
                        } else {
                            NativeBridge.closeCore(nativeHandle)
                            nativeHandle = 0L
                            log("Handshake failed. Device rejected protocol $protocolLabel.", "ERROR")
                        }
                    }
                } else {
                    runOnUiThread {
                        log("Native Init Failed (Handle=0). USB Config Error?", "ERROR")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    log("Native Exception: ${e.message}", "ERROR")
                    e.printStackTrace()
                }
            }
        }.start()
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

        sb.appendLine("Current connection state: ${latestSession.state}")
        sb.appendLine("DeviceKey: ${latestSession.deviceKey}")
        sb.appendLine("HasPermission: ${latestSession.hasPermission}")
        sb.appendLine("Protocol: ${latestSession.protocol}")
        sb.appendLine("LastError: ${latestSession.lastError}")
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

    /**
     * License activation dialog — shows current auth status,
     * allows token entry or dev quick-set.
     * Hidden feature: triple-tap on DEEPEYE title bar.
     */
    private fun showLicenseDialog() {
        val current = LicenseManager.currentRole
        val isLicensed = LicenseManager.isLicensed
        val expiry = LicenseManager.licenseExpiry
        val expiryStr = if (expiry > 0) {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(expiry))
        } else "N/A"

        val status = buildString {
            appendLine("Current Role: ${current.label} (level ${current.level})")
            appendLine("Licensed: $isLicensed")
            appendLine("Expiry: $expiryStr")
            appendLine("Expired: ${LicenseManager.isExpired}")
            appendLine()
            appendLine("Token format: DEEPEYE-{ROLE}-{EXPIRY_MS}-{SIG}")
            appendLine("Roles: CONSUMER, POWER_USER, TECHNICIAN, ENTERPRISE, DEV")
        }

        val input = EditText(this).apply {
            hint = "Enter license token..."
            setPadding(48, 24, 48, 24)
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF888888.toInt())
            setBackgroundColor(0xFF2C2C30.toInt())
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🔑 License Manager")
            .setMessage(status)
            .setView(input)
            .setPositiveButton("Activate") { _, _ ->
                val token = input.text.toString().trim()
                if (token.isNotEmpty()) {
                    lifecycleScope.launch {
                        val result = LicenseManager.activateFromBackend(this@OtgActivity, token)
                        if (result.isSuccess) {
                            val role = result.getOrNull()
                            log("[AUTH] License activated from backend: ${role?.label}", "SUCCESS")
                            Toast.makeText(this@OtgActivity, "License activated: ${role?.label}", Toast.LENGTH_SHORT).show()
                        } else {
                            val err = result.exceptionOrNull()?.message ?: "Unknown error"
                            log("[AUTH] Backend activation failed: $err", "ERROR")
                            Toast.makeText(this@OtgActivity, "Invalid token or network error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Deactivate") { _, _ ->
                LicenseManager.deactivate()
                log("[AUTH] License deactivated — reverted to Consumer", "WARNING")
                Toast.makeText(this, "Reverted to Consumer", Toast.LENGTH_SHORT).show()
            }

        // Dev quick-set: add extra option
        if (current == com.deepeye.otg.policy.UserRole.DEV || !isLicensed) {
            builder.setNeutralButton("Dev Mode") { _, _ ->
                LicenseManager.setRole(com.deepeye.otg.policy.UserRole.DEV)
                log("[AUTH] Dev mode activated — all tiers unlocked", "SUCCESS")
                Toast.makeText(this, "Dev mode: all tiers unlocked", Toast.LENGTH_SHORT).show()
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

    // --- STATE/UI BINDING ---

    private fun updateUiFromSession(session: UsbSessionState) {
        // Badge
        connectionIndicator.text = "● ${session.state}"
        connectionIndicator.setTextColor(ContextCompat.getColor(this, badgeColor(session.state)))

        // MTP banner
        if (session.state == ConnState.CONNECTED_MTP_ONLY) {
            showMtpBanner()
        } else if (session.state == ConnState.DISCONNECTED || session.state == ConnState.DEVICE_FOUND) {
            mtpBanner.visibility = View.GONE
        }

        // Logging on state transitions
        log("[STATE] ${latestSession.state} → ${session.state}", "INFO")

        when (session.state) {
            ConnState.REENUMERATION_WAIT -> {
                log("[USB] MTK re-enumeration detected — waiting for device to re-attach...", "WARNING")
            }
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
            ConnState.CONNECTED_MTP_ONLY -> {
                log("[UX] Connected in MTP_ONLY; showing guidance banner", "WARNING")
            }
            ConnState.PERMISSION_DENIED -> {
                log("[PERM] USB permission denied", "ERROR")
            }
            ConnState.ERROR -> {
                session.lastError?.let { log("[ERROR] $it", "ERROR") }
            }
            else -> {}
        }
    }

    private fun badgeColor(state: ConnState): Int {
        return when (state) {
            ConnState.DISCONNECTED, ConnState.ERROR -> R.color.deepeye_error
            ConnState.DEVICE_FOUND, ConnState.PERMISSION_PENDING, ConnState.PERMISSION_DENIED -> R.color.deepeye_warning
            ConnState.REENUMERATION_WAIT -> R.color.deepeye_cyan
            ConnState.CONNECTED_PROTOCOL_DETECT -> R.color.deepeye_cyan
            ConnState.CONNECTED_READY -> R.color.deepeye_success
            ConnState.CONNECTED_MTP_ONLY -> R.color.deepeye_warning
        }
    }

    private fun attachComposeSessionPanel() {
        val root = findViewById<FrameLayout>(android.R.id.content)
        val compose = ComposeView(this).apply {
            setContent {
                SessionPanel(sessionFlow = controller.state, onRetry = { controller.retry() })
            }
        }
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.TOP
        root.addView(compose, params)
    }

    /**
     * Full-screen Compose overlay driven by [UsbSessionManager.state].
     * Shows WaitingForDevice, ExecutingOperation, OperationComplete,
     * and ReenumerationWait screens on top of the XML layout.
     */
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
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        root.addView(overlay, params)
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Queue & Wait Compose overlay — driven by SessionState
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun QueueWaitOverlay(
    session: SessionState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    when (session) {
        is SessionState.WaitingForDevice -> {
            WaitingForDeviceScreen(
                queuedOp = session.queuedOp,
                onCancel = onCancel
            )
        }
        is SessionState.ReenumerationWait -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                ReenumerationWaitBanner()
            }
        }
        is SessionState.ExecutingOperation -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                ExecutingOperationOverlay(
                    op = session.op,
                    protocol = session.protocol,
                    progress = session.progress,
                    statusMsg = session.statusMsg
                )
            }
        }
        is SessionState.OperationComplete -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                OperationCompleteBanner(
                    op = session.op,
                    success = session.success,
                    message = session.message,
                    onDismiss = onDismiss
                )
            }
        }
        is SessionState.PermissionDenied -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠\uFE0F", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("USB Permission Denied", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Re-plug the cable and tap Allow", color = Color.LightGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onRetry) { Text("Retry") }
                }
            }
        }
        is SessionState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\u274C", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Error", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(session.message, color = Color.LightGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onRetry) { Text("Retry") }
                }
            }
        }
        // Idle / DeviceFound / PermissionPending / ProtocolDetect / ConnectedReady / MtpOnly
        // → overlay is invisible, main XML layout shows through
        else -> {}
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Legacy connection-indicator panel (reads UsbConnectionController state)
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun SessionPanel(
    sessionFlow: kotlinx.coroutines.flow.StateFlow<UsbSessionState>,
    onRetry: () -> Unit
) {
    val session by sessionFlow.collectAsState()
    Surface(color = Color(0xFF1E1E22)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "USB: ${session.state}",
                    fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White
                )
                Text(
                    text = "Proto: ${session.protocol}",
                    fontSize = 12.sp, color = Color(0xFF00F2FF)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Device: ${session.deviceKey ?: "-"}", fontSize = 12.sp, color = Color.LightGray)
            session.lastError?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Error: $it", fontSize = 12.sp, color = Color(0xFFFF5252))
            }
            if (session.state == ConnState.ERROR || session.state == ConnState.PERMISSION_DENIED) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}
