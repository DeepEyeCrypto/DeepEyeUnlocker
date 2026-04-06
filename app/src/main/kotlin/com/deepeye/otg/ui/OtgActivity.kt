package com.deepeye.otg.ui

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.deepeye.otg.BuildConfig
import com.deepeye.otg.DeepEyeApplication
import com.deepeye.otg.service.ModelSyncManager
import com.deepeye.otg.service.TunnelManager
import com.deepeye.otg.service.UsbForegroundService
import com.deepeye.otg.ui.screens.LoadingScreen
import com.deepeye.otg.ui.theme.DeepEyeTheme
import com.deepeye.otg.usb.SessionState
import com.deepeye.otg.viewmodel.UsbViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@dagger.hilt.android.AndroidEntryPoint
class OtgActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DeepEye-OtgActivity"
    }

    private val app by lazy { application as DeepEyeApplication }

    private val viewModel: UsbViewModel by viewModels()
    
    @javax.inject.Inject
    lateinit var tunnelManager: TunnelManager
 
    // ── Engine loading state ────────────────────────────────────
    private val engineLoaded = MutableStateFlow(false)
    private val loadingStatus = MutableStateFlow("Initializing...")

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            debugListUsbDevices()
        }

        handleUsbIntent(intent)

        setContent {
            DeepEyeTheme {
                val ready = engineLoaded.collectAsStateWithLifecycle().value
                val status = loadingStatus.collectAsStateWithLifecycle().value
                val activeState = viewModel.activeUsbState.collectAsStateWithLifecycle().value
                val context = androidx.compose.ui.platform.LocalContext.current

                LaunchedEffect(activeState) {
                    val serviceIntent = Intent(context, UsbForegroundService::class.java)
                    when (activeState) {
                        is SessionState.ConnectedReady,
                        is SessionState.ExecutingOperation,
                        is SessionState.ConnectedMtpOnly,
                        is SessionState.PartitionPreview,
                        is SessionState.Reporting -> {
                            // Keep service alive for active USB sessions
                            serviceIntent.action = UsbForegroundService.ACTION_START
                            context.startForegroundService(serviceIntent)
                        }
                        is SessionState.Idle,
                        is SessionState.Error,
                        is SessionState.PermissionDenied,
                        is SessionState.OperationComplete -> {
                            // Stop service when session ends or error occurs
                            serviceIntent.action = UsbForegroundService.ACTION_STOP
                            context.startService(serviceIntent)
                        }
                        else -> {
                            // Transitional states (WaitingForDevice, PermissionPending, etc.)
                            // Do nothing, keep previous service state
                        }
                    }
                }

                if (!ready) {
                    LoadingScreen(status = status)
                } else {
                    DeepEyeApp(viewModel = viewModel)
                }
            }
        }

        // Handle Remote Session (Stage H)
        intent?.getStringExtra("REMOTE_SESSION")?.let { code ->
            tunnelManager.joinSession(code)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            loadingStatus.value = "Loading native bridge..."
            com.deepeye.otg.NativeBridge.loadAsync()

            loadingStatus.value = "Syncing cloud models..."
            ModelSyncManager.sync(this@OtgActivity)

            loadingStatus.value = "Initializing database..."
            ModelSyncManager.load(this@OtgActivity)

            loadingStatus.value = "Ready"
            withContext(Dispatchers.Main) {
                engineLoaded.value = true
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUsbIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Process-scoped manager is owned by Application, not Activity.
    }

    private fun handleUsbIntent(intent: Intent?) {
        val action = intent?.action ?: return
        val device = extractDeviceFromIntent(intent) ?: return

        when (action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                Log.i(
                    TAG,
                    "USB attach intent received vid=0x${device.vendorId.toString(16)} pid=0x${device.productId.toString(16)} name=${device.deviceName}"
                )
                app.usbLifecycleManager.onDeviceAttached(device)
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                Log.i(
                    TAG,
                    "USB detach intent received vid=0x${device.vendorId.toString(16)} pid=0x${device.productId.toString(16)} name=${device.deviceName}"
                )
                app.usbLifecycleManager.onDeviceDetached(device)
            }
        }
    }

    private fun debugListUsbDevices() {
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        usbManager.deviceList.forEach { (name, device) ->
            Log.d(
                "USB_DEBUG",
                "Device: $name | VID: ${device.vendorId.toString(16)} | PID: ${device.productId.toString(16)} | Name: ${device.deviceName}"
            )
        }
    }

    private fun extractDeviceFromIntent(intent: Intent?): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }
}
