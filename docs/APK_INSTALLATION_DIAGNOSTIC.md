# APK Installation Diagnostic Report

**Date**: 2026-04-17  
**Status**: ✅ **INSTALLATION SUCCESSFUL**  
**APK**: app-debug.apk (84MB)  
**Package**: com.deepeye.otg.debug

---

## ✅ Installation Result

```bash
$ adb install -r app/build/outputs/apk/debug/app-debug.apk
Performing Streamed Install
Success

$ adb shell pm list packages | grep deepeye
package:com.deepeye.otg.debug  ← INSTALLED!
```

**The APK is now installed on your device!** 🎉

---

## 📱 Device Status

### Current Mode: **Normal Android** ✅

```
Device: motorola_edge_30_pro (hiphi)
Serial: ZD2226X6RW
USB: 20-1
Mode: device (normal Android)
```

**This is CORRECT for APK installation!**

### ⚠️ Important Clarification:

**BROM mode is NOT needed for APK installation!**

| Task                   | Required Mode     | VID:PID             |
| ---------------------- | ----------------- | ------------------- |
| **Install APK**        | Normal Android ✅ | Any (ADB must work) |
| **Run SLA Bypass**     | BROM Mode         | 0x0e8d:0x0003       |
| **Run DA Auth Bypass** | BROM Mode         | 0x0e8d:0x0003       |
| **Test BROM Protocol** | BROM Mode         | 0x0e8d:0x0003       |

---

## 🔄 When to Switch to BROM Mode

**You only need BROM mode when:**

1. Tapping "SLA Auth Bypass" button
2. Tapping "DA Auth Bypass" button
3. Testing MTK BROM protocol

**For everything else (installing APK, browsing UI, etc.), keep device in normal Android mode.**

---

## 📋 Next Steps: Testing BROM Features

### Step 1: Verify App Launches

```bash
# Launch the app
adb shell am start -n com.deepeye.otg.debug/.MainActivity

# Or just tap the app icon on your device
```

**Expected**: App opens, shows main screen with features

### Step 2: Prepare Device for BROM Testing

**CRITICAL: Device must be in BROM mode before tapping SLA/DA buttons!**

```
1. POWER OFF device completely
   (Settings → Power off, or hold power button)

2. Wait 5 seconds after screen goes black

3. Hold Volume DOWN (Vol-) button

4. Connect USB cable WHILE holding Vol-

5. Keep holding Vol- for 3-5 seconds

6. Verify on Mac:
   system_profiler SPUSBDataType | grep -A 5 "MediaTek"

   Should show:
   Vendor ID: 0x0e8d  ← MediaTek
   Product ID: 0x0003 ← BROM mode
```

### Step 3: Start Logcat Monitoring

Open a **NEW terminal** and run:

```bash
# Clear old logs
adb logcat -c

# Start monitoring BROM-related logs
adb logcat | grep -E "DeepEye|Mtk|BROM|session|handshake|HW|DA|bypass" --line-buffered
```

**Keep this terminal open while testing!**

### Step 4: Test SLA → DA Flow

1. **Open DeepEye app** on device
2. **Navigate to "SLA Bypass" tab**
3. **Tap "SLA Auth Bypass" button**
4. **Watch logcat terminal** for:

   ```
   💾 BROM session saved — DA bypass ready!
   ⏱️ Session expires in 60 seconds
   ```

5. **IMMEDIATELY** (within 60 seconds):
   - Navigate to "DA Auth Bypass" tab
   - Tap "DA Auth Bypass" button
   - Watch logcat for:
   ```
   ♻️ Reusing BROM session (age: XXs)
   ⏭️ Skipping handshake — already in BROM mode
   📦 DA: XXX KB loaded
   📤 Upload: 100%
   🎉 DA Auth Bypass COMPLETE!
   ```

---

## 🔍 Troubleshooting Guide

### Issue 1: "App not installed" Error

**Symptom**: Installation fails with "App not installed"

**Possible Causes**:

1. Corrupted APK file
2. Insufficient storage on device
3. Conflicting signature (older version installed)

**Solutions**:

```bash
# Solution 1: Uninstall old version first
adb uninstall com.deepeye.otg.debug
adb uninstall com.deepeye.otg  # Try both package names

# Solution 2: Clear package manager cache
adb shell pm clear com.deepeye.otg.debug

# Solution 3: Rebuild APK
./gradlew :app:clean :app:assembleDebug --no-daemon

# Solution 4: Install without replacing
adb install app/build/outputs/apk/debug/app-debug.apk  # Remove -r flag
```

### Issue 2: "adb: device not found"

**Symptom**: `adb devices` shows empty list or "unauthorized"

**Solutions**:

```bash
# Solution 1: Check USB connection
adb devices -l

# If empty:
# - Check USB cable
# - Try different USB port
# - Enable USB debugging on device:
#   Settings → About phone → Tap "Build number" 7 times
#   Settings → Developer options → USB debugging → ON

# Solution 2: Restart ADB server
adb kill-server
adb start-server
adb devices

# Solution 3: Check USB mode on device
# Pull down notification shade → Tap "USB for..." → Select "File transfer" or "MTP"

# Solution 4: Authorize computer (if shows "unauthorized")
# Check device screen for authorization dialog → Tap "Allow"
```

