# DeepEye Unlocker v2027.1.1

## [2027.1.1]
### 🎨 Universal Brand Identity Refresh
All platforms have been updated with the new official DeepEye Unlocker logo.
- **Desktop**: macOS (Universal), Windows (NSIS), and Linux (AppImage) now feature high-DPI platform-specific icons.
- **Mobile**: Android launcher assets (adaptive and legacy) have been completely refreshed.
- **Web**: Favicons and Apple Touch Icons are now synchronized with the new aesthetic.

## [2027.1.0]
### 🚀 Final Production Release
- **Gold Master**: Stabilized all core forensic and bypass protocols for public distribution.
- **Asset Pipeline**: Established secure release infrastructure with automated tagging.

### Removed
- RemoteShare functionality due to security concerns

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

### Build Metrics
- **Android**: `app-release.apk` (v20330)
- **Desktop**: macOS DMG (Aarch64/x64)