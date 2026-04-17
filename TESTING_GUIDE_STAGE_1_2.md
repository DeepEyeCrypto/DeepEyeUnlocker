# Stage 1+2 Testing Guide

**Date**: 2026-04-17  
**Stages**: Stage 1 (Real Protocol) + Stage 2 (Session Persistence)  
**Status**: ⏳ Building → Ready to Test

---

## 🎯 What We're Testing

### Stage 1 Features:
- ✅ Real MTK BROM protocol (zero mock code)
- ✅ Byte-by-byte handshake (0xA0→0x5F, 0x0A→0xF5, 0x50→0xAF, 0x05→0xFA)
- ✅ GET_HW_CODE verification
- ✅ DISABLE_WATCHDOG
- ✅ GET_TARGET_CONFIG
- ✅ DA Part0 parser (extracts from 13MB MTK_DA_V6.bin)
- ✅ Full DA upload + jump sequence

### Stage 2 Features:
- ✅ BROM session persistence (SLA→DA)
- ✅ 60-second session timeout
- ✅ Session reuse (skip handshake on DA step)
- ✅ Expired session handling

---

## 📱 Pre-Test Setup

### 1. Device Preparation (RMX3845 - MT6789 Helio G99)

**CRITICAL: Device MUST be in BROM mode!**

```
Step 1: POWER OFF device completely
        (Not just screen off - full shutdown!)
        
Step 2: Wait 5 seconds after power off

Step 3: Hold Volume DOWN (Vol-) button

Step 4: Connect USB cable WHILE holding Vol-

Step 5: Keep holding Vol- for 3-5 seconds

Step 6: Check macOS for new USB device:
        System Profiler → USB → Should show MediaTek device
```

### 2. Verify BROM Mode (on Mac)

```bash
# Check USB devices
system_profiler SPUSBDataType | grep -A 10 "MediaTek"

# Expected output:
# MediaTek Inc. USB Device:
#   Vendor ID: 0x0e8d  ← MediaTek
#   Product ID: 0x0003 ← BROM mode
```

**If you see VID=0x22d9, device is in Android mode (WRONG!)**

### 3. Start ADB Logcat

```bash
# Clear old logs
adb logcat -c

# Start monitoring (run in separate terminal)
adb logcat | grep -E "DeepEye|Mtk|BROM|session|handshake|HW|DA|bypass" --line-buffered
```

---

## 🧪 Test Procedure

### Test 1: Fresh DA Auth Bypass (No SLA)

**Purpose**: Test Stage 1 (real protocol) without session

```
Step 1: Ensure device is in BROM mode (VID=0x0e8d PID=0x0003)

Step 2: Open DeepEye app

Step 3: Go to "DA Auth Bypass" tab

Step 4: Tap "DA Auth Bypass" button
```

**Expected Logs:**
```
🛡️ Preloader DA Auth Bypass
🔍 Target chip: 0x6789
🔌 No active session — performing fresh BROM connection
📟 USB: VID=0x0e8d PID=0x0003
✅ MediaTek BROM detected
🤝 BROM Handshake...
[HS1] 0xa0 → 0x5f ✅
[HS1] 0x0a → 0xf5 ✅
[HS1] 0x50 → 0xaf ✅
[HS1] 0x05 → 0xfa ✅
✅ BROM handshake PERFECT — 5F F5 AF FA confirmed!
📟 Sending CMD_GET_HW_CODE (0xFD)...
📟 HW Code: 0x6789
✅ HW Code 0x6789 confirmed!
🛡️ Sending CMD_DISABLE_WD (0xD4)...
🛡️ Watchdog disable status: 0x0000 ✅
🎯 Sending CMD_GET_TARGET_CFG (0xD8)...
🎯 Target config: status=0x0, config=0x...
   SBC=NO, DAA=NO, SLA=NO
📦 Loading Download Agent...
📦 MTK DA V6 total size: XXXX KB
📦 Found DA Part0:
   Load Addr: 0x00201000
   Size: XXX KB
✅ Part0 contains ARM code — valid DA!
📦 DA loaded: XXX KB
📦 DA header: 0x... 0x... 0x...
📤 CMD_SEND_DA (0xD7) — Loading XXX KB DA to 0x201000...
📤 Header sent: 13 bytes
📥 CMD_SEND_DA ACK: 0x0000 ✅
📤 Upload: 20% (XX KB / XXX KB)
📤 Upload: 40% (XX KB / XXX KB)
📤 Upload: 60% (XX KB / XXX KB)
📤 Upload: 80% (XX KB / XXX KB)
📤 Upload: 100% (XXX KB / XXX KB)
📥 BROM checksum: 0x....
▶ Sending CMD_JUMP_DA (0xD5)...
▶ CMD_JUMP_DA ACK: 0x0000 ✅
🎉 DA uploaded and executed successfully!
🎉 DA Auth Bypass COMPLETE!
✅ Download Agent is running in BROM
🔌 USB connection closed
```

