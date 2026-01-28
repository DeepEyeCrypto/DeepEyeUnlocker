# RELEASE v1.1.1 - DeepEyeUnlocker "Archon"

## 🔷 The Engine Room Update

We are excited to release **DeepEyeUnlocker v1.1.1**, codenamed "Archon". This update brings a massive overhaul to the core flashing and diagnostics engines, enabling professional-grade operations on modern devices with large storage.

### 🌟 Key Features

#### 🏗️ High-Speed Streaming Flash

- **Gigabyte-Scale Support**: New streaming architecture allows flashing files of any size (system, super, userdata) with minimal RAM usage.
- **Smart Manifest Parsing**: Automatically reads Qualcomm `rawprogram.xml`, MTK Scatter, and Samsung formats to determine precise partition layouts.
- **Sector-Level Precision**: Implemented "Pass-Through" flashing for Qualcomm, targeting exact physical sectors defined in firmware manifests.

#### 🔐 Advanced Diagnostics Hub 2.0

- **Unified Analysis**: The new `LockFrpDiagnosticsManager` now works across all supported protocols (EDL, BROM, DB) to inspect partition data directly.
- **Deep Factory Reset**: Smart format operation that auto-detects `userdata`, `cache`, and `metadata` via GPT for a clean device wipe.
- **Safe Mode**: Flash UI now includes protections for critical partitions (EFS, Modem, Persist) to prevent accidental IMEI loss.

#### ⚡ Protocol Enhancements

- **Multi-Protocol Architecture**: Unified `IProtocol` interface now standardizes operations across Qualcomm, MediaTek, and Samsung engines.
- **Samsung & MTK Foundations**: Added streaming support structures for Odin and BROM protocols (preliminary support).

### 🐛 Bug Fixes

- Fixed memory spikes during large backups.
- Resolved partition detection issues on newer Qualcomm SoCs.
- Improved "Device Not Found" handling in operation panels.

### 📦 Installation

1. Download `DeepEyeUnlocker-v1.1.1-archon.zip` from the Assets below.
2. Extract to your DeepEyeUnlocker directory (overwrite existing files).
3. Run `DeepEyeUnlocker.exe` as Administrator.

---
**[Download v1.1.1](https://github.com/yourusername/deepeyeunlocker/releases/tag/v1.1.1)**
