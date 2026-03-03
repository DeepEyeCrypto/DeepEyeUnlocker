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

    /** User explicitly denied permission */
    PERMISSION_DENIED,

    /** Permission granted, UsbDeviceConnection acquired, FD available */
    USB_OPEN,

    /** Protocol detection / descriptor inspection phase */
    CONNECTED_PROTOCOL_DETECT,

    /** Native core initialization in progress */
    NATIVE_INITIALIZING,

    /** Native core ready, all operations allowed */
    CONNECTED,

    /** Connected but only MTP/MSC style interfaces are present (read-only UX) */
    CONNECTED_MTP_ONLY,

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
            PERMISSION_DENIED -> "● DENIED"
            USB_OPEN -> "● OPENING..."
            CONNECTED_PROTOCOL_DETECT -> "● PROBING..."
            NATIVE_INITIALIZING -> "● INIT..."
            CONNECTED -> "● READY"
            CONNECTED_MTP_ONLY -> "● MTP ONLY"
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
            PERMISSION_DENIED -> R.color.deepeye_error
            USB_OPEN -> R.color.deepeye_warning
            CONNECTED_PROTOCOL_DETECT -> R.color.deepeye_cyan
            NATIVE_INITIALIZING -> R.color.deepeye_cyan
            CONNECTED -> R.color.deepeye_success
            CONNECTED_MTP_ONLY -> R.color.deepeye_warning
            ERROR -> R.color.deepeye_error
        }
    }
}