**Success Criteria:**
- [ ] Handshake PERFECT (all 4 steps match)
- [ ] HW Code: 0x6789
- [ ] DA loaded (size > 64KB)
- [ ] Upload reaches 100%
- [ ] CMD_JUMP_DA ACK: 0x0000
- [ ] "DA Auth Bypass COMPLETE!"

---

### Test 2: SLA → DA Session Persistence

**Purpose**: Test Stage 2 (session reuse)

```
Step 1: Ensure device is in BROM mode

Step 2: Open DeepEye app

Step 3: Go to "SLA Bypass" tab

Step 4: Tap "SLA Auth Bypass" button
        → Wait for "💾 BROM session saved"

Step 5: IMMEDIATELY (within 60 seconds)
        → Go to "DA Auth Bypass" tab
        → Tap "DA Auth Bypass" button
```

**Expected Logs - SLA Step:**
```
🔐 SLA Auth Bypass
🔍 Chip: 0x6789
📋 Chip family: Helio G99 (RMX3845) ✅
🤝 BROM Handshake...
[HS1] 0xa0 → 0x5f ✅
[HS1] 0x0a → 0xf5 ✅
[HS1] 0x50 → 0xaf ✅
[HS1] 0x05 → 0xfa ✅
✅ BROM handshake PERFECT — 5F F5 AF FA confirmed!
✅ SLA bypassed!
💾 BROM session saved — DA bypass ready!    ← NEW!
⏱️ Session expires in 60 seconds            ← NEW!
▶ Go to DA tab and tap 'DA Auth Bypass' NOW! ← NEW!
```

**Expected Logs - DA Step (Session Reuse):**
```
🛡️ Preloader DA Auth Bypass
🔍 Target chip: 0x6789
♻️ Reusing BROM session (age: 12s)           ← NEW!
⏭️ Skipping handshake — already in BROM mode ← NEW!
🔍 Verifying BROM session — GET_HW_CODE...
📟 HW Code: 0x6789 ✅
🔧 Disabling watchdog...
🛡️ Watchdog disable status: 0x0000 ✅
🔍 GET_TARGET_CONFIG...
🎯 Target config: status=0x0, config=0x...
  SBC: DISABLED✅
  DAA: DISABLED✅
📦 Loading MTK DA Part0...
📦 DA: 384KB ✅
📤 CMD_SEND_DA (0xD7) — Loading 384KB DA to 0x201000...
📤 Upload: 100% (384KB / 384KB)
📥 BROM checksum: 0x....
▶ CMD_JUMP_DA ACK: 0x0000 ✅
🎉 DA uploaded and executed successfully!
🎉 DA Auth Bypass COMPLETE!
🔌 BROM session closed                       ← NEW!
```

**Success Criteria:**
- [ ] SLA shows "💾 BROM session saved"
- [ ] DA shows "♻️ Reusing BROM session"
- [ ] DA shows "⏭️ Skipping handshake"
- [ ] DA upload completes without re-handshake
- [ ] Session closed after DA success

---

### Test 3: Session Timeout (> 60 seconds)

**Purpose**: Test expired session handling

```
Step 1: Ensure device is in BROM mode

Step 2: Run SLA Bypass → wait for "💾 BROM session saved"

Step 3: WAIT 65 seconds (do NOT click DA)

Step 4: Tap "DA Auth Bypass" button
```

**Expected Logs:**
```
🛡️ Preloader DA Auth Bypass
🔍 Target chip: 0x6789
⏰ BROM session expired (65s old)             ← NEW!
💡 Please reconnect device in BROM mode and retry
❌ Failed
```

**Success Criteria:**
- [ ] Shows "⏰ BROM session expired"
- [ ] Shows age in seconds
- [ ] Fails gracefully (no crash)

---

### Test 4: Wrong USB Mode (VID=0x22d9)

**Purpose**: Test VID/PID validation

```
Step 1: Device in NORMAL Android mode (NOT BROM)
        (Just connect USB normally while device is on)

Step 2: Open DeepEye app

Step 3: Tap "DA Auth Bypass"
```

**Expected Logs:**
```
🛡️ Preloader DA Auth Bypass
🔍 Target chip: 0x6789
🔌 No active session — performing fresh BROM connection
📟 USB: VID=0x22d9 PID=0x0006
❌ OPPO/Realme Android mode — NOT BROM!
💡 Enter BROM: Power off → Hold Vol- → Connect USB
❌ Failed
```

