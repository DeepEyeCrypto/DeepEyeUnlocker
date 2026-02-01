# DeepEyeUnlocker v3.2.0 - "The Hybrid Engine"

**Release Date:** 2026-02-01
**Codename:** Chimera

This major release introduces the **Hybrid Engine**, a revolutionary architecture that combines the universal power of chipset-based exploits ("Miracle Mode") with the precision of model-specific handlers ("UnlockTool Mode").

## 🚀 New Features

### 🧠 The Hybrid Engine

The core of DeepEyeUnlocker has been rewritten to feature a smart routing system:

- **Smart Routing**: Auomatically analyzes the connected device and decides the safest and most effective unlocking strategy.
- **Auto-Detection**: Distinguishes between "Generic/Legacy" devices and "Modern Flagships" (e.g., Samsung S24).
- **Fallback System**: If a precision method fails, the system can offer to try universal methods (with user confirmation).

### 🛠️ Miracle Mode (Universal)

Designed for offline use and unbranded devices:

- **MTK Universal**: BROM Exploit with SLA/DAA Authentication Bypass. Supports generic Read/Write/Format on MTK chips.
- **SPD Universal**: FDL Injection support for Spreadtrum/Unisoc devices (SC9863A, T606, etc.).
- **Qualcomm Universal**: Generic Firehose loader support for offline EDL operations.
- **Keypad Support**: Universal logic for legacy feature phones (Nokia, Jio).

### 🎯 UnlockTool Mode (Model-Specific)

Designed for high-security modern devices:

- **Cloud Profile Sync**: Simulates fetching live handler scripts for specific models.
- **Samsung Handler**: Precision handling for S24 Ultra (Odin v4, Knox Guard).
- **Xiaomi Handler**: HyperOS Fastboot and Account Bypass logic for Xiaomi 14.

### 🖥️ UI Improvements

- **Integrated Operations**: The main operation buttons (Format, FRP, Flash) now route through the Hybrid Engine.
- **Safety Dialogs**: New warning system when attempting risky universal operations on modern hardware.
- **Progress Tracking**: Enhanced progress bar and status logging for multi-stage hybrid operations.

## 🐛 Bug Fixes

- Fixed CI build failures related to TestSprite CLI integration.
- Resolved dependency injection issues in `MainForm`.
- Fixed "No Engine Available" errors for supported chipsets.

## 📦 For Developers

- **Architecture**: New `IUniversalPlugin` and `IModelSpecificHandler` interfaces.
- **Testing**: Added `HybridRouterTests` to verify smart routing logic.
- **Services**: Added `DeviceClassifier` and `CloudProfileService`.

---
*DeepEyeUnlocker Team*
