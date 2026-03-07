package com.deepeye.otg.ui

import android.content.*
import android.hardware.usb.UsbManager
import android.os.*
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.deepeye.otg.DeviceModel
import com.deepeye.otg.ui.viewmodel.LogEntry
import com.deepeye.otg.usb.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class OtgActivity : AppCompatActivity() {
    
    // Logic Variables
    private var selectedBrand = "Xiaomi"
    private var selectedModelName = "Auto-Detect"
    private var nativeHandle: Long = 0
    private var deviceDatabase: MutableMap<String, List<com.deepeye.otg.DeviceModel>> = mutableMapOf()
    private var allModels: List<com.deepeye.otg.DeviceModel> = emptyList()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Lifecycle Manager
    private val lifecycleManager by lazy { 
        com.deepeye.otg.usb.UsbLifecycleManager(
            this, 
            getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager,
            appScope
        ) 
    }

    private val usbReceiver by lazy {
        com.deepeye.otg.usb.UsbBroadcastReceiver(
            lifecycleManager = lifecycleManager,
            scope = appScope
        )
    }

    // Log State for Compose
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    // ── Engine loading state ────────────────────────────────────
    private val _engineLoaded = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val settingsManager = com.deepeye.otg.data.SettingsManager(this)
        val viewModel = com.deepeye.otg.viewmodel.UsbViewModel(
            context = this,
            lifecycleManager = lifecycleManager,
            settings = settingsManager,
            usbState = kotlinx.coroutines.flow.MutableStateFlow(SessionState.Idle).asStateFlow(),
            logs = logs
        )

        setContent {
            com.deepeye.otg.ui.theme.DeepEyeTheme {
                val engineReady by _engineLoaded.collectAsState()
                
                // Foreground Service Orchestrator
                val activeState by viewModel.activeUsbState.collectAsState()
                val context = androidx.compose.ui.platform.LocalContext.current
                
                LaunchedEffect(activeState) {
                    val serviceIntent = Intent(context, com.deepeye.otg.service.UsbForegroundService::class.java)
                    if (activeState is SessionState.ConnectedReady) {
                        serviceIntent.action = com.deepeye.otg.service.UsbForegroundService.ACTION_START
                        context.startForegroundService(serviceIntent)
                    } else if (activeState is SessionState.Idle || activeState is SessionState.Error) {
                        serviceIntent.action = com.deepeye.otg.service.UsbForegroundService.ACTION_STOP
                        context.startService(serviceIntent)
                    }
                }

                if (!engineReady) {
                    com.deepeye.otg.ui.screens.LoadingScreen()
                } else {
                    com.deepeye.otg.ui.DeepEyeApp(viewModel = viewModel)
                }
            }
        }
        
        registerUsbReceiver()

        // Handle device that was already connected when app launched
        val deviceFromIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(android.hardware.usb.UsbManager.EXTRA_DEVICE, android.hardware.usb.UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(android.hardware.usb.UsbManager.EXTRA_DEVICE)
        }
        
        deviceFromIntent?.let { device ->
            lifecycleManager.onDeviceAttached(device)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            com.deepeye.otg.NativeBridge.loadAsync()
            loadDeviceDatabase()
            withContext(Dispatchers.Main) {
                _engineLoaded.value = true
            }
        }
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(com.deepeye.otg.usb.UsbSessionManager.ACTION_USB_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(usbReceiver) }
        lifecycleManager.destroy()
        appScope.cancel()
    }

    private fun loadDeviceDatabase() {
        try {
            val jsonString = assets.open("models.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            allModels = (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                DeviceModel(obj.getString("name"), obj.getString("chipset"), obj.getString("brand"))
            }
        } catch (e: Exception) {
            Log.e("OtgActivity", "DB Load Error: ${e.message}")
        }
    }
}
