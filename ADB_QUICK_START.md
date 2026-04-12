# 🚀 DeepEyeUnlocker - ADB Quick Start (5 Minutes)

## Step 1: Install ADB (2 min)

```bash
# Install via Homebrew (recommended)
brew install android-platform-tools

# Verify
adb version
fastboot --version
```

**Expected:**
```
Android Debug Bridge version 1.0.41
```

---

## Step 2: Prepare Device (1 min)

1. **Enable Developer Options:**
   - Settings → About Phone → Tap "Build Number" 7 times

2. **Enable USB Debugging:**
   - Settings → Developer Options → USB Debugging → ON

3. **Connect USB:**
   - Use original cable
   - Select "Transfer files" mode
   - Accept RSA dialog ("Always allow")

---

## Step 3: Test Connection (1 min)

```bash
# Quick test
adb devices

# Expected output:
List of devices attached
ABC123456789	device
```

**Status:**
- ✅ `device` = Connected
- ⚠️ `unauthorized` = Accept RSA on device
- ❌ `offline` = Reconnect USB

---

## Step 4: Run Full Test Suite (1 min)

```bash
# Make executable (first time only)
chmod +x scripts/adb_setup_and_test.sh

# Run tests
./scripts/adb_setup_and_test.sh
```

**Output:**
```
╔══════════════════════════════════════════════════════════╗
║          ✅ ALL TESTS PASSED - ADB INTEGRATION OK       ║
╚══════════════════════════════════════════════════════════╝
```

---

## Step 5: Install DeepEyeUnlocker

```bash
# Build & install
./gradlew installDebug

# Launch app
adb shell monkey -p com.deepeye.otg -c android.intent.category.LAUNCHER 1
```

---

## Essential Commands Cheat Sheet

### Device Info
```bash
adb shell getprop ro.product.device       # Codename
adb shell getprop ro.product.model         # Model
adb shell getprop ro.miui.ui.version.name  # MIUI version
```

### File Operations
```bash
adb push local.txt /sdcard/remote.txt      # Upload
adb pull /sdcard/remote.txt local.txt      # Download
```

### Package Management
```bash
adb install app.apk                        # Install
adb uninstall com.example.app              # Remove
```

### Reboot
```bash
adb reboot                                 # System
adb reboot bootloader                      # Fastboot
adb reboot recovery                        # Recovery
```

---

## Troubleshooting

### "adb: command not found"
```bash
# Check PATH
echo $PATH

# Reinstall
brew reinstall android-platform-tools
```

### "device unauthorized"
```bash
# On device:
# Settings → Developer Options → Revoke USB debugging authorizations

# Then reconnect and accept RSA dialog
```

### No devices detected
```bash
# Restart ADB
adb kill-server
adb start-server

# Check USB
system_profiler SPUSBDataType | grep Android
```

---

## Next Steps

1. ✅ ADB installed & tested
2. ✅ Device connected
3. ✅ DeepEyeUnlocker installed
4. 🎯 **Start using Pro Tools!**
   - MTK Unlock Tool
   - Xiaomi Flash Tool
   - Exploit Engines (Parts 1-10 complete!)

---

**Full Documentation:** [ADB_SETUP_GUIDE.md](ADB_SETUP_GUIDE.md)  
**GitHub:** https://github.com/DeepEyeCrypto/DeepEyeUnlocker
