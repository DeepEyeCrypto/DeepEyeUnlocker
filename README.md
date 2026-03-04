# DeepEye Unlocker v5.6.1 (Android Evolution)

"Universal Power. Precision Control. Zero Latency."

![DeepEye Banner](assets/deepeye_readme_banner.png)

[![Build Status](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/actions/workflows/build.yml/badge.svg)](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/actions/workflows/build.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%20(OTG)-green.svg)](https://github.com/DeepEyeCrypto/DeepEyeUnlocker)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://github.com/DeepEyeCrypto/DeepEyeUnlocker)

## 🔷 Professional Mobile Repair. Reimagined for Android

DeepEye Unlocker is a high-performance Android application designed for mobile technicians. It enables full hardware-level access to connected devices via USB OTG, supporting advanced operations like firmware flashing, lock removal, and FRP bypass—all from your Android phone.

---

## ✨ Features (v5.6.1)

### 🎨 Modern Glassmorphism UI

- **Jetpack Compose Native:** Built from the ground up with a premium, zero-latency interface.
- **Glassmorphism Design:** Sleek, transparent UI elements with vibrant accents (Indigo, Cyan).
- **Responsive Layout:** Grid-based operation selector and horizontal brand tabs.
- **Terminal Console:** Real-time log overlay for deep diagnostic visibility.

### 🔌 Advanced USB OTG Engine

- **Chipset Autodetect:** Instant detection of Qualcomm, MTK, and Samsung boot modes.
- **Queue & Wait:** intelligent operation queuing—plug in the device when ready, and the app takes care of the rest.
- **Native Core:** High-speed C++ engine (`libusb`) performing raw USB state transitions.

### 🛠️ Core Operations

- **Category A (Flashing):** Firmware Read/Write, EFS Backup/Restore, Partition Manager.
- **Category B (Reset):** Factory Reset, Demo Unlock, Safe Wipe.
- **Category C (FRP/Account):** Erase FRP, Remove Mi Account, EFRP MDM Hook.
- **Category D (Security):** Remove PIN/Pattern, Bootloader Unlock, MDM Remove.
- **Category E (Network):** IMEI Restore, Modem Repair, Network Unlock.

---

## 🚀 Quick Start (Developers)

### Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 26+ (Compatible with Android 8 to 14+)
- NDK 25.x+ for native core builds
- OTG Adapter (Type-C to USB-A or C-to-C)

### Build

1. Clone the repo: `git clone https://github.com/DeepEyeCrypto/DeepEyeUnlocker.git`
2. Open in Android Studio.
3. Sync Gradle and build the `:app` module.
4. Run on a physical Android device (Emulator does not support OTG).

---

## 🏗️ Architecture

- **UI Layer:** Jetpack Compose (Kotlin)
- **Business Logic:** Kotlin Coroutines + Flow
- **Native Layer:** C++17 (NDK + CMake)
- **USB Transport:** Android USB Host API + `libusb`
- **Authentication:** LicenseManager (Backend Validation)

---

## 🤝 Contributing

Contributions are welcome! Please follow the `GOD PROMPT` style for code changes and ensure 0.5ms-level performance for the typing/input paths if applicable.

---

## ⚖️ Legal

This tool is provided for **educational purposes** and for use on devices you legally own. Users are responsible for compliance with local laws.

---

### Built with ❤️ by the DeepEye Community
