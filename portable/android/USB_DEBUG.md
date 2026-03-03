# DeepEye OTG USB Debug Guide

## State Machine (Java/Kotlin Layer)

States:
- `DISCONNECTED` → `DEVICE_FOUND` → `PERMISSION_PENDING` → `PERMISSION_DENIED` | `USB_OPEN` → `CONNECTED_PROTOCOL_DETECT` → `CONNECTED` (ready) or `CONNECTED_MTP_ONLY`
- `NATIVE_INITIALIZING` between `USB_OPEN` and `CONNECTED`
- `ERROR` reachable from any state on failure; `DISCONNECTED` on detach.

High-level flow:
1. Hotplug (`ACTION_USB_DEVICE_ATTACHED`) → `DEVICE_FOUND`
2. If `hasPermission=false` → request permission (explicit broadcast intent); `PERMISSION_PENDING`
3. Broadcast `EXTRA_PERMISSION_GRANTED=true` → `USB_OPEN` (openDevice) → probe protocol → `CONNECTED_PROTOCOL_DETECT`
4. Protocol classification:
   - Known diag/flash (FASTBOOT / MTK_BROM / QUALCOMM_EDL / etc.) → proceed to native init → `CONNECTED`
   - MTP/MSC-only → `CONNECTED_MTP_ONLY` (no retries, guidance banner)
   - Unknown → guidance dialog, remains `ERROR`
5. Detach → `DISCONNECTED`, release wakelock.

Re-enumeration handling:
- If same VID/PID but different `deviceId`, treat as detach+attach: close old, re-request permission.

Permission handling:
- PendingIntent explicit to app package, `FLAG_MUTABLE | FLAG_UPDATE_CURRENT` on API 31+, single receiver registered once in `UsbPermissionManager`.
- `PermissionState` is set only from `usbManager.hasPermission()` and broadcast `EXTRA_PERMISSION_GRANTED`.

Error throttling:
- Same USB error is suppressed if repeated within 5s.

## Protocol Detector (ProtocolProbe)

- Dumps every interface: class/subclass/proto and endpoints.
- If no bulk IN+OUT and interfaces are still-image/mass-storage only → `MTP_ONLY`.
- Fastboot probe: send `getvar:version`, expect `OKAY/FAIL`.
- MTK probe: send `A0 0A 50 05`, check first byte response.
- Qualcomm Sahara probe: read bulk IN, look for cmd `0x01` HELLO.
- Otherwise `UNKNOWN`.

## UX Guidance

- `CONNECTED_MTP_ONLY` shows sticky banner: set phone USB mode to File Transfer/MTP, enable USB debugging (and vendor-specific security toggle like Xiaomi “USB debugging (security)”).
- Permission denied → state `PERMISSION_DENIED`, prompt user to re-plug and approve.

## Repro/Debug Checklist

1) Permission granted flow
- Plug device, approve prompt.
- Expect: DEVICE_FOUND → PERMISSION_PENDING → USB_OPEN → CONNECTED_PROTOCOL_DETECT → CONNECTED.
- No fallback to PERMISSION_PENDING after open.

2) Permission denied flow
- Tap “Deny”. Expect PERMISSION_DENIED, no openDevice call.

3) MTP-only device
- Connect phone in MTP/charging. Expect CONNECTED_MTP_ONLY, banner shown, no permission spam.

4) Re-enumeration
- Switch device mode (e.g., charging→MTP). Expect detach+attach handling, permission re-request with clear log.

5) Error spam throttle
- Repeated failures within 5s should not spam user/UI.

## Key Log Tags
- `[USB]` attach/detach/open
- `[PERM]` permission lifecycle
- `[PROTO]` interface dump + classification
- `[STATE]` state transitions
- `[UX]` banner/guidance (future)
