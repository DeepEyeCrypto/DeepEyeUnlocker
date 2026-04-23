package com.deepeye.otg.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.deepeye.otg.util.detectAppleMode
import com.deepeye.otg.usb.DeviceMatrix
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for observing USB device attachments, specifically Apple devices.
 */
@Singleton
class DeviceRepository @Inject constructor(
    private val context: Context
) {

    /**
     * Observe Apple device attachments.
     * Emits [AppleDeviceState] when an Apple device is attached.
     */
    fun observeAppleDevice(): Flow<AppleDeviceState> = callbackFlow {
        val filter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val device = intent.getParcelableExtra<UsbDevice>(
                    UsbManager.EXTRA_DEVICE
                ) ?: return
                val mode = device.detectAppleMode()
                if (mode != DeviceMatrix.AppleMode.UNKNOWN) {
                    trySend(AppleDeviceState.Detected(device, mode))
                }
            }
        }
        context.registerReceiver(receiver, filter, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_NOT_EXPORTED
        } else {
            0
        })
        awaitClose { context.unregisterReceiver(receiver) }
    }

    fun observeDfuMode(): Flow<Boolean> = callbackFlow {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val attached = intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (device == null || device.vendorId != DeviceMatrix.APPLE_VID) {
                    return
                }
                val isDfu = attached && device.detectAppleMode() == DeviceMatrix.AppleMode.DFU
                trySend(isDfu)
            }
        }

        context.registerReceiver(receiver, filter, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_NOT_EXPORTED
        } else {
            0
        })

        awaitClose { context.unregisterReceiver(receiver) }
    }
}

/**
 * Sealed class representing Apple device state.
 */
sealed class AppleDeviceState {
    object Idle : AppleDeviceState()
    data class Detected(
        val device: UsbDevice,
        val mode: DeviceMatrix.AppleMode
    ) : AppleDeviceState()
    data class Error(val reason: String) : AppleDeviceState()
}
