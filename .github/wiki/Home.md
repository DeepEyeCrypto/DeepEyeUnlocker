# DeepEye Unlocker

> **Advanced Mobile Forensics · Decryption · Signal Analysis**

[![Release](https://img.shields.io/github/v/release/DeepEyeCrypto/DeepEyeUnlocker?style=for-the-badge&color=8B5CF6)](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/latest)
[![Build Status](https://img.shields.io/github/actions/workflow/status/DeepEyeCrypto/DeepEyeUnlocker/build.yml?style=for-the-badge&label=ENGINE_STATUS)](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/actions)
[![Integrity](https://img.shields.io/badge/STAGE-600.1_SHIELDED-00E676?style=for-the-badge)](https://github.com/DeepEyeCrypto/DeepEyeUnlocker)

---

## Overview

**DeepEye Unlocker** is a professional-grade mobile forensic engine designed for high-assurance device acquisition and decryption. Built for security researchers and digital forensics experts, it provides bit-level access to secure storage via ultra-low latency USB orchestration.

## Platform Support

| Platform | Minimum Version | Download Format | Status |
|----------|----------------|-----------------|--------|
| **macOS** | 11.0 (Big Sur+) | `.dmg` Universal (Intel + Apple Silicon) | ✅ Supported |
| **Windows** | Windows 10/11 | `.msi`, `.exe` (NSIS) | ✅ Supported |
| **Linux** | Ubuntu 20.04+ | `.AppImage`, `.deb` | ✅ Supported |
| **Android** | 8.0+ (API 26+) | `.apk` Universal | ✅ Supported |

## Core Capabilities

### 🔓 Data Decryption (Stage 300.1)
- Double-Layer Decryption: FBE-encrypted UserData + Adoptable Storage
- MTK Dimensity Support: Native decryption for Dimensity 9000+ chipsets
- TEE Key Extraction: Automated Keystore blob retrieval from RPMB
- AES-256 Hardware Acceleration

### 🛡️ Physical Integrity (Stage 600.1)
- Eye-Diagram Analysis: Real-time USB signal integrity monitoring
- Tamper Detection: Hardware interposer detection
- Signal Impedance Guard: Auto-disconnect on anomalies
- EMI Shielding protection

### 🔌 Hardened Protocol Engine
- Multi-SoC Handshake: Qualcomm Sahara (EDL), MTK BROM, Samsung Odin, UniSoc FDL
- RSA-4096 Crypto: ADB communication with SHA256-standard encryption
- Low-Latency I/O: Direct libusb orchestration (< 2ms bulk transfer)
- Fail-Safe Recovery: Automatic protocol fallback

## Quick Links

### Getting Started
- [Installation Guide](Installation.md) — Download and setup for all platforms
- [USB Setup](Installation.md#usb-setup--drivers) — Enable USB debugging and drivers
- [First Connection](Installation.md#first-device-connection) — Connect your first device

### Features
- [Android Features](Android-Features.md) — FRP Bypass, Bootloader Unlock, EDL Mode, ROM Flash
- [Apple Features](Apple-Features.md) — FMI Check, SHSH Blobs, Recovery/DFU Mode

### Development
- [Architecture Overview](Architecture.md) — Tauri v2 + React + Rust structure
- [CI/CD Pipeline](CI-CD.md) — Release workflow and build configuration
- [Contributing](../CONTRIBUTING.md) — Contribution guidelines

### Support
- [Troubleshooting](Troubleshooting.md) — Common issues and fixes
- [Changelog](Changelog.md) — Version history and stage map
- [GitHub Issues](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/issues) — Bug reports
- [GitHub Discussions](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/discussions) — Community

## System Requirements

### Minimum Hardware
- **USB 3.0 Port** — For optimal data transfer speeds
- **8GB RAM** — 16GB+ recommended for large acquisitions
- **SSD Storage** — Required for reasonable acquisition times

### Software Requirements
- **Android SDK** — API 26 to 35
- **NDK** — 25.1.8937393 (for development)
- **JDK** — 17 (Target 1.8 compatibility)
- **Build Tools** — Gradle 8.0+, CMake 3.18+

## Safety & Compliance

**DeepEye Unlocker** is developed for legitimate digital research and forensic audit purposes only.

- Use only on devices with explicit legal authorization
- Compliance with local privacy and data protection laws is mandatory
- No support provided for unauthorized access or illegal activities

---

<div align="center">

**Built with Precision for the Global Research Community.**

[Download Latest](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/latest) · [Documentation](Installation.md) · [Report Issue](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/issues)

</div>
