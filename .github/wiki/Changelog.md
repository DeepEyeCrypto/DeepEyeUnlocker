# Changelog

Version history and stage development map for DeepEye Unlocker.

---

## Version History

### [2026.32.4] — 2026-03-24

**Registry Sanitization & CI Survival**
- Surgically removed duplicate property assignments in `UnifiedBypassRegistry.kt`
- Ensured 100% syntactic correctness for GitHub Actions
- Verified KSP and Kotlin compilation pass without metadata corruption

### [2026.32.3] — 2026-03-24

**CI Stabilization & Model Fixes**
- Resolved GitHub Actions build regression
- Added missing properties to `BypassFeature`: `isOffline`, `isUntethered`, `noDataLoss`, `isFree`
- Fixed `BypassScreen.kt` and `MainScreen.kt` for Bottom Bar architecture
- Added `DevicePlatform.MODEM_ROUTER` support

### [2026.32.2] — 2026-03-24

**Bottom Bar UI Migration & System Stabilization**
- Migrated `RemoteShare`, `Bug`, and `Settings` to bottom navigation bar
- Removed redundant top-bar action icons and floating FAB
- Fixed syntax corruption in `RealMtkV6Executor.kt` and `UsbSessionManager.kt`
- Fixed Hilt injection failures with `FirmwareAssetManager` provider
- Switched to `UnifiedBypassRegistry.buildPlan` for execution planning

### [2026.32.0] — 2026-03-24

**Stability Hardening & Premium UI Orchestration**
- USB Transport Reliability: `bulkIn`/`bulkOut` retries, `claimWithSettle`
- Android 14 Compliance: `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`
- Responsive Mission Header with screen width detection
- Compose Performance: `LazyColumn` stable keys, `@Immutable` state models
- Protocol Hardening: DA fallback chain (Chip-specific → V6 → V5)
- Build Optimization: Disabled R8 full-mode, added Baseline Profiles

### [2026.31.0] — 2026-03-22

**MTK V6 & Checkm8 — Intelligence Orchestration**
- MTK V6 Protocol: Stable V6 forensic protocol implementation
- Checkm8 Exploit Platform: Initial orchestration for iPhone X/8/7
- Intelligence Suite: `AnomalyDetector` (TFLite), `ApkAnalyzer` (JADX)
- Bypass V5 synchronization with UI orchestration layer

### [2026.30.0] — 2026-03-22

**Stable Release — Platform Synchronization & Hardware Bridge**
- Unified Bridge: Android v20300 and Desktop 2026.30.0 sync
- Resource Orchestration: Python forensic scripts bundling
- Hardware Telemetry: Real-time `ios_poll_orchestrator` integration
- Identity Forensic Audit: Sub-millisecond extraction error handling
- Liquid Glass v2.1: High-DPI glassmorphism tokens

### [2026.28.0] — 2026-03-15

**Owned Device Mode — Policy Bypass & UI Fixes**
- Availability Engine: Bypassed Tier 2 (POLICY) and Tier 3 (RESTRICTED) gates
- EXECUTE Button Fix: Always tappable regardless of restrictions
- UI Descriptors: Removed restrictive terminology from feature cards

### [2026.27-RC] — 2026-03-15

**Release Candidate — macOS Universal & Android Multi-ABI**
- macOS Universal Build: Apple Silicon (arm64) + Intel (x86_64)
- Android Universal APK: All ABIs with R8 minification
- MtkFsDecryptor: Double-Layer decryption for Dimensity SoCs
- Physical Integrity Dash (Stage 600.1): Hardware-level integrity indicators
- ADB Crypto Hardening: RSA-4096, SHA256withRSA

### [2026.20] — 2026-03-09

**Stitch UI Overhaul & Design DNA Integration**
- Stitch Design DNA: Google Stitch MCP tokens
- Main Portal Redesign: Security Grid background, neon glowing edges
- Professional Settings Engine: ADB signatures, USB debounce, performance modes
- Hardware Debug Overlay: Real-time USB descriptor snapshots
- Premium Operation Feedback: Pulsing animations, terminal logs

### [2026.19] — 2026-03-08

**Remote Relay & Cloud Ecosystem (Stage C–J)**
- Remote Tunnel Bridge: DeepEye Tunnel technology (WebSocket relay)
- Navigation & Settings Portal: Liquid Glass bottom navigation
- Cloud Model Sync: OTA device definition engine
- Security Hardening: Hardware-attested license binding
- OTA Update Orchestration: Automated update notifications

