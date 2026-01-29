# DeepEyeUnlocker v1.4.0 "Sentinel Pro"

![DeepEye Banner](assets/deepeye_readme_banner.png)

[![Download DeepEyeUnlocker v1.4.0](https://img.shields.io/badge/Download-v1.4.0-blue.svg)](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/tag/v1.4.0)
[![Build Status](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/actions/workflows/build.yml/badge.svg)](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/DeepEyeCrypto/DeepEyeUnlocker?color=cyan)](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 🔷 Professional Mobile Repair. For Free. Forever

DeepEyeUnlocker is a free, open-source alternative to expensive mobile repair boxes. It provides enterprise-grade device unlocking, firmware management, and diagnostic tools.

---

## 🔽 Download

Latest stable release: **v1.4.0**

- ⬇️ [Download DeepEyeUnlocker v1.4.0](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/tag/v1.4.0)
- Platform: Windows (x64)
- Status: Stable Release (Sentinel Pro) - Operations, Stealth, and Fleet enabled.

---

## ✨ Features

### Core Operations

- **Device Detection:** Reactive USB discovery with WMI event-based detection
- **Device Info Read:** Brand, model, IMEI, bootloader status, Android version
- **Firmware Backup:** Streaming partition backup (supports 100GB+ devices)
- **Firmware Flash:** Write firmware via Qualcomm Firehose / MTK DA / Samsung Odin
- **Factory Reset / Format:** Erase userdata + cache partitions
- **FRP Bypass:** Factory Reset Protection removal (QC + MTK)
- **Pattern/PIN Removal:** Lock clearing without data loss

### v1.4.0 "Sentinel Pro" Highlights

- **⚡ FRP Bypass Engine:** Automated partition-level reset for Qualcomm and MediaTek chipsets (Epic A).
- **🛡️ Flash Integrity Center:** Granular flashing with mandatory SHA-256 checks and size-mismatch guards (Epic B).
- **🕵️ Cloak Stealth Pro:** Advanced system prop hiding (`resetprop`) to fool modern integrity checks (Epic C).
- **🛠️ Driver Center Pro:** Host-side utility for resolving chipset driver conflicts and INF repair (Epic D).
- **🚢 Fleet Management:** Parallel ADB orchestration and batch operations across multiple workstations (Epic E).
- **🥇 GOLD Build Standard:** Enforced "0 Errors / 0 Warnings" CI/CD with artifact integrity verification.

---

## 📱 Supported Chipsets

| Platform                | Mode             | Protocol           |
|-------------------------|------------------|--------------------|
| **Qualcomm Snapdragon** | EDL 9008         | Sahara + Firehose  |
| **MediaTek (MTK)**      | BROM / Preloader | MTK Download Agent |
| **Samsung Exynos/QC**   | Download Mode    | Odin / Loke        |
| **Generic Android**     | Fastboot / ADB   | Standard Commands  |

---

## 🏗️ Architecture

DeepEyeUnlocker v1.2.0 follows a **Clean Layered Architecture**:

```text
┌─────────────────────────────────────────┐
│              UI Layer                   │
│         (WinForms / MainForm)           │
├─────────────────────────────────────────┤
│           Operations Layer              │
│   (DeviceInfo, Backup, Flash, FRP)      │
├─────────────────────────────────────────┤
│            Core Domain                  │
│  (DeviceContext, Profiles, Resources)   │
├─────────────────────────────────────────┤
│         Protocol Engines                │
│   (Qualcomm, MTK, Samsung, ADB)         │
├─────────────────────────────────────────┤
│          Infrastructure                 │
│    (USB, Logging, Cloud, Drivers)       │
└─────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

- **Language:** C# (.NET 8.0)
- **UI Framework:** Windows Forms
- **USB:** LibUsbDotNet + WMI Event Watcher
- **Serialization:** Newtonsoft.Json
- **Database:** SQLite (History), JSON (Profiles)
- **CI/CD:** GitHub Actions

---

## 🚀 Quick Start

### Prerequisites

- Windows 10/11
- .NET 8.0 Runtime
- USB Drivers (auto-installed via Driver Center)

### Build from Source

```powershell
git clone https://github.com/DeepEyeCrypto/DeepEyeUnlocker.git
cd DeepEyeUnlocker
.\scripts\setup-dev.ps1
.\scripts\build.ps1
```

### Run

```powershell
.\artifacts\portable\DeepEyeUnlocker.exe
```

---

## 📂 Project Structure

```text
DeepEyeUnlocker/
├── src/
│   ├── Core/           # Domain models, managers
│   ├── Operations/     # Business logic operations
│   ├── Protocols/      # Chipset-specific engines
│   ├── Infrastructure/ # USB, Logging, Cloud
│   ├── Features/       # DSU Sandbox, future features
│   └── UI/             # WinForms components
├── assets/             # Profiles.json, resources
├── tests/              # Unit & integration tests
├── docs/               # Documentation
└── scripts/            # Build & setup automation
```

---

## 🗺️ Next Milestone

You can track upcoming work via the [`Next Milestone`](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/issues?q=is%3Aissue+label%3A%22Next+Milestone%22) label.

Planned for the next releases (high level):

### Planned for v1.4.0+

- **FRP Bypass Engine** – Standardized one-click FRP removal (QC Firehose / MTK DA).
- **Partition Restore Center** – Secure flashing of specific partitions (EDL/ADB) with safety checks.
- **Cloak Stealth Mode** – Active Magisk/Zygisk/Shamiko hiding automation.
- **Driver Center Pro** – One-click driver repair and USB filtering.
- **Fleet Management** – Basic support for managing multiple connected devices.

Contributions, ideas and bug reports are welcome – just open an issue and tag it with `Next Milestone` if it's roadmap material.

---

## 📦 Changelog

### v1.4.0 – Sentinel Pro Release (2026-01-29)

#### New in v1.4.0

- **FRP Engine:** Standardized partition-erase workflows for Qualcomm (Firehose) and MTK (BROM).
- **Flash Safety:** Implemented partition size validation and SHA-256 integrity checks.
- **Stealth Pro:** Automated system property manipulation (`ro.boot.*`) via surgical `resetprop` injection.
- **Fleet Hub:** Multi-device device context support with parallel batch command execution.
- **CI/CD:** Hardened build pipeline enforcing "Warnings as Errors" (GOLD Standard).

### v1.3.0 – Sentinel Release (2026-01-29)

#### New in v1.3.0

- **Report Center:** Complete device health and security audit dashboard (IMEI, Battery, Kernel, Patch Level).
- **Backup Center:** Safe partition-level backup orchestration with manifest generation and SHA256 verify.
- **ROM Sandbox integration:** Fully integrated DSU testing tab in MainForm.
- **Architecture:** Canonical models for health reports and backup jobs; standardized `IProtocolEngine`.
- **UI:** Tabbed interface updated with Report Center, Backup Center, and ROM Sandbox.
- **Tests:** Added model validation unit tests for core DTOs.

### v1.2.0 – Sandbox Preview (2026-01-29)

#### New in v1.2.0

- Core operation engine for Android device detection, ADB/Fastboot orchestration, and logging.
- Initial "Safe Unlock" workflows (structured operations layer, no aggressive one-click bypasses).
- Basic FRP & screen-lock diagnostics models (foundation for future Lock & FRP Center).
- Qualcomm protocol scaffolding (EDL/partition table interfaces) for upcoming backup/restore features.
- DSU Sandbox feature for safe ROM testing via Dynamic System Updates.
- GSI Database with curated AOSP and LineageOS images.
- Build pipeline with GitHub Actions and signed release artifacts for Windows.

#### Known Issues / Limitations

- Partition/FRP models and protocol engines are still evolving; some advanced features are stubbed or disabled.
- No production-ready IMEI/EDL/FRP write operations yet (read/diagnostics focus only).
- DSU sandbox UI is implemented but not yet integrated into MainForm tabs.
- UI/UX is functional but not final; layout and theming will change in upcoming releases.

### v1.1.1 – Archon (2026-01-28)

- Core model refactoring and type unification
- Consolidated FRP models, established canonical PartitionInfo
- Refactored operation pipeline to use DeviceContext

### v1.1.0 – Gold (2026-01-27)

- Full protocol handshakes for Qualcomm, MTK, and Samsung
- Streaming Flash Engine for gigabyte-scale flashing
- Cloak Center, Device Info Center, ADB Tools Center
- GitHub Actions CI/CD pipeline

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## ⚖️ Legal

This tool is provided for **educational purposes** and for use on devices you legally own. Users are responsible for compliance with local laws regarding IMEI modification and device unlocking.

See [docs/LEGAL.md](docs/LEGAL.md) for full disclaimer.

---

## 📄 License

MIT License - See [LICENSE](LICENSE) for details.

---

### Built with ❤️ by the DeepEyeUnlocker Community
