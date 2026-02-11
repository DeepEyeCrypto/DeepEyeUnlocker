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
    
    // Logic Variables
    private lateinit var usbHostManager: UsbHostManager
    private var selectedBrand = "Xiaomi"
    private var selectedModelName = "Auto-Detect"
    private var nativeHandle: Long = 0
    private var deviceDatabase: MutableMap<String, List<DeviceModel>> = mutableMapOf()
    private var allModels: List<DeviceModel> = emptyList()

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
    
            // Setup Listeners
            btnSelectModel.setOnClickListener { showModelSelectionDialog() }
            btnCloseLog.setOnClickListener { logOverlay.visibility = View.GONE }
            
            btnRemote.setOnClickListener {
                hapticFeedback()
                val intent = android.content.Intent(this, RemoteShareActivity::class.java)
                startActivity(intent)
            }
            
            // Load Data & Setup
            loadDeviceDatabase()
            setupBrandTabs()
            setupOperationButtons()
            
            // USB Manager
            usbHostManager = UsbHostManager(this, object : UsbHostManager.HotplugListener {
                override fun onDeviceAttached(device: android.hardware.usb.UsbDevice) {
                    runOnUiThread {
                        connectionIndicator.text = "● ATTACHED"
                        connectionIndicator.setTextColor(ContextCompat.getColor(this@OtgActivity, R.color.deepeye_warning))
                        log("Device detected: ${device.productName} (${device.vendorId}:${device.productId})", "WARNING")
                    }
                }

                override fun onDeviceReady(fd: Int, vid: Int, pid: Int) {
                    runOnUiThread {
                        initializeCore(fd, vid, pid)
                    }
                }

                override fun onDeviceError(message: String) {
                    runOnUiThread {
                        connectionIndicator.text = "● ERROR"
                        connectionIndicator.setTextColor(ContextCompat.getColor(this@OtgActivity, R.color.deepeye_error))
                        log("USB Error: $message", "ERROR")
                    }
                }

                override fun onStatusUpdate(message: String) {
                    runOnUiThread {
                        log(message, "INFO")
                    }
                }
            })
            
            log("DeepEye Unlocker v5.1.3 Ready - ${allModels.size} models loaded.", "SUCCESS")
            
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
                
                if (nativeHandle == 0L) {
                    log("Connect device first! (Native Core Offline)", "ERROR")
                    // Toast.makeText(this, "Connect device via OTG first", Toast.LENGTH_SHORT).show()
                } else {
                    log("Executing: $opName...", "INFO")
                    // Call Native Logic Here
                }
            }
        }
    }

    private fun initializeCore(fd: Int, vid: Int, pid: Int) {
        log("Initializing native core ($vid:$pid)...", "INFO")
        try {
            nativeHandle = NativeBridge.initCore(fd, vid, pid)
            if (nativeHandle != 0L) {
                if (NativeBridge.identifyDevice(nativeHandle)) {
                    log("Device Link Secured (Handle: $nativeHandle)", "SUCCESS")
                    connectionIndicator.text = "● READY"
                    connectionIndicator.setTextColor(ContextCompat.getColor(this, R.color.deepeye_success))
                } else {
                    log("Handshake failed during identification.", "ERROR")
                }
            } else {
                log("Native Core Init failed (Handle is 0).", "ERROR")
            }
        } catch (e: Exception) {
            log("Core Init Exception: ${e.message}", "ERROR")
            e.printStackTrace()
        }
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