### [2026.16] — 2026-03-07

**Identity Repair & Real-Device Integration Testing**
- Professional Identity Repair: MTK NVRAM, Qualcomm NV_ITEM 550
- Stage T1 Test Harness: USB attach/detach monitoring, raw IO console

### [2026.15] — 2026-03-07

**Unconditional UI Rendering & Domain Architecture**
- Domain Model Source of Truth: `DomainModels.kt`
- Unconditional UI Generation: Immutable `DeepEyeCatalogs`
- Availability Engine: Dynamic `OperationAvailability` calculation
- PolicyEngine Upgrades: Semantic `PolicyTier` rules

### [2026.14] — 2026-03-06

**USB Stability God Patch**
- SafeBulkTransfer Engine: Protocol-tuned timeouts, 16KB chunking
- USB Lifecycle Manager: 10-state machine
- OEM Compatibility Layer: Samsung, MIUI, Huawei, Vivo
- USB Foreground Service: Prevents battery-saver kill

### [2026.6] — 2026-03-06

**Liquid Glass UI Rebuild**
- Zero-Latency Glassmorphism: Jetpack Compose + Haze
- Haze Integration: Physically-correct blurred glass
- Crash Prevention: SolidColor caching, `remember{}` layers

### [2026.5] — 2026-03-06

**Crash Fixes**
- LinearGradient.nativeCreate Crash fix
- Progress Bar: `fraction.coerceIn(0.02f, 1.0f)`
- Glass Ambient Orbs: Fixed 0-size radius

### [2026.4] — 2026-03-06

**ANR & Cold-Start Fix**
- Asynchronous JNI Load: `Dispatchers.IO`
- Fast Startup UI: `DeepEyeLoadingScreen`
- Eliminated White Flashes: Splash theme

### [2026.3] — 2026-03-06

**Liquid Glass Polish — Full Theme + Cleanup**
- RemoteShareScreen: Liquid Glass upgrade
- README: v2026.3 documentation
- Fixed deprecated `Divider` → `HorizontalDivider`

### [2026.2] — 2026-03-06

**Liquid Glass UI — Glassmorphism Overhaul**
- DeepSpaceBackground: Gradient bg with radial orbs
- GlassCard: Frosted glass component
- GradientRunButton: Purple gradient
- macOS-style terminal

### [2026.1] — 2026-03-05

**Hardening Release — Native Transport & EDL Stabilization**
- CalVer versioning migration (YYYY.N)
- EDL Manager: Sahara hello/handshake rewrite
- USB Transport: Hardened bulk transfer
- ITransport: Pure-virtual interface

### [1.0.0] — 2026-03-05

**Initial Release — DeepEye Unlocker**
- Android-only OTG application
- Minimum SDK: Android 8.0 (API 26)
- 24 features across 6 groups
- C++ native core engine
- Jetpack Compose UI

---

## Stage Development Map

DeepEye Unlocker is developed in stages, each representing a major feature or system.

### Core Stages (1–10)

| Stage | Name | Description | Status |
|-------|------|-------------|--------|
| 1 | USB Protocol Foundation | libusb integration, basic transport | ✅ Complete |
| 2 | Protocol Detection | VID/PID classification, mode detection | ✅ Complete |
| 3 | Android UI Foundation | Jetpack Compose, navigation | ✅ Complete |
| 4 | Native Bridge | JNI interface, C++ core | ✅ Complete |
| 5 | Device Database | Model detection, brand support | ✅ Complete |
| 6 | Liquid Glass UI | Glassmorphism, animations | ✅ Complete |
| 7 | Apple Support | iOS device detection, iRecovery | ✅ Complete |
| 8 | FRP Bypass | Factory Reset Protection bypass | ✅ Complete |
| 9 | Audit System | Code quality gates, compliance | ✅ Complete |
| 10 | Never-Do List | Safety rules, prohibited operations | ✅ Complete |

### Feature Stages (11–20)

