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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import android.util.Log

class OtgActivity : AppCompatActivity() {
    
    // Logic Variables
    private var selectedBrand = "Xiaomi"
    private var selectedModelName = "Auto-Detect"
    private var nativeHandle: Long = 0
    private var deviceDatabase: MutableMap<String, List<DeviceModel>> = mutableMapOf()
    private var allModels: List<DeviceModel> = emptyList()

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
    val logs = _logs.asStateFlow()

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
            usbState = kotlinx.coroutines.flow.MutableStateFlow(com.deepeye.otg.UsbSessionState.Idle).asStateFlow(),
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
                    if (activeState is com.deepeye.otg.UsbSessionState.ConnectedReady) {
                        serviceIntent.action = com.deepeye.otg.service.UsbForegroundService.ACTION_START
                        context.startForegroundService(serviceIntent)
                    } else if (activeState is com.deepeye.otg.UsbSessionState.Idle || activeState is com.deepeye.otg.UsbSessionState.Error) {
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
                com.deepeye.otg.auth.LicenseManager.init(this@OtgActivity)
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
