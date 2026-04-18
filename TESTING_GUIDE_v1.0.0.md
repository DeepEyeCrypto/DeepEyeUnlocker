# 🧪 DeepEyeUnlocker v1.0.0 - Testing Guide

**Date**: 2026-04-17  
**Version**: 1.0.0  
**Build**: Debug (app-debug.apk)  
**Status**: ✅ Installed & Ready for Testing

---

## 📱 Current Installation Status

```bash
✅ Package: com.deepeye.otg.debug
✅ APK: app/build/outputs/apk/debug/app-debug.apk
✅ Status: Installed on device
```

---

## 🎯 Test Scenarios

### Test 1: MediaTek BROM Mode (Primary Target)

**Device**: RMX3845 (Realme C55) - MT6789 Helio G99  
**Mode**: BROM (Power off + Vol- + USB)

#### Prerequisites:
```bash
# Verify device is NOT connected in normal mode
adb devices
# Should show empty or different device
```

#### Steps:

1. **Enter BROM Mode**:
   ```
   a. Power OFF device completely
   b. Wait 5 seconds after screen goes black
   c. Hold Volume DOWN (Vol-) button
   d. Connect USB cable WHILE holding Vol-
   e. Keep holding for 3-5 seconds
   f. Verify on Mac:
   ```
   
   ```bash
   system_profiler SPUSBDataType | grep -A 5 "MediaTek"
   # Should show:
   # Vendor ID: 0x0e8d (MediaTek)
   # Product ID: 0x0003 (BROM mode)
   ```

2. **Start Logcat Monitoring**:
   ```bash
   # Open NEW terminal
   adb logcat -c
   adb logcat | grep -E "BROM|handshake|HW Code|DA|FRP|ERASED|session" --line-buffered
   ```

3. **Test SLA Bypass**:
   ```
   a. Open DeepEye app on device
   b. Navigate to "SLA Bypass" or "BROM" tab
   c. Tap "SLA Auth Bypass" button
   d. Watch logcat for:
   ```
   
   **Expected Logs**:
   ```
   🔍 Target chip: 0x6789
   🔌 Opening USB connection...
   ✅ USB connected
   🤝 Starting BROM handshake...
   ✅ BROM handshake PERFECT — 5F F5 AF FA confirmed!
   📟 HW Code: 0x6789 ✅
   🔧 Disabling watchdog...
   🛡️ Watchdog disable status: 0x0000 ✅
   ✅ SLA bypassed!
   💾 BROM session saved — DA bypass ready!
   ⏱️ Session expires in 60 seconds
   ▶ Go to DA tab and tap 'DA Auth Bypass' NOW!
   ```

4. **Test DA Auth Bypass** (within 60 seconds):
   ```
   a. Navigate to "DA Auth Bypass" tab
   b. Tap "DA Auth Bypass" button
   c. Watch logcat for:
   ```
   
   **Expected Logs**:
   ```
   ♻️ Reusing BROM session (age: 12s)
   ⏭️ Skipping handshake — already in BROM mode
   🔍 Verifying BROM session — GET_HW_CODE...
   📟 HW Code: 0x6789 ✅
   🔧 Disabling watchdog...
   📦 Loading MTK DA Part0...
   📦 MTK DA V6 total size: 13312 KB
   📦 Found DA Part0:
      Load Addr: 0x00201000
      Size: 384 KB
   ✅ Part0 contains ARM code — valid DA!
   📦 DA: 384KB ✅
   📤 CMD_SEND_DA (0xD7) — Loading 384KB DA to 0x201000...
   📤 Upload: 20% (76KB / 384KB)
   📤 Upload: 40% (153KB / 384KB)
   📤 Upload: 60% (230KB / 384KB)
   📤 Upload: 80% (307KB / 384KB)
   📤 Upload: 100% (384KB / 384KB)
   ▶ Sending CMD_JUMP_DA (0xD5)...
   🎉 DA uploaded and executed successfully!
   ⏳ DA executing — waiting for DA protocol sync...
     DA sent: 0xC0
   ✅ DA sync received (0xC0) — DA is running!
   🎉 DA is RUNNING! BROM→DA handoff complete!
   📋 DA version string: "MTK_DA_V6.0"
   🔥 Starting FRP Erase Sequence...
   📋 Reading partition table from eMMC...
   🎯 FRP-related partitions: [frp, userdata, misc]
   🗑️ Erasing partition: frp
     Format response: 0x0000 ✅ ERASED!
   ✅ frp ERASED SUCCESSFULLY!
   🎉 FRP ERASE COMPLETE!
   📱 Rebooting device...
   🔄 Sending reboot command to DA...
   📱 Device rebooting...
   🏆 SUCCESS! FRP ERASED! Device will reboot.
   ```

