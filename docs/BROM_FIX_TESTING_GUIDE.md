# 🧪 BROM Connection Crash Fix - Testing Guide

**Quick Start:** Test the fixes in under 5 minutes

---

## 📱 STEP 1: Install Updated App

```bash
cd /Users/enayat/Documents/DeepEyeUnlocker

# Uninstall old version (if exists)
adb uninstall com.deepeye.otg

# Install new version
./gradlew installDebug

# Launch app
adb shell monkey -p com.deepeye.otg -c android.intent.category.LAUNCHER 1
```

---

## 🔍 STEP 2: Connect MTK Device in BROM Mode

### Method 1: Power Off + Volume Up

1. Power off your MTK device completely
2. Hold **Volume Up** button
3. Connect USB cable to computer
4. Device should show as: `MediaTek USB Port` (VID:0E8D, PID:0003)

### Method 2: Verify BROM Mode

```bash
# Check if device is in BROM mode
lsusb | grep -i "0e8d:0003"

# Expected output:
# Bus 001 Device 012: ID 0e8d:0003 MediaTek Inc. MT65xx Preloader
```

---

## 🧪 STEP 3: Test Scenarios

### Test 1: Permission Denied (Should NOT Crash)

1. Open DeepEyeUnlocker app
2. Navigate to: **MTK Exploit** screen
3. Connect MTK device in BROM mode
4. When permission dialog appears → **DENY**
5. Click any exploit button (e.g., "Voltage Glitch")

**Expected Result:**

```
❌ USB permission not granted
💡 Please accept USB permission dialog and retry
```

**✅ PASS Criteria:**

- ✅ App does NOT crash
- ✅ Error message displayed in log console
- ✅ Buttons re-enabled after error

---

### Test 2: Permission Granted (Should Work)

1. Open DeepEyeUnlocker app
2. Navigate to: **MTK Exploit** screen
3. Connect MTK device in BROM mode
4. When permission dialog appears → **ACCEPT**
5. Wait for chip info to appear (if detected)
6. Click "Voltage Glitch" button

**Expected Result:**

```
⚡ BROM Voltage Glitch Attack
📋 CVE-2022-20223 exploit sequence
📡 Opening USB connection to BROM...
✅ Claimed interface #1
✅ USB endpoints found (IF#1)
🤝 Sending BROM handshake: A0 0A 50 05
  ↳ BROM response: 5F F5 AF FA
...
```

**✅ PASS Criteria:**

- ✅ Exploit runs without crashes
- ✅ Detailed logs shown
- ✅ Progress indicator visible
- ✅ Completion message shown

---

### Test 3: Invalid Device (Should NOT Crash)

1. Open DeepEyeUnlocker app
2. Navigate to: **MTK Exploit** screen
3. Connect **non-MTK device** (e.g., Samsung, Qualcomm)
4. Click any exploit button

**Expected Result:**

```
⚠️ Device not in BROM mode (VID:XXXX, PID:XXXX)
💡 Put device in BROM mode: Power off → Hold Vol+ → Connect USB
```

OR

```
❌ Bulk endpoints not found on any interface
💡 Device interfaces: 1
```

**✅ PASS Criteria:**

- ✅ App does NOT crash
- ✅ Clear error message shown
- ✅ Device VID:PID displayed for debugging

---

### Test 4: Cable Disconnect (Should NOT Crash)

1. Open DeepEyeUnlocker app
2. Navigate to: **MTK Exploit** screen
3. Connect MTK device in BROM mode
4. Grant USB permission
5. Click "DA Auth Bypass"
6. **Mid-operation**, disconnect USB cable

**Expected Result:**

```
❌ Exception: bulkTransfer failed
OR
❌ NullPointerException: Device not properly connected
💡 Device not properly connected
🔌 USB connection closed
```

**✅ PASS Criteria:**

- ✅ App does NOT crash
- ✅ Exception caught and logged
- ✅ Connection cleaned up safely
- ✅ Buttons re-enabled

---

### Test 5: All 3 USB Methods

Test each method individually:

| Method         | Button            | Expected Behavior                  |
| -------------- | ----------------- | ---------------------------------- |
| Voltage Glitch | ⚡ Voltage Glitch | Runs 3 attempts with timing attack |
| DA Auth Bypass | 🛡️ DA Auth Bypass | Uploads DA binary with checksum    |
| SLA Bypass     | 🔐 SLA Bypass     | Attempts Dimensity auth bypass     |

