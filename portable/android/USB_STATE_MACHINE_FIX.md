# DeepEyeUnlocker Android - USB State Machine Diagnostic Report

## State Flow Diagram

```text
┌─────────────────┐
│  DISCONNECTED   │  (Initial state, no device)
└────────┬────────┘
         │ USB hotplug event
         ▼
┌─────────────────┐
│  DEVICE_FOUND   │  (Enumerated, no permission)
└────────┬────────┘
         │ requestPermission() called
         ▼
┌─────────────────┐
│PERMISSION_PENDING│ (Waiting for user approval)
└────────┬────────┘
         │ Permission granted
         ▼
┌─────────────────┐
│    USB_OPEN     │  (FD acquired from UsbDeviceConnection)
└────────┬────────┘
         │ NativeBridge.initCore() called
         ▼
┌─────────────────┐
│NATIVE_INITIALIZING│ (JNI init in progress)
└────────┬────────┘
         │ initCore OK + identifyDevice OK
         ▼
┌─────────────────┐
│    CONNECTED    │  ✅ OPERATIONS ALLOWED
└─────────────────┘

ERROR state can be reached from any state on failure.
```

## Fix Summary

### Problem

The app was checking `nativeHandle == 0L` **synchronously** in button handlers while native initialization was happening **asynchronously** in a background callback, creating a race condition where:

1. Device is detected → UI shows "ATTACHED"
2. User taps button → check fails because init hasn't completed
3. "Native Core Offline" error shown

### Solution

1. **State Machine**: Explicit `ConnectionState` enum tracks lifecycle
2. **Thread-Safe Checks**: Buttons check `connectionState.canExecuteOperations()` under lock
3. **UI Feedback**: Status badge shows exact state ("DETECTED", "OPENING...", "INIT...", "READY")
4. **Async Init**: Native core init moved to background thread with proper callbacks
5. **Error Paths**: All failure modes update state to ERROR with specific messages

### State Transitions in Code

- `onDeviceAttached` → DEVICE_FOUND
- `handleDevice` (has permission) → USB_OPEN
- `initializeCore` starts → NATIVE_INITIALIZING
- `NativeBridge.initCore` success + `identifyDevice` success → CONNECTED
- Any failure → ERROR

### Log Prefix Convention

- `[STATE]` - State machine transitions
- `[OTG-JAVA]` - Java/Kotlin USB layer
- `[OTG-NATIVE]` - JNI boundary
- `[OTG-OP]` - Operation execution
- `[EXEC]` - Actual operation call

## Testing Checklist

- [ ] Plug device → See "DETECTED" → "OPENING..." → "INIT..." → "READY"
- [ ] Tap button before READY → Get specific error (not generic "offline")
- [ ] Tap button when READY → Operation executes
- [ ] Unplug during init → State goes to ERROR
- [ ] Re-plug → State resets through full flow
- [ ] Permission denial → State goes to ERROR with clear message
