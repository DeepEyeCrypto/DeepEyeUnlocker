# DeepEyeUnlocker - Complete System Documentation

> **Version 2.0** | Last Updated: 2026-01-28

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Core Features](#core-features)
4. [EDL (Emergency Download) System](#edl-system)
5. [Firehose Protocol](#firehose-protocol)
6. [Partition Management](#partition-management)
7. [Backup & Restore](#backup--restore)
8. [FRP Bypass](#frp-bypass)
9. [Device Profiles](#device-profiles)
10. [UI Components](#ui-components)
11. [API Reference](#api-reference)

---

## Overview

DeepEyeUnlocker is a professional Android device servicing tool supporting:

- **Qualcomm EDL (9008)** mode operations
- **MediaTek BROM** mode operations
- **Samsung Download** mode (Odin)
- **Partition management** (read/write/erase)
- **Full device backup and restore**
- **FRP (Factory Reset Protection) bypass**
- **Pattern/PIN lock clearing**

### Supported Platforms

- Windows 10/11 (primary)
- macOS (limited - no GUI)
- Linux (limited - CLI only)

### Supported Device Brands

| Brand | Protocol | Support Level |
|-------|----------|---------------|
| Xiaomi/Redmi/POCO | Qualcomm/MTK | ⭐⭐⭐⭐⭐ |
| OnePlus | Qualcomm | ⭐⭐⭐⭐ |
| Samsung | Download Mode | ⭐⭐⭐⭐ |
| Realme/Oppo | Qualcomm/MTK | ⭐⭐⭐⭐ |
| Motorola | Qualcomm | ⭐⭐⭐⭐ |
| Google Pixel | Qualcomm | ⭐⭐⭐ |
| LG (legacy) | Qualcomm | ⭐⭐⭐⭐ |
| Nokia | Qualcomm | ⭐⭐⭐ |
| Asus | Qualcomm | ⭐⭐⭐⭐ |
| Vivo | Qualcomm/MTK | ⭐⭐⭐ |

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                         DeepEyeUnlocker                               │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │    UI       │  │  Operations │  │   Engines   │  │   Models    │  │
│  │  (WinForms) │  │  (Tasks)    │  │  (Protocols)│  │   (DTOs)    │  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  │
│         │                │                │                │         │
│         └────────────────┴────────────────┴────────────────┘         │
│                                   │                                   │
│                           ┌───────┴───────┐                          │
│                           │    Core       │                          │
│                           │ (DeviceManager│                          │
│                           │  Logger, etc) │                          │
│                           └───────┬───────┘                          │
│                                   │                                   │
│  ┌────────────┬───────────────────┼───────────────────┬────────────┐ │
│  │            │                   │                   │            │ │
│  ▼            ▼                   ▼                   ▼            ▼ │
│ ┌──────┐  ┌──────┐           ┌──────┐           ┌──────┐  ┌──────┐  │
│ │Qualco│  │Samsu│           │  ADB │           │ MTK  │  │Driver│  │
│ │ mm   │  │ng   │           │      │           │      │  │Mgr   │  │
│ │Engine│  │Engine│          │Engine│           │Engine│  │      │  │
│ └──────┘  └──────┘           └──────┘           └──────┘  └──────┘  │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

### Directory Structure

```
DeepEyeUnlocker/
├── src/
│   ├── Core/
│   │   ├── DeviceManager.cs         # USB device discovery
│   │   ├── Logger.cs                # Logging system
│   │   ├── OperationFactory.cs      # Operation/engine factory
│   │   ├── ProfileManager.cs        # Brand profile management
│   │   ├── EdlProfileProvider.cs    # EDL profile loading
│   │   ├── Models/
│   │   │   ├── DeviceContext.cs
│   │   │   ├── BrandProfile.cs
│   │   │   └── EdlModels.cs
│   │   └── Engines/
│   │       ├── IEdlManager.cs
│   │       └── EdlManager.cs
│   │
│   ├── Operations/
│   │   ├── Operation.cs             # Base operation class
│   │   ├── EdlOperations.cs         # EDL reboot operations
│   │   ├── FirehoseOperations.cs    # Firehose read/write/erase
│   │   ├── BackupRestoreManager.cs  # Full backup/restore
│   │   ├── FrpBypassManager.cs      # FRP bypass methods
│   │   └── [Other operations...]
│   │
│   ├── Protocols/
│   │   ├── IProtocol.cs             # Protocol interface
│   │   ├── Qualcomm/
│   │   │   ├── QualcommEngine.cs
│   │   │   ├── FirehoseManager.cs   # Firehose session management
│   │   │   └── PartitionTableParser.cs
│   │   ├── Samsung/
│   │   │   ├── SamsungEngine.cs
│   │   │   └── OdinProtocol.cs
│   │   ├── MTK/
│   │   │   └── MTKEngine.cs
│   │   └── Android/
│   │       └── AdbEngine.cs
│   │
│   ├── Drivers/
│   │   └── DriverManager.cs         # Driver detection/installation
│   │
│   ├── UI/
│   │   └── Panels/
│   │       ├── EdlControlPanel.cs
│   │       ├── FirehoseControlPanel.cs
│   │       ├── BackupRestorePanel.cs
│   │       └── FrpBypassPanel.cs
│   │
│   └── Infrastructure/
│       └── Logging/
│
├── assets/
│   ├── Profiles.json                # Brand profiles
│   └── EdlProfiles.json             # EDL device profiles (70+)
│
├── programmers/                     # Firehose programmer files
│   └── *.elf, *.mbn
│
├── drivers/                         # Bundled drivers
│
└── docs/
    ├── BUILD.md
    ├── WALKTHROUGH.md
    └── EDL_GUIDE.md
```

---

## Core Features

### Device Discovery

```csharp
// Automatic device detection
var deviceManager = new DeviceManager();
var devices = await deviceManager.ScanForDevicesAsync();

foreach (var device in devices)
{
    Console.WriteLine($"{device.Brand} {device.Model} - {device.Mode}");
}
```

### Connection Modes

| Mode | USB VID:PID | Description |
|------|-------------|-------------|
| `ADB` | Various | Android Debug Bridge |
| `Fastboot` | Various | Bootloader mode |
| `EDL` | `05C6:9008` | Qualcomm Emergency Download |
| `BROM` | `0E8D:*` | MediaTek Boot ROM |
| `Download` | `04E8:*` | Samsung Download Mode |

---

## EDL System

### EDL Capability Levels

```
SOFTWARE_DIRECT_SUPPORTED     - adb/fastboot reboot to EDL works
SOFTWARE_MAY_REQUIRE_UNLOCK   - Bootloader unlock may be required  
SOFTWARE_RESTRICTED           - Auth tool required
HARDWARE_ONLY                 - Test-point required
UNKNOWN                       - Not profiled
```

### Entry Methods

#### 1. ADB Reboot

```bash
adb reboot edl
```

#### 2. Fastboot OEM Command

```bash
fastboot oem edl
# or
fastboot reboot emergency
```

#### 3. Test Point (Hardware)

Requires shorting specific pads on the device motherboard. See `EdlProfiles.json` for device-specific diagrams.

### Using EDL Manager

```csharp
var edlManager = new EdlManager();

// Check capability
var capability = edlManager.GetCapabilityFor(deviceContext);

// Attempt EDL reboot
var result = await edlManager.RebootToEdlAsync(deviceContext, progress, ct);

if (result.Success)
{
    Console.WriteLine("Device is in EDL mode!");
}
```

---

## Firehose Protocol

### Session Lifecycle

```
1. DISCONNECTED      - No active session
2. SAHARA_HANDSHAKE  - Processing Sahara Hello
3. UPLOADING_PROG    - Sending programmer binary
4. CONFIGURING       - Setting up Firehose XML
5. READY             - Session active, can execute commands
6. TRANSFER          - Read/write in progress
```

### Programmer Files

Located in `programmers/` directory:

- `prog_emmc_firehose_*.mbn` - for eMMC devices
- `prog_ufs_firehose_*.elf` - for UFS devices

### Firehose Manager Usage

```csharp
var firehose = new FirehoseManager();

// Initialize session
var result = await firehose.InitializeSessionAsync(
    usbDevice,
    programmerPath,  // or null for auto-detect
    deviceContext,
    progress,
    cancellationToken);

if (result.Success)
{
    // Read partition
    var bootData = await firehose.ReadPartitionAsync("boot", progress, ct);
    
    // Write partition
    await firehose.WritePartitionAsync("boot", newBootData, progress, ct);
    
    // Erase partition
    await firehose.ErasePartitionAsync("frp", progress, ct);
}
```

---

## Partition Management

### GPT Parsing

```csharp
var parser = new PartitionTableParser();

// From raw sector data
byte[] gptData = await firehose.ReadPartitionAsync("gpt", null, ct);
var table = parser.Parse(gptData);

// From file
var table = parser.ParseFromFile("gpt_backup.bin");

// List partitions
foreach (var p in table.Partitions)
{
    Console.WriteLine($"{p.Name}: {p.SizeFormatted} at LBA {p.StartLba}");
}
```

### Common Partitions

| Partition | Description | Typical Size |
|-----------|-------------|--------------|
| `boot` | Android boot image | 64-128 MB |
| `recovery` | Recovery mode image | 64-128 MB |
| `system` | Android OS | 2-8 GB |
| `vendor` | Vendor binaries | 500 MB-2 GB |
| `userdata` | User data | Remaining space |
| `frp` | Factory Reset Protection | 512 KB-1 MB |
| `persist` | Calibration data | 32-64 MB |
| `modem` | Baseband firmware | 128-256 MB |

---

## Backup & Restore

### Backup Types

| Type | Description |
|------|-------------|
| `Full` | All partitions |
| `Critical` | Boot, recovery, modem, persist, etc. |
| `Userdata` | Only userdata partition |
| `Custom` | User-selected partitions |

### Backup Formats

| Format | Extension | Description |
|--------|-----------|-------------|
| `Raw` | folder | Individual .img files |
| `Compressed` | .zip | ZIP archive |
| `DeepEyeBackup` | .deb | Proprietary with metadata |

### Creating Backups

```csharp
var backupManager = new BackupRestoreManager(firehoseManager);

var result = await backupManager.CreateBackupAsync(
    deviceContext,
    outputDirectory: "C:/Backups",
    type: BackupType.Critical,
    format: BackupFormat.Compressed,
    progress,
    cancellationToken);

if (result.Success)
{
    Console.WriteLine($"Backup saved to: {result.OutputPath}");
    Console.WriteLine($"Partitions: {result.PartitionsBackedUp}");
    Console.WriteLine($"Size: {result.TotalBytes:N0} bytes");
}
```

### Restoring Backups

```csharp
var result = await backupManager.RestoreBackupAsync(
    backupPath: "backup_xiaomi_redmi_20260128.zip",
    selectedPartitions: null,  // All partitions, or specify list
    progress,
    cancellationToken);
```

### Manifest Format

```json
{
    "Version": "1.0",
    "DeviceBrand": "Xiaomi",
    "DeviceModel": "Redmi Note 10 Pro",
    "DeviceSerial": "ABC123",
    "Chipset": "SM7150",
    "CreatedAt": "2026-01-28T12:00:00Z",
    "Type": "Critical",
    "Format": "Compressed",
    "Partitions": [
        {
            "Name": "boot",
            "SizeBytes": 67108864,
            "FileName": "boot.img",
            "Sha256": "abc123...",
            "Verified": true
        }
    ]
}
```

---

## FRP Bypass

### Understanding FRP

Factory Reset Protection (FRP) is a security feature that requires Google account verification after factory reset. It's stored in:

- `frp` partition (primary)
- `config` partition (some devices)
- `persist` partition (includes calibration data)

### Bypass Methods

| Method | Requires | Risk Level | Success Rate |
|--------|----------|------------|--------------|
| `PartitionErase` | EDL mode | Low | High |
| `PartitionOverwrite` | EDL mode | Low | High |
| `PersistClear` | EDL mode | Medium* | High |
| `FastbootUnlock` | Fastboot | Low | Variable |
| `AdbBypass` | USB Debug | Low | Medium |

*Clears calibration data

### Usage

```csharp
var frpManager = new FrpBypassManager(firehoseManager);

// Detect FRP status
var frpInfo = await frpManager.DetectFrpStatusAsync(deviceContext, progress, ct);

if (frpInfo.Status == FrpStatus.Locked)
{
    // Attempt bypass
    var result = await frpManager.BypassFrpAsync(
        deviceContext,
        method: null,  // Auto-select best method
        progress,
        cancellationToken);
    
    if (result.Success)
    {
        Console.WriteLine($"FRP bypassed using {result.MethodUsed}");
        Console.WriteLine($"Next: {result.AdditionalSteps}");
    }
}
```

### Brand-Specific FRP Partitions

```csharp
// Built-in mappings
XIAOMI    -> ["frp", "config"]
SAMSUNG   -> ["persistent", "frp", "sec_efs"]
ONEPLUS   -> ["frp", "config"]
OPPO      -> ["frp", "opporeserve2"]
REALME    -> ["frp", "oplusreserve2"]
VIVO      -> ["frp", "reserve1"]
MOTOROLA  -> ["frp", "utags"]
LG        -> ["frp", "OP"]
HUAWEI    -> ["frp", "oeminfo"]
```

---

## Device Profiles

### Profile Structure (EdlProfiles.json)

```json
{
    "Brand": "OnePlus",
    "Model": "OnePlus 8",
    "Codename": "instantnoodle",
    "SoC": "SM8250",
    "SupportsAdbRebootEdl": false,
    "SupportsFastbootOemEdl": false,
    "RequiresAuthTool": false,
    "RequiresTestPoint": true,
    "TestPointDiagramUrl": "https://...",
    "Notes": "OxygenOS 11+ blocks software EDL"
}
```

### Test Point Information

```json
{
    "DeviceModel": "OnePlus 8",
    "Brand": "OnePlus",
    "Description": "Test point near volume button under shielding",
    "DiagramUrl": "https://...",
    "ToolsNeeded": "Tweezers, T2 screwdriver",
    "Difficulty": "Moderate",
    "RequiresBatteryDisconnect": true
}
```

### Brand Defaults

```json
{
    "Xiaomi": {
        "defaultAuthTool": "Mi Flash Pro",
        "defaultCapability": "SOFTWARE_RESTRICTED",
        "notes": "MIUI 13+ and HyperOS typically block software EDL"
    },
    "Samsung": {
        "defaultCapability": "HARDWARE_ONLY",
        "notes": "Samsung never supports software EDL"
    }
}
```

---

## UI Components

### Available Panels

| Panel | Purpose |
|-------|---------|
| `EdlControlPanel` | EDL entry and driver management |
| `FirehoseControlPanel` | Programmer loading and partition ops |
| `BackupRestorePanel` | Full/critical backup and restore |
| `FrpBypassPanel` | FRP detection and bypass |

### Using Panels

```csharp
// Add to your WinForms application
var edlPanel = new EdlControlPanel();
edlPanel.SetDevice(deviceContext, profil);
this.Controls.Add(edlPanel);

var firehosePanel = new FirehoseControlPanel();
firehosePanel.SetDevice(deviceContext, usbDevice);
this.Controls.Add(firehosePanel);
```

---

## API Reference

### Core Classes

#### DeviceContext

```csharp
public class DeviceContext
{
    public string Brand { get; set; }
    public string Model { get; set; }
    public string Serial { get; set; }
    public string Chipset { get; set; }
    public string SoC { get; set; }
    public ConnectionMode Mode { get; set; }
}
```

#### ProgressUpdate

```csharp
public class ProgressUpdate
{
    public int ProgressPercent { get; set; }
    public string Message { get; set; }
    public LogLevel Level { get; set; }
    
    public static ProgressUpdate Info(int percent, string msg);
    public static ProgressUpdate Warn(int percent, string msg);
}
```

### Key Interfaces

#### IEdlManager

```csharp
public interface IEdlManager
{
    Task<EdlResult> RebootToEdlAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct);
    Task<bool> IsInEdlModeAsync(CancellationToken ct);
    Task<bool> WaitForEdlModeAsync(TimeSpan timeout, CancellationToken ct);
    EdlCapability GetCapabilityFor(DeviceContext device);
    EdlProfile? GetProfileFor(DeviceContext device);
}
```

#### IProtocol

```csharp
public interface IProtocol : IDisposable
{
    DeviceContext Context { get; }
    Task ConnectAsync(CancellationToken ct);
    Task<bool> ReadPartitionToStreamAsync(string partition, Stream output, IProgress<ProgressUpdate> progress, CancellationToken ct);
    Task<bool> WritePartitionFromStreamAsync(string partition, Stream input, IProgress<ProgressUpdate> progress, CancellationToken ct);
}
```

---

## Safety Guidelines

### ⚠️ Critical Warnings

1. **Always backup before modifying** - Use `BackupRestoreManager` to create full backups
2. **Verify device compatibility** - Check `EdlProfiles.json` for your specific model
3. **Use correct programmers** - Wrong programmer = potential brick
4. **Never interrupt operations** - Especially during writes
5. **FRP bypass responsibly** - Only use on devices you own or have authorization for

### Recovery from Brick

If your device becomes unresponsive:

1. **Try EDL mode** - Use test-point if software methods fail
2. **Restore from backup** - Use the backup you created earlier
3. **Flash stock firmware** - Download from official sources
4. **Last resort** - Contact manufacturer service center

---

## License

© 2026 DeepEye Technologies. For authorized technician use only.
