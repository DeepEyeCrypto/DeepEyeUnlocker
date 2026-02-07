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
    
    // Device database
    private val deviceDatabase = mapOf(
        "Xiaomi" to listOf(
            DeviceModel("Redmi Note 9", "MT6765", "Xiaomi"),
            DeviceModel("Redmi Note 9 Pro", "SM7125", "Xiaomi"),
            DeviceModel("Redmi Note 10", "SM6115", "Xiaomi"),
            DeviceModel("Redmi Note 10 Pro", "SM7150", "Xiaomi"),
            DeviceModel("Redmi Note 11", "SM6225", "Xiaomi"),
            DeviceModel("Redmi Note 11 Pro", "MT6785", "Xiaomi"),
            DeviceModel("Redmi Note 12", "SM4375", "Xiaomi"),
            DeviceModel("Redmi 9", "MT6762G", "Xiaomi"),
            DeviceModel("Redmi 9A", "MT6762G", "Xiaomi"),
            DeviceModel("Redmi 9C", "MT6765", "Xiaomi"),
            DeviceModel("Redmi 10", "MT6769", "Xiaomi"),
            DeviceModel("Redmi 10C", "SM6225", "Xiaomi"),
            DeviceModel("POCO X3", "SM7125", "Xiaomi"),
            DeviceModel("POCO X3 Pro", "SM8150", "Xiaomi"),
            DeviceModel("POCO F3", "SM8250", "Xiaomi"),
            DeviceModel("POCO M3", "SM6115", "Xiaomi"),
            DeviceModel("Mi 11", "SM8350", "Xiaomi"),
            DeviceModel("Mi 11 Lite", "SM7150", "Xiaomi"),
            DeviceModel("Mi 10T", "SM8250", "Xiaomi"),
        ),
        "Samsung" to listOf(
            DeviceModel("Galaxy A12", "MT6765", "Samsung"),
            DeviceModel("Galaxy A13", "Exynos 850", "Samsung"),
            DeviceModel("Galaxy A14", "Exynos 1330", "Samsung"),
            DeviceModel("Galaxy A32", "MT6769V", "Samsung"),
            DeviceModel("Galaxy A52", "SM7125", "Samsung"),
            DeviceModel("Galaxy A53", "Exynos 1280", "Samsung"),
            DeviceModel("Galaxy M12", "Exynos 850", "Samsung"),
            DeviceModel("Galaxy M32", "MT6769V", "Samsung"),
            DeviceModel("Galaxy S21", "Exynos 2100", "Samsung"),
            DeviceModel("Galaxy S22", "Exynos 2200", "Samsung"),
            DeviceModel("Galaxy S23", "SM8550", "Samsung"),
        ),
        "OPPO" to listOf(
            DeviceModel("OPPO A15", "MT6765", "OPPO"),
            DeviceModel("OPPO A16", "MT6765", "OPPO"),
            DeviceModel("OPPO A53", "SM4250", "OPPO"),
            DeviceModel("OPPO A74", "SM6115", "OPPO"),
            DeviceModel("OPPO F19", "SM6115", "OPPO"),
            DeviceModel("OPPO Reno 5", "SM7125", "OPPO"),
            DeviceModel("OPPO Reno 6", "MT6877", "OPPO"),
            DeviceModel("OPPO Reno 7", "SM7325", "OPPO"),
        ),
        "Vivo" to listOf(
            DeviceModel("Vivo Y12", "MT6762", "Vivo"),
            DeviceModel("Vivo Y15", "MT6762", "Vivo"),
            DeviceModel("Vivo Y20", "SM4250", "Vivo"),
            DeviceModel("Vivo Y21", "MT6765", "Vivo"),
            DeviceModel("Vivo Y33s", "MT6769", "Vivo"),
            DeviceModel("Vivo V21", "MT6853", "Vivo"),
            DeviceModel("Vivo V23", "MT6877", "Vivo"),
        ),
        "Realme" to listOf(
            DeviceModel("Realme C11", "MT6765", "Realme"),
            DeviceModel("Realme C12", "MT6765", "Realme"),
            DeviceModel("Realme C21", "MT6765", "Realme"),
            DeviceModel("Realme C25", "MT6765", "Realme"),
            DeviceModel("Realme 8", "MT6785", "Realme"),
            DeviceModel("Realme 8 Pro", "SM7125", "Realme"),
            DeviceModel("Realme 9", "SM6225", "Realme"),
            DeviceModel("Realme Narzo 30", "MT6785", "Realme"),
            DeviceModel("Realme Narzo 50", "MT6769", "Realme"),
        ),
        "Huawei" to listOf(
            DeviceModel("Huawei Y6p", "MT6762R", "Huawei"),
            DeviceModel("Huawei Y7a", "Kirin 710A", "Huawei"),
            DeviceModel("Huawei Y9a", "MT6765", "Huawei"),
            DeviceModel("Huawei Nova 7i", "Kirin 810", "Huawei"),
            DeviceModel("Huawei P30 Lite", "Kirin 710", "Huawei"),
            DeviceModel("Huawei P40 Lite", "Kirin 810", "Huawei"),
        ),
        "OnePlus" to listOf(
            DeviceModel("OnePlus Nord", "SM7250", "OnePlus"),
            DeviceModel("OnePlus Nord CE", "SM7225", "OnePlus"),
            DeviceModel("OnePlus Nord N10", "SM6350", "OnePlus"),
            DeviceModel("OnePlus 9", "SM8350", "OnePlus"),
            DeviceModel("OnePlus 9 Pro", "SM8350", "OnePlus"),
        ),
        "Motorola" to listOf(
            DeviceModel("Moto G30", "SM6115", "Motorola"),
            DeviceModel("Moto G50", "SM4350", "Motorola"),
            DeviceModel("Moto G60", "SM6150", "Motorola"),
            DeviceModel("Moto E7", "MT6762", "Motorola"),
            DeviceModel("Moto E20", "Unisoc T606", "Motorola"),
        ),
        "Tecno" to listOf(
            DeviceModel("Tecno Spark 7", "MT6761", "Tecno"),
            DeviceModel("Tecno Spark 8", "MT6762", "Tecno"),
            DeviceModel("Tecno Camon 17", "MT6769", "Tecno"),
            DeviceModel("Tecno Camon 18", "MT6769", "Tecno"),
            DeviceModel("Tecno Pova 2", "MT6765", "Tecno"),
        ),
        "Infinix" to listOf(
            DeviceModel("Infinix Hot 10", "MT6762", "Infinix"),
            DeviceModel("Infinix Hot 11", "MT6762", "Infinix"),
            DeviceModel("Infinix Note 10", "MT6769", "Infinix"),
            DeviceModel("Infinix Note 11", "MT6769", "Infinix"),
            DeviceModel("Infinix Zero X", "MT6785", "Infinix"),
        ),
    )

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
        
        log("DeepEye Unlocker v4.3.0 Ready", "SUCCESS")
    }
    
    private fun loadModelsForBrand(brand: String) {
        val models = deviceDatabase[brand] ?: emptyList()
        modelAdapter.updateModels(models)
        modelCount.text = "${models.size} models"
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
                
                loadModelsForBrand(brand)
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
                val query = s.toString()
                val allModels = deviceDatabase[selectedBrand] ?: emptyList()
                val filtered = if (query.isEmpty()) {
                    allModels
                } else {
                    allModels.filter { 
                        it.name.contains(query, ignoreCase = true) || 
                        it.chipset.contains(query, ignoreCase = true) 
                    }
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
            R.id.btnFlashBoot to "Flash Boot"
        )
        
        operations.forEach { (viewId, opName) ->
            findViewById<Button>(viewId)?.setOnClickListener {
                hapticFeedback()
                if (nativeHandle == 0L) {
                    log("Connect device first!", "ERROR")
                    Toast.makeText(this, "Connect device via OTG first", Toast.LENGTH_SHORT).show()
                } else {
                    log("Executing: $opName...", "INFO")
                    progressBar.isIndeterminate = true
                    // TODO: Implement actual operations
                    Toast.makeText(this, "$opName - Coming Soon!", Toast.LENGTH_SHORT).show()
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
