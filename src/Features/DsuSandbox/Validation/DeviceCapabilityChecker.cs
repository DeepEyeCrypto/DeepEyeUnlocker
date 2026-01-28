using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.DsuSandbox.Models;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.Features.DsuSandbox.Validation
{
    /// <summary>
    /// Checks device DSU capabilities
    /// </summary>
    public class DeviceCapabilityChecker
    {
        private readonly IAdbClient _adb;

        // Known DSU-compatible Qualcomm VID:PID for EDL
        private static readonly HashSet<string> KnownDsuCompatibleChipsets = new(StringComparer.OrdinalIgnoreCase)
        {
            "SM8350", "SM8450", "SM8550", "SM8650", // Snapdragon 8 Gen series
            "SM7325", "SM7450", "SM7550",           // Snapdragon 7 series
            "SM6350", "SM6375", "SM6450",           // Snapdragon 6 series
            "MT6893", "MT6895", "MT6983",           // MediaTek Dimensity
        };

        public DeviceCapabilityChecker(IAdbClient adb)
        {
            _adb = adb;
        }

        /// <summary>
        /// Check DSU capability for connected device
        /// </summary>
        public async Task<DsuCapability> CheckCapabilityAsync(DeviceContext device, CancellationToken ct = default)
        {
            var capability = new DsuCapability
            {
                DeviceModel = $"{device.Brand} {device.Model}",
                AndroidVersion = ParseAndroidVersion(device.AndroidVersion)
            };

            try
            {
                // Check Android version (DSU requires 10+)
                if (capability.AndroidVersion < 10)
                {
                    capability.Level = DsuCapabilityLevel.NotSupported;
                    capability.Notes = "DSU requires Android 10 or higher.";
                    return capability;
                }

                // Check bootloader status
                var bootloaderStatus = await GetPropertyAsync("ro.boot.flash.locked");
                capability.BootloaderUnlocked = bootloaderStatus == "0" || 
                    device.BootloaderStatus?.ToLower() == "unlocked";

                // Check for DSU property support
                var dsuSupport = await GetPropertyAsync("ro.boot.dynamic_partitions");
                var retrofitDp = await GetPropertyAsync("ro.boot.dynamic_partitions_retrofit");
                
                capability.SupportsDsuAdb = dsuSupport == "true" || retrofitDp == "true";

                // Check A/B slot support
                var slotSuffix = await GetPropertyAsync("ro.boot.slot_suffix");
                capability.SupportsABSlot = !string.IsNullOrEmpty(slotSuffix) && 
                    (slotSuffix == "_a" || slotSuffix == "_b");

                // Check free space in /data
                var freeSpace = await GetFreeSpaceAsync("/data");
                capability.FreeSpaceBytes = freeSpace;

                // Check for TWRP or custom recovery
                var recoveryVersion = await GetPropertyAsync("ro.twrp.version");
                capability.SupportsDsuRecovery = !string.IsNullOrEmpty(recoveryVersion);

                // Determine overall capability level
                capability.Level = DetermineCapabilityLevel(capability);
                capability.PreferredMethod = DeterminePreferredMethod(capability);
                
                // Add warnings
                if (!capability.BootloaderUnlocked && capability.SupportsABSlot)
                {
                    capability.Warnings.Add("A/B slot flashing requires unlocked bootloader.");
                }
                
                if (freeSpace < 5L * 1024 * 1024 * 1024) // Less than 5GB
                {
                    capability.Warnings.Add($"Low storage space ({freeSpace / (1024 * 1024 * 1024.0):F1} GB free). " +
                        "Consider freeing up space before testing ROMs.");
                }

                // Generate helpful notes
                capability.Notes = GenerateCapabilityNotes(capability);
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to check DSU capability");
                capability.Level = DsuCapabilityLevel.NotSupported;
                capability.Notes = $"Error checking capability: {ex.Message}";
            }

            return capability;
        }

        /// <summary>
        /// Quick check if DSU is likely supported
        /// </summary>
        public async Task<bool> QuickDsuCheckAsync()
        {
            try
            {
                var dynamicPartitions = await GetPropertyAsync("ro.boot.dynamic_partitions");
                return dynamicPartitions == "true";
            }
            catch
            {
                return false;
            }
        }

        private async Task<string> GetPropertyAsync(string property)
        {
            if (!_adb.IsConnected()) return string.Empty;
            
            try
            {
                return await _adb.ExecuteShellAsync($"getprop {property}");
            }
            catch
            {
                return string.Empty;
            }
        }

        private async Task<long> GetFreeSpaceAsync(string path)
        {
            try
            {
                var output = await _adb.ExecuteShellAsync($"df {path}");
                // Parse df output: Filesystem 1K-blocks Used Available Use% Mounted on
                var lines = output.Split('\n', StringSplitOptions.RemoveEmptyEntries);
                if (lines.Length >= 2)
                {
                    var parts = lines[1].Split(new[] { ' ', '\t' }, StringSplitOptions.RemoveEmptyEntries);
                    if (parts.Length >= 4 && long.TryParse(parts[3], out var kblocks))
                    {
                        return kblocks * 1024; // Convert KB to bytes
                    }
                }
            }
            catch (Exception ex)
            {
                Logger.Warn($"Failed to get free space: {ex.Message}");
            }
            return 0;
        }

        private static int ParseAndroidVersion(string? version)
        {
            if (string.IsNullOrEmpty(version)) return 0;
            var parts = version.Split('.');
            return int.TryParse(parts[0], out var major) ? major : 0;
        }

        private static DsuCapabilityLevel DetermineCapabilityLevel(DsuCapability cap)
        {
            if (!cap.SupportsDsuAdb && !cap.SupportsABSlot)
                return DsuCapabilityLevel.NotSupported;

            if (cap.SupportsDsuAdb && cap.SupportsABSlot && cap.BootloaderUnlocked)
                return DsuCapabilityLevel.Excellent;

            if (cap.SupportsDsuAdb && cap.SupportsABSlot)
                return DsuCapabilityLevel.FullSupport;

            if (cap.SupportsDsuAdb || cap.SupportsDsuRecovery)
                return DsuCapabilityLevel.PartialSupport;

            return DsuCapabilityLevel.NotSupported;
        }

        private static string DeterminePreferredMethod(DsuCapability cap)
        {
            if (cap.SupportsDsuAdb)
                return "dsu_adb";
            if (cap.SupportsDsuRecovery)
                return "dsu_recovery";
            if (cap.SupportsABSlot && cap.BootloaderUnlocked)
                return "ab_slot";
            return "manual";
        }

        private static string GenerateCapabilityNotes(DsuCapability cap)
        {
            return cap.Level switch
            {
                DsuCapabilityLevel.Excellent => 
                    "Full DSU + A/B slot support. Best candidate for safe ROM testing.",
                DsuCapabilityLevel.FullSupport => 
                    "DSU and A/B supported. Unlock bootloader for full A/B slot access.",
                DsuCapabilityLevel.PartialSupport => 
                    "DSU supported via ADB or recovery. A/B slot may be limited.",
                DsuCapabilityLevel.NotSupported => 
                    "DSU not detected. Device may not support safe ROM testing.",
                _ => "Unknown capability"
            };
        }
    }
}
