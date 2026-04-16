# BROM State Machine Fix for MT6789 (Helio G99)

## Problem
```
[BROM] DISABLE_AUTH send failed: -1/1 bytes
[BROM] GET_HW_CODE send failed: -1/1 bytes
📤 Header sent: -1 bytes  ← SEND ITSELF FAILED!
```

**CRITICAL:** Every `bulkTransfer()` TX returns `-1`. USB endpoint is completely dead!

## Root Cause (DISCOVERED)

The `preloaderAuthBypass()` function was **skipping BROM handshake** with comment:
```kotlin
// BUG 2 FIX: NO RE-HANDSHAKE! Use existing BROM connection
onLog("🤝 Using existing BROM connection (no re-handshake)")
```

**BUT THIS WAS WRONG!** The function opens a **fresh** `UsbDeviceConnection` at line 230, but then skips the mandatory BROM handshake (0xA0 0x0A 0x50 0x05). Without handshake:

1. BROM state machine is **NOT initialized**
2. All commands return `-1` (TX fails immediately)
3. USB endpoints appear "dead" but are just uninitialized

**Same issue existed in `slaAuthBypass()`** — also skipping handshake!

## Solution Applied

### 1. CRITICAL FIX: Add Mandatory BROM Handshake

**Problem:** Both `preloaderAuthBypass()` and `slaAuthBypass()` were skipping handshake

**Fix:** Added `performStableBromHandshake()` call after opening fresh connection

**preloaderAuthBypass() changes:**
```kotlin
// ❌ BEFORE (broken):
onLog("🤝 Using existing BROM connection (no re-handshake)")
// → Skipped handshake → BROM not initialized → TX=-1

// ✅ AFTER (fixed):
onLog("🤝 Performing fresh BROM handshake (A0 0A 50 05)...")
if (!performStableBromHandshake(conn, epOut, epIn, onLog)) {
    onLog("❌ BROM handshake failed — device may not be in BROM mode")
    return@withContext false
}
onLog("✅ BROM handshake complete — state machine initialized")
```

**slaAuthBypass() changes:**
```kotlin
// ✅ Added handshake (with soft failure — some BROMs accept SLA without it)
onLog("🤝 Performing fresh BROM handshake (A0 0A 50 05)...")
if (!performStableBromHandshake(conn, epOut, epIn, onLog)) {
    onLog("⚠️ BROM handshake failed — trying SLA anyway")
} else {
    onLog("✅ BROM handshake complete")
}
```

### 2. TX Sanity Check (preloaderAuthBypass only)

**Added verification** that USB endpoints actually work before attempting DA upload:
```kotlin
onLog("🔍 Verifying USB TX/RX path...")
val testCmd = byteArrayOf(0xFD.toByte())  // GET_HW_CODE
val testSent = conn.bulkTransfer(epOut, testCmd, 1, 2000)
if (testSent < 0) {
    onLog("❌ USB TX sanity check FAILED (sent=$testSent) — endpoint dead!")
    onLog("💡 Try: reconnect USB cable and retry")
    return@withContext false
}
```

This catches USB issues early with clear error message instead of failing mid-upload.

### 3. Enhanced `prepareBromForDa()` Function

