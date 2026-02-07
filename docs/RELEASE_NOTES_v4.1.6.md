# Release Notes: v4.1.6-android "Sentinel Pro"

**Release Date**: February 7, 2026
**Tag**: `v4.1.6-android`

## 🚀 Overview

This release focuses on stabilizing the **Android Portable Engine** and finalizing the cross-platform CI/CD pipeline. Following the major v4.0.0 release, this update addresses critical blockers for mobile OTG operations and improves the overall branding consistent with the "Sentinel Pro" standard.

## ✨ New Features & Enhancements

### 📱 Android Portable Engine (OTG)

- **Official Support**: Successfully enabled the Android JNI build system. DeepEyeUnlocker can now run natively on Android devices for OTG device repair.
- **Launcher Icons**: Added high-fidelity adaptive icons for all pixel densities (mdpi to xxxhdpi).
- **Manifest Optimization**: Refactored `AndroidManifest.xml` to correctly handle USB host permissions and activity intent filters.

### 🛡️ FRP Bypass HQ

- **Protocol Parity**: Synchronized simulation engines with real-world hardware responses for improved success rates on modern chipsets.

## 🔧 Bug Fixes

- **CI/CD Stabilization**:
  - Fixed APK upload paths in the GitHub Actions workflow.
  - Forced Gradle wrapper usage to avoid environment version mismatches on runner nodes.
  - Resolved `libusb.h` include errors in the Android JNI bridge.
- **Header Files**: Cleaned up duplicate `.cpp` includes causing symbol collisions during the linking phase.
- **USB Transport**: Fixed `LibUsbTransport` header/implementation split for better cross-platform compatibility.

## 📂 Artifacts

- **DeepEyeUnlocker-v4.1.6-android-Portable.zip**: Contains the standalone APK and portable assets.
- **Source Code**: Available in `.zip` and `.tar.gz` formats.

---
*For more details on implementation, see `docs/SYSTEM_DOCUMENTATION.md`.*
