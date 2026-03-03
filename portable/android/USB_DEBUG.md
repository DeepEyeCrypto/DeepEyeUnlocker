# DeepEye OTG USB Debug Guide

## State Machine (Java/Kotlin Layer)

### States

| State | Badge | Description |
|-------|-------|-------------|
| `DISCONNECTED` | ● OFFLINE | No device detected |
| `DEVICE_FOUND` | ● DETECTED | USB device enumerated, checking permission |
| `PERMISSION_PENDING` | ● WAITING... | `requestPermission()` called, user dialog showing |
| `PERMISSION_DENIED` | ● DENIED | User denied or system blocked permission |
| `USB_OPEN` | ● OPENING... | Permission granted, `openDevice()` succeeded |
| `CONNECTED_PROTOCOL_DETECT` | ● PROBING... | Inspecting interfaces and probing protocol |
| `NATIVE_INITIALIZING` | ● INIT... | Native core handshake in progress |
| `CONNECTED` | ● READY | All operations allowed |
| `CONNECTED_MTP_ONLY` | ● MTP ONLY | Only MTP/MSC interfaces found, read-only |
| `ERROR` | ● ERROR | Operation or connection failed |

### Transition Table

```
DISCONNECTED      ──(USB_ATTACH)──→     DEVICE_FOUND
DEVICE_FOUND      ──(HAS_PERMISSION)──→ USB_OPEN
DEVICE_FOUND      ──(REQ_PERMISSION)──→ PERMISSION_PENDING
PERMISSION_PENDING──(PERM_GRANTED)───→  USB_OPEN
PERMISSION_PENDING──(PERM_DENIED)────→  PERMISSION_DENIED
PERMISSION_DENIED ──(USB_ATTACH/retry)→ DEVICE_FOUND
USB_OPEN          ──(PROBE_START)────→  CONNECTED_PROTOCOL_DETECT
PROTOCOL_DETECT   ──(PROTO_KNOWN)────→  NATIVE_INITIALIZING
PROTOCOL_DETECT   ──(PROTO_MTP)──────→  CONNECTED_MTP_ONLY
PROTOCOL_DETECT   ──(PROTO_UNKNOWN)──→  ERROR
NATIVE_INIT       ──(HANDSHAKE_OK)───→  CONNECTED
NATIVE_INIT       ──(HANDSHAKE_FAIL)─→  ERROR
ANY               ──(USB_DETACH)─────→  DISCONNECTED
ANY               ──(SECURITY_EX)────→  ERROR
ERROR/CONNECTED   ──(RE-ENUMERATION)─→  DEVICE_FOUND
```

### Critical Rule — `canTransitionTo()`
**Never transition from a post-open state (USB_OPEN, CONNECTED_*, NATIVE_INITIALIZING) back to PERMISSION_PENDING.**
The `ConnectionState.canTransitionTo(target)` method validates every transition before it is applied. Illegal transitions are logged and silently dropped. SecurityExceptions after `openDevice()` go to ERROR or trigger re-enumeration flow.

### Connection Flow

1. Hotplug (`ACTION_USB_DEVICE_ATTACHED`) → `DEVICE_FOUND`
2. If `hasPermission=false` → request permission (explicit broadcast intent); `PERMISSION_PENDING`
3. Broadcast `EXTRA_PERMISSION_GRANTED=true` → `USB_OPEN` (openDevice) → probe protocol → `CONNECTED_PROTOCOL_DETECT`
4. Protocol classification (6-phase):
   - **Phase 1 — VID/PID fast-path**: Known chipset signatures (Qualcomm 05C6:9008, MTK 0E8D:0003/2000, Samsung 04E8:6601, Google ADB/Fastboot, Xiaomi 2717)
   - **Phase 2 — Interface heuristics**: ADB (class=0xFF, sub=0x42, proto=0x01), Fastboot (proto=0x03)
   - **Phase 3 — Bulk endpoint check**: Non-MTP interfaces with both bulk IN+OUT
   - **Phase 4 — Active probes**: Fastboot ASCII, MTK sync, Sahara hello (500ms timeouts)
   - **Phase 5 — MTP fallback**: All interfaces class=0x06 (Still-Image) or 0x08 (Mass Storage)
   - **Phase 6 — UNKNOWN**: Nothing matched
5. Known diag/flash protocol → native init → `CONNECTED`
6. MTP/MSC-only → `CONNECTED_MTP_ONLY` (no retries, guidance banner shown)
7. Unknown → guidance dialog, `ERROR`
8. Detach → `DISCONNECTED`, release wakelock, close connection.

## Re-enumeration Handling

When a device re-enumerates (same VID/PID but different `deviceId`):
1. Old `UsbDeviceConnection` is closed immediately via `currentConnection?.close()`
2. State resets to `DISCONNECTED` → `DEVICE_FOUND`
3. Permission is re-requested for the new `UsbDevice` instance
4. Error throttle counter resets so next attempt starts fresh
5. Log: `[USB] Re-enumeration detected: oldId=X newId=Y`

## Permission Handling

- PendingIntent explicit to app package (`setPackage`), with `FLAG_MUTABLE | FLAG_UPDATE_CURRENT` on API 31+
- Single `BroadcastReceiver` registered once in `UsbPermissionManager.register()` and unregistered on destroy
- `PermissionState` is set **only** from:
  - `usbManager.hasPermission()` check at device discovery
  - `EXTRA_PERMISSION_GRANTED` in broadcast receiver
