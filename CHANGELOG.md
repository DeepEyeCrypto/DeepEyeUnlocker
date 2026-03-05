# Changelog

All notable changes to DeepEye Unlocker are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
Versioning: [Semantic Versioning](https://semver.org/)

---

## [1.0.2] — 2026-03-05

### Fixed

- UI: Black screen issue resolved by transitioning from ComposeView XML injection to direct `setContent` in `OtgActivity`.
- Build: Explicitly specified `ndkVersion "25.1.8937393"` in `app/build.gradle` to ensure stable NDK resolution.

---

## [1.0.1] — 2026-03-05

### Fixed

- CI: Updated APK locate pattern in GitHub Actions to match `-unsigned.apk`.
- Build: Added `release` signingConfig stub in `app/build.gradle` for local keystores.

---

## [1.0.0] — 2026-03-05

### 🎉 Initial Release — DeepEye Unlocker

First public release of DeepEye Unlocker — an Android OTG
service tool for device repair, firmware operations, and
security research on personal/lab devices.

---

### Added

#### Platform

- Android-only OTG application (Windows/Desktop build removed)
- Minimum SDK: Android 8.0 (API 26) | Target: API 35
- ABI support: arm64-v8a + armeabi-v7a
- Native C++ core engine via NDK 25.1 + CMake 3.22.1
- Kotlin + Jetpack Compose UI (Material3, dark theme)

#### UI / UX

- Full dark theme UI — bg #0D0D1A, primary #6C3EF4
- Brand tabs: Xiaomi, Samsung, Oppo, Vivo, Realme, OnePlus
- Model selector dropdown with brand-scoped device list
- 24 feature cards across 6 groups — always expanded, 2-column
- Tier badge system: TIER 1 SAFE / TIER 2 POLICY / TIER 3 RESTRICTED
- REMOTE mode indicator with live status dot
- Waiting for Device screen with USB pulse animation
- Executing Operation screen with live progress + terminal log
- Operation Complete screen with success/fail state
- Connected MTP Only guidance screen
- Error screen with retry flow
- Bottom nav: Home / Devices / Settings

#### Feature Groups (24 Total)

- GROUP A — Unlock Operations (4): Bootloader unlock/relock,
  FRP erase, Factory reset
- GROUP B — Security Repair (4): Screen lock removal,
  Mi Cloud removal, Auth bypass (SLA/DA), Demo→Retail
- GROUP C — FRP & Account (4): Google FRP, Samsung/Mi account,
  Enterprise EFRP, MTK MetaMode FRP
- GROUP D — Firmware & Partitions (4): Write/read firmware,
  Partition manager, EFS backup/restore
- GROUP E — IMEI & Network (4): IMEI check, IMEI restore (NV),
  5G modem/CPID repair, Network/SIM unlock
- GROUP F — Advanced & Diagnostics (4): Deep device info,
  ADB/Diag enable, One-click root (Magisk), ADB app manager

#### C++ Engine (Native Core)

- libusb 1.0.26 statically linked (Android-safe build)
- Protocol engines: EDL (Qualcomm), BROM (MediaTek),
  Odin (Samsung), FDL (Unisoc), Firehose, DA Handler
- Supporting modules: GPT parser, sparse image handler,
  boot patcher
- JNI bridge: native-lib.cpp → NativeBridge.kt
- ITransport interface for USB + TCP transport abstraction

#### Build / CI

- GitHub Actions CI: assembleRelease on push to main
- Gradle 8.12 + AGP 8.x
- NDK + CMake auto-install in CI
- Single workflow: android_build.yml (TestSprite removed)
- APK artifact upload on every green build

---

### Fixed (during v1.0.0 development)

- `config.h` missing for libusb Android NDK build
- libusb `hotplug.c` / `linux_netlink.c` removed
  (no udev on Android)
- `EdlManager` class symbol mismatch with `edl_proto.h`
- Gradle allprojects repository conflict resolved
- CMake include paths corrected for NDK cross-compilation
- `uint32_t` overflow comparison in `odin_manager.cpp:392`
- Kotlin unresolved references: `clickable`, `SessionState`, `SurfaceDark`
- Missing XML layouts: `item_model.xml`, `item_partition.xml`
- Gradle daemon OOM (heap bumped to 4096m)
- Java 25 incompatibility (pinned JDK 17 for Gradle)

---

### Architecture Notes

- State machine: `UsbSessionState` sealed class drives all screens
- No NavController — screen switching via `when(state)` in
  `OtgActivity.kt`
- All feature data in `FeatureData.kt` — single source of truth
- Design tokens in `DeepEyeColors.kt` — no hardcoded hex strings
- C++ symbols namespaced: `DeepEye::Protocols::*`
- Transport abstracted via `ITransport` interface

---

### Known Limitations (v1.0.0)

- Protocol implementations are stub-complete (return OK/empty).
  Real byte-level EDL/BROM/Odin protocol bodies in v1.1.0
- armeabi-v7a: compiled, not tested on 32-bit devices
- No APK signing in CI (unsigned APK artifact only)
- Remote mode (server ↔ client) UI-only, backend in v1.1.0
- Phone model database: static list, dynamic sync in v1.2.0

---

## [Unreleased]

- EDL Sahara/Firehose full implementation
- BROM META/BROM handshake complete
- APK release signing in CI
- Remote backend WebSocket server
- Dynamic model database sync
- armeabi-v7a device testing

---

[1.0.0]: https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/tag/v1.0.0
