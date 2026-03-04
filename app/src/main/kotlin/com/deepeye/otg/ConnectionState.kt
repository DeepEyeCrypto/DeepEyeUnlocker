package com.deepeye.otg

/**
 * Explicit USB OTG connection state machine.
 * Prevents "Native Core Offline" errors by tracking lifecycle.
 *
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │                    STATE TRANSITION TABLE                           │
 * ├────────────────────────┬───────────────────┬───────────────────────┤
 * │ Current State          │ Event             │ Next State            │
 * ├────────────────────────┼───────────────────┼───────────────────────┤
 * │ DISCONNECTED           │ USB_ATTACH        │ DEVICE_FOUND          │
 * │ DEVICE_FOUND           │ HAS_PERMISSION    │ USB_OPEN              │
 * │ DEVICE_FOUND           │ REQ_PERMISSION    │ PERMISSION_PENDING    │
 * │ PERMISSION_PENDING     │ PERM_GRANTED      │ USB_OPEN              │
 * │ PERMISSION_PENDING     │ PERM_DENIED       │ PERMISSION_DENIED     │
 * │ PERMISSION_DENIED      │ USB_ATTACH(retry) │ DEVICE_FOUND          │
 * │ USB_OPEN               │ PROBE_START       │ CONNECTED_PROTO_DETECT│
 * │ CONNECTED_PROTO_DETECT │ PROTO_KNOWN       │ NATIVE_INITIALIZING   │
 * │ CONNECTED_PROTO_DETECT │ PROTO_MTP         │ CONNECTED_MTP_ONLY    │
 * │ CONNECTED_PROTO_DETECT │ PROTO_UNKNOWN     │ ERROR                 │
 * │ NATIVE_INITIALIZING    │ HANDSHAKE_OK      │ CONNECTED             │
 * │ NATIVE_INITIALIZING    │ HANDSHAKE_FAIL    │ ERROR                 │
 * │ CONNECTED_MTP_ONLY     │ USB_DETACH        │ DISCONNECTED          │
 * │ CONNECTED              │ USB_DETACH        │ DISCONNECTED          │
 * │ ERROR                  │ USB_DETACH        │ DISCONNECTED          │
 * │ ERROR                  │ USB_ATTACH(retry) │ DEVICE_FOUND          │
 * │ ANY                    │ USB_DETACH        │ DISCONNECTED          │
 * │ ANY                    │ SECURITY_EX       │ ERROR                 │
 * └────────────────────────┴───────────────────┴───────────────────────┘
 *
 * IMPORTANT: Never transition from a post-open state (USB_OPEN, CONNECTED_*,
 * NATIVE_INITIALIZING, CONNECTED) back to PERMISSION_PENDING.
 * SecurityExceptions after open should go to ERROR or trigger re-enumeration.
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
     * Validate whether a transition from this state to [target] is allowed.
     * Returns true if allowed, false if the transition would violate the state machine contract.
     */
    fun canTransitionTo(target: ConnectionState): Boolean {
        // Universal: any state can go to DISCONNECTED (detach) or ERROR
        if (target == DISCONNECTED || target == ERROR) return true

        return when (this) {
            DISCONNECTED -> target == DEVICE_FOUND
            DEVICE_FOUND -> target in setOf(PERMISSION_PENDING, USB_OPEN)
            PERMISSION_PENDING -> target in setOf(USB_OPEN, PERMISSION_DENIED, DEVICE_FOUND)
            PERMISSION_DENIED -> target == DEVICE_FOUND
            USB_OPEN -> target == CONNECTED_PROTOCOL_DETECT
            CONNECTED_PROTOCOL_DETECT -> target in setOf(NATIVE_INITIALIZING, CONNECTED_MTP_ONLY)
            NATIVE_INITIALIZING -> target == CONNECTED
            CONNECTED -> target == DEVICE_FOUND // re-enumeration
            CONNECTED_MTP_ONLY -> target == DEVICE_FOUND // re-enumeration
            ERROR -> target == DEVICE_FOUND // retry on re-plug
        }
    }
    
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
