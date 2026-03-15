## [2026.27-RC] — 2026-03-15

### 🚀 Release Candidate — macOS Universal & Android Multi-ABI
- **macOS Universal Build**: Introduced a unified packaging pipeline for Apple Silicon (arm64) and Intel (x86_64). Validated via `lipo` and manual `bundle_dmg.sh` orchestration.
- **Android Universal APK**: Consolidated all target ABIs into a single, high-assurance production APK with R8 minification and native libusb stripping.
- **MtkFsDecryptor Refinement**: Implemented "Double-Layer" decryption support for MTK Dimensity SoCs, enabling simultaneous access to FBE-encrypted userdata and Adoptable Storage (SD card) volumes.
- **Physical Integrity Dash (Stage 600.1)**: Enhanced the Forensic Dashboard with hardware-level integrity indicators (Shield/Verified). Monitors for signal impedance anomalies and unauthorized hardware interposers.
- **ADB Crypto Hardening**: Upgraded RSA keys to 4096-bit and switched to SHA256withRSA to ensure compliance with Android 15 security policies.
- **R8 Production Audit**: Strengthened obfuscation by minimizing -keep wildcards in ProGuard rules, protecting sensitive internal forensic logic from reverse engineering.
- **Stability Fixes**: Resolved Tauri release bundling errors and synchronized Cargo dependencies for reproducible CI/CD parity.

---

## [2026.27] — 2026-03-13

### ✨ Stability Patch & Desktop Console Alignment

---

## [2026.26] — 2026-03-11

### ✨ iPhone 15 Pro Forensic Research & USB-C Analyzer

- **iPhone 15 Pro/Max Forensic Research (Stage 600.1)**: Support for Apple A17 Pro (iPhone 15 Pro) forensic analysis.
- **USB-C PD Protocol Analyzer**: Low-level sniffing of USB-C Power Delivery handshakes on the iPhone 15 series.
- **Vulnerability Mapping (CVE Integration)**: Mapping connected iPhone 15 hardware to known vulnerabilities (CVE-2023-42824 etc).
- **A17 Pro DFU Handshaking**: Experimental support for DFU state monitoring on iPhone 15 Pro models.

---

## [2026.25] — 2026-03-10

### ✨ Multi-Device Forensic Laboratory & Fleet Management

- **Forensic Multi-Device Dashboard (Stage 500.1)**: Introduced a professional cockpit grid for managing multiple concurrent forensic sessions. Includes real-time per-node telemetry for SoC, mode, and connection health.
- **AI-Driven Global Prioritization**: Integrated `ForensicAiAssistant` to analyze the entire connected fleet. The engine automatically identifies chipset clusters and prioritizes forensic targets based on forensic value and exploitability.
- **Batch Forensic Operations (Stage 200.2)**: Implemented collective command execution. Technicians can now perform "Identify All", "Sahara Handshake", and "Mass Extraction" across the entire fleet with atomic precision.
- **Safety-First Batch Guard**: Added Material3 confirmation dialogs for all batch actions to prevent accidental mass triggers on investigative evidence.
- **Forensic File System Explorer (Stage 50.2)**: Launched a native browser for decrypted userdata partitions. Supports directory traversal, file metadata inspection, and integrated real-time MTK decryption path.
- **Mass Evidence Extraction (Stage 50.3)**: New high-speed parallel extraction engine. Allows recursive folder pulling from all selected nodes in the cockpit directly to centralized forensic storage.
- **Fleet-Wide Live Expert Sharing (Stage 20.2)**: Upgraded the DeepEye Tunnel to support full fleet broadcasting. Remote experts can now synchronize with the entire multiple-device dashboard for collaborative joint analysis.
- **Consolidated Fleet Audit (Stage 100.1)**: Enhanced `ReportManager` to generate a unified, multi-node forensic report (JSON/PDF), including hardware metadata and atomic operation logs for every device in the lab.

---

## [2026.19-RC] — 2026-03-10

### ✨ Multi-Window Forensic Workspace & Official Reporting

- **Forensic Workspace (Stage 50.1)**: Introduced a split-screen "Forensic Workspace" mode. Allows technicians to view live AI Intel and terminal logs simultaneously alongside the operation catalog for true real-time feedback.
- **Official PDF Documentation (Stage 100.2)**: Implemented a native PDF report generator. Every investigative session can now be exported as a professional, forensically-sound PDF audit trail including Evidence Hashes (SHA256).
- **High-Assurance Identity Repair (Stage 11.2)**: Finalized the "Magic Repair" commit logic. Enforced a mandatory bit-level `SafeDump` of NVRAM partitions before any restoration sequence to ensure zero-risk research.
- **Secure Enclave Stubs (Stage 30.1)**: Integrated low-level JNI hooks for TEE (Trusted Execution Environment) and RPMB interaction, enabling deep security analysis of Keystore blobs.
- **Universal JNI Hardening**: Updated ProGuard rules to protect native boundaries (MTK/Qualcomm/Apple) and enforced Java 17 toolchain stability.

---

## [2026.20] — 2026-03-09

### ✨ Stitch UI Overhaul & Design DNA Integration

- **Stitch Design DNA**: Completely refactored the design system to use Google Stitch MCP tokens (`StitchTokens.kt`). Implemented a Dark-First, high-assurance security aesthetic with neon accents and premium glassmorphism.
- **Main Portal Redesign**: Overhauled `MainScreen` with an animated "Security Grid" background, mode-sensitive neon glowing edges, and streamlined layout for Disconnected vs. Active states.
- **Professional Settings Engine**: Introduced a comprehensive settings architecture in `SettingsScreen.kt`. Includes fine-grained control over ADB signatures, USB debounce timing, and UI performance modes.
- **Hardware Debug Overlay**: Implemented a real-time `DebugOverlayPanel` providing live USB descriptor snapshots and hardware-level telemetry without leaving the home dashboard.
- **Premium Operation Feedback**: Redesigned `ExecutingScreen`, `WaitingScreen`, and `CompleteScreen` with pulsing animations, professional terminal logs, and state-aware action clusters.
- **Edge-to-Edge Synergy**: Enforced consistent transparent system bar styling across all screens for a truly immersive console experience.
- **Engine Recovery**: Fixed critical syntax/logic errors in `ProtocolDetector.kt` and stabilized `UsbViewModel` execution path.
- **R8 Build Optimization**: Resolved release-build hangs by fine-tuning ProGuard rules and disabling problematic bytecode optimizations (`-dontoptimize`).

---

## [2026.19] — 2026-03-08

### ✨ Remote Relay & Cloud Ecosystem (Stage C–J)

- **Remote Tunnel Bridge (Stage H)**: Pioneered "DeepEye Tunnel" technology allowing technicians to relay physical USB OTG packets over a secure WebSocket pipe (`wss://relay.deepeye.cloud`). Includes a live USB Proxy engine for remote `READ/WRITE` operations.
- **Navigation & Settings Portal (Stage G)**: Integrated a native Liquid Glass bottom navigation bar for seamless switching between Home, Devices, and Settings. Added "High-Performance Mode" and "Aggressive Polling" toggles.
- **Cloud Model Sync (Stage I)**: Implemented an over-the-air (OTA) device definition engine. The app now syncs the latest brand/chipset mappings dynamically, with transparent fallback to the local embedded database if offline.
- **Security Hardening (Stage C/E)**: Enforced hardware-attested license binding using `EncryptedSharedPreferences`. Introduced "Secure Identity Boot" to prevent unauthorized session cloning.
- **OTA Update Orchestration (Stage F)**: Integrated an automated update notification system that polls the DeepEye release channel and provides one-tap browser-based upgrades.
- **Production Hardening (Stage J)**: Final ProGuard/R8 optimization pass and production build fingerprinting for public distribution.

---

## [2026.16] — 2026-03-07

### ✨ Identity Repair & Real-Device Integration Testing

- **Professional Identity Repair (Group 5)**: Implemented native MTK NVRAM and Qualcomm NV_ITEM 550 (IMEI) repair engines. Added bit-level BCD encoding/decoding and checksum recalculation logic directly in `native-lib.cpp`.
- **Stage T1 Test Harness**: Introduced a dedicated high-fidelity integration test screen. Features live USB attach/detach monitoring, VID:PID detection, and a raw IO bulk transfer console for debugging real device handshakes.
- **Native Stability Hotfix**: Successfully merged external NV engines into the core JNI bridge to bypass subdirectory indexer limitations and resolve Clang type-visibility errors.
- **UI Enhancements**: Added an "Integration Test" group to the main catalog and a quick-access "TEST" button in the glass top bar for immediate hardware validation.

## [2026.15] — 2026-03-07

### ✨ Unconditional UI Rendering & Domain Architecture

