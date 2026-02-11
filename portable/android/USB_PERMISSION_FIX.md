# USB Permission Broadcast Fix - v5.2.2

## Symptom

On Android 12+ (API 31+), the app would log:

1. `Requesting USB permission...`
2. `[STATE] DEVICE_FOUND → PERMISSION_PENDING`
3. Then **nothing**. No `GRANTED` or `DENIED` broadcast was ever received, even if the user tapped "Allow".

## Root Cause

The `PendingIntent` used for the permission request was created with `FLAG_IMMUTABLE` only. On newer Android versions, when the system tries to fill in the `EXTRA_DEVICE` and `EXTRA_PERMISSION_GRANTED` extras into an immutable PendingIntent, it may fail silently or simply not deliver the modified intent back to the app because the intent is immutable.

Additionally, to ensure security and proper delivery, the Intent should be **explicit** (targeting our own package).

## The Fix (v5.2.2)

Updated `UsbPermissionManager.kt`:

```kotlin
// Before (Broken on Android 12+ for system callbacks):
val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
val intent = Intent(ACTION_USB_PERMISSION)

// After (Fixed):
val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    // Mutable is required so system can add extras (granted status, device)
    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
} else {
    PendingIntent.FLAG_UPDATE_CURRENT
}

// Explicit intent ensures delivery to our app's receiver
val intent = Intent(ACTION_USB_PERMISSION).apply {
    setPackage(context.packageName)
}
```

## Verification Steps

1. Install **v5.2.2**.
2. Plug in USB device.
3. Tap "Allow" on permission dialog.
4. Verify logcat shows:
   - `[BROADCAST] Permission granted: true`
   - `[STATE] USB_OPEN` → `CONNECTED`

## References

- [Android PendingIntent Mutability](https://developer.android.com/about/versions/12/behavior-changes-12#pending-intent-mutability)
- [StackOverflow: USB Permission Broadcast not received](https://stackoverflow.com/questions/73850448)
