# USB Permission Flow Fix - v5.2.1

## Problem Summary

**Symptom**: App stuck in infinite loop:

```
[STATE] DISCONNECTED → DEVICE_FOUND
Requesting USB permission...
Cannot execute: Waiting for USB permission...
```

**Root Causes Identified**:

1. ❌ **PendingIntent flags incomplete** (Line 91 in old UsbHostManager.kt)
   - Was: `PendingIntent.FLAG_IMMUTABLE` only
   - Fix: `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT` (Android 12+)
   - Result: System couldn't deliver permission result back to app

2. ❌ **No permission denial handling**
   - Broadcast receiver only handled grant case (line 36-38)
   - No `else` branch to handle user denying permission
   - Result: App stuck in REQUESTING state forever

3. ❌ **Silent permission broadcast**
   - No logging in permission granted path
   - Impossible to debug if broadcast was firing

4. ❌ **String comparison using `==`**
   - Should use `.equals()` for action comparison
   - Potential cause of missed broadcasts

## Solution Architecture

### New Files Created

#### 1. `UsbPermissionManager.kt`

- **Responsibility**: Dedicated permission state machine
- **Features**:
  - Explicit `PermissionState` enum (NONE, REQUESTING, GRANTED, DENIED)
  - Comprehensive logging at every step
  - Proper PendingIntent flags for Android 12+
  - Handles both grant AND denial cases
  - Device identity verification

#### 2. Refactored `UsbHostManager.kt`

- **Changes**:
  - Delegates all permission logic to `UsbPermissionManager`  
  - Registers permission callbacks
  - Cleaner separation: hotplug vs permission

### Permission Flow (Fixed)

```text
┌──────────────────────────────────────────┐
│ 1. Device Detected (scanExistingDevices)│
└────────────────┬─────────────────────────┘
                 ▼
         ┌───────────────┐
         │hasPermission?│
         └───────┬───────┘
            NO   │   YES
       ┌─────────┴──────────┐
       ▼                    ▼
┌────────────────┐   ┌──────────────┐
│requestPermission│   │openAndPassFd│
└────────┬────────┘   └──────────────┘
         ▼
┌─────────────────────────┐
│PermissionState.REQUESTING│
│  (Show system dialog)    │
└────────┬─────────────────┘
         ▼
┌──────────────────────┐
│ USER TAPS BUTTON     │
│ "Allow" OR "Deny"    │
└──────┬──────────┬────┘
    ALLOW       DENY
       │          │
       ▼          ▼
┌──────────┐  ┌────────┐
│ GRANTED  │  │ DENIED │
└────┬─────┘  └────┬───┘
     ▼             ▼
┌──────────┐  ┌─────────┐
│openDevice│  │ ERROR   │
│ + notify │  │ state   │
└──────────┘  └─────────┘
```

### State Transitions in v5.2.1

| Event | Old State | New State | Log Message |
|-------|-----------|-----------|-------------|
| Device detected | DISCONNECTED | DEVICE_FOUND | "Device detected: RMX3945..." |
| No permission | DEVICE_FOUND | PERMISSION_PENDING | "[STATE] DEVICE_FOUND → PERMISSION_PENDING: Requesting USB permission from user..." |
| User taps "Allow" | PERMISSION_PENDING | USB_OPEN | "[PERM-GRANTED] Opening device: 8921:10089" |
| openDevice succeeds | USB_OPEN | NATIVE_INITIALIZING | "[STATE] USB_OPEN → NATIVE_INITIALIZING..." |
| Native init OK | NATIVE_INITIALIZING | CONNECTED | "[STATE] NATIVE_INITIALIZING → CONNECTED: Handshake OK" |
| User taps "Deny" | PERMISSION_PENDING | ERROR | "USB Permission DENIED by user" |

## Key Code Changes

### UsbPermissionManager.requestPermission()

**Before (Broken)**:

```kotlin
val permissionIntent = PendingIntent.getBroadcast(
    context, 0, Intent(ACTION_USB_PERMISSION), 
    PendingIntent.FLAG_IMMUTABLE  // ❌ Incomplete!
)
```

