# DeepEyeUnlocker v5.5.0 - "The OTG Overhaul" 🔌

**Release Date:** 2026-03-03  
**Focus:** Complete USB OTG permission, protocol detection, and state machine overhaul

---

## 💎 Key Highlights

- **USB Permission Fix:** Eliminated the "USB Permission Denied by System" false-positive that occurred when devices re-enumerate after `openDevice()`. Permission is now re-verified before every open call.
- **Protocol Detector Rewrite:** 6-phase detection pipeline with VID/PID fast-path lookup for Qualcomm, MTK, Samsung, Google, and Xiaomi chipsets. ADB and Fastboot interfaces are now recognized by descriptor fingerprints.
- **State Machine Hardening:** New `canTransitionTo()` validation prevents illegal state regressions (e.g., CONNECTED → PERMISSION_PENDING). Every transition is validated before application.
- **USB Diagnostic Self-Test:** Long-press the connection indicator to enumerate all connected USB devices with VID/PID, permission status, and interface breakdown — invaluable for field support.
- **16 Regression Tests:** Comprehensive unit test suite covering permission flow, state machine transitions, protocol enum completeness, and error handling.

## 🔧 Technical Changes

### ConnectionState.kt
- Added comprehensive transition table documentation (ASCII art)
- New `canTransitionTo(target)` method validates every state transition
- Blocks illegal post-open → PERMISSION_PENDING regressions
- Universal escapes to DISCONNECTED and ERROR always permitted

### UsbHostManager.kt
- Added `currentConnection: UsbDeviceConnection?` field for proper cleanup on re-enumeration/detach
- `handleDetach()` now closes active connection and resets error throttle counter
- `openAndPassFd()` re-checks `hasPermission()` before open, re-requests if lost
- SecurityException handler no longer emits misleading "USB Permission Denied by System"
- Error throttle upgraded: counter with `ERROR_THROTTLE_MAX=3` within 5s window

### ProtocolProbe.kt — Complete Rewrite
- Added `ADB` to `DetectedProtocol` enum
- Added `claimInterfaceIndex` to `ProtocolDetectionResult`
- VID/PID fast-path lookup table:
  - Qualcomm EDL: `05C6:9008`
  - MTK BROM: `0E8D:0003/2000/2001`
  - Samsung Odin: `04E8:6601/685D`
  - Google ADB: `18D1:4EE2`, Fastboot: `18D1:4EE0/D00D`
  - Xiaomi: `2717:*`
- Interface descriptor heuristics (ADB: class=0xFF/sub=0x42/proto=0x01, Fastboot: proto=0x03)
- Non-MTP interfaces preferred for bulk endpoint selection
- Probe timeouts increased from 150-200ms to 500ms for slow USB controllers
- Rich hex-formatted interface dump logging

### OtgActivity.kt
- `updateConnectionState()` validates transitions via `canTransitionTo()`, blocks illegal ones
- ADB protocol handler with bootloader-mode warning
- MTP banner expanded with brand-specific checklist (Xiaomi/Samsung)
- USB Diagnostic self-test via long-press on connection indicator
- Re-enumeration messages filtered to avoid spamming ERROR state

### UsbHostManagerTest.kt — 16 Tests
- Permission granted flow (6 transitions)
- No back-transition to PERMISSION_PENDING after open
- Permission denied flow
- MTP-only behavior
- Re-enumeration transitions
- Error handling
- Badge text validation
- Protocol enum completeness

### USB_DEBUG.md — Complete Rewrite
- Full state table with badge descriptions
- ASCII transition diagram
- 6-phase protocol detection pipeline documentation
- Supported protocols table (VID/PID, detection method)
- USB Diagnostic self-test usage guide
- 6 repro/debug checklist scenarios
- Log tag reference table

### Infrastructure
- `.gitignore`: Added `dotnet-sdk/`, `*.tsbuildinfo`, `*.db`, `vite.config.d.ts`
- Removed 707 accidentally tracked `dotnet-sdk/` files from version control

## 🐛 Bugs Fixed

| Bug | Root Cause | Fix |
|-----|-----------|-----|
| "Unknown Protocol" after permission grant | Protocol detector had no VID/PID lookup; vendor-spec interfaces (0xFF) fell through to UNKNOWN | VID/PID fast-path + ADB interface fingerprint matching |
| "USB Permission Denied by System" loop | SecurityException on re-enumerated device triggered misleading error message | Re-verify permission before open; SecurityException → re-enumeration flow |
| Infinite "Try re-plugging" spam | Error throttle only tracked time window, not repeat count | Added `lastErrorCount` counter with `ERROR_THROTTLE_MAX=3` |
| State bounces to PERMISSION_PENDING | No transition validation; re-enum triggered permission request from connected state | `canTransitionTo()` blocks illegal regressions |

## 📂 Artifact Details

- **Tag**: `v5.5.0`
- **Desktop Core**: `src/DeepEyeUnlocker.csproj` → `5.5.0`
- **Modern UI**: `DeepEye.UI.Modern/DeepEye.UI.Modern.csproj` → `5.5.0`
- **Android OTG**: `portable/android/app/build.gradle` → `versionName "5.5.0"`, `versionCode 550`

## ⬆️ Upgrade Notes

- No breaking API changes
- Android OTG app requires reinstall for updated protocol detection
- Users experiencing "Unknown Protocol" errors should update to this version

---
*Democratizing Mobile Repair & Security Tools.*
