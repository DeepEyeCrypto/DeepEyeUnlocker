# DeepEye Cloak Stealth: The 2025 Golden Standard

DeepEyeUnlocker integrates the latest research and community-consensus for root hiding and banking app compatibility as of 2025. This document outlines the technical layers and strategies used in the Cloak Center.

## 🥇 The Golden Standard Toolchain

To bypass 99% of modern application detection (including strict banking, enterprise, and gaming apps), the following toolchain is recommended:

### 1. Rooting Tool (The Base)

- **KernelSU (KSU)**: Kernel-level root with whitelist-based access. Inherently stealthier than Magisk.
- **APatch**: Modern kernel patching that combines KSU's stealth with Magisk's broad device support.
- **Magisk (Official/Kitsune)**: The classic choice. Kitsune Mask is preferred for its "SuList" (whitelist) approach.

### 2. Core Hiding Layer (Zygisk)

- **Zygisk / Zygisk Next**: Hooks into the Zygote process to permit per-app hooking. Without Zygisk, app-level hiding is nearly impossible.

### 3. The Great Hider (Shamiko)

- The industry standard for hiding root files, properties, and processes from apps in the DenyList.
- **CRITICAL**: When using Shamiko, the "Enforce DenyList" toggle in Magisk/Kitsune MUST be **OFF**.

### 4. Integrity Spoofing (Play Integrity Fix)

- Essential for passing Google Play Integrity API checks (MEETS_DEVICE_INTEGRITY).
- Crucial for Google Wallet and many modern banking apps.

### 5. Advanced App Hiding (Hide My Applist / HMA)

- Used for apps that scan your installed packages for "Magisk", "Termux", "Lucky Patcher", etc.
- Requires LSPosed.

---

## 🚫 Common "Detection Traps" & Fixes

| Trap | Detection Mechanism | Cloak Center Fix |
|------|--------------------|------------------|
| **Developer Options** | `settings get global development_settings_enabled` | Detected by Health Check; manual disable recommended. |
| **USB Debugging** | `settings get global adb_enabled` | Detected by Health Check; Cloak Stealth can spoof this flag. |
| **Accessibility Services** | Presence of active services | Detected by Health Check; user advised to disable non-essential services. |
| **Build Props** | `ro.debuggable=1`, `ro.secure=0` | **Surgical ResetProp**: Orchestrator forces these to safe values. |
| **Bootloader State** | `ro.boot.flash.locked`, `verifiedbootstate` | **Targeted Spoofing**: Orchestrator injects fakes into the process environment. |

---

## 🛠️ DeepEye Cloak Orchestration Tiers

### 🟢 Basic (DenyList Only)

Simple addition of apps to the DenyList. Sufficient for basic banking apps with mild detection.

### 🟡 Hybrid (DenyList + DevMode Hiding)

Adds host-side manipulation of Developer Options and ADB settings to fool apps into thinking the device is in a standard consumer state.

### 🟣 Maximum (Sentinel Pro)

- **Multi-layer Root Hiding**: Automated Shamiko/Zygisk configuration assistance.
- **Surgical ResetProp**: Real-time injection of system properties to mimic a locked/unrooted device.
- **Stealth Setup Wizard**: Comprehensive diagnostic and guidance for the 2025 Golden Standard.

---

## 📄 References & Credits

- **Magisk**: John Wu (@topjohnwu)
- **KernelSU**: Weishu (@tiann)
- **Shamiko/LSPosed**: LSPosed Team
- **Play Integrity Fix**: @chiteroman
- **Hide My Applist**: @Dr-TSNG