5. **Verify FRP Removed**:
   ```
   a. Wait for device to reboot
   b. Complete initial setup (language, WiFi)
   c. Should NOT see "Verify your Google account" screen
   d. Should reach home screen directly
   e. FRP BYPASSED! 🎉
   ```

---

### Test 2: MediaTek META Mode (ADB Fallback)

**Device**: Any MediaTek with USB debugging enabled  
**Mode**: Normal Android with ADB

#### Prerequisites:
```bash
# Enable USB debugging on device:
# Settings → About phone → Tap "Build number" 7 times
# Settings → Developer options → USB debugging → ON

# Verify ADB connection
adb devices
# Should show device in "device" mode
```

#### Steps:

1. **Start Logcat**:
   ```bash
   adb logcat -c
   adb logcat | grep -E "META|ADB|FRP|frp|wipe|bypass|settings" --line-buffered
   ```

2. **Test META Mode Bypass**:
   ```
   a. Open DeepEye app
   b. Navigate to "META Mode" or "ADB Bypass" tab
   c. Tap "META Mode FRP Bypass" button
   d. Watch logcat for:
   ```
   
   **Expected Logs**:
   ```
   📡 Stage 7: META Mode FRP Bypass
   📡 META Mode FRP Bypass — ADB method
   🔍 Checking ADB device...
   ✅ ADB device found: ZD2226X6RW
   🔍 Checking access level...
   👤 User: shell
   ⚠️ Shell only (may still work)
   🔍 Finding FRP partition...
   ✅ FRP found: /dev/block/by-name/frp
      Size: 1024KB
   🗑️ Wiping FRP partition...
      Path: /dev/block/by-name/frp
   ✅ FRP partition wiped! (1MB zeros written)
   🧹 Clearing FRP settings database...
   ✅ Cleared: frp_credential_handle
   ✅ Cleared: user_setup_complete
   ✅ Cleared: device_provisioned
   📊 Settings cleared: 6/6
   🔄 Attempting MASTER_CLEAR broadcast...
   🔄 Rebooting device...
   🎉 META Mode FRP bypass complete!
   ```

3. **Verify FRP Removed**:
   ```
   a. Device will reboot automatically
   b. Complete setup
   c. Should NOT see Google account verification
   d. FRP BYPASSED! 🎉
   ```

---

### Test 3: Qualcomm EDL Mode

**Device**: Any Snapdragon device  
**Mode**: EDL (Power off + Vol+ + Vol- + USB)

#### Prerequisites:
```bash
# Device must be in EDL mode
# Verify:
system_profiler SPUSBDataType | grep -A 5 "Qualcomm"
# Should show:
# Vendor ID: 0x05c6 (Qualcomm)
# Product ID: 0x9008 (EDL mode)
```

#### Steps:

1. **Enter EDL Mode**:
   ```
   a. Power OFF device completely
   b. Hold Volume UP + Volume DOWN
   c. Connect USB cable
   d. Device appears as EDL (0x05C6:0x9008)
   ```

2. **Start Logcat**:
   ```bash
   adb logcat -c
   adb logcat | grep -E "EDL|Sahara|Firehose|FRP|erase|ACK" --line-buffered
   ```

3. **Test EDL FRP Bypass**:
   ```
   a. Open DeepEye app
   b. Navigate to "EDL" tab
   c. Tap "EDL FRP Bypass" button
   d. Watch logcat for:
   ```
   
   **Expected Logs**:
   ```
   🔵 Stage 9: Qualcomm EDL FRP Bypass
   🔵 Qualcomm EDL Mode FRP Bypass
   🔍 VID=0x05C6 PID=0x9008
   ✅ Qualcomm EDL device detected!
   ✅ USB connection established
   📡 Starting Sahara handshake...
     Cmd: 0x01
     Ver: 2.0
     Mode: 0
     MaxPkt: 1024 bytes
   ✅ Sahara HELLO received!
   ✅ Sahara handshake complete!
   📤 Uploading Firehose (256KB)...
     Upload: 20% (51KB / 256KB)
     Upload: 40% (102KB / 256KB)
     Upload: 60% (153KB / 256KB)
     Upload: 80% (204KB / 256KB)
     Upload: 100% (256KB / 256KB)
   ✅ Firehose upload complete!
   ⏳ Firehose initializing (3s)...
   🗑️ Firehose: Erasing FRP partition...
     📤 FH CMD: <?xml version="1.0" ?><data><erase SECTOR_SIZE_IN_BYTES=...
     📥 FH RESP: <?xml version="1.0" ?><response value="ACK" rawmode="fal...
   ✅ FRP erased via Firehose (partition name)!
   🎉 FRP ERASED SUCCESSFULLY!
   🔄 Rebooting device...
   📱 Device rebooting via Firehose!
   ```

4. **Verify FRP Removed**:
   ```
   a. Device reboots
   b. Complete setup
   c. Should NOT see Google account prompt
   d. FRP BYPASSED! 🎉
   ```

---

