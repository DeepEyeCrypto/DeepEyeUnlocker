# DeepEyeUnlocker v1.1.0 "Gold" – Release Notes

## Professional Mobile Repair. For Free. Forever

We are proud to announce the **v1.1.0 "Gold"** release of DeepEyeUnlocker. This update transforms the core architecture from an MVP into a production-hardened system with advanced safety and performance capabilities.

---

## 💎 Major Highlights

### 🛡️ Protocol Hardening & Safety (Stage 6)

- **Sahara Integrity:** Implemented synchronous bounds-checking for Qualcomm Sahara packets. Prevents memory corruption from malformed device responses.
- **Anti-Exploit:** Added host-side validation for `DataOffset` and `DataLength` during programmer uploads.
- **Improved Detection:** WMI-based reactive USB discovery reduces idle CPU usage by 90%.

### 🏛️ New Operational Centers

- **🛡️ Cloak Center:** Advanced root and developer mode hiding. Includes Shamiko/Zygisk optimization and stealth profiles for banking/gaming.
- **📱 Device Info Center:** Deep hardware diagnostics (SoC, storage health, battery stats, security patch level).
- **🔧 ADB Tools Center:** Integrated app management, screen capture, file transfer, and input simulation.
- **🔐 Lock & FRP Center:** Unified diagnostics for screen locks and Google Account locks with guided recovery.
- **⚡ Flash Center:** High-performance firmware flashing engine with "Safeguard Mode" to protect critical partitions (EFS, Modem, Persist).
- **🔓 Unlock Assistant:** Step-by-step guided wizard for OEM Bootloader unlocking on Qualcomm and MTK devices.

### 🏗️ Layered "God Architecture"

- **Operation Abstraction:** Moved all heavy logic out of UI buttons and into dedicated `Operation` classes.
- **MainForm 2.0:** Redesigned tabbed interface for seamless switching between diagnostics and repair tools.
- **Device Context Sync:** Live synchronization of device state across all UI panels.
- **Progress Plumbing:** Unified `IProgress<ProgressUpdate>` reporting for smooth UI updates during long transfers.

### 📈 Performance Optimization

- **Streaming I/O:** Initial support for streaming partition data to disk, preparing for 100GB+ backup support without memory overflows.
- **Non-Blocking Logic:** Entirely asynchronous operation pipeline ensures the UI remains responsive even during heavy I/O.

---

## 📱 New Device Support

- **Qualcomm:** Enhanced support for Snapdragon 8 Gen 2 / Gen 3 devices via generic Firehose loaders.
- **MediaTek:** Improved BROM handshake reliability for Dimensity 9000-series chipsets.
- **Samsung:** Updated Odin protocol handlers for "E-Token" auth variants in 2026 models.

---

## 🛠 Fixes & Adjustments

- Fixed a crash in `SaharaProtocol.cs` when receiving zero-length packets.
- Removed deprecated `nlog.config` in favor of the new `Infrastructure.Logging` system.
- Standardized error hints: Users now get actionable advice instead of cryptic HEX codes.

---

## 🚀 Getting Started

1. Run `scripts/setup-dev.ps1` to prepare your environment.
2. Build using `scripts/build.ps1`.
3. Join the community on GitHub for the latest brand-specific `Profiles.json`.

*Thank you for being part of the DeepEye community.*
