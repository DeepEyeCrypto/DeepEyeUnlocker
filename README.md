# DeepEye Unlocker v2026.3

> Universal Power. Precision Control. Liquid Glass.

![DeepEye Banner](assets/deepeye_readme_banner.png)

[![Release](https://img.shields.io/github/v/release/DeepEyeCrypto/DeepEyeUnlocker?style=flat-square&color=9C6FFF)](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/latest)
[![Build Status](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/actions/workflows/build.yml/badge.svg)](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/actions/workflows/build.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%20(OTG)-green.svg)](https://github.com/DeepEyeCrypto/DeepEyeUnlocker)
[![UI](https://img.shields.io/badge/UI-Liquid%20Glass%20·%20Compose-9C6FFF.svg)](https://github.com/DeepEyeCrypto/DeepEyeUnlocker)

---

## 🔷 Professional Mobile Repair — Reimagined for Android

DeepEye Unlocker is a high-performance Android application designed for mobile technicians and security researchers. It enables full hardware-level access to connected devices via USB OTG, supporting advanced operations like firmware flashing, lock removal, and FRP bypass — all from your Android phone.

---

## ✨ Features

### 🎨 Liquid Glass UI (v2)

- **Jetpack Compose Native:** Built from the ground up with a premium, zero-latency interface
- **Liquid Glass Theme:** Deep space gradient bg (`#05050F` → `#0A0015`) with animated purple/blue orbs
- **Glassmorphism:** Frosted glass cards (`white/5%` + `white/12%` border) throughout
- **Gradient Accents:** Purple gradient buttons (`#9747FF` → `#6B2FE0`), frosted pill badges
- **6 Brand Tabs:** Xiaomi, Samsung, Oppo, Vivo, Realme, OnePlus — glass pill style
- **24 Feature Cards:** Organized across 6 groups with glow-border tier badges
- **macOS-Style Terminal:** Traffic light dots (🔴🟡🟢) with color-coded logs and blinking cursor
- **Remote Tunnel:** Glass-themed remote device sharing with session ID display

### 🔌 Advanced USB OTG Engine

- **Chipset Autodetect:** Instant detection of Qualcomm (EDL/Sahara), MTK (BROM), and Samsung (Odin) boot modes
- **Hardened Transport:** Production-grade USB bulk transfer with timeout handling & error recovery
- **Native Core:** High-speed C++17 engine (`libusb 1.0.26`) performing raw USB state transitions
- **ITransport Abstraction:** Clean interface for USB + TCP transport

### 🛠️ Core Operations (24 Features)

| Group | Operations |
|---|---|
| **A — Unlock** | Bootloader unlock/relock, FRP erase, Factory reset |
| **B — Security Repair** | Screen lock removal, Mi Cloud removal, Auth bypass, Demo→Retail |
| **C — FRP & Account** | Google FRP, Samsung/Mi account, Enterprise EFRP, MTK MetaMode |
| **D — Firmware** | Write/read firmware, Partition manager, EFS backup/restore |
| **E — IMEI & Network** | IMEI check/restore, 5G modem repair, Network/SIM unlock |
| **F — Advanced** | Deep device info, ADB/Diag enable, One-click root, App manager |

---

## 🚀 Quick Start

### Prerequisites

- JDK 17 (Temurin recommended)
- Android SDK 26+ (Android 8.0 → 15+)
- NDK 25.1.8937393 + CMake 3.22.1
- OTG Adapter (Type-C to USB-A or C-to-C)

### Build

```bash
git clone https://github.com/DeepEyeCrypto/DeepEyeUnlocker.git
cd DeepEyeUnlocker

# Set environment
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME=/usr/local/share/android-commandlinetools

# Build signed release
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

Run on a **physical Android device** — emulators do not support OTG.

---

## 🏗️ Architecture

```text
┌─────────────────────────────────────┐
│  Jetpack Compose UI (Kotlin)        │
│  Material3 · Liquid Glass · Compose │
├─────────────────────────────────────┤
│  Business Logic                     │
│  Coroutines · Flow · ViewModel      │
├─────────────────────────────────────┤
│  JNI Bridge                         │
│  native-lib.cpp ↔ NativeBridge.kt   │
├─────────────────────────────────────┤
│  C++17 Native Core (NDK)            │
│  EDL · BROM · Odin · FDL · Firehose │
│  libusb 1.0.26 · ITransport         │
└─────────────────────────────────────┘
```

### Design System

```
DeepEyeColors (DeepEyeUI.kt)
├── BgStart: #05050F → BgEnd: #0A0015  (deep space gradient)
├── PrimaryGlow: #7C4DFF               (accent purple)
├── AccentPurple: #9C6FFF              (highlights)
├── GradientButton: #9747FF → #6B2FE0  (RUN buttons)
├── GlassCardBg: white/5%             (frosted cards)
├── GlassBorder: white/12%            (card borders)
├── Tier1: #69FF47  Tier2: #FFD740  Tier3: #FF6E6E
└── Terminal: green #4ADE80, yellow #FACC15
```

---

## 📋 Changelog

See [CHANGELOG.md](CHANGELOG.md) for full release history.

---

## 🤝 Contributing

Contributions are welcome! Please follow clean, minimal diff practices and ensure performance-critical paths stay at sub-millisecond latency.

---

## ⚖️ Legal

This tool is provided for **educational purposes** and for use on devices you legally own. Users are responsible for compliance with local laws.

---

### Built with ❤️ by the DeepEye Community