## 🐛 Troubleshooting

### Issue 1: "No ADB device found"

**Symptom**: `adb devices` shows empty list

**Solution**:
```bash
# Check USB cable
# Try different USB port
# Enable USB debugging on device
# Authorize computer (check device screen)

adb kill-server
adb start-server
adb devices
```

### Issue 2: Device Not in BROM Mode

**Symptom**: VID=0x22d9 instead of 0x0e8d

**Solution**:
```
1. Power OFF device completely (don't just reboot)
2. Wait 5 seconds after screen goes black
3. Hold Vol- BEFORE connecting USB
4. Keep holding for 3-5 seconds after connection
5. Verify: system_profiler SPUSBDataType | grep "0x0e8d"
```

### Issue 3: BROM Handshake Fails

**Symptom**: "BROM handshake failed" in logs

**Possible Causes**:
- Wrong USB mode (not BROM)
- USB connection unstable
- Device not fully powered off

**Solution**:
```bash
# Verify BROM mode
system_profiler SPUSBDataType | grep -A 5 "MediaTek"

# Check logs
adb logcat | grep -E "BROM|handshake|VID|PID" --line-buffered

# Retry with fresh connection
# Power off → Wait 5s → Hold Vol- → Connect USB
```

### Issue 4: DA Sync Timeout

**Symptom**: "DA sync timeout" in logs

**Possible Causes**:
- DA binary incompatible
- DA upload failed
- USB re-enumeration needed

**Solution**:
```bash
# Check logs
adb logcat | grep -E "DA sync|JUMP_DA|0xC0" --line-buffered

# Device may have re-enumerated
# Check for PID change: 0x0003 → 0x0002
system_profiler SPUSBDataType | grep "0x0e8d"

# DA may still be running even without sync
# App will proceed anyway
```

### Issue 5: FRP Erase Fails

**Symptom**: "FRP erase failed" or "Format response: 0x0001"

**Possible Causes**:
- Wrong partition name
- DA doesn't support format command
- Partition protected

**Solution**:
```bash
# Check logs for partition table
adb logcat | grep -E "partition table|FRP-related|frp" --line-buffered

# App tries 10 different partition names:
# frp, FRP, oem_dontuse_p, persistent, misc, metadata, etc.

# If all fail, app tries direct eMMC offset erase
# (RMX3845-specific: offset 0x00A00000, size 1MB)
```

---

## 📊 Test Results Template

Use this template to record test results:

```
## Test Results - DeepEyeUnlocker v1.0.0

### Test 1: MediaTek BROM Mode
- Device: RMX3845 (MT6789)
- Date: 2026-04-17
- BROM Mode Entry: ✅ / ❌
- SLA Bypass: ✅ / ❌
- DA Upload: ✅ / ❌
- DA Sync: ✅ / ❌
- FRP Erase: ✅ / ❌
- Device Reboot: ✅ / ❌
- FRP Removed: ✅ / ❌
- Notes: [any issues or observations]

### Test 2: MediaTek META Mode
- Device: [device model]
- Date: 2026-04-17
- ADB Connection: ✅ / ❌
- FRP Partition Found: ✅ / ❌
- FRP Wipe: ✅ / ❌
- Settings Cleared: ✅ / ❌
- Device Reboot: ✅ / ❌
- FRP Removed: ✅ / ❌
- Notes: [any issues or observations]

### Test 3: Qualcomm EDL Mode
- Device: [device model]
- Date: 2026-04-17
- EDL Mode Entry: ✅ / ❌
- Sahara Handshake: ✅ / ❌
- Firehose Upload: ✅ / ❌
- FRP Erase: ✅ / ❌
- Device Reboot: ✅ / ❌
- FRP Removed: ✅ / ❌
- Notes: [any issues or observations]
```

---

## 🎯 Quick Commands Reference

```bash
# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.deepeye.otg.debug/.MainActivity

# Monitor BROM logs
adb logcat | grep -E "BROM|DA|FRP|ERASED" --line-buffered

# Monitor META logs
adb logcat | grep -E "META|ADB|frp|wipe" --line-buffered

# Monitor EDL logs
adb logcat | grep -E "EDL|Sahara|Firehose|ACK" --line-buffered

# Check USB devices (Mac)
system_profiler SPUSBDataType | grep -A 5 "MediaTek\|Qualcomm"

# Force stop app
adb shell am force-stop com.deepeye.otg.debug

# Clear app data
adb shell pm clear com.deepeye.otg.debug

# Uninstall app
adb uninstall com.deepeye.otg.debug
```

---

## 🚀 Ready to Test!

**Current Status**: ✅ APK installed, ready for testing

**Next Steps**:
1. Choose test scenario (BROM / META / EDL)
2. Put device in correct mode
3. Start logcat monitoring
4. Run test in app
5. Record results
6. Report any issues

**Good luck with testing!** 🎉
