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
    
    // UI Elements
    private lateinit var btnSelectModel: Button
    private lateinit var logOverlay: View
    private lateinit var terminalText: TextView
    private lateinit var logScrollView: ScrollView
    private lateinit var btnCloseLog: View
    
    private lateinit var statusLog: TextView // Keeping for compatibility or remove?
    private lateinit var connectionIndicator: TextView
    private lateinit var usbHostManager: UsbHostManager
    private lateinit var btnRemote: Button
    
    private var selectedBrand = "Xiaomi"
    private var selectedModelName = "Auto-Detect"
    // ... existing vars ...

    private fun log(message: String, type: String = "INFO") {
        val colorHex = when(type) {
            "ERROR" -> "#FF5252" // Red
            "SUCCESS" -> "#00E676" // Green
            "WARNING" -> "#FFB300" // Orange
            else -> "#00F2FF" // Cyan
        }
        
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formattedMsg = "<font color='#888888'>$timestamp</font> <font color='$colorHex'>$message</font><br/>"
        
        runOnUiThread {
            // Show overlay if hidden (Auto-popup on log)
            if (logOverlay.visibility != View.VISIBLE) {
                logOverlay.visibility = View.VISIBLE
            }
            
            terminalText.append(android.text.Html.fromHtml(formattedMsg, android.text.Html.FROM_HTML_MODE_LEGACY))
            logScrollView.post { logScrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otg)
        
        // Bind New UI
        btnSelectModel = findViewById(R.id.btnSelectModel)
        logOverlay = findViewById(R.id.logOverlay)
        terminalText = findViewById(R.id.terminalText)
        logScrollView = findViewById(R.id.logScrollView)
        btnCloseLog = findViewById(R.id.btnCloseLog)
        
        connectionIndicator = findViewById(R.id.connectionIndicator)
        btnRemote = findViewById(R.id.btnRemoteUnlock)

        // Setup Model Selector
        btnSelectModel.setOnClickListener {
            showModelSelectionDialog()
        }
        
        // Setup Log Close
        btnCloseLog.setOnClickListener {
            logOverlay.visibility = View.GONE
        }
        
        // ... (Existing Setup) ...
    
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

    private fun loadModelsForBrand(brand: String) {
        // Legacy stub
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
            // ... (other ops)
            R.id.btnSamsungQr to "Samsung QR Bypass",
            R.id.btnRemoteUnlock to "Remote Unlock" // Add this to map if needed, but it has separate listener
        )
        
        // Remote button has its own listener, so we don't need to add it to generic map
        // But we need to ensure other buttons don't crash if native is missing
        
        operations.forEach { (viewId, opName) ->
            findViewById<Button>(viewId)?.setOnClickListener {
                hapticFeedback()
                
                // Allow QR and Remote without Native Core
                val allowWithoutCore = opName == "Samsung QR Bypass" || opName == "Remote Unlock"
                
                if (nativeHandle == 0L && !allowWithoutCore) { 
                    log("Connect device first! (Native Core Offline)", "ERROR")
                    Toast.makeText(this, "Connect device via OTG first", Toast.LENGTH_SHORT).show()
                } else {
                    // ...
                }
            }
        }
    }
    
    private fun initializeCore(fd: Int) {
        log("Initializing native core...", "INFO")
        progressBar.isIndeterminate = true
        
        try {
            nativeHandle = NativeBridge.initCore(fd, 0x0E8D, 0x0003)
            if (nativeHandle != 0L) {
                val identified = NativeBridge.identifyDevice(nativeHandle)
                if (identified) {
                    log("Device secured! Ready for operations.", "SUCCESS")
                    progressBar.progress = 100
                } else {
                    log("Handshake failed - Device unresponsive", "ERROR")
                }
            } else {
                log("Engine failure - Native init failed (Stubbed?)", "ERROR")
            }
        } catch (e: UnsatisfiedLinkError) {
            log("Native Core Missing (JNI Error) - Limited Functionality", "WARNING")
            e.printStackTrace()
        } catch (e: Exception) {
            log("Core Init Crash: ${e.message}", "ERROR")
            e.printStackTrace()
        } finally {
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