**Success Criteria:**
- [ ] Detects VID=0x22d9
- [ ] Shows clear error message
- [ ] Provides BROM entry instructions
- [ ] Fails immediately (no handshake attempt)

---

## 🔍 Debugging Guide

### Issue 1: Handshake Fails

**Symptom:**
```
[HS1] 0xa0 → 0x-1 ❌
❌ Handshake failed after 3 attempts
```

**Causes:**
1. Device NOT in BROM mode (most common)
2. USB cable not connected properly
3. OTG adapter issue

**Fix:**
```bash
# Check VID/PID
system_profiler SPUSBDataType | grep -E "Vendor ID|Product ID"

# Should see:
# Vendor ID: 0x0e8d  ← MediaTek
# Product ID: 0x0003 ← BROM

# If VID=0x22d9 → Device is in Android mode!
# → Power off → Hold Vol- → Reconnect USB
```

### Issue 2: DA Upload Fails at 0KB

**Symptom:**
```
❌ Upload failed at offset 0KB after 3 retries
```

**Causes:**
1. DA binary not found in assets
2. DA Part0 parsing failed
3. BROM rejected DA binary

**Fix:**
```bash
# Check if DA file exists in APK
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "MTK_DA_V6.bin"

# Should show:
# 13631488  2026-04-17 00:00   assets/da/MTK_DA_V6.bin

# Check logs for DA parsing:
adb logcat | grep "DA Part0\|DA loaded\|DA header"
```

### Issue 3: Session Not Reused

**Symptom:**
```
🔌 No active session — performing fresh BROM connection
```

**Causes:**
1. SLA bypass failed (session never saved)
2. User waited > 60 seconds
3. App was closed/restarted

**Fix:**
```bash
# Check SLA logs first:
adb logcat | grep "SLA\|session saved"

# Should see:
# ✅ SLA bypassed!
# 💾 BROM session saved — DA bypass ready!

# If not → SLA failed, check SLA error logs
```

---

## 📊 Test Results Template

Copy this and fill in your results:

```
=== TEST RESULTS ===

Test 1: Fresh DA Auth Bypass
- Handshake: [ ] PASS / [ ] FAIL
- HW Code: [ ] 0x6789 / [ ] Other: ___
- DA Loaded: [ ] YES / [ ] NO (Size: ___ KB)
- Upload: [ ] 100% / [ ] Failed at ___%
- Jump DA: [ ] 0x0000 ✅ / [ ] Other: ___
- Overall: [ ] PASS / [ ] FAIL
- Logs: [ ] Attached / [ ] Not attached

Test 2: SLA → DA Session Persistence
- SLA Session Saved: [ ] YES / [ ] NO
- DA Session Reused: [ ] YES / [ ] NO
- Handshake Skipped: [ ] YES / [ ] NO
- DA Upload: [ ] PASS / [ ] FAIL
- Overall: [ ] PASS / [ ] FAIL

Test 3: Session Timeout
- Expired Message: [ ] YES / [ ] NO
- Graceful Fail: [ ] YES / [ ] NO
- Overall: [ ] PASS / [ ] FAIL

Test 4: Wrong USB Mode
- VID Detection: [ ] 0x22d9 / [ ] Other
- Error Message: [ ] Clear / [ ] Unclear
- Instructions: [ ] Shown / [ ] Not shown
- Overall: [ ] PASS / [ ] FAIL

=== NOTES ===
- Device: RMX3845 (MT6789 Helio G99)
- Android Version: ___
- DeepEye Version: ___
- Date: 2026-04-17
```

---

## 🚀 Quick Commands Reference

```bash
# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Start logcat
adb logcat -c && adb logcat | grep -E "DeepEye|Mtk|BROM|session|handshake|HW|DA" --line-buffered

# Check USB devices (Mac)
system_profiler SPUSBDataType | grep -A 10 "MediaTek"

# Check USB devices (Linux)
lsusb | grep -i "media\|0e8d"

# Check if device is in BROM mode
adb shell getprop ro.hardware  # Should fail in BROM mode

# Force stop app (if stuck)
adb shell am force-stop com.deepeye.unlocker

# Clear app data
adb shell pm clear com.deepeye.unlocker

# Check APK contents
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "assets/da/"
```

---

## 🎯 Next Steps After Testing

If all tests PASS:
1. Commit Stage 1 + Stage 2
2. Move to Stage 3 (DA Protocol Handler)
3. Implement DA commands (read partitions, erase FRP)

If tests FAIL:
1. Share logs with AI
2. Debug specific issue
3. Fix and retest

---

**Good luck with testing! 🍀**
