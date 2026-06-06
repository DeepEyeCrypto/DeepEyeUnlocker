# 🔴 BROM Connection Crash Analysis & Fix

**Date:** April 12, 2026  
**Issue:** Application crashes when connecting to MTK BROM mode via OTG  
**Severity:** CRITICAL - App completely crashes  
**Status:** ✅ ROOT CAUSE IDENTIFIED + FIX PROVIDED

---

## 🎯 EXECUTIVE SUMMARY

**ROOT CAUSE:** The crash occurs due to **missing USB permission checks** and **unsafe USB device opening** in `MtkExploitEngine.kt` before attempting BROM handshake operations.

The engine directly calls `usbManager.openDevice(usbDevice)` without verifying:

1. ✅ USB permission is granted
2. ✅ Device is actually in BROM mode
3. ✅ USB endpoints are properly enumerated
4. ✅ Interface claim succeeded

This causes **NullPointerException** or **SecurityException** when the device is not properly initialized.

---

## 🔍 CRASH ANALYSIS

### Crash Point #1: Missing USB Permission Check

**File:** `MtkExploitEngine.kt`  
**Lines:** 51-58, 172-179, 681-688

```kotlin
// ❌ CRASH RISK: No permission check before openDevice
val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
val conn = usbManager.openDevice(usbDevice) ?: run {
    onLog("❌ Cannot open USB device")
    return@withContext GlitchResult.FAILED
}
```

**Problem:**

- `openDevice()` returns `null` if permission not granted
- But on some devices, it throws `SecurityException` instead
- No try-catch around the `openDevice()` call itself
- No permission validation before attempting to open

**Stack Trace (Expected):**

```
java.lang.SecurityException: User has not given permission to device UsbDevice
    at android.hardware.usb.UsbManager.openDevice(UsbManager.java:372)
    at com.deepeye.otg.engine.mtk.MtkExploitEngine.bromVoltageGlitch(MtkExploitEngine.kt:55)
```

---

### Crash Point #2: Unsafe Interface Access

**File:** `MtkExploitEngine.kt`  
**Lines:** 61-62, 182-183, 691-692

```kotlin
// ❌ CRASH RISK: No null check on getInterface(0)
val iface = usbDevice.getInterface(0)
conn.claimInterface(iface, true)
```

**Problem:**

- `getInterface(0)` can return `null` if device has no interfaces
- BROM devices sometimes enumerate with 0 interfaces initially
- No null check before calling `claimInterface()`
- Causes `NullPointerException`

**Stack Trace (Expected):**

```
java.lang.NullPointerException: Parameter specified as non-null is null
    at android.hardware.usb.UsbDeviceConnection.claimInterface(UsbDeviceConnection.java:179)
    at com.deepeye.otg.engine.mtk.MtkExploitEngine.bromVoltageGlitch(MtkExploitEngine.kt:62)
```

---

### Crash Point #3: Endpoint Enumeration Failure

**File:** `MtkExploitEngine.kt`  
**Lines:** 65-78, 186-199, 694-706

```kotlin
// ⚠️ PARTIAL: Checks for null endpoints, but doesn't handle all cases
var epIn: UsbEndpoint? = null
var epOut: UsbEndpoint? = null
for (i in 0 until iface.endpointCount) {
    val ep = iface.getEndpoint(i)
    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
        if (ep.direction == UsbConstants.USB_DIR_IN) epIn = ep
        else epOut = ep
    }
}

if (epIn == null || epOut == null) {
    onLog("❌ Bulk endpoints not found")
    return@withContext GlitchResult.NO_ENDPOINT
}
```

**Problem:**

- BROM mode devices sometimes have endpoints on **interface #1**, not #0
- Code only checks `usbDevice.getInterface(0)`
- Should check both interfaces (like `RealMtkBromExecutor.kt` does correctly)

---

### Crash Point #4: Missing Connection Null Check in Bulk Transfer

**File:** `MtkExploitEngine.kt`  
**Lines:** 87-90, 204-206, 710-713

```kotlin
// ❌ CRASH RISK: conn could be null if openDevice failed silently
conn.bulkTransfer(epOut, handshake, handshake.size, 100)
```

**Problem:**

- If `openDevice()` somehow returns a connection in bad state
- No validation that connection is actually open
- Can cause native crash in USB driver

---

## 📊 COMPARISON: BAD vs GOOD IMPLEMENTATION

### ❌ BAD: MtkExploitEngine.kt (Current - Crashes)

```kotlin
suspend fun bromVoltageGlitch(usbDevice: UsbDevice, onLog: (String) -> Unit): GlitchResult {
    // ❌ No permission check
    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    val conn = usbManager.openDevice(usbDevice) ?: return GlitchResult.FAILED

    // ❌ No interface null check
    val iface = usbDevice.getInterface(0)
    conn.claimInterface(iface, true)

    // ❌ Only checks interface #0
    var epIn: UsbEndpoint? = null
    var epOut: UsbEndpoint? = null
    for (i in 0 until iface.endpointCount) { ... }

    // ❌ No try-catch around openDevice
    // ❌ No connection state validation
}
```

### ✅ GOOD: RealMtkBromExecutor.kt (Reference - Safe)

```kotlin
suspend fun eraseFrp(device: UsbDevice, daBytes: ByteArray, ...): ProtocolResult {
    // ✅ Safe open with proper null handling
    val conn = usbManager.openDevice(device)
        ?: return ProtocolResult.UsbTransportError(
            reason = "Cannot open device — permission denied?",
            sessionId = sessionId,
        )

    try {
        // ✅ Finds endpoints on correct interface
        val epOut = findBulkOut(device, sessionId)
            ?: return ProtocolResult.UsbTransportError(...)
        val epIn = findBulkIn(device, sessionId)
            ?: return ProtocolResult.UsbTransportError(...)

        // ✅ Claims BOTH interfaces safely
        val iface0 = device.getInterface(0)
        if (!claimWithSettle(conn, iface0, sessionId)) {
            return ProtocolResult.UsbTransportError(...)
        }
        if (device.interfaceCount > 1) {
            val iface1 = device.getInterface(1)
            claimWithSettle(conn, iface1, sessionId)
        }

        // ✅ Proper error handling throughout
        // ✅ Finally block ensures cleanup
    } catch (e: Exception) {
        Timber.e("[MTK_BROM] error: ${e.message}")
        return ProtocolResult.UsbTransportError(...)
    } finally {
        runCatching { conn.close() }
    }
}
```

---

## 🔧 ROOT CAUSES IDENTIFIED

### 1. **Missing USB Permission Validation** (CRITICAL)

- **Location:** `MtkExploitEngine.kt` lines 51-58, 172-179, 681-688
- **Impact:** SecurityException or NullPointerException
- **Fix:** Check `usbManager.hasPermission(device)` before `openDevice()`

### 2. **No Try-Catch Around openDevice()** (CRITICAL)

- **Location:** `MtkExploitEngine.kt` lines 55, 176, 685
- **Impact:** Unhandled SecurityException crashes app
- **Fix:** Wrap in try-catch block

### 3. **Interface Null Check Missing** (HIGH)

- **Location:** `MtkExploitEngine.kt` lines 61, 182, 691
- **Impact:** NullPointerException on devices with 0 interfaces
- **Fix:** Check `device.interfaceCount > 0` and validate interface not null

### 4. **Only Checks Interface #0** (MEDIUM)

- **Location:** `MtkExploitEngine.kt` lines 67-73, 188-194, 696-702
- **Impact:** Fails to find endpoints on interface #1
- **Fix:** Check both interfaces like `RealMtkBromExecutor.kt`

### 5. **No Connection State Validation** (MEDIUM)

- **Location:** `MtkExploitEngine.kt` throughout
- **Impact:** Bulk transfers on closed/invalid connection
- **Fix:** Validate connection is open before transfers

---

## ✅ FIXES REQUIRED

### Fix #1: Add USB Permission Check (CRITICAL)

**File:** `MtkExploitEngine.kt`  
**Lines:** 51-58

```kotlin
// BEFORE (❌ Crashes):
val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
val conn = usbManager.openDevice(usbDevice) ?: run {
    onLog("❌ Cannot open USB device")
    return@withContext GlitchResult.FAILED
}

// AFTER (✅ Safe):
val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

// Check permission first
if (!usbManager.hasPermission(usbDevice)) {
    onLog("❌ USB permission not granted")
    onLog("💡 Please accept USB permission dialog and retry")
    return@withContext GlitchResult.FAILED
}

val conn = try {
    usbManager.openDevice(usbDevice)
} catch (e: SecurityException) {
    onLog("❌ SecurityException: ${e.message}")
    return@withContext GlitchResult.FAILED
} ?: run {
    onLog("❌ Cannot open USB device (returned null)")
    return@withContext GlitchResult.FAILED
}
```

---

### Fix #2: Safe Interface Access (HIGH)

**File:** `MtkExploitEngine.kt`  
**Lines:** 61-62

```kotlin
// BEFORE (❌ Crashes):
val iface = usbDevice.getInterface(0)
conn.claimInterface(iface, true)

// AFTER (✅ Safe):
if (usbDevice.interfaceCount == 0) {
    onLog("❌ Device has no interfaces")
    onLog("💡 Device may not be in BROM mode")
    return@withContext GlitchResult.FAILED
}

val iface = usbDevice.getInterface(0)
    ?: run {
        onLog("❌ Interface #0 is null")
        return@withContext GlitchResult.FAILED
    }

if (!conn.claimInterface(iface, true)) {
    onLog("⚠️ Failed to claim interface #0 (continuing anyway)")
}

// Also claim interface #1 if available (BROM devices often need this)
if (usbDevice.interfaceCount > 1) {
    val iface1 = usbDevice.getInterface(1)
    if (iface1 != null) {
        conn.claimInterface(iface1, true)
        onLog("✅ Claimed interface #1")
    }
}
```

---

### Fix #3: Check Both Interfaces for Endpoints (MEDIUM)

**File:** `MtkExploitEngine.kt`  
**Lines:** 65-78

```kotlin
// BEFORE (❌ Only checks interface #0):
var epIn: UsbEndpoint? = null
var epOut: UsbEndpoint? = null
for (i in 0 until iface.endpointCount) {
    val ep = iface.getEndpoint(i)
    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
        if (ep.direction == UsbConstants.USB_DIR_IN) epIn = ep
        else epOut = ep
    }
}

// AFTER (✅ Checks both interfaces):
var epIn: UsbEndpoint? = null
var epOut: UsbEndpoint? = null

// Try interface #1 first (BROM often uses IF#1)
for (ifaceIdx in listOf(1, 0)) {
    if (ifaceIdx < usbDevice.interfaceCount) {
        val iface = usbDevice.getInterface(ifaceIdx)
        if (iface != null) {
            for (i in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(i)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == UsbConstants.USB_DIR_IN) epIn = ep
                    else epOut = ep
                }
            }
        }
    }
    if (epIn != null && epOut != null) break
}

if (epIn == null || epOut == null) {
    onLog("❌ Bulk endpoints not found on any interface")
    onLog("💡 Device interfaces: ${usbDevice.interfaceCount}")
    return@withContext GlitchResult.NO_ENDPOINT
}

onLog("✅ USB endpoints found (IF#${if (usbDevice.interfaceCount > 1) 1 else 0})")
```

---

### Fix #4: Comprehensive Error Handling Wrapper

**Add to all 5 exploit methods in `MtkExploitEngine.kt`:**

```kotlin
suspend fun bromVoltageGlitch(
    usbDevice: UsbDevice,
    onLog: (String) -> Unit
): GlitchResult = withContext(Dispatchers.IO) {
    try {
        // All existing code here with fixes above

    } catch (e: SecurityException) {
        onLog("❌ SecurityException: ${e.message}")
        onLog("💡 Grant USB permission and retry")
        GlitchResult.FAILED
    } catch (e: NullPointerException) {
        onLog("❌ NullPointerException: ${e.message}")
        onLog("💡 Device not properly connected")
        GlitchResult.FAILED
    } catch (e: Exception) {
        onLog("❌ Unexpected error: ${e.message}")
        e.printStackTrace()
        GlitchResult.FAILED
    } finally {
        // Safe connection cleanup
        try {
            conn?.close()
            onLog("🔌 USB connection closed")
        } catch (e: Exception) {
            onLog("⚠️ Error closing connection: ${e.message}")
        }
    }
}
```

---

## 🔬 ADDITIONAL SAFETY MEASURES

### 1. Add Connection Validation Helper

```kotlin
private fun isValidConnection(conn: UsbDeviceConnection?): Boolean {
    if (conn == null) return false
    // Try a simple operation to verify connection is alive
    return try {
        // If this doesn't throw, connection is valid
        conn.getFileDescriptor() >= 0
    } catch (e: Exception) {
        false
    }
}
```

