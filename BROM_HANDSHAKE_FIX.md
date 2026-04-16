# MTK BROM Handshake Protocol Fix

## 🚨 Critical Bug Discovered

**Previous Implementation (WRONG):**
```kotlin
// ❌ Sent ALL 4 bytes at once!
val handshake = byteArrayOf(0xA0, 0x0A, 0x50, 0x05)
conn.bulkTransfer(epOut, handshake, 4, timeout)
val response = ByteArray(4)
conn.bulkTransfer(epIn, response, 4, timeout)
// Expected: 5F F5 AF FA
// Got: 5F or AF (only 1 byte!)
```

**What Actually Happened:**
```
Host sends: [0xA0, 0x0A, 0x50, 0x05] as single packet
BROM receives: 0xA0 → responds 0x5F ✅
               0x0A → treated as NEW command! → responds 0xF5
               0x50 → treated as NEW command! → responds 0xAF
               0x05 → treated as NEW command! → responds 0xFA

BUT: Android USB bulkTransfer reads only FIRST byte (0x5F)
Remaining bytes (F5 AF FA) stay in buffer → state machine CONFUSED!

All subsequent commands return garbage:
  0xFE → 0xFF (NAK)
  0xFD → 0x03 (garbage)
  0xD7 → -1 (rejected)
```

## ✅ Correct Protocol

**MTK BROM requires 4 SEPARATE byte exchanges:**

```
Exchange 1:
  Host → BROM: 0xA0
  BROM → Host: 0x5F

Exchange 2:
  Host → BROM: 0x0A
  BROM → Host: 0xF5

Exchange 3:
  Host → BROM: 0x50
  BROM → Host: 0xAF

Exchange 4:
  Host → BROM: 0x05
  BROM → Host: 0xFA

Result: "5F F5 AF FA" = BROM initialized!
```

## 🔧 Implementation

```kotlin
val handshakeSeq = listOf(
    Triple("Step 1", 0xA0, 0x5F),
    Triple("Step 2", 0x0A, 0xF5),
    Triple("Step 3", 0x50, 0xAF),
    Triple("Step 4", 0x05, 0xFA)
)

for ((stepName, sendByte, expectedResp) in handshakeSeq) {
    // Send SINGLE byte
    val tx = byteArrayOf(sendByte.toByte())
    conn.bulkTransfer(epOut, tx, 1, timeout)
    
    // Read SINGLE byte response
    val rx = ByteArray(1)
    val read = conn.bulkTransfer(epIn, rx, 1, timeout)
    val got = rx[0].toInt().and(0xFF)
    
    // Validate exact match
    if (got != expectedResp) {
        // FAIL - state machine broken
        return false
    }
}
// ✅ Handshake PERFECT!
```

## 📊 Expected Logs After Fix

```
📟 USB Device: VID=0x0e8d PID=0x0003
📟 Device name: /dev/bus/usb/001/015
📟 Interfaces: 1

🤝 Performing fresh BROM handshake (A0 0A 50 05)...
  [HS] Step 1: Send 0xA0 → Got 0x5F (expected 0x5F)
  [HS] Step 2: Send 0x0A → Got 0xF5 (expected 0xF5)
  [HS] Step 3: Send 0x50 → Got 0xAF (expected 0xAF)
  [HS] Step 4: Send 0x05 → Got 0xFA (expected 0xFA)
✅ BROM handshake PERFECT — 5F F5 AF FA confirmed!

🔍 Verifying BROM mode with GET_HW_CODE (0xFD)...
📟 HW Code: 0x6789
✅ HW Code 0x6789 confirmed — BROM mode active!

🔍 Verifying USB TX/RX path...
✅ USB TX/RX verified! HW code: 0x6789
```

## 🔍 USB VID/PID Reference

| PID | Mode | Description |
|-----|------|-------------|
| 0x0003 | **BROM** | What we want! Pre-Boot ROM mode |
| 0x2000 | Preloader | Wrong mode - need to reconnect |
| 0x0001 | Download | DA already loaded |
| 0x200A | DA Mode | Download Agent running |

**Check logs for:**
```
📟 USB Device: VID=0x0e8d PID=0x0003  ← CORRECT!
📟 USB Device: VID=0x0e8d PID=0x2000  ← WRONG! Reconnect with Vol-
```