**✅ PASS Criteria for All:**

- ✅ No crashes
- ✅ Detailed logging
- ✅ Proper error handling
- ✅ Safe cleanup

---

## 📊 STEP 4: Verify Log Output

### Good Log Example:

```
[14:23:45] ⚡ Starting: BROM Voltage Glitch
[14:23:45] ⚡ BROM Voltage Glitch Attack
[14:23:45] 📋 CVE-2022-20223 exploit sequence
[14:23:45] 📡 Opening USB connection to BROM...
[14:23:45] ✅ Claimed interface #1
[14:23:45] ✅ USB endpoints found (IF#1)
[14:23:45] 🤝 Sending BROM handshake: A0 0A 50 05
[14:23:45]   ↳ BROM response: 5F F5 AF FA
[14:23:45] 💥 Opening glitch window (timing critical)...
[14:23:46] ✅ GLITCH SUCCESS on attempt 1!
[14:23:46] 🔓 BROM security bypassed!
[14:23:46] 🔌 USB connection closed
[14:23:46] ✅ Voltage Glitch: SUCCESS
```

### Error Log Example (No Permission):

```
[14:25:10] ⚡ Starting: BROM Voltage Glitch
[14:25:10] ⚡ BROM Voltage Glitch Attack
[14:25:10] 📋 CVE-2022-20223 exploit sequence
[14:25:10] 📡 Opening USB connection to BROM...
[14:25:10] ❌ USB permission not granted
[14:25:10] 💡 Please accept USB permission dialog and retry
[14:25:10] ❌ Voltage Glitch: FAILED
```

---

## 🔧 STEP 5: Debug Commands

### Check USB Permission Status:

```bash
adb shell dumpsys usb | grep -A 10 "Granted Permissions"
```

### View App Logs in Real-Time:

```bash
adb logcat -v time | grep -i "deepeye\|mtk\|brom"
```

### Check Device VID:PID:

```bash
adb shell cat /proc/bus/usb/devices | grep -A 5 "0e8d"
```

---

## ✅ PASS/FAIL CHECKLIST

| Test               | Expected                    | Actual | Status |
| ------------------ | --------------------------- | ------ | ------ |
| Permission Denied  | Error message, no crash     |        | ⬜     |
| Permission Granted | Exploit runs, detailed logs |        | ⬜     |
| Invalid Device     | Clear error, no crash       |        | ⬜     |
| Cable Disconnect   | Exception caught, cleanup   |        | ⬜     |
| Voltage Glitch     | 3 attempts, success/fail    |        | ⬜     |
| DA Auth Bypass     | DA upload, checksum         |        | ⬜     |
| SLA Bypass         | Challenge/response          |        | ⬜     |

---

## 🐛 TROUBLESHOOTING

### Issue: App Still Crashes

**Solution:**

```bash
# Check crash logs
adb logcat -v time | grep -i "FATAL\|CRASH\|Exception"

# Reinstall app
adb uninstall com.deepeye.otg
./gradlew installDebug
```

### Issue: Permission Dialog Doesn't Appear

**Solution:**

```bash
# Reset USB permissions
adb shell pm grant com.deepeye.otg android.permission.USB_PERMISSION

# Or manually grant in Settings → Apps → DeepEyeUnlocker → Permissions
```

### Issue: Device Not Detected

**Solution:**

```bash
# Check USB connection
adb devices -l

# Check if in BROM mode
lsusb | grep -i media

# Try different USB port/cable
```

---

## 📝 REPORT RESULTS

If you encounter any issues, please report:

1. **Device Model:** (e.g., Redmi Note 9, Realme C11)
2. **Chip Model:** (e.g., MT6769, MT6765)
3. **BROM Mode:** (How you entered BROM mode)
4. **VID:PID:** (From lsusb or logs)
5. **Error Message:** (Full log output)
6. **Crash Log:** (If app crashes, attach logcat)

---

## 🎯 SUCCESS CRITERIA

The fix is considered **successful** if:

- ✅ App **NEVER crashes** during BROM connection attempts
- ✅ **Clear error messages** shown for all failure scenarios
- ✅ **Detailed logs** help diagnose issues
- ✅ **Safe cleanup** on all exit paths
- ✅ **All 3 USB methods** work correctly with valid device

---

**Testing Time:** ~15 minutes  
**Difficulty:** Easy  
**Required:** MTK device, USB cable, OTG adapter (if testing from phone)