### Issue 3: "INSTALL_FAILED_UPDATE_INCOMPATIBLE"

**Symptom**: Cannot install over existing version

**Solution**:

```bash
# Uninstall existing version
adb uninstall com.deepeye.otg.debug

# Or clear data
adb shell pm clear com.deepeye.otg.debug

# Then install again
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Issue 4: "INSTALL_FAILED_INSUFFICIENT_STORAGE"

**Symptom**: Not enough space on device

**Solution**:

```bash
# Check available storage
adb shell df -h /data

# If low on space:
# - Delete unused apps
# - Clear app caches
adb shell pm clear com.deepeye.otg.debug

# Or install to external storage (if supported)
adb install -s app/build/outputs/apk/debug/app-debug.apk
```

### Issue 5: APK Builds but Installation Fails

**Symptom**: Build succeeds but `adb install` fails

**Check APK integrity**:

```bash
# Verify APK exists and has reasonable size
ls -lh app/build/outputs/apk/debug/app-debug.apk

# Should be 50-100MB for debug build
# If 0 bytes or very small → Build failed silently

# Check APK contents
unzip -l app/build/outputs/apk/debug/app-debug.apk | head -20

# Should show AndroidManifest.xml, classes.dex, etc.
```

**Rebuild from scratch**:

```bash
# Clean and rebuild
./gradlew :app:clean :app:assembleDebug --no-daemon 2>&1 | tail -20

# Check for build errors
./gradlew :app:assembleDebug --no-daemon 2>&1 | grep -E "error:|FAILED"
```

---

## 🛠️ Alternative Installation Methods

### Method 1: Android Studio

1. Open project in Android Studio
2. Click "Run" button (green play icon)
3. Select device from dialog
4. Android Studio will build, install, and launch app

**Advantages**:

- Automatic build + install + launch
- Shows logcat in real-time
- Can set breakpoints for debugging

### Method 2: Manual Install via File Manager

1. Copy APK to device:

   ```bash
   adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/
   ```

2. On device:
   - Open "Files" app
   - Navigate to Download folder
   - Tap `app-debug.apk`
   - Allow "Install from unknown sources" if prompted
   - Tap "Install"

**Note**: Only works if "Unknown sources" is enabled in Settings

### Method 3: Wireless ADB (WiFi)

```bash
# Enable wireless ADB (Android 11+)
# On device: Settings → Developer options → Wireless debugging → ON

# Connect via WiFi
adb tcpip 5555
adb connect 192.168.1.XXX:5555  # Replace with device IP

# Then install normally
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📊 Installation Checklist

Use this checklist to verify everything is ready for testing:

```
□ APK installed successfully
  Run: adb shell pm list packages | grep deepeye
  Expected: package:com.deepeye.otg.debug

□ App launches without crash
  Run: adb shell am start -n com.deepeye.otg.debug/.MainActivity
  Expected: App opens on device

□ USB debugging enabled
  Settings → Developer options → USB debugging → ON

□ Device can enter BROM mode
  □ Power off completely
  □ Hold Vol-
  □ Connect USB
  □ Verify: system_profiler SPUSBDataType | grep "0x0e8d"

□ Logcat monitoring ready
  Terminal 1: adb logcat -c && adb logcat | grep -E "DeepEye|Mtk|BROM"

□ Test plan reviewed
  Read: TESTING_GUIDE_STAGE_1_2.md
```

---

## 🎯 Quick Reference Commands

```bash
# Check if app is installed
adb shell pm list packages | grep deepeye

# Launch app
adb shell am start -n com.deepeye.otg.debug/.MainActivity

# Force stop app
adb shell am force-stop com.deepeye.otg.debug

# Clear app data
adb shell pm clear com.deepeye.otg.debug

# Uninstall app
adb uninstall com.deepeye.otg.debug

# View app logs
adb logcat | grep -i deepeye

# Check ADB connection
adb devices -l

# Verify APK file
ls -lh app/build/outputs/apk/debug/app-debug.apk

# Rebuild APK
./gradlew :app:assembleDebug --no-daemon

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📝 Important Notes

1. **BROM mode is ONLY for SLA/DA testing**
   - Keep device in normal mode for everything else
   - You CANNOT install APK in BROM mode (ADB won't work)

2. **Session timeout is 60 seconds**
   - After SLA bypass, you have 60 seconds to tap DA bypass
   - If timeout expires, reconnect device in BROM mode

3. **Each test requires fresh BROM connection**
   - Power off → Hold Vol- → Connect USB
   - Don't just reboot - must be full power off

4. **Logcat is your best friend**
   - Always run logcat while testing
   - Shows exactly what's happening in real-time
   - Essential for debugging issues

---

## ✅ Current Status Summary

| Item              | Status     | Details                       |
| ----------------- | ---------- | ----------------------------- |
| APK File          | ✅ EXISTS  | 84MB, built at 15:07          |
| ADB Connection    | ✅ WORKING | Device: ZD2226X6RW            |
| Installation      | ✅ SUCCESS | package:com.deepeye.otg.debug |
| Device Mode       | ✅ NORMAL  | Correct for installation      |
| Ready for Testing | ⏳ PENDING | Need BROM mode for SLA/DA     |

---

**Next Action**: Power off device → Enter BROM mode → Test SLA → DA flow! 🚀
