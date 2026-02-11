package com.deepeye.otg

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log

/**
 * USB Permission State Machine Manager
 * 
 * Fixes the "Requesting USB permission..." infinite loop by:
 * 1. Properly handling permission broadcast responses
 * 2. Using correct PendingIntent flags for Android 12+
 * 3. Explicit state tracking and logging
 * 4. Handling both grant and denial cases
 */
class UsbPermissionManager(
    private val context: Context,
    private val usbManager: UsbManager
) {
    companion object {
        private const val ACTION_USB_PERMISSION = "com.deepeye.otg.USB_PERMISSION"
        private const val TAG = "DeepEye-Permission"
    }
    
    enum class PermissionState {
        NONE,           // No permission requested
        REQUESTING,     // requestPermission() called, waiting for user
        GRANTED,        // User approved
        DENIED          // User rejected or system blocked
    }
    
    interface PermissionListener {
        fun onPermissionGranted(device: UsbDevice)
        fun onPermissionDenied(device: UsbDevice)
        fun onPermissionStateChanged(state: PermissionState, message: String)
    }
    
    @Volatile
    private var permissionState: PermissionState = PermissionState.NONE
    private var listener: PermissionListener? = null
    private var pendingDevice: UsbDevice? = null
    
    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            Log.d(TAG, "[BROADCAST] onReceive: action=${intent.action}")
            
            if (ACTION_USB_PERMISSION != intent.action) {
                Log.w(TAG, "[BROADCAST] Wrong action: ${intent.action}, expected: $ACTION_USB_PERMISSION")
                return
            }
            
            val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            
            if (device == null) {
                Log.e(TAG, "[BROADCAST] No device in intent!")
                updateState(PermissionState.DENIED, "Permission broadcast missing device")
                return
            }
            
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            
            Log.i(TAG, "[BROADCAST] Device: ${device.deviceName} (${device.vendorId}:${device.productId})")
            Log.i(TAG, "[BROADCAST] Permission granted: $granted")
            
            // Verify this is the device we requested permission for
            if (device.deviceId != pendingDevice?.deviceId) {
                Log.w(TAG, "[BROADCAST] Device mismatch: got ${device.deviceId}, expected ${pendingDevice?.deviceId}")
                return
            }
            
            if (granted) {
                updateState(PermissionState.GRANTED, "USB permission GRANTED by user")
                listener?.onPermissionGranted(device)
            } else {
                updateState(PermissionState.DENIED, "USB permission DENIED by user")
                listener?.onPermissionDenied(device)
            }
            
            pendingDevice = null
        }
    }
    
    fun register(permListener: PermissionListener) {
        listener = permListener
        
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(permissionReceiver, filter)
        }
        
        Log.i(TAG, "[INIT] Permission receiver registered")
    }
    
    fun unregister() {
        try {
            context.unregisterReceiver(permissionReceiver)
            Log.i(TAG, "[CLEANUP] Permission receiver unregistered")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "[CLEANUP] Receiver was not registered: ${e.message}")
        }
    }
    
    /**
     * Request USB permission from the user.
     * Shows system dialog if not already granted.
     */
    fun requestPermission(device: UsbDevice) {
        // Check if we already have permission
        if (usbManager.hasPermission(device)) {
            Log.i(TAG, "[REQ] Permission already granted for ${device.vendorId}:${device.productId}")
            updateState(PermissionState.GRANTED, "Permission already granted (cached)")
            listener?.onPermissionGranted(device)
            return
        }
        
        pendingDevice = device
        updateState(PermissionState.REQUESTING, "Requesting USB permission from user...")
        
        Log.i(TAG, "[REQ] Requesting permission for ${device.deviceName} (${device.vendorId}:${device.productId})")
        
        // Create PendingIntent with correct flags for Android 12+
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            flags
        )
        
        try {
            usbManager.requestPermission(device, permissionIntent)
            Log.d(TAG, "[REQ] requestPermission() called successfully")
        } catch (e: Exception) {
            Log.e(TAG, "[REQ] requestPermission() failed: ${e.message}")
            updateState(PermissionState.DENIED, "Failed to request permission: ${e.message}")
            listener?.onPermissionDenied(device)
        }
    }
    
    fun getState(): PermissionState = permissionState
    
    fun isGranted(): Boolean = permissionState == PermissionState.GRANTED
    
    private fun updateState(newState: PermissionState, message: String) {
        val oldState = permissionState
        permissionState = newState
        
        Log.i(TAG, "[STATE] $oldState → $newState: $message")
        listener?.onPermissionStateChanged(newState, message)
    }
}
