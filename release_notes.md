# DeepEye Unlocker v2026.31.0

## [Unreleased]
### Added
- New device support for Xiaomi 13 Ultra- EDL protocol enhancements for Qualcomm SD 8 Gen 2

### Removed- RemoteShare functionality due to security concerns

### Highlights

#### 🔓 MTK V6 Protocol Support
We have successfully implemented the MediaTek V6 forensic protocol. This allows for:
- Direct Auth (DA) selection for modern chipsets.
- Memory dumping and partition extraction.
- Secure boot bypass for V6 series devices.

#### 🍏 Checkm8 iOS Orchestration
Initial support for the checkm8 exploit is now integrated into the hardware bridge.
- Precise timing profiles for USB-level heap spray.
- Auto-detection of DFU-mode Apple devices.
- Foundation for complete NAND extraction on A7-A11 devices.

#### 🧠 Intelligence Suite
Introducing the DeepEye Intelligence Suite:
- **Anomaly Detection**: Real-time TFLite-powered signal analysis to detect device-side traps.
- **APK Analysis**: Seamless JADX integration for automated application deconstruction and sensitive key discovery.

### Build Metrics- **Android**: `app-release.apk` (v20310)
- **Desktop**: macOS DMG (Aarch64/x64)