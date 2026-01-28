using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Features.DsuSandbox.Models
{
    /// <summary>
    /// DSU capability level for a device
    /// </summary>
    public enum DsuCapabilityLevel
    {
        NotSupported,
        PartialSupport,     // DSU only via recovery
        FullSupport,        // DSU + A/B slot
        Excellent           // Full DSU + A/B + verified device
    }

    /// <summary>
    /// Test method for ROM sandbox
    /// </summary>
    public enum DsuTestMethod
    {
        DsuAdb,             // DSU via ADB sideload (safest)
        DsuRecovery,        // DSU via TWRP/recovery
        ABSlot,             // Flash to alternate slot
        Manual              // Manual instructions only
    }

    /// <summary>
    /// Represents a GSI/Custom ROM image
    /// </summary>
    public class DsuImage
    {
        public string Id { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string OsName { get; set; } = "AOSP";
        public string Version { get; set; } = string.Empty;
        public string Architecture { get; set; } = "arm64";
        public long SizeMb { get; set; }
        public string Sha256Checksum { get; set; } = string.Empty;
        public string DownloadUrl { get; set; } = string.Empty;
        public int MinAndroidVersion { get; set; } = 10;
        public string Source { get; set; } = "google";
        public string Notes { get; set; } = string.Empty;
        public Dictionary<string, string> Compatibility { get; set; } = new();
        public bool IsDownloaded { get; set; }
        public string LocalPath { get; set; } = string.Empty;

        public string SizeFormatted => SizeMb >= 1024 
            ? $"{SizeMb / 1024.0:F1} GB" 
            : $"{SizeMb} MB";
    }

    /// <summary>
    /// Device DSU capability information
    /// </summary>
    public class DsuCapability
    {
        public string DeviceModel { get; set; } = string.Empty;
        public string Bootloader { get; set; } = string.Empty;
        public int AndroidVersion { get; set; }
        public DsuCapabilityLevel Level { get; set; } = DsuCapabilityLevel.NotSupported;
        public bool SupportsDsuAdb { get; set; }
        public bool SupportsDsuRecovery { get; set; }
        public bool SupportsABSlot { get; set; }
        public bool BootloaderUnlocked { get; set; }
        public long FreeSpaceBytes { get; set; }
        public string PreferredMethod { get; set; } = "dsu";
        public string Notes { get; set; } = string.Empty;
        public List<string> Warnings { get; set; } = new();
    }

    /// <summary>
    /// Pre-flight validation result
    /// </summary>
    public class DsuValidationResult
    {
        public bool IsValid => Errors.Count == 0;
        public List<string> Errors { get; set; } = new();
        public List<string> Warnings { get; set; } = new();
        public bool ImageFormatValid { get; set; }
        public bool ChecksumValid { get; set; }
        public bool SpaceAvailable { get; set; }
        public bool CompatibilityOk { get; set; }
    }

    /// <summary>
    /// Boot health report after DSU boot
    /// </summary>
    public class BootHealthReport
    {
        public bool IsHealthy => BootErrors.Count == 0 && AdbResponsive && BootTimeSeconds < 180;
        public bool AdbResponsive { get; set; }
        public string OsVersion { get; set; } = string.Empty;
        public string BuildFingerprint { get; set; } = string.Empty;
        public List<string> BootErrors { get; set; } = new();
        public bool CrashLoopsDetected { get; set; }
        public bool PlayServicesPresent { get; set; }
        public int BootTimeSeconds { get; set; }
        public DateTime BootTimestamp { get; set; }
        public string SlotSuffix { get; set; } = string.Empty;
        public bool IsDsuBoot { get; set; }
    }

    /// <summary>
    /// Flashing progress update
    /// </summary>
    public class DsuFlashProgress
    {
        public string Stage { get; set; } = string.Empty;
        public int StageNumber { get; set; }
        public int TotalStages { get; set; }
        public int PercentComplete { get; set; }
        public string Message { get; set; } = string.Empty;
        public long BytesTransferred { get; set; }
        public long TotalBytes { get; set; }
        public TimeSpan Elapsed { get; set; }
        public TimeSpan EstimatedRemaining { get; set; }
        public bool IsComplete { get; set; }
        public bool HasError { get; set; }
        public string ErrorMessage { get; set; } = string.Empty;
    }

    /// <summary>
    /// Test history entry
    /// </summary>
    public class DsuTestHistoryEntry
    {
        public string Id { get; set; } = Guid.NewGuid().ToString();
        public string ImageId { get; set; } = string.Empty;
        public string ImageName { get; set; } = string.Empty;
        public string DeviceModel { get; set; } = string.Empty;
        public DsuTestMethod Method { get; set; }
        public DateTime StartTime { get; set; }
        public DateTime? EndTime { get; set; }
        public bool Success { get; set; }
        public string Result { get; set; } = string.Empty; // "kept", "reverted", "failed"
        public int BootTimeSeconds { get; set; }
        public BootHealthReport? HealthReport { get; set; }
    }
}