## 🎯 HW Code Verification

After handshake, we verify BROM mode by reading chip ID:

```kotlin
// Send GET_HW_CODE (0xFD)
conn.bulkTransfer(epOut, byteArrayOf(0xFD), 1, 2000)

// Read 4 bytes response
val hwResp = ByteArray(4)
conn.bulkTransfer(epIn, hwResp, 4, 2000)

// Extract chip code (big-endian)
val hwCode = (hwResp[0] shl 8) or hwResp[1]

// MT6789 should return 0x6789
if (hwCode == 0x6789) {
    ✅ BROM mode confirmed!
} else {
    ⚠️ Wrong code - device may be in preloader mode
}
```

**Common HW Codes:**
- 0x6789 = MT6789 Helio G99 (RMX3845)
- 0x6785 = MT6785 Helio G95
- 0x6768 = MT6768 Helio G85
- 0x6877 = MT6877 Dimensity 900

## 🐛 Troubleshooting

### Handshake Step Fails

**Symptom:**
```
[HS] Step 1: Send 0xA0 → Got 0xFF (expected 0x5F)
❌ Handshake mismatch at Step 1!
```

**Causes:**
1. **Device not in BROM mode** (PID=0x2000)
   - Solution: Reconnect with Vol- button held
   - Device must be POWERED OFF

2. **USB cable/port issue**
   - Solution: Try USB 2.0 port, different cable

3. **Interface not claimed properly**
   - Solution: Check `claimInterface(iface, true)` called

### HW Code Wrong

**Symptom:**
```
📟 HW Code: 0x0000
⚠️ Expected 0x6789 but got 0x0000
```

**Causes:**
1. **Preloader mode** - reconnect to BROM
2. **Corrupted BROM** - rare, try different device
3. **Wrong chip** - check device model

### Handshake Succeeds But Commands Fail

**Symptom:**
```
✅ BROM handshake PERFECT
📟 HW Code: 0x6789
...
[BROM] GET_VERSION RX: 0xFF  ← Should be version bytes!
```

**Causes:**
1. **Missing prep sequence** - need 0xFE, 0xD4, 0xD8, 0xC7, 0xFD before 0xD7
2. **Wrong DA binary** - check Part0 extraction
3. **BROM state timeout** - add delays between commands

## 📝 Protocol Reference

### Complete BROM Initialization Sequence

```
1. USB Connection
   - Open device (VID=0x0e8d, PID=0x0003)
   - Claim interface #0 (and #1 if exists)
   - Find bulk endpoints (IN + OUT)

2. Handshake (4 byte exchanges)
   - 0xA0 → 0x5F
   - 0x0A → 0xF5
   - 0x50 → 0xAF
   - 0x05 → 0xFA

3. HW Code Verification
   - Send: 0xFD
   - Receive: [0x67, 0x89, 0x00, 0x00]
   - Verify: 0x6789

4. SLA Bypass (if needed)
   - Send: 0xC8 (GET_SLA_CHALLENGE)
   - Receive: challenge or ACK
   - Send: null auth or bypass payload

5. BROM Prep Sequence
   - 0xFE (GET_VERSION) + 50ms delay
   - 0xD4 (DISABLE_WATCHDOG) + 50ms delay
   - 0xD8 (TARGET_CONFIG) + 50ms delay
   - 0xC7 (DISABLE_AUTH) + 50ms delay
   - 0xFD (GET_HW_CODE) + 100ms delay

6. DA Upload
   - Send: 0xD7 + header (13 bytes)
   - Wait: 200ms for ACK (may not come)
   - Upload: DA Part0 in 4KB chunks
   - Send: checksum
   - Receive: 0x5A 0xA5 (success)

7. Jump to DA
   - Send: 0xD5 + address + args
   - DA executes!
```

## 🔗 References

- MTK BROM Protocol: https://github.com/bkerler/mtkclient
- Handshake Analysis: https://github.com/NeiroGun/mtk-brom-handshake
- USB Protocol Docs: MediaTek SP Flash Tool internals

## Files Modified

- `MtkExploitEngine.kt`: Complete handshake rewrite
  - `performStableBromHandshake()`: Byte-by-byte protocol
  - HW code verification after handshake
  - USB VID/PID logging
  - Better error messages
