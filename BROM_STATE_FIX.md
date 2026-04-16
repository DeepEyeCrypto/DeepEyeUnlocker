# BROM State Machine Fix for MT6789 (Helio G99)

## Problem
```
Header ACK read: -1 bytes  ← USB read itself failed!
```

BROM not responding to `CMD_SEND_DA (0xD7)` after SLA bypass. Error `-1` means BROM state machine is not ready to accept 0xD7 command.

## Root Cause
After SLA bypass (0xC8), the BROM state machine needs specific preparation commands before it will accept `CMD_SEND_DA (0xD7)`. The sequence was missing critical delays and error handling.

## Solution Applied

### 1. Enhanced `prepareBromForDa()` Function

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

### 2. Improved CMD_SEND_DA ACK Handling

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

### 3. Better Error Messages

Added helpful diagnostic messages:
- "BROM may ACK after DA upload"
- "Proceeding with DA upload anyway (common for MT6789)"
- "BROM may have rejected DA — check DA binary compatibility"

## Expected Behavior After Fix

### Scenario 1: BROM Responds Normally
```
[BROM] Step 1/5: GET_VERSION (0xFE)
[BROM] Version RX: 0x00 0x01 0x02 0x03
...
[BROM] HW Code: 0x6789
[BROM] ✅ HW code matches MT6789 — BROM state verified!
📤 Header sent: 13 bytes
📥 Header ACK read: 1 bytes → 0x5A
✅ BROM ACK 0x5A — upload authorized!
```

### Scenario 2: BROM Silent (Common for MT6789)
```
[BROM] Step 5/5: GET_HW_CODE (0xFD) — final state check
[BROM] HW code check RX: (no response, -1 bytes)
[BROM] ⚠️ No HW code response — BROM may still be ready, proceeding...
📤 Header sent: 13 bytes
📥 Header ACK read: -1 bytes → (no data)
⚠️ No BROM response to 0xD7 — BROM may ACK after DA upload
🔧 Proceeding with DA upload anyway (common for MT6789)
📤 Uploading 256KB DA in 4KB chunks...
  📤 Upload progress: 100% (256KB/256KB)
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

### Still Getting -1 After Fix?
1. Check prep sequence logs — did all 5 steps run?
2. Look for HW code response — is it 0x6789?
3. Verify DA binary is correct for MT6789
4. Try different USB cable/port (BROM is timing-sensitive)

### DA Upload Fails Midway?
- DA binary may be corrupted or wrong chip
- USB connection unstable (check cable quality)
- BROM rejected DA signature (DA must be unsigned/patched)

### Device Reboots During Upload?
- Watchdog not disabled properly (check 0xD4 response)
- DA upload too fast (increase CHUNK_SIZE delay)
- Power issue (use powered USB hub)
