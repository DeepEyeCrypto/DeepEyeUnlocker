# DeepEyeUnlocker v1.1

![DeepEye Banner](assets/deepeye_readme_banner.png)

[![Build Status](https://github.com/yourusername/DeepEyeUnlocker/actions/workflows/build.yml/badge.svg)](https://github.com/yourusername/DeepEyeUnlocker/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/yourusername/DeepEyeUnlocker?color=cyan)](https://github.com/yourusername/DeepEyeUnlocker/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

### 🔷 Professional Mobile Repair. For Free. Forever

### ✨ Core Features (v1.0 MVP)

- **Device detection:** USB enumeration and mode identification.
- **Device info read:** Brand, model, IMEI, bootloader status, Android version.
- **Firmware backup:** Full read from flash.
- **Firmware flash:** Write to device via protocol.
- **Factory reset / Format:** Erase userdata + cache.
- **FRP bypass:** Factory Reset Protection removal (Qualcomm + MTK).
- **Pattern/PIN/Password removal:** Lock clearing without data loss.
- **ADB/Fastboot operations:** Reboot, shell commands, partition ops.

### 📱 Supported Chipsets (2026 Edition)

- **Qualcomm Snapdragon:** via Firehose + Sahara (Auth Bypass / EDL Unlock).
- **MediaTek (MTK):** via BROM Exploit (One-click Bootloader Unlock).
- **Samsung Galaxy:** via Download/Odin (E-Token Legacy & Modern Support).
- **Generic:** Fastboot OEM / ADB Sideload.

### 🛠 Tech Stack

- **Frontend:** C# WinForms (.NET 6.0)
- **Protocols:** Custom C# implementations or ports of MTKClient and Heimdall.
- **USB Communication:** LibUsbDotNet.
- **OS Support:** Windows 10/11 (Primary), Linux (Planned).

### 🌍 World Status: PRODUCTION READY ✅

The DeepEyeUnlocker ecosystem is now live. Automated builds and releases are configured for GitHub Actions.

---
*Disclaimer: This tool is for educational purposes or for use on devices you own. Respect local laws regarding IMEI and device unlocking.*