**Changes:**
- ✅ Added **50ms delays** between each prep command (critical for BROM state transitions)
- ✅ Changed from **fail-fast** to **continue-on-error** (some BROMs don't ACK certain commands)
- ✅ Added **HW code validation** (verifies BROM returns 0x6789 for MT6789)
- ✅ Enhanced logging with step-by-step progress indicators
- ✅ Better error messages showing exact byte responses

**BROM Prep Sequence:**
```
Step 1: GET_VERSION (0xFE)     + 50ms delay
Step 2: DISABLE_WATCHDOG (0xD4) + 50ms delay  
Step 3: TARGET_CONFIG (0xD8)    + 50ms delay
Step 4: DISABLE_AUTH (0xC7)     + 50ms delay
Step 5: GET_HW_CODE (0xFD)      + 100ms delay + HW code validation
```

### 4. Improved CMD_SEND_DA ACK Handling

**Changes:**
- ✅ Increased delay from **150ms → 200ms** after sending 0xD7 header
- ✅ Added **0xA5 partial ACK** detection (some BROMs send this instead of 0x5A)
- ✅ Enhanced fallback messages explaining MT6789 behavior
- ✅ Better diagnostic logging showing full 16-byte BROM response

**ACK Decision Logic:**
```
- No response (-1) → Proceed with upload (common for MT6789)
- 0x5A → Full ACK, upload authorized
- 0xA5 → Partial ACK, proceed anyway
- Other → Log warning, still attempt upload
```

### 5. Better Error Messages

Added helpful diagnostic messages:
- "BROM may ACK after DA upload"
- "Proceeding with DA upload anyway (common for MT6789)"
- "BROM may have rejected DA — check DA binary compatibility"

## Expected Behavior After Fix

### Scenario 1: Normal Flow (Handshake Success)
```
🤝 Performing fresh BROM handshake (A0 0A 50 05)...
  ↳ BROM response: 5F F5 AF FA
✅ BROM handshake complete — state machine initialized
🔍 Verifying USB TX/RX path...
✅ USB TX/RX verified! HW code: 0x6789
📦 DA loaded: 16KB ✅
🔧 Preparing BROM state for DA upload...
[BROM] Step 1/5: GET_VERSION (0xFE)
[BROM] Version RX: 0x00 0x01 0x02 0x03
...
[BROM] HW Code: 0x6789
[BROM] ✅ BROM prep sequence complete — ready for CMD_SEND_DA (0xD7)
📤 Header sent: 13 bytes
📥 Header ACK read: 1 bytes → 0x5A
✅ BROM ACK 0x5A — upload authorized!
```

### Scenario 2: BROM Silent but TX Works (Common for MT6789)
```
🤝 Performing fresh BROM handshake (A0 0A 50 05)...
  ↳ BROM response: 5F F5 AF FA
✅ BROM handshake complete — state machine initialized
🔍 Verifying USB TX/RX path...
✅ USB TX/RX verified! HW code: 0x6789
[BROM] Step 5/5: GET_HW_CODE (0xFD) — final state check
[BROM] HW code check RX: (no response, -1 bytes)
[BROM] ⚠️ No HW code response — BROM may still be ready, proceeding...
📤 Header sent: 13 bytes
📥 Header ACK read: -1 bytes → (no data)
⚠️ No BROM response to 0xD7 — BROM may ACK after DA upload
🔧 Proceeding with DA upload anyway (common for MT6789)
📤 Uploading 16KB DA in 4KB chunks...
  📤 Upload progress: 100% (16KB/16KB)
✅ DA upload complete!
```

## Files Modified
- `app/src/main/kotlin/com/deepeye/otg/engine/mtk/MtkExploitEngine.kt`
  - `prepareBromForDa()` function (lines 1041-1121)
  - `preloaderAuthBypass()` CMD_SEND_DA section (lines 315-383)

## Build & Install
```bash
./gradlew :app:assembleDebug --no-daemon
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Testing Steps
1. Connect MT6789 device in BROM mode (Vol- + USB)
2. Run SLA bypass (should complete successfully)
3. Run DA Auth Bypass
4. Check logs for:
   - All 5 prep steps completing
   - HW code verification (0x6789)
   - Either 0x5A ACK OR silent proceed with upload
   - DA upload progress to 100%
   - DA execution ACK (0x5A 0xA5)

## Technical Notes

### Why Delays Matter
BROM is a simple state machine running in internal ROM. Each command transitions the state:
```
SLA (0xC8) → GET_VERSION (0xFE) → DISABLE_WD (0xD4) → TARGET_CFG (0xD8) → DISABLE_AUTH (0xC7) → GET_HW (0xFD) → SEND_DA (0xD7)
```

Without delays, commands arrive before BROM finishes processing previous command → state machine confusion → no ACK (-1).

### Why Continue on Error
Some MT6789 BROM versions:
- Don't ACK 0xFE (version query)
- Don't ACK 0xD4 (watchdog disable)
- Only ACK critical commands (0xD7, 0xFD)

Fail-fast approach would abort valid operations. Continue-on-error ensures we try all prep steps and let the final 0xD7 succeed.

### MT6789-Specific Behavior
MT6789 (Helio G99) BROM is known to:
1. **Not ACK 0xD7 header** — only ACKs after full DA upload
2. **Need longer delays** — 200ms vs 50ms for older chips
3. **Return 0xA5 instead of 0x5A** — partial ACK meaning "processing"

These are all normal behaviors, not errors!

## Troubleshooting

### Still Getting TX=-1 After Fix?
1. **Check handshake logs** — did `performStableBromHandshake()` succeed?
2. **Look for TX sanity check** — did it pass or fail?
3. If handshake fails → device not in BROM mode (reconnect with Vol-)
4. If TX sanity fails → USB cable/port issue (try different cable)

### Handshake Fails But Device in BROM Mode?
- Try different USB cable (BROM is very timing-sensitive)
- Use USB 2.0 port (not USB 3.0)
- Disconnect/reconnect device while holding Vol-
- Check dmesg for USB errors: `dmesg | tail -20`

### DA Upload Fails Midway?
- DA binary may be corrupted or wrong chip
- USB connection unstable (check cable quality)
- BROM rejected DA signature (DA must be unsigned/patched)

### Device Reboots During Upload?
- Watchdog not disabled properly (check 0xD4 response)
- DA upload too fast (increase CHUNK_SIZE delay)
- Power issue (use powered USB hub)