- **Domain Model Source of Truth**: Completely migrated the core state engine to `DomainModels.kt`. Introducing `ProtocolFamily`, `DeviceMode`, `PolicyTier`, `OperationAvailability`, and `DeepEyeOperation` as unified application states.
- **Unconditional UI Generation**: Decommissioned legacy map-based conditional rendering in `MainScreen.kt`. Replaced with immutable `DeepEyeCatalogs` assuring 100% of Modes and Features are visually accessible immediately on launch.
- **Availability Engine**: Implemented `AvailabilityEngine` to dynamically calculate `OperationAvailability`, handling alpha dimming and precise visual explanation (e.g. "Requires FASTBOOT") without destroying layout integrity.
- **PolicyEngine Upgrades**: Standardized enforcement pipelines to exclusively use semantic `PolicyTier` rules (SAFE, POLICY, RESTRICTED, NEVER) over ambiguous integers.
- **Legacy Bridging**: Maintained backward-compatibility for native `EngineDispatcher` by implementing seamless `DeepEyeOperation` companion object shims spanning to the C++ NDK boundary.
- **Build System**: Resolved NDK deprecation warnings by eliminating static `ndk.dir` bindings, fully yielding to `android.ndkVersion`.

---

## [2026.14] — 2026-03-06

### ✨ USB Stability God Patch

- **SafeBulkTransfer Engine**: Implemented protocol-tuned timeouts, 16KB chunking, and automatic stall detection (CLEAR_FEATURE) to stabilize high-speed data transfers.
- **USB Lifecycle Manager**: 10-state machine (Idle→Connected→Dead) to prevent race conditions during rapid cable flapping and activity recreation.
- **OEM Compatibility Layer**: Hardened logic for Samsung (buffer padding), MIUI (50ms post-claim delay), Huawei/Honor (3x open retry), and Vivo (OTG settings link).
- **USB Foreground Service**: Prevents Android battery-savers from killing long-running USB sessions like full firmware flashing.
- **Connection Health UI**: Real-time health monitoring (Healthy/Degraded/Dead) integrated into top bar for visual assurance.
- **Connection Test Screen**: Comprehensive 8-step diagnostic for testing phone-to-phone OTG connectivity without drivers.

## [2026.6] — 2026-03-06

### ✨ Liquid Glass UI Rebuild

- **Zero-Latency Glassmorphism**: Fully implemented Stitch UI designs utilizing native Jetpack Compose and Haze, keeping strict zero frame-drop rendering constraints.
- **Haze Integration**: Integrated `dev.chrisbanes.haze` to render physically-correct blurred glass material natively. Safe usage scoped to single instances per root screen.
- **Crash Prevention Native Core**: Eliminated all `linearGradient` bounding crashes by caching SolidColors and `Brush` items behind `remember{}` layers.
- **Dynamic Assets**: Converted USB Type-C and custom branding assets into math-modeled `Canvas` arrays allowing zero-cost scaling and hardware acceleration.
- **Refined Safe Mode Padding**: Applied `windowInsetsPadding(WindowInsets.safeDrawing)` globally on app-level scope.

---

## [2026.5] — 2026-03-06

### 🔥 Crash Fixes

- **LinearGradient.nativeCreate Crash**: Fixed `IllegalArgumentException` triggered by `Brush.linearGradient` or `Brush.radialGradient` on zero-width bounds.
- **Progress Bar**: Added `fraction.coerceIn(0.02f, 1.0f)` to prevent zero-width crash.
- **Glass Ambient Orbs**: Fixed 0-size radius crash by explicitly setting `radius = 450f` and `radius = 600f`.
- **Card Borders**: Adopted solid `Color` backgrounds/borders to replace dangerous `Brush.linearGradient()` usages without start/end coordinates.

---

## [2026.4] — 2026-03-06

### 🚀 ANR & Cold-Start Fix

- **Asynchronous JNI Load**: Refactored `NativeBridge.loadAsync()` to execute on `Dispatchers.IO`, effectively fixing the 5-sec ANR ("System.loadLibrary blocking main thread" dialog).
- **Fast Startup UI`: Introduced`DeepEyeLoadingScreen` for immediate visual feedback.
- **Main Thread Unblocked**: Prevented blocking calls to USB init and LicenseManager. All device setup is deferred entirely until `setContent` is called.
- **Eliminated White Flashes**: `Theme.DeepEyeUnlocker.Splash` prevents white screen flash by matching the `#05050F` background immediately upon app open.

## [2026.3] — 2026-03-06

### 🎨 Liquid Glass Polish — Full Theme + Cleanup

Final polish pass: every screen now uses Liquid Glass, all compiler
warnings resolved, README updated, version bumped.

### Changed

- **RemoteShareScreen**: Upgraded to Liquid Glass — frosted back button,
  glass status card, monospace session code, gradient START / red STOP,
  glass text field + CONNECT button
- **README.md**: Updated to v2026.3 — Liquid Glass branding, design
  system docs, color token reference, build instructions with env vars
- **CHANGELOG.md**: Added v2026.2 + v2026.3 entries
- **Version**: Bumped `versionCode` 20263, `versionName` "2026.3"

### Fixed

