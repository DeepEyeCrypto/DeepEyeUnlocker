package com.deepeye.otg

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import android.os.Build

class OtgActivity : AppCompatActivity() {
    
    private lateinit var modelList: RecyclerView
    private lateinit var modelAdapter: ModelAdapter
    private lateinit var statusLog: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var connectionIndicator: TextView
    private lateinit var usbStatus: TextView
    private lateinit var modelCount: TextView
    private lateinit var usbHostManager: UsbHostManager
    
    private var selectedBrand = "Xiaomi"
    private var selectedMode = "BROM"
    private var nativeHandle: Long = 0
    
    // Device database loaded from JSON
    private var deviceDatabase: MutableMap<String, List<DeviceModel>> = mutableMapOf()
    private var allModels: List<DeviceModel> = emptyList()

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
        val prefix = when(type) {
            "ERROR" -> "[ERROR] "
            "SUCCESS" -> "[OK] "
            "WARNING" -> "[WARN] "
            else -> "[INFO] "
        }
        val color = when(type) {
            "ERROR" -> R.color.deepeye_error
            "SUCCESS" -> R.color.deepeye_success
            "WARNING" -> R.color.deepeye_warning
            else -> R.color.deepeye_cyan
        }
        statusLog.text = "$prefix$message"
        statusLog.setTextColor(ContextCompat.getColor(this, color))
    }
    
    private fun loadDeviceDatabase() {
        try {
            val jsonString = assets.open("models.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            
            var totalCount = 0
            
            // Clear existing
            deviceDatabase.clear()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val brand = obj.getString("brand")
                val name = obj.getString("name")
                val chipset = obj.getString("chipset")
                
                val model = DeviceModel(name, chipset, brand)
                
                if (!deviceDatabase.containsKey(brand)) {
                    deviceDatabase[brand] = mutableListOf()
                }
                (deviceDatabase[brand] as MutableList).add(model)
                totalCount++
            }
            
            // Build all models list for search
            allModels = deviceDatabase.values.flatten()
            
            log("Loaded $totalCount models from ${deviceDatabase.size} brands", "SUCCESS")
        } catch (e: Exception) {
            log("Failed to load models.json: ${e.message}", "ERROR")
            // Fallback to legacy loading if new format fails
            loadLegacyDatabase()
        }
    }

    private fun loadLegacyDatabase() {
        try {
            val jsonString = assets.open("device_database.json").bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            val brands = json.getJSONObject("brands")
            
            brands.keys().forEach { brandName ->
                val brandObj = brands.getJSONObject(brandName)
                val modelsArray = brandObj.getJSONArray("models")
                val modelsList = mutableListOf<DeviceModel>()
                
                for (i in 0 until modelsArray.length()) {
                    val model = modelsArray.getJSONObject(i)
                    modelsList.add(DeviceModel(
                        name = model.getString("name"),
                        chipset = model.getString("chipset"),
                        brand = brandName
                    ))
                }
                deviceDatabase[brandName] = modelsList
            }
            allModels = deviceDatabase.values.flatten()
        } catch (e: Exception) {
            log("Legacy DB load failed too.", "ERROR")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otg)
        
        // Initialize views
        modelList = findViewById(R.id.modelList)
        statusLog = findViewById(R.id.statusLog)
        progressBar = findViewById(R.id.progressBar)
        connectionIndicator = findViewById(R.id.connectionIndicator)
        usbStatus = findViewById(R.id.usbStatus)
        modelCount = findViewById(R.id.modelCount)
        
        // Load device database
        loadDeviceDatabase()
        
        // Setup model list
        modelAdapter = ModelAdapter { model ->
            hapticFeedback()
            log("Selected: ${model.name} (${model.chipset})", "INFO")
        }
        modelList.apply {
            layoutManager = LinearLayoutManager(this@OtgActivity)
            adapter = modelAdapter
        }
        
        // Load default brand models
        loadModelsForBrand("Xiaomi")
        
        // Setup brand tabs
        setupBrandTabs()
        
        // Setup mode tabs
        setupModeTabs()
        
        // Setup search
        setupSearch()
        
        // Setup operation buttons
        setupOperationButtons()
        
        // Setup USB Host Manager
        usbHostManager = UsbHostManager(this, object : UsbHostManager.HotplugListener {
            override fun onDeviceAttached(device: android.hardware.usb.UsbDevice) {
                runOnUiThread {
                    connectionIndicator.text = "● CONNECTED"
                    connectionIndicator.setTextColor(ContextCompat.getColor(this@OtgActivity, R.color.deepeye_success))
                    usbStatus.text = " - ${device.productName ?: "Unknown Device"}"
                    log("Device attached: ${device.productName}", "SUCCESS")
                }
            }

            override fun onDeviceReady(fd: Int) {
                runOnUiThread {
                    initializeCore(fd)
                }
            }
        })
        
        log("DeepEye Unlocker v4.4.0 Ready - ${allModels.size} models", "SUCCESS")
    }
    
    private fun loadModelsForBrand(brand: String) {
        val models = deviceDatabase[brand] ?: emptyList()
        modelAdapter.updateModels(models)
        modelCount.text = "${models.size} models"
    }
    
    private fun updateButtonLabels() {
        val prefix = "[$selectedMode]"
        findViewById<Button>(R.id.btnUnlockBl)?.text = "$prefix UNLOCK BOOTLOADER"
        findViewById<Button>(R.id.btnRelockBl)?.text = "$prefix RELOCK BOOTLOADER"
        findViewById<Button>(R.id.btnEraseFrp)?.text = "$prefix ERASE FRP"
        
        // Mi Account Logic
        val miBtn = findViewById<Button>(R.id.btnRemoveMiAccount)
        if (selectedMode == "ADB") {
             miBtn?.text = "[ADB] DISABLE MI CLOUD"
        } else {
             miBtn?.text = "$prefix REMOVE MI ACCOUNT"
        }
    }
    
    private fun setupBrandTabs() {
        val brandMap = mapOf(
            R.id.brandXiaomi to "Xiaomi",
            R.id.brandSamsung to "Samsung",
            R.id.brandHuawei to "Huawei",
            R.id.brandOppo to "OPPO",
            R.id.brandVivo to "Vivo",
            R.id.brandRealme to "Realme",
            R.id.brandOnePlus to "OnePlus",
            R.id.brandMotorola to "Motorola",
            R.id.brandLenovo to "Lenovo",
            R.id.brandNokia to "Nokia",
            R.id.brandLG to "LG",
            R.id.brandAsus to "Asus",
            R.id.brandTecno to "Tecno",
            R.id.brandInfinix to "Infinix",
            R.id.brandItel to "Itel",
            R.id.brandZTE to "ZTE",
            R.id.brandGoogle to "Google"
        )
        
        brandMap.forEach { (viewId, brand) ->
            findViewById<TextView>(viewId)?.setOnClickListener { view ->
                hapticFeedback()
                selectedBrand = brand
                
                // Update visual selection
                brandMap.keys.forEach { id ->
                    findViewById<TextView>(id)?.setBackgroundResource(R.drawable.brand_tab_unselected)
                }
                (view as TextView).setBackgroundResource(R.drawable.brand_tab_selected)
                
                // Samsung QR Button Logic
                val qrBtn = findViewById<Button>(R.id.btnSamsungQr)
                if (brand == "Samsung") {
                    qrBtn?.visibility = View.VISIBLE
                } else {
                    qrBtn?.visibility = View.GONE
                }

                loadModelsForBrand(brand)
                updateButtonLabels()
                log("Brand: $brand selected", "INFO")
            }
        }
    }
    
    private fun setupModeTabs() {
        val modeMap = mapOf(
            R.id.modeBrom to "BROM",
            R.id.modeEdl to "EDL",
            R.id.modeAdb to "ADB",
            R.id.modeFastboot to "FASTBOOT",
            R.id.modeTestpoint to "TESTPOINT"
        )
        
        modeMap.forEach { (viewId, mode) ->
            findViewById<TextView>(viewId)?.setOnClickListener { view ->
                hapticFeedback()
                selectedMode = mode
                
                // Update visual selection
                modeMap.keys.forEach { id ->
                    val tv = findViewById<TextView>(id)
                    tv?.background = null
                    tv?.setTextColor(ContextCompat.getColor(this, R.color.deepeye_white))
                    tv?.alpha = 0.5f
                }
                (view as TextView).setBackgroundResource(R.drawable.mode_tab_selected)
                view.setTextColor(ContextCompat.getColor(this, R.color.deepeye_obsidian))
                view.alpha = 1f
                
                updateButtonLabels()
                log("Mode: $mode selected", "INFO")
            }
        }
    }
    
    private fun setupSearch() {
        val searchBox = findViewById<EditText>(R.id.searchModels)
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                
                val filtered = if (query.isEmpty()) {
                    deviceDatabase[selectedBrand] ?: emptyList()
                } else if (query.length >= 2) {
                    allModels.filter { 
                        it.name.contains(query, ignoreCase = true) || 
                        it.chipset.contains(query, ignoreCase = true) ||
                        it.brand.contains(query, ignoreCase = true)
                    }
                } else {
                    deviceDatabase[selectedBrand]?.filter { 
                        it.name.contains(query, ignoreCase = true) || 
                        it.chipset.contains(query, ignoreCase = true) 
                    } ?: emptyList()
                }
                
                modelAdapter.updateModels(filtered)
                modelCount.text = "${filtered.size} models"
            }
        })
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
            R.id.btnPatchCert to "Patch CERT Auto",
            R.id.btnReadInfo to "Read Device Info",
            R.id.btnReadPartition to "Read Partition",
            R.id.btnWritePartition to "Write Partition",
            R.id.btnFormatData to "Format Userdata",
            R.id.btnReadImei to "Read IMEI",
            R.id.btnWriteImei to "Write IMEI",
            R.id.btnBackupNvram to "Backup NVRAM",
            R.id.btnRestoreNvram to "Restore NVRAM",
            R.id.btnWipeNv to "Wipe NV",
            R.id.btnFlashRom to "Flash Full ROM",
            R.id.btnFlashRecovery to "Flash Recovery",
            R.id.btnFlashBoot to "Flash Boot",
            R.id.btnSamsungQr to "Samsung QR Bypass"
        )
        
        operations.forEach { (viewId, opName) ->
            findViewById<Button>(viewId)?.setOnClickListener {
                hapticFeedback()
                if (nativeHandle == 0L && opName != "Samsung QR Bypass") { // Allow QR without native connection
                    log("Connect device first!", "ERROR")
                    Toast.makeText(this, "Connect device via OTG first", Toast.LENGTH_SHORT).show()
                } else {
                    log("Executing: $opName...", "INFO")
                    progressBar.isIndeterminate = true
                    
                    if (opName == "Samsung QR Bypass") {
                        // Launch QR Logic (Simulated)
                        Toast.makeText(this, "Generating QR Code...", Toast.LENGTH_SHORT).show()
                        progressBar.isIndeterminate = false
                    } else {
                        Toast.makeText(this, "$opName - Processing...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun initializeCore(fd: Int) {
        log("Initializing native core...", "INFO")
        progressBar.isIndeterminate = true
        
        nativeHandle = NativeBridge.initCore(fd, 0x0E8D, 0x0003)
        if (nativeHandle != 0L) {
            val identified = NativeBridge.identifyDevice(nativeHandle)
            if (identified) {
                log("Device secured! Ready for operations.", "SUCCESS")
                progressBar.isIndeterminate = false
                progressBar.progress = 100
            } else {
                log("Handshake failed - Device unresponsive", "ERROR")
                progressBar.isIndeterminate = false
            }
        } else {
            log("Engine failure - Native init failed", "ERROR")
            progressBar.isIndeterminate = false
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
