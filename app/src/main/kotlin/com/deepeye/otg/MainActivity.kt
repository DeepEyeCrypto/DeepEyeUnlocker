package com.deepeye.otg

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.deepeye.otg.BuildConfig
import com.deepeye.otg.ui.DeepEyeApp
import com.deepeye.otg.viewmodel.DeviceViewModel
import com.deepeye.otg.viewmodel.UsbViewModel
import com.deepeye.otg.viewmodel.UiEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val usbManager by lazy { getSystemService(USB_SERVICE) as UsbManager }
    private val usbViewModel by viewModels<UsbViewModel>()
    
    // USB hotplug receiver
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = if (Build.VERSION.SDK_INT >= 33)
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    else
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    
                    device?.let { dev ->
                        val vm = ViewModelProvider(this@MainActivity)[DeviceViewModel::class.java]
                        if (!usbManager.hasPermission(dev)) {
                            // Auto-request permission with explicit Intent to avoid FLAG_MUTABLE crash
                            val permissionIntent = Intent(ACTION_USB_PERMISSION)
                                .setPackage(this@MainActivity.packageName) // Make it explicit
                            
                            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                            } else {
                                PendingIntent.FLAG_UPDATE_CURRENT
                            }
                            
                            val pi = PendingIntent.getBroadcast(
                                this@MainActivity,
                                0,
                                permissionIntent,
                                flags
                            )
                            usbManager.requestPermission(dev, pi)
                        } else {
                            vm.onUsbDeviceAttached(dev)
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = if (Build.VERSION.SDK_INT >= 33)
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    else
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    device?.let {
                        val vm = ViewModelProvider(this@MainActivity)[DeviceViewModel::class.java]
                        vm.onUsbDeviceDetached(it)
                    }
                }
                ACTION_USB_PERMISSION -> {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    val device = if (Build.VERSION.SDK_INT >= 33)
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    else
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (granted && device != null) {
                        val vm = ViewModelProvider(this@MainActivity)[DeviceViewModel::class.java]
                        vm.onUsbDeviceAttached(device)
                    }
                }
            }
        }
    }

    companion object { const val ACTION_USB_PERMISSION = "com.deepeye.otg.USB_PERMISSION" }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable StrictMode in debug builds to catch UI thread violations
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
        
        super.onCreate(savedInstanceState)
        
        // Register USB receiver
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= 33)
            registerReceiver(usbReceiver, filter, RECEIVER_NOT_EXPORTED)
        else
            registerReceiver(usbReceiver, filter)

        // Handle UiEvent from ViewModel (file pickers)
        val vm = ViewModelProvider(this)[DeviceViewModel::class.java]
        lifecycleScope.launch {
            vm.uiEvent.collect { event ->
                when (event) {
                    is UiEvent.PickDaFile         -> daFilePicker.launch(arrayOf("*/*"))
                    is UiEvent.PickProgrammerFile -> progFilePicker.launch(arrayOf("*/*"))
                    is UiEvent.PickFlashImage     -> flashFilePicker.launch(arrayOf("*/*"))
                }
            }
        }
        
        setContent { DeepEyeApp(viewModel = usbViewModel) }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }

    // ── File Pickers ──────────────────────────────────────────────
    private val daFilePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val vm = ViewModelProvider(this)[DeviceViewModel::class.java]
            val path = copyUriToCache(it, "da_file.bin")
            vm.sendDa(path, 0x201000L)
        }
    }

    private val progFilePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val vm = ViewModelProvider(this)[DeviceViewModel::class.java]
            val path = copyUriToCache(it, "programmer.mbn")
            vm.sendProgrammer(path)
        }
    }

    private val flashFilePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val vm = ViewModelProvider(this)[DeviceViewModel::class.java]
            val path = copyUriToCache(it, "flash_image.bin")
            vm.firehoseFlash("userdata", path)
        }
    }

    // Copy content URI to app cache (USB ops need real file path)
    private fun copyUriToCache(uri: Uri, filename: String): String {
        val dest = File(cacheDir, filename)
        contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        return dest.absolutePath
    }
}