- Deprecated `Divider` → `HorizontalDivider` in RemoteShareScreen
- Unused `containerColor` parameter warnings in `PrimaryButton` / `PrimaryIconButton`
- Hard-coded v1 colors in RemoteShareScreen replaced with design tokens

---

## [2026.2] — 2026-03-06

### 🎨 Liquid Glass UI — Glassmorphism Overhaul from Stitch

Complete visual retheme from solid dark to Liquid Glass — frosted glass
cards, gradient borders, animated orbs, macOS-style terminal.

### Added

- **DeepSpaceBackground**: Gradient bg (#05050F → #0A0015) with purple/blue
  radial gradient orbs
- **GlassCard**: Frosted glass component (white/5% bg + white/12% border)
- **GlassPill**: Frosted pill badges/buttons
- **GradientRunButton**: Purple gradient (#9747FF → #6B2FE0) replacing solid
- **OperationTierBadge**: Glow-border pill with tier colors
- macOS-style terminal: traffic light dots (🔴🟡🟢) + blinking cursor
- PAUSE (glass) + ABORT (red glass) buttons on executing screen
- Bottom navigation bar (Home / Devices / Settings)

### Changed

- **MainScreen**: Glass header, frosted pill brand tabs, glass model selector
- **FeatureListScreen**: Frosted pill category headers, 140dp glass cards
- **WaitingForDeviceScreen**: Animated orbs, frosted card, radial icon bg
- **ExecutingOperationScreen**: Glass header + gradient progress + terminal
- **OperationCompleteScreen**: DeepSpaceBackground + glass card
- **ErrorScreen**: DeepSpaceBackground + red glass RETRY
- **PermissionDeniedScreen**: DeepSpaceBackground + amber glass TRY AGAIN
- **ConnectedMtpOnlyScreen**: DeepSpaceBackground + glass DISMISS
- **Color tokens**: BgStart #05050F, AccentPurple #9C6FFF, brighter tiers
  (#69FF47, #FFD740, #FF6E6E)

### Fixed

- **ANR on API < 31**: Replaced `Modifier.blur()` (requires Android 12+) with
  `Brush.radialGradient()` — identical visual, works on all API levels from 26
- **Build**: Moved mid-file imports to top (invalid Kotlin syntax)
- **Compose compat**: Used `Modifier.composed` + `remember { MutableInteractionSource() }`
  instead of `interactionSource = null` (requires Compose 1.7+)

---

## [2026.1] — 2026-03-05

### 🔒 Hardening Release — Native Transport & EDL Stabilization

Switched to **CalVer** (YYYY.N) versioning. This release focuses on
production-hardening the native USB transport layer and EDL handshake,
making the tool more resilient on real Qualcomm devices.

### Changed

- **Versioning**: Migrated from SemVer (1.0.x) to CalVer (2026.N)
- **EDL Manager**: Rewrote Sahara hello/handshake flow with proper
  state machine, command validation, and retry logic (~250 lines added)
- **USB Transport**: Hardened bulk transfer with timeout handling,
  error recovery, and endpoint validation (~180 lines added)
- **ITransport**: Introduced pure-virtual `ITransport` interface header
  for clean transport abstraction
- **Remote Share**: Improved `RemoteShareActivity` with better error
  handling and connection lifecycle management

### Fixed

- EDL handshake failing silently on certain Qualcomm chipsets
- USB bulk read/write not respecting timeout boundaries
- Transport layer crash on unexpected USB disconnect
- Remote share activity not cleaning up connection state

### Includes all changes from v1.0.0 → v1.0.2

- `v1.0.2`: Black screen fix (setContent vs ComposeView), NDK version pin
- `v1.0.1`: CI APK locate pattern fix, release signingConfig
- `v1.0.0`: Initial release — 24 features, 6 brand tabs, C++ native core

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
  Real byte-level EDL/BROM/Odin protocol bodies planned for next release.
- armeabi-v7a: compiled, not tested on 32-bit devices
- Remote mode (server ↔ client) UI-only, backend coming soon
- Phone model database: static list, dynamic sync planned

---

## [Unreleased]

- EDL Sahara/Firehose byte-level protocol completion
- BROM META/BROM handshake complete
- Remote backend WebSocket server
- Dynamic model database sync
- armeabi-v7a device testing

---

[2026.20]: https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/tag/v2026.20
[2026.19]: https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/tag/v2026.19
[2026.3]: https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/tag/v2026.3
[2026.2]: https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/tag/v2026.2
[2026.1]: https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/tag/v2026.1
[1.0.2]: https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/tag/v1.0.2
[1.0.1]: https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/tag/v1.0.1
[1.0.0]: https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/tag/v1.0.0
