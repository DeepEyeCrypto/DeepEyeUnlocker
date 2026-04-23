package com.deepeye.otg.device

import android.app.PendingIntent
import android.content.*
import android.hardware.usb.*
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

const val ACTION_USB_PERMISSION = "com.deepeye.otg.USB_PERMISSION"

object UsbPermissionHelper {

    // Request USB permission and await result via coroutine
    suspend fun requestPermission(
        context: Context,
        device:  UsbDevice,
    ): Boolean = suspendCancellableCoroutine { cont ->
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        if (usbManager.hasPermission(device)) {
            cont.resume(true)
            return@suspendCancellableCoroutine
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                ctx.unregisterReceiver(this)
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                cont.resume(granted)
            }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else
            PendingIntent.FLAG_UPDATE_CURRENT
        
        // Make Intent explicit to avoid FLAG_MUTABLE crash on Android 14+
        val permissionIntent = Intent(ACTION_USB_PERMISSION)
            .setPackage(context.packageName)
        
        val pi = PendingIntent.getBroadcast(context, 0, permissionIntent, flags)

        context.registerReceiver(receiver, IntentFilter(ACTION_USB_PERMISSION), if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_NOT_EXPORTED
        } else {
            0
        })
        usbManager.requestPermission(device, pi)

        cont.invokeOnCancellation { context.unregisterReceiver(receiver) }
    }
}
