package com.deepeye.otg.ui

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
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

        setContent {
            DeepEyeTheme {
                val ready = engineLoaded.collectAsStateWithLifecycle().value
                val status = loadingStatus.collectAsStateWithLifecycle().value
                val activeState = viewModel.activeUsbState.collectAsStateWithLifecycle().value
                val context = androidx.compose.ui.platform.LocalContext.current

                LaunchedEffect(activeState) {
                    val serviceIntent = Intent(context, UsbForegroundService::class.java)
                    if (activeState is SessionState.ConnectedReady) {
                        serviceIntent.action = UsbForegroundService.ACTION_START
                        context.startForegroundService(serviceIntent)
                    } else if (activeState is SessionState.Idle || activeState is SessionState.Error) {
                        serviceIntent.action = UsbForegroundService.ACTION_STOP
                        context.startService(serviceIntent)
                    }
                }

                if (!ready) {
                    LoadingScreen(status = status)
                } else {
                    DeepEyeApp(viewModel = viewModel)
                }
            }
        }

        // Handle device already connected when app launched from USB attach intent.
        extractDeviceFromIntent(intent)?.let { device ->
            app.usbLifecycleManager.onDeviceAttached(device)
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

    override fun onDestroy() {
        super.onDestroy()
        // Process-scoped manager is owned by Application, not Activity.
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
