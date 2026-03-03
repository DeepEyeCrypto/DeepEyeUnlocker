# DeepEyeUnlocker v5.5.0 Post-Release Summary

**Generated**: March 3, 2026  
**Release Date**: March 3, 2026  
**Tag**: `v5.5.0`  
**Status**: 🚀 **RELEASED**

---

## ✅ Changes Summary

### USB OTG Overhaul (6 files changed, ~800 lines)

| File | Change |
|------|--------|
| `ConnectionState.kt` | State machine hardening with `canTransitionTo()` validation |
| `UsbHostManager.kt` | Permission re-verification, connection tracking, enhanced error throttle |
| `ProtocolProbe.kt` | Complete rewrite: VID/PID lookup, ADB detection, interface heuristics |
| `OtgActivity.kt` | Transition validation, ADB handling, USB diagnostic self-test |
| `UsbHostManagerTest.kt` | 16 regression tests (up from 2 placeholders) |
| `USB_DEBUG.md` | Complete documentation rewrite with protocol table, transition diagram |

### Infrastructure

| File | Change |
|------|--------|
| `.gitignore` | Added `dotnet-sdk/`, `*.tsbuildinfo`, `*.db`, `vite.config.d.ts` |
| `dotnet-sdk/` | Removed 707 accidentally tracked SDK binary files |

### Version Sync

| File | Version |
|------|---------|
| `src/DeepEyeUnlocker.csproj` | 5.5.0 |
| `DeepEye.UI.Modern/DeepEye.UI.Modern.csproj` | 5.5.0 |
| `portable/android/app/build.gradle` | versionName 5.5.0, versionCode 550 |
| `OtgActivity.kt` | v5.5.0 |
| `README.md` | v5.5.0 |
| `PROJECT_MANIFEST.md` | 5.5.0 |

## 🐛 Bugs Fixed

1. **"Unknown Protocol" after permission grant** — VID/PID fast-path + ADB fingerprinting
2. **"USB Permission Denied by System" loop** — Re-verify permission before open; SecurityException → re-enum flow
3. **Infinite "Try re-plugging" spam** — Error throttle counter (max 3 in 5s)
4. **State bounces to PERMISSION_PENDING** — `canTransitionTo()` validates all transitions

## 🎯 Next Steps

1. Monitor `release.yml` CI run for v5.5.0 tag.
2. Confirm GitHub Release artifact creation (Windows + Android).
3. Test Android OTG APK with physical Qualcomm/MTK/Samsung devices.

---
*Democratizing Mobile Repair & Security Tools.*
