# DeepEyeUnlocker - ADB Setup & Testing Guide

## 📋 Table of Contents
1. [ADB Installation (macOS)](#1-adb-installation-macos)
2. [Device Preparation](#2-device-preparation)
3. [Connectivity Testing](#3-connectivity-testing)
4. [Integration Testing](#4-integration-testing)
5. [Troubleshooting](#5-troubleshooting)
6. [DeepEyeUnlocker Integration](#6-deepeyeunlocker-integration)

---

## 1. ADB Installation (macOS)

### Method 1: Homebrew (Recommended)

```bash
# Install Homebrew (if not installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install ADB & Fastboot
brew install android-platform-tools

# Verify installation
adb version
fastboot --version
```

**Expected Output:**
```
Android Debug Bridge version 1.0.41
Version 34.0.5-10900687
```

### Method 2: Manual Installation

```bash
# Download platform tools
curl -O https://dl.google.com/android/repository/platform-tools-latest-darwin.zip

# Extract to /opt/android
sudo mkdir -p /opt/android
sudo unzip platform-tools-latest-darwin.zip -d /opt/android/

# Add to PATH (~/.zshrc)
echo 'export PATH="/opt/android/platform-tools:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Verify
adb version
fastboot --version
```

### Verify Installation

```bash
# Check ADB
which adb
# Expected: /opt/homebrew/bin/adb or /opt/android/platform-tools/adb

# Check Fastboot
which fastboot
# Expected: /opt/homebrew/bin/fastboot or /opt/android/platform-tools/fastboot
```

---

## 2. Device Preparation

### Enable Developer Options

1. **Open Settings** → **About Phone**
2. **Tap "Build Number" 7 times**
3. You'll see: *"You are now a developer!"*

### Enable USB Debugging

1. **Settings** → **Developer Options**
2. **Enable:**
   - ✅ USB Debugging
   - ✅ OEM Unlocking (for bootloader operations)
   - ✅ Stay Awake (optional, for testing)

### Connect Device

1. **Use original USB cable** (charging-only cables won't work)
2. **Connect to Mac**
3. **On device popup, select:**
   - Transfer files / Android Auto
4. **Accept RSA fingerprint:**
   - Check "Always allow from this computer"
   - Tap "Allow"

### Verify Connection

```bash
# List connected devices
adb devices

# Expected output:
List of devices attached
ABC123456789	device
```

**Status Meanings:**
- `device` → ✅ Connected and authorized
- `unauthorized` → ⚠️ Accept RSA dialog on device
- `offline` → ❌ Reconnect USB or restart ADB

---

## 3. Connectivity Testing

### Quick Test Script

```bash
# Make executable
chmod +x scripts/adb_setup_and_test.sh

# Run full test suite
./scripts/adb_setup_and_test.sh
```

### Manual Tests

#### Device Detection
```bash
# List devices
adb devices

# Get device count
adb devices | grep -c "device$"

# Get serial
adb shell getprop ro.serialno
```

#### Device Information
```bash
# Basic info
adb shell getprop ro.product.device      # Codename (e.g., "vili")
adb shell getprop ro.product.model        # Model (e.g., "2201123G")
adb shell getprop ro.product.manufacturer # Manufacturer (e.g., "Xiaomi")
adb shell getprop ro.build.version.release # Android version (e.g., "13")
adb shell getprop ro.build.version.sdk    # SDK level (e.g., "33")

# Full build fingerprint
adb shell getprop ro.build.fingerprint

# All properties
adb shell getprop | less
```

#### Xiaomi-Specific
```bash
# MIUI version
adb shell getprop ro.miui.ui.version.name  # e.g., "V14"
adb shell getprop ro.miui.ui.version.code  # e.g., "14"

# Xiaomi market name
adb shell getprop ro.product.marketname

# Check if HyperOS
adb shell getprop ro.miui.ui.version.name | grep -i "hyper"
```

#### Shell Operations
```bash
# Shell access
adb shell whoami        # Expected: "shell"
adb shell id            # Expected: uid=2000(shell)

# File system access
adb shell ls /sdcard/
adb shell ls /system/

# Battery info
adb shell dumpsys battery

# Memory info
adb shell dumpsys meminfo | head -20
```

#### File Operations
```bash
# Push file to device
echo "Test file" > /tmp/test.txt
adb push /tmp/test.txt /sdcard/test.txt

# Verify
adb shell cat /sdcard/test.txt

# Pull file from device
adb pull /sdcard/test.txt /tmp/pulled_test.txt

# Cleanup
rm /tmp/test.txt /tmp/pulled_test.txt
adb shell rm /sdcard/test.txt
```

#### Package Management
```bash
# List all packages
adb shell pm list packages

# Third-party only
adb shell pm list packages -3

# System packages
adb shell pm list packages -s

# Check if app installed
adb shell pm list packages | grep deepeye

# Get app version
adb shell dumpsys package com.deepeye.otg | grep versionName

# Install APK
adb install path/to/app.apk

# Reinstall (keep data)
adb install -r path/to/app.apk

# Uninstall
adb uninstall com.example.app
```

---

## 4. Integration Testing

### Test All DeepEyeUnlocker ADB Commands

```bash
# 1. Device detection
adb shell getprop ro.product.device
adb shell getprop ro.product.model
adb shell getprop ro.miui.ui.version.name

# 2. Input events (used in screen lock bypass)
adb shell input keyevent 82    # MENU
adb shell input keyevent 66    # ENTER
adb shell input keyevent 4     # BACK
adb shell input keyevent 20    # DOWN

# 3. Activity management
adb shell am start -a android.settings.SECURITY_SETTINGS
adb shell am force-stop com.android.settings
adb shell am start -n com.deepeye.otg/.ui.MainActivity

# 4. Package operations
adb shell pm clear com.android.settings
adb shell pm disable-user --user 0 com.miui.guardprovider

# 5. Settings manipulation
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb shell settings get global adb_enabled

# 6. Content provider operations
adb shell content insert --uri content://settings/global --bind name:s:device_provisioned --bind value:s:1

# 7. Broadcast intents
adb shell am broadcast -a android.intent.action.MASTER_CLEAR_NOTIFICATION
```

### Fastboot Tests (Device must be in bootloader mode)

```bash
# Reboot to fastboot
adb reboot bootloader

# Wait for device, then test:
fastboot devices
fastboot getvar unlocked
fastboot getvar anti
fastboot getvar serialno
fastboot getvar product
fastboot getvar version-bootloader

# Reboot back to system
fastboot reboot
```

### EDL Mode Tests (Qualcomm devices)

```bash
# Reboot to EDL (requires root or engineering cable)
adb reboot edl

# Check for EDL device (macOS)
system_profiler SPUSBDataType | grep -A 10 "05c6:9008"

# Check with lsusb (Linux)
lsusb | grep "05c6:9008"
```

---

## 5. Troubleshooting

### Common Issues

#### "adb: command not found"
```bash
# Check PATH
echo $PATH

# If missing, add to ~/.zshrc:
echo 'export PATH="/opt/homebrew/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

#### "device unauthorized"
```bash
# Revoke USB debugging authorizations on device:
# Settings → Developer Options → Revoke USB debugging authorizations

# Restart ADB server
adb kill-server
adb start-server

# Reconnect USB cable and accept RSA dialog
```

#### "device offline"
```bash
# Restart ADB server
adb kill-server
adb start-server

# Check USB cable (try different port/cable)
# Ensure "Transfer files" mode is selected
```

#### No devices detected
```bash
# 1. Check USB connection
system_profiler SPUSBDataType | grep -A 5 "Android"

# 2. Restart ADB
adb kill-server
adb start-server

# 3. Check if USB debugging is enabled on device

# 4. Try different USB port/cable

# 5. Check Mac USB permissions (System Settings → Privacy & Security)
```

#### Permission denied
```bash
# On macOS, no udev rules needed
# On Linux, add udev rules:
sudo nano /etc/udev/rules.d/51-android.rules

# Add (replace with your device VID):
SUBSYSTEM=="usb", ATTR{idVendor}=="05c6", MODE="0666", GROUP="plugdev"

# Reload rules
sudo udevadm control --reload-rules
sudo udevadm trigger
```

### Advanced Debugging

```bash
# Verbose ADB output
adb -v devices

# ADB server log
adb logcat -s AdbDebugging

# Check ADB connection
adb shell echo "Connection OK"

# Test shell responsiveness
adb shell "echo test && sleep 1 && echo done"

# Check ADB version compatibility
adb version
adb shell getprop ro.build.version.release
```

---

## 6. DeepEyeUnlocker Integration

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│  React Frontend (TypeScript)                            │
│  ├── src/hooks/useAdb.ts                                │
│  ├── src/pages/AdbPage.tsx                              │
│  └── src/pages/SettingsPage.tsx                         │
└────────────────────┬────────────────────────────────────┘
                     │ Tauri IPC
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Tauri Backend (Rust)                                   │
│  ├── src-tauri/src/commands/adb.rs                      │
│  └── src-tauri/src/adb/                                 │
└────────────────────┬────────────────────────────────────┘
                     │ System commands
                     ▼
┌─────────────────────────────────────────────────────────┐
│  ADB Binary (platform-tools)                            │
│  ├── /opt/homebrew/bin/adb (Homebrew)                   │
│  └── /opt/android/platform-tools/adb (Manual)           │
└────────────────────┬────────────────────────────────────┘
                     │ USB/Network
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Android Device                                         │
│  ├── USB Debugging enabled                              │
│  └── DeepEyeUnlocker APK installed                      │
└─────────────────────────────────────────────────────────┘
```

### Configuration

#### Settings Page (`src/pages/SettingsPage.tsx`)

```typescript
// ADB binary path configuration
settings.adbBinaryPath = "/opt/homebrew/bin/adb"  // Default: "adb"

// ADB over TCP
settings.adbOverTcp = false  // Enable for wireless ADB
settings.tcpPort = 5555      // Default TCP port
```

#### Tauri Backend (`src-tauri/src/commands/adb.rs`)

```rust
// ADB command execution
#[tauri::command]
async fn adb_test_binary(path: String) -> Result<String, String> {
    // Executes: adb version
    // Returns version string or error
}

// Device detection
#[tauri::command]
async fn adb_detect_devices() -> Result<Vec<DeviceInfo>, String> {
    // Executes: adb devices
    // Returns list of connected devices
}
```

#### Kotlin Android (`app/src/main/kotlin/com/deepeye/otg/device/AdbFastbootEngine.kt`)

```kotlin
// ADB operations in Android app
class AdbFastbootEngine {
    suspend fun detectDevice(): DeviceInfo {
        // Uses adb shell getprop commands
    }
    
    suspend fun executeShell(command: String): String {
        // Executes: adb shell <command>
    }
    
    suspend fun flashImage(partition: String, image: File): Boolean {
        // Executes: fastboot flash <partition> <image>
    }
}
```

### Testing Integration

#### 1. Test ADB Binary Path

```bash
# In DeepEyeUnlocker Settings:
# Settings → ADB Config → ADB binary path
# Enter: /opt/homebrew/bin/adb (or your ADB path)
# Click: "Test ADB"
```

#### 2. Test Device Detection

```bash
# Ensure device is connected
adb devices

# In DeepEyeUnlocker:
# Main Screen → Command → Devices
# Device should appear in list
```

#### 3. Test Pro Tools

```bash
# Navigate to:
# Main Screen → Pro Tools → Device

# Test MTK Unlock:
# - Detect MTK chip
# - Run voltage glitch (CVE-2022-20223)
# - Execute DA auth bypass

# Test Xiaomi Flash:
# - Detect Xiaomi device
# - Read device info (codename, MIUI version)
# - Flash boot image (dry run)
```

#### 4. Test Exploit Engines

```bash
# MTK Exploit Engine:
# - Connect MTK device in BROM mode
# - Run: MtkExploitEngine.bromVoltageGlitch()
# - Check logs in UI

# Xiaomi Exploit Engine:
# - Connect Xiaomi device via ADB
# - Run: XiaomiExploitEngine.bypassMiAccount()
# - Verify in device settings
```

### Automated Testing

```bash
# Run Jest tests (Tauri/React)
npm test
# or
pnpm test

# Run ADB integration tests
./test_xiaomi_flash_adb.sh

# Run full setup & test suite
./scripts/adb_setup_and_test.sh

# Run Kotlin tests
./gradlew test
```

### Logging & Debugging

```bash
# View ADB logs (Tauri)
# Check console in developer tools (Cmd+Option+I)

# View Android logs
adb logcat | grep -i deepeye

# View Kotlin logs
adb logcat -s MtkExploitEngine
adb logcat -s XiaomiExploitEngine

# Verbose USB logging (in Settings)
# Enable: USB debug logging
```

---

## Quick Reference

### Essential Commands

```bash
# Device management
adb devices                          # List devices
adb -s <serial> shell                # Shell on specific device
adb kill-server && adb start-server  # Restart ADB

# Information
adb shell getprop ro.product.device  # Codename
adb shell getprop ro.miui.ui.version.name  # MIUI version

# File operations
adb push <local> <remote>            # Push file
adb pull <remote> <local>            # Pull file

# Package management
adb install app.apk                  # Install APK
adb uninstall com.example.app        # Uninstall

# Reboot
adb reboot                           # System
adb reboot bootloader                # Fastboot
adb reboot recovery                  # Recovery
adb reboot edl                       # EDL mode
```

### DeepEyeUnlocker Specific

```bash
# Install app
./gradlew installDebug

# Launch app
adb shell monkey -p com.deepeye.otg -c android.intent.category.LAUNCHER 1

# View logs
adb logcat | grep -E "DeepEye|MtkExploit|XiaomiExploit"

# Test ADB from Settings
# Settings → ADB Config → Test ADB
```

---

## Support

- **GitHub:** https://github.com/DeepEyeCrypto/DeepEyeUnlocker
- **Issues:** Report bugs with full ADB logs
- **Documentation:** Check `/docs` folder for detailed guides

---

**Last Updated:** April 2026  
**ADB Version:** 34.0.5+  
**Tested On:** macOS 15.7.3, Android 13/14, MIUI 14/HyperOS
