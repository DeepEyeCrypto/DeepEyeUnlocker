# EDL (Emergency Download Mode) Guide

## Overview

EDL (Emergency Download Mode) is Qualcomm's lowest-level recovery mechanism, running directly from the **Primary Boot Loader (PBL)** burned into the SoC's ROM. This guide covers how DeepEyeUnlocker handles EDL operations.

## Boot Chain Architecture

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│   PBL   │───▶│  SBL1   │───▶│  Aboot  │───▶│ Android │
│  (ROM)  │    │ (Flash) │    │   (LK)  │    │ Kernel  │
└────┬────┘    └─────────┘    └─────────┘    └─────────┘
     │
     │  If SBL corrupted OR test-point shorted
     ▼
┌─────────┐
│   EDL   │◀── PBL falls back to USB as 9008
│  9008   │    Waits for Sahara handshake
└─────────┘
```

## USB Identifiers

| VID:PID | Mode | Description |
|---------|------|-------------|
| `05C6:9008` | EDL 9008 | Standard EDL, Sahara active |
| `05C6:9006` | EDL 9006 | Xiaomi variant |
| `05C6:900E` | DIAG | Diagnostic mode |
| `05C6:F006` | HS-USB QDLoader | Legacy mode |

## EDL Capability Classification

DeepEyeUnlocker classifies devices into four categories:

### ✅ SOFTWARE_DIRECT_SUPPORTED

- Device supports `adb reboot edl` or `fastboot oem edl`
- Examples: Xiaomi (MIUI ≤12), OnePlus 6/6T, Motorola Moto G

### ⚠️ SOFTWARE_RESTRICTED

- Older firmware supports EDL, newer blocks it
- May work after downgrade or with specific firmware versions
- Examples: Xiaomi (MIUI 13+), OnePlus 7 series

### 🔩 HARDWARE_ONLY  

- All software EDL paths blocked
- Requires physical test-point shorting
- Examples: Samsung, Google Pixel, Oppo/Realme/Vivo

### ❓ UNKNOWN

- No profile data available
- DeepEyeUnlocker will attempt standard methods

## Software EDL Commands

### ADB Path

```bash
adb reboot edl
```

**Requirements:**

- Android booted
- USB debugging enabled
- ADB authorized on PC

### Fastboot Path

```bash
fastboot oem edl
fastboot reboot-edl
fastboot reboot edl
```

**Requirements:**

- Device in fastboot mode
- Command not blocked by OEM

## Brand-Specific Information

### Xiaomi

| MIUI Version | EDL Support |
|--------------|-------------|
| MIUI 10-11 | ✅ Full support |
| MIUI 12 | ⚠️ Some models blocked |
| MIUI 13+ | ❌ Requires Mi Flash Pro Auth |

**Notes:**

- HyperOS generally blocks software EDL
- Mi Unlock tool may be required first

### OnePlus

| OxygenOS Version | EDL Support |
|------------------|-------------|
| OOS 9-10 | ✅ Works |
| OOS 11+ | ❌ Blocked |
| ColorOS-based | ❌ Blocked |

### Samsung

- **Never** supports software EDL
- Uses Download Mode (Odin) instead
- True EDL requires test-point

### Google Pixel

- EDL blocked at aboot level since Pixel 2
- Tensor SoC (Pixel 6+) is not Qualcomm

### Realme/Oppo

- Blocked since ColorOS 7
- Requires MSMDownloadTool with authorization

## Test-Point EDL

When software methods fail, hardware test-point can force EDL:

1. Power off device completely
2. Disconnect battery (if possible)
3. Short specific test pad to GND
4. Connect USB while holding short
5. Device enumerates as 9008
6. Release short

**⚠️ Risks:**

- Physical damage if wrong point shorted
- Permanent brick possible
- Voids warranty

## Using DeepEyeUnlocker EDL Features

### EDL Control Panel

The EDL Control Panel provides:

- Device status and mode display
- EDL capability indicator
- One-click reboot to EDL
- Driver status checking
- Detailed operation logs

### Workflow

1. **Connect Device** - Via ADB or Fastboot
2. **Check Capability** - Panel shows EDL support level
3. **Verify Drivers** - Ensure Qualcomm QDLoader installed
4. **Reboot to EDL** - Click button, confirm warning
5. **Wait for 9008** - USB rescans automatically
6. **Load Firehose** - Proceed with flash operations

## Driver Requirements

### Qualcomm QDLoader 9008

Required for EDL communication. Install via:

- Qualcomm USB Driver package
- DeepEyeUnlocker bundled drivers
- QPST/QFIL installation

### Verification

```
Device Manager → Ports (COM & LPT)
  └─ Qualcomm HS-USB QDLoader 9008 (COMx)
```

## Troubleshooting

### Command Rejected

```
"unknown command" or "not allowed"
```

**Solution:** OEM has removed EDL command. Try test-point or authorized tool.

### Device Disappears But No 9008

```
Device not appearing as Qualcomm 9008
```

**Solutions:**

1. Install Qualcomm drivers
2. Try different USB port (no hub)
3. Use original cable
4. Check Device Manager for unknown device

### EDL Timeout

```
Device did not enter EDL within 15 seconds
```

**Solutions:**

1. Retry operation
2. Check if device has anti-EDL fuses blown
3. Consider test-point method

## Security Considerations

EDL provides **factory-level access**:

- Read any partition (including encrypted userdata)
- Write any partition (flash custom firmware)
- Erase any partition (bypass FRP)
- Modify IMEI and calibration data

**⚠️ Use responsibly and legally!**

## API Reference

### IEdlManager

```csharp
interface IEdlManager
{
    Task<EdlResult> RebootToEdlAsync(DeviceContext device, CancellationToken ct);
    Task<bool> IsInEdlModeAsync(CancellationToken ct);
    EdlCapability GetCapabilityFor(DeviceContext device);
    EdlProfile? GetProfileFor(DeviceContext device);
}
```

### EdlResult

```csharp
class EdlResult
{
    bool Success;
    string? FailureReason;
    EdlAttemptMethod MethodUsed;  // AdbRebootEdl, FastbootOemEdl, etc.
    string Log;
    TimeSpan ElapsedTime;
}
```

## Adding New Device Profiles

Edit `assets/EdlProfiles.json`:

```json
{
  "Brand": "YourBrand",
  "Model": "Model Name",
  "Codename": "codename",
  "SoC": "SM1234",
  "SupportsAdbRebootEdl": true,
  "SupportsFastbootOemEdl": true,
  "RequiresAuthTool": false,
  "RequiresTestPoint": false,
  "Notes": "Additional information"
}
```

---

*Last updated: 2026-01-28*
