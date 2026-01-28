# DSU Sandbox - Safe ROM Testing Feature

## Overview

The DSU (Dynamic System Updates) Sandbox feature allows users to safely test custom ROMs, GSI images, and system builds without permanently modifying their device. This is achieved using Android's built-in DSU mechanism or A/B slot architecture.

## Key Features

1. **Zero-Risk Testing**: Original system partition is never modified
2. **One-Reboot Revert**: Return to original ROM with a single reboot
3. **Multiple Methods**: DSU via ADB, Recovery, or A/B slot flashing
4. **Pre-flight Validation**: Automatic compatibility and safety checks
5. **GSI Database**: Curated catalog of tested GSI images

## How It Works

### DSU (Dynamic System Updates)

DSU is a Google feature (Android 10+) that stores a temporary system image in the `/data` partition. On boot, the bootloader detects the DSU metadata and boots the temporary system instead of the main system partition.

**Safety Features:**

- Original `/system` partition is never modified
- If DSU boot fails, device automatically falls back to original system
- User data can be isolated or shared (configurable)
- One reboot returns to normal operation

### A/B Slot Architecture

Modern Android devices have dual system partitions (A and B). When running slot A, you can flash a test ROM to slot B without affecting your current system.

**Requirements:**

- Unlocked bootloader
- A/B partition scheme
- Fastboot access

## Architecture

```
src/Features/DsuSandbox/
├── Orchestration/
│   └── DsuFlashingOrchestrator.cs   # Main workflow coordinator
├── Validation/
│   ├── DsuImageValidator.cs          # Image format & checksum validation
│   └── DeviceCapabilityChecker.cs    # DSU support detection
├── ImageManagement/
│   └── GsiDatabase.cs                # GSI catalog & download manager
├── Models/
│   └── DsuModels.cs                  # Data models
├── UI/
│   └── RomSandboxPanel.cs            # WinForms UI
└── Database/
    └── gsi_images.json               # Default image catalog
```

## Workflow

```
┌──────────────────────────────────────────────────────────────┐
│                    ROM SANDBOX WORKFLOW                       │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  1. Select ROM/GSI Image                                     │
│     ├── From catalog (Google AOSP, LineageOS, etc.)         │
│     └── Browse local .img file                               │
│                                                               │
│  2. Pre-flight Checks                                        │
│     ├── Device compatibility (Android 10+, DSU support)     │
│     ├── Free space (≥2.5x image size)                       │
│     ├── Battery level (≥50%)                                │
│     └── Image validation (format, checksum)                  │
│                                                               │
│  3. Select Method                                            │
│     ├── DSU via ADB (safest, recommended)                   │
│     ├── DSU via Recovery (TWRP)                             │
│     └── A/B Slot (requires unlock)                          │
│                                                               │
│  4. Flash & Boot                                             │
│     ├── Push image to device                                │
│     ├── Create DSU metadata                                  │
│     ├── Reboot to test system                               │
│     └── Validate boot health                                │
│                                                               │
│  5. Test & Decide                                            │
│     ├── Test device functionality                           │
│     ├── Keep (make permanent) or Revert                     │
│     └── Auto-revert timer (optional safety)                 │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

## Device Compatibility

| Device Type | Android | DSU Support | A/B Support | Recommended Method |
|-------------|---------|-------------|-------------|-------------------|
| Pixel 5+    | 10+     | ✅ Full     | ✅ Yes      | DSU ADB           |
| OnePlus 9+  | 11+     | ✅ Full     | ✅ Yes      | DSU Recovery      |
| Samsung S21+| 11+     | ⚠️ Partial  | ✅ Yes      | A/B Slot          |
| Xiaomi 12+  | 12+     | ✅ Full     | ✅ Yes      | DSU ADB           |
| Generic MTK | 10+     | ⚠️ Varies   | ⚠️ Varies   | Check device      |

## GSI Image Sources

1. **Google AOSP GSI**: <https://developer.android.com/topic/generic-system-image>
2. **LineageOS GSI**: <https://github.com/AyanYan/gsi-project>
3. **Pixel Experience GSI**: Community builds
4. **crDroid GSI**: Community builds

## API Reference

### DsuFlashingOrchestrator

```csharp
// Flash DSU image
var report = await orchestrator.FlashDsuAsync(
    device,
    image,
    DsuTestMethod.DsuAdb,
    progressReporter,
    cancellationToken
);

// Revert to original
await orchestrator.RevertToOriginalAsync();

// Set active slot (A/B only)
await orchestrator.SetActiveSlotAsync("b");
```

### DeviceCapabilityChecker

```csharp
// Check device DSU capability
var capability = await checker.CheckCapabilityAsync(device);

if (capability.Level == DsuCapabilityLevel.Excellent)
{
    // Full DSU + A/B support
}
```

### GsiDatabase

```csharp
// Load database
await database.LoadAsync();

// Get compatible images
var images = database.GetCompatibleImages("snapdragon");

// Download image
await database.DownloadImageAsync(imageId, progressReporter, ct);

// Add custom image
await database.AddCustomImageAsync("MyROM", "/path/to/rom.img");
```

## Security Considerations

1. **Image Validation**: All images are validated for format and optional checksum
2. **Source Verification**: Only recommend images from trusted sources
3. **Automatic Fallback**: Failed DSU boots automatically revert to original
4. **No System Modification**: Original partition is never written
5. **User Data Isolation**: Optional data isolation during testing

## Limitations

1. **Android 10+ Required**: DSU is not available on older Android versions
2. **Device-Specific**: Not all devices implement DSU correctly
3. **Feature Gaps**: GSI ROMs may lack camera, fingerprint, etc.
4. **Storage Requirement**: Need ~3GB free for typical GSI
5. **Performance**: GSI may perform differently than optimized OEM build

## Future Enhancements

- [ ] Automatic GSI database updates from cloud
- [ ] Community compatibility reporting
- [ ] Multiple DSU slots (test multiple ROMs)
- [ ] ROM comparison reports
- [ ] Backup current ROM before testing
- [ ] Scheduled auto-revert timer
