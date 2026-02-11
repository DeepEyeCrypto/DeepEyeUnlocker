package com.deepeye.otg

/**
 * Explicit USB OTG connection state machine.
 * Prevents "Native Core Offline" errors by tracking lifecycle.
 */
enum class ConnectionState {
    /** No device detected or waiting for hotplug event */
    DISCONNECTED,
    
    /** USB device enumerated but no permission granted yet */
    DEVICE_FOUND,
    
    /** requestPermission() called, awaiting user approval */
    PERMISSION_PENDING,
    
    /** Permission granted, UsbDeviceConnection acquired, FD available */
    USB_OPEN,
    
    /** Native core initialization in progress */
    NATIVE_INITIALIZING,
    
    /** Native core ready, all operations allowed */
    CONNECTED,
    
    /** Initialization failed or device detached with error */
    ERROR;
    
    /**
     * Check if operations should be allowed in this state.
     */
    fun canExecuteOperations(): Boolean {
        return this == CONNECTED
    }
    
    /**
     * Get user-facing status badge text.
     */
    fun getBadgeText(): String {
        return when (this) {
            DISCONNECTED -> "● OFFLINE"
            DEVICE_FOUND -> "● DETECTED"
            PERMISSION_PENDING -> "● WAITING..."
            USB_OPEN -> "● OPENING..."
            NATIVE_INITIALIZING -> "● INIT..."
            CONNECTED -> "● READY"
            ERROR -> "● ERROR"
        }
    }
    
    /**
     * Get status badge color resource ID.
     */
    fun getBadgeColorRes(): Int {
        return when (this) {
            DISCONNECTED -> R.color.deepeye_error
            DEVICE_FOUND -> R.color.deepeye_warning
            PERMISSION_PENDING -> R.color.deepeye_warning
            USB_OPEN -> R.color.deepeye_warning
            NATIVE_INITIALIZING -> R.color.deepeye_cyan
            CONNECTED -> R.color.deepeye_success
            ERROR -> R.color.deepeye_error
        }
    }
}