- Before `openDevice()`, permission is re-verified; if lost (re-enumeration), re-request is triggered instead of throwing SecurityException

## Error Throttling

- Same error message repeated more than **3 times** within 5 seconds is suppressed (`ERROR_THROTTLE_MAX=3`)
- Counter (`lastErrorCount`) and window reset on device detach so next connection starts fresh
- SecurityExceptions are handled as re-enumeration events, not "Permission Denied" errors

## Protocol Detector (ProtocolProbe)

### Supported Protocols

| Enum | VID/PID Example | Detection Method |
|------|-----------------|------------------|
| `QUALCOMM_EDL` | 05C6:9008 | VID/PID fast-path |
| `MTK_BROM` | 0E8D:0003/2000/2001 | VID/PID + active probe (sync bytes) |
| `FASTBOOT` | 18D1:4EE0/D00D | VID/PID + interface class + ASCII probe |
| `SAMSUNG_ODIN` | 04E8:6601/685D | VID/PID match |
| `ADB` | 18D1:4EE2 / 2717:FF48 | Interface heuristic (class=0xFF, sub=0x42, proto=0x01) |
| `MTP_ONLY` | Various | All interfaces Still-Image (0x06) / Mass Storage (0x08) |
| `UNKNOWN` | — | No match after all phases |

### Detection Details

- Dumps every interface with hex-formatted class/subclass/protocol codes and endpoint details
- VID/PID fast-path for known chipset signatures (Qualcomm, MTK, Samsung, Google ADB/Fastboot, Xiaomi)
- ADB/Fastboot interface descriptor matching (class=0xFF, sub=0x42, proto=0x01/0x03)
- `claimInterfaceIndex` returned in `ProtocolDetectionResult` for downstream use
- Active probes with **500ms timeout** (increased from 150-200ms for slow USB controllers):
  - Fastboot: send `getvar:version`, expect `OKAY/FAIL`
  - MTK BROM: send `A0 0A 50 05`, check for `0x5F` echo
  - Qualcomm Sahara: read bulk IN for cmd `0x01` HELLO
- MTP classification: **all** interfaces are Still-Image (0x06) or Mass Storage (0x08) → `MTP_ONLY`
- Non-MTP interfaces are preferred when selecting bulk endpoints for probing

## UX Guidance

- `CONNECTED_MTP_ONLY` → sticky banner with checklist:
  - Switch USB mode to File Transfer/MTP
  - Enable USB Debugging in Developer Options
  - Xiaomi: Enable "USB debugging (Security settings)" in Developer Options
  - Samsung: Enable "OEM Unlock" in Developer Options
  - Boot into Download/EDL/Fastboot mode for flash operations
- `ADB` protocol detected → warning that bootloader/download mode needed for flash operations
- `UNKNOWN` protocol → dialog with chipset-specific mode switch instructions
- `PERMISSION_DENIED` → clear message, prompt to re-plug
- Re-enumeration → informational log, no error spam to user

## USB Diagnostic Self-Test

Long-press the connection indicator badge (top-right) to trigger the USB Diagnostic:
- Lists all connected USB devices
- Shows VID/PID, name, deviceId
- Shows `hasPermission` status for each
- Shows interface class/subclass/protocol breakdown for every interface
- Shows current connection state
- Results are displayed in a dialog and can be copied to clipboard for support

## Repro/Debug Checklist

### 1) Permission Granted Flow
- Plug device, approve prompt.
- Expect: `DEVICE_FOUND → PERMISSION_PENDING → USB_OPEN → CONNECTED_PROTOCOL_DETECT → CONNECTED`
- **Verify**: No fallback to PERMISSION_PENDING after open.

### 2) Permission Denied Flow
- Tap "Deny". Expect `PERMISSION_DENIED`, no `openDevice()` call.
- Re-plug to retry: `PERMISSION_DENIED → DEVICE_FOUND → PERMISSION_PENDING`.

### 3) MTP-Only Device
- Connect phone in MTP/Charging mode.
- Expect: `CONNECTED_MTP_ONLY`, banner shown, no permission spam, operations disabled.

### 4) Re-enumeration
- Switch device USB mode (e.g., charging→MTP).
- Expect: old connection closed, `DISCONNECTED → DEVICE_FOUND`, permission re-requested with clear log.
- **Verify**: No "USB Permission Denied by System" error.

### 5) Error Spam Throttle
- Repeated failures within 5s should not spam user/UI (max 3 within window).
- Counter resets on detach.

### 6) ADB Device Detection
- Connect phone with USB Debugging enabled (normal Android mode).
- Expect: `CONNECTED_PROTOCOL_DETECT` → ADB detected → warning banner about bootloader mode.
- **Verify**: Not classified as UNKNOWN.

## Key Log Tags

| Tag | Subsystem | Examples |
|-----|-----------|---------|
| `[USB]` | Attach/detach/open/close | `[USB] Device Attached (2717:FF48)` |
| `[PERM]` | Permission lifecycle | `[PERM] Permission GRANTED - Opening device...` |
| `[PROTO]` | Interface dump + classification | `[PROTO] VID/PID match → QUALCOMM_EDL` |
| `[STATE]` | State transitions | `[STATE] DEVICE_FOUND → PERMISSION_PENDING` |
| `[UX]` | Banner/guidance display | `[UX] Guidance shown for Xiaomi Redmi Note 12` |
| `[BROADCAST]` | Permission broadcast events | `[BROADCAST] Permission granted: true` |