| Stage | Name | Description | Status |
|-------|------|-------------|--------|
| 11 | Identity Repair | IMEI, NVRAM repair | ✅ Complete |
| 12 | Remote Share | USB over network relay | ✅ Complete |
| 13 | Cloud Sync | OTA model updates | ✅ Complete |
| 14 | Tauri Desktop | Desktop app (Rust + React) | ✅ Complete |
| 15 | FRP Bypass System | Complete FRP orchestration | ✅ Complete |
| 16 | Logging & Audit | Session logging, forensics | ✅ Complete |
| 17 | Settings Engine | Professional settings UI | ✅ Complete |
| 18 | Integration Testing | Real-device test harness | ✅ Complete |
| 19 | Remote Tunnel | WebSocket USB proxy | ✅ Complete |
| 20 | Cloud Ecosystem | Complete cloud integration | ✅ Complete |

### Advanced Stages (21–30)

| Stage | Name | Description | Status |
|-------|------|-------------|--------|
| 21 | Multi-Device Lab | Fleet management dashboard | ✅ Complete |
| 22 | AI Assistant | Forensic AI, anomaly detection | ✅ Complete |
| 23 | Batch Operations | Mass command execution | ✅ Complete |
| 24 | File System Explorer | Native file browser | ✅ Complete |
| 25 | Mass Extraction | Parallel data extraction | ✅ Complete |
| 26 | iPhone 15 Research | USB-C forensic analysis | ✅ Complete |
| 27 | macOS Universal | Apple Silicon + Intel builds | ✅ Complete |
| 28 | Release Hardening | Production optimization | ✅ Complete |
| 29 | MTK V6 Protocol | MediaTek V6 forensics | ✅ Complete |
| 30 | Checkm8 Platform | Exploit orchestration | ✅ Complete |

### Specialized Stages (31–40)

| Stage | Name | Description | Status |
|-------|------|-------------|--------|
| 31 | Intelligence Suite | TFLite, APK analysis | ✅ Complete |
| 32 | Stability Hardening | USB reliability, Android 14 | ✅ Complete |
| 33 | UI Migration | Bottom bar navigation | ✅ Complete |
| 34 | Registry Cleanup | Code sanitization | ✅ Complete |
| 35 | CI/CD Survival | Build pipeline hardening | 🔄 Active |
| 36 | Documentation | Wiki, API docs | 🔄 Active |
| 37 | Testing Framework | Unit/integration tests | 📋 Planned |
| 38 | Security Audit | Penetration testing | 📋 Planned |
| 39 | Performance Optimization | Speed, memory optimization | 📋 Planned |
| 40 | v2027.0 Release | Major stable release | 📋 Planned |

### Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Complete |
| 🔄 | In Progress |
| 📋 | Planned |
| ⏸️ | Paused |

---

## Stage Details

### Stage 14 — Apple / iPhone USB Support

**Components:**
- Apple VID/PID Table (`DeviceMatrix.kt`)
- Apple Mode Detection (`UsbExtensions.kt`)
- AppleDeviceState and Observer (`DeviceRepository.kt`)
- Tauri Commands (`apple.rs`): `apple_device_info`, `apple_irecovery_cmd`, `apple_exit_recovery`, `apple_enter_dfu`
- AppleDeviceViewModel with Hilt injection
- AppleDeviceScreen UI

### Stage 15 — FRP Bypass System

**Components:**
- FRP Decision Tree (`FrpRouter.kt`)
- 4 Bypass Paths: EDL Firehose, ADB Sideload, CVE Exploit, Fastboot OEM
- CVE Database (`CveDatabase.kt`)
- FRP Use Case (`FrpUseCase.kt`)
- FRP ViewModel (`FrpViewModel.kt`)
- FrpBypassPanel UI

### Stage 600.1 — Physical Integrity

**Components:**
- Eye-Diagram Analysis
- Tamper Detection
- Signal Impedance Guard
- EMI Shielding
- Hardware-level integrity indicators

---

## Versioning

DeepEye Unlocker uses **CalVer** (Calendar Versioning):

```
YYYY.N[.P]

YYYY = Year (2026)
N    = Release number within year (31, 32, etc.)
P    = Patch number (optional)

Examples:
- 2026.31.0 = First release of 2026, version 31
- 2026.32.4 = 2026, version 32, patch 4
```

### Platform Version Sync

| Platform | Current Version |
|----------|----------------|
| Desktop (Tauri) | 2026.31.0 |
| Android | v20300 (2026.30.0) |

---

## Release Checklist

Before each release:

- [ ] Version bumped in all files
- [ ] CHANGELOG.md updated
- [ ] All tests passing
- [ ] Documentation updated
- [ ] CI builds green
- [ ] Signed artifacts generated
- [ ] Release notes prepared