### 2. Add Device State Validation

```kotlin
private fun isDeviceInBromMode(device: UsbDevice): Boolean {
    // MTK BROM VID:PID = 0E8D:0003
    return device.vendorId == 0x0E8D && device.productId == 0x0003
}
```

### 3. Add Pre-Flight Check Function

```kotlin
private fun preFlightCheck(
    usbDevice: UsbDevice,
    usbManager: UsbManager,
    onLog: (String) -> Unit
): Boolean {
    // Check 1: USB Permission
    if (!usbManager.hasPermission(usbDevice)) {
        onLog("❌ USB permission not granted")
        return false
    }

    // Check 2: BROM Mode
    if (!isDeviceInBromMode(usbDevice)) {
        onLog("⚠️ Device not in BROM mode (VID:0x${usbDevice.vendorId.toString(16).uppercase()}, PID:0x${usbDevice.productId.toString(16).uppercase()})")
        onLog("💡 Put device in BROM mode: Power off → Hold Vol+ → Connect USB")
    }

    // Check 3: Interfaces
    if (usbDevice.interfaceCount == 0) {
        onLog("❌ Device has no interfaces")
        return false
    }

    // Check 4: Device Name
    onLog("📱 Device: ${usbDevice.productName ?: "Unknown"} (${usbDevice.manufacturerName ?: "Unknown"})")
    onLog("🔌 VID:0x${usbDevice.vendorId.toString(16).uppercase()} PID:0x${usbDevice.productId.toString(16).uppercase()}")
    onLog("📊 Interfaces: ${usbDevice.interfaceCount}")

    return true
}
```

---

## 📋 TESTING CHECKLIST

After applying fixes, verify:

- [ ] App doesn't crash when BROM device connected without permission
- [ ] App shows proper error message when permission not granted
- [ ] App handles devices with 0 interfaces gracefully
- [ ] App finds endpoints on both interface #0 and #1
- [ ] App closes USB connection properly in all cases
- [ ] App handles SecurityException without crashing
- [ ] App handles NullPointerException without crashing
- [ ] App shows detailed logs for debugging
- [ ] App validates device is actually in BROM mode
- [ ] All 5 exploit methods have proper error handling

---

## 🎯 PRIORITY ORDER

1. **P0 (CRITICAL):** Fix #1 - USB Permission Check
2. **P0 (CRITICAL):** Fix #4 - Comprehensive Error Handling
3. **P1 (HIGH):** Fix #2 - Safe Interface Access
4. **P2 (MEDIUM):** Fix #3 - Check Both Interfaces
5. **P3 (LOW):** Additional Safety Measures

---

## 📊 EXPECTED IMPACT

| Metric         | Before               | After             |
| -------------- | -------------------- | ----------------- |
| Crash Rate     | 100% (no permission) | 0% ✅             |
| Error Messages | Generic              | Detailed ✅       |
| Device Support | Interface #0 only    | Both #0 and #1 ✅ |
| Safety         | Low                  | High ✅           |
| Debug Info     | Minimal              | Comprehensive ✅  |

---

## 🔗 RELATED FILES

### Files to Modify:

1. ✅ `MtkExploitEngine.kt` - Main exploit engine (5 methods need fixes)

### Reference Files (Good Examples):

1. ✅ `RealMtkBromExecutor.kt` - Proper USB handling
2. ✅ `BromExecutor.kt` - Safe handshake implementation
3. ✅ `UsbPermissionGuard.kt` - Permission management
4. ✅ `UsbSessionManager.kt` - Safe connection opening

### Files to Review:

1. ⚠️ `MtkExploitViewModel.kt` - Add permission state tracking
2. ⚠️ `MtkExploitScreen.kt` - Show permission status UI

---

## ✅ CONCLUSION

The BROM connection crash is caused by **missing USB permission validation** and **unsafe device opening** in `MtkExploitEngine.kt`. The fix requires:

1. ✅ Adding `usbManager.hasPermission()` checks
2. ✅ Wrapping `openDevice()` in try-catch
3. ✅ Validating interfaces and endpoints
4. ✅ Checking both interface #0 and #1
5. ✅ Adding comprehensive error handling

**Estimated Fix Time:** 2-3 hours  
**Risk Level:** Low (adding safety checks only)  
**Breaking Changes:** None

**Status:** 🔴 **READY TO IMPLEMENT**