**After (Fixed)**:

```kotlin
val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT  // ✅
} else {
    PendingIntent.FLAG_UPDATE_CURRENT
}

val permissionIntent = PendingIntent.getBroadcast(
    context, 0, Intent(ACTION_USB_PERMISSION), flags
)
```

### Permission Broadcast Receiver

**Before (Silent)**:

```kotlin
if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
    device?.apply { openAndPassFd(this) }  // ❌ No logging, no denial handling
}
```

**After (Loud & Clear)**:

```kotlin
val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)

Log.i(TAG, "[BROADCAST] Permission granted: $granted")

if (granted) {
    Log.i(TAG, "[PERM-GRANTED] Opening device...")
    listener?.onPermissionGranted(device)
} else {
    Log.w(TAG, "[PERM-DENIED] User denied permission")
    listener?.onPermissionDenied(device)
}
```

## Testing Checklist

### Unit Tests (Simulated)

- [ ] `requestPermission()` sets state to REQUESTING
- [ ] Broadcast with `granted=true` → GRANTED state + `onPermissionGranted` called
- [ ] Broadcast with `granted=false` → DENIED state + `onPermissionDenied` called
- [ ] Broadcast for wrong device → ignored
- [ ] Already has permission → skip dialog, immediate GRANTED

### Manual Testing (Real Device)

1. **Clean Install Flow**:

   ```
   adb install -r app-release.apk
   adb logcat | grep -E "(DeepEye-OTG|DeepEye-Permission)"
   ```

2. **Expected Logs (Success)**:

   ```
   [DeepEye-Permission] [INIT] Permission receiver registered
   [DeepEye-OTG] Scanning existing USB devices...
   [DeepEye-OTG] Found: 8921:10089
   [STATE] DISCONNECTED → DEVICE_FOUND: Device detected: RMX3945 (8921:10089)
   [DeepEye-Permission] [REQ] Requesting permission for... 
   [STATE] DEVICE_FOUND → PERMISSION_PENDING: Requesting USB permission from user...
   
   (User taps "Allow")
   
   [DeepEye-Permission] [BROADCAST] onReceive: action=com.deepeye.otg.USB_PERMISSION
   [DeepEye-Permission] [BROADCAST] Permission granted: true
   [DeepEye-Permission] [STATE] REQUESTING → GRANTED: USB permission GRANTED by user
   [DeepEye-OTG] [PERM-GRANTED] Opening device: 8921:10089
   [DeepEye-OTG] Direct Link Established. FD=23
   [STATE] USB_OPEN → NATIVE_INITIALIZING...
   ```

3. **Expected Logs (Denial)**:

   ```
   (User taps "Deny")
   
   [DeepEye-Permission] [BROADCAST] Permission granted: false
   [DeepEye-Permission] [STATE] REQUESTING → DENIED: USB permission DENIED by user
   [STATE] PERMISSION_PENDING → ERROR: USB Permission DENIED by user
   ```

4. **Button Behavior**:
   - ✅ Before permission granted: "Core is initializing, wait..." or "Permission pending, please approve."
   - ✅ After DENIED: "Connection error. Try re-plugging device."
   - ✅ After GRANTED + init complete: Operation executes

## Regression Prevention

### Pre-Commit Checks

- Verify PendingIntent flags include both IMMUTABLE and UPDATE_CURRENT
- Ensure all broadcast receivers have comprehensive logging
- Check that state machines handle ALL enum cases (not just happy path)

### Code Review Checklist

- [ ] Permission requests use `UsbPermissionManager`
- [ ] No direct calls to `usbManager.requestPermission()` outside permission manager
- [ ] Both grant and denial cases handled
- [ ] State transitions logged with `[STATE]` prefix
- [ ] Device identity verified in broadcasts

## Version History

- **v5.2.0**: Initial state machine implementation
- **v5.2.1**: Permission flow fix with dedicated UsbPermissionManager
