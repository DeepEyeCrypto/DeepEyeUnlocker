using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Features.Drivers
{
    public enum DriverStatus
    {
        Healthy,
        Missing,
        Conflict,
        Outdated,
        Unknown
    }

    public class DriverInfo
    {
        public string Name { get; set; } = string.Empty;
        public string HardwareId { get; set; } = string.Empty;
        public string Provider { get; set; } = string.Empty;
        public string Version { get; set; } = string.Empty;
        public DriverStatus Status { get; set; } = DriverStatus.Unknown;
        public string? InfPath { get; set; }
    }

    /// <summary>
    /// Host-side engine for detecting and diagnosing Android-related driver stacks.
    /// </summary>
    public class DriverCenterEngine
    {
        private readonly Infrastructure.Drivers.DriverConflictManager _conflictManager = new();

        public async Task<List<DriverInfo>> ScanDriversAsync()
        {
            Logger.Info("Scanning host for Android/Chipset drivers and conflicts...");
            var drivers = new List<DriverInfo>();

            // 1. Scan for PnP Entities via WMI
            try
            {
                await Task.Run(() => {
                    #if WINDOWS
                    using var searcher = new System.Management.ManagementObjectSearcher("SELECT * FROM Win32_PnPEntity WHERE (PNPDeviceID LIKE 'USB%' OR Service LIKE '%mtk%' OR Service LIKE '%qusb%')");
                    foreach (var device in searcher.Get())
                    {
                        var status = device["Status"]?.ToString() == "OK" ? DriverStatus.Healthy : DriverStatus.Conflict;
                        drivers.Add(new DriverInfo
                        {
                            Name = device["Name"]?.ToString() ?? "Unknown Device",
                            HardwareId = device["PNPDeviceID"]?.ToString() ?? "N/A",
                            Provider = device["Manufacturer"]?.ToString() ?? "System Driver",
                            Version = "Native",
                            Status = status
                        });
                    }
                    #else
                    // Fallback for non-windows compilation or local mocks
                    drivers.Add(new DriverInfo { Name = "Qualcomm HS-USB QDLoader 9008", HardwareId = "USB\\VID_05C6&PID_9008", Provider = "Qualcomm Incorporated", Status = DriverStatus.Healthy });
                    #endif
                });
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Hardware audit failed. Fallback to registry-only scanning.");
            }

            // 2. Scan for Deep-System Conflicts via Registry
            var conflicts = await _conflictManager.DetectConflictsAsync();
            foreach (var c in conflicts)
            {
                drivers.Add(new DriverInfo
                {
                    Name = $"AUDIT: {c.Name}",
                    HardwareId = "System Registry Node",
                    Provider = c.Reason, 
                    Status = c.IsCritical ? DriverStatus.Conflict : DriverStatus.Outdated
                });
            }

            return drivers;
        }

        public async Task<bool> RepairDriverPresetAsync(string presetName)
        {
            Logger.Info($"Applying driver repair sequence: {presetName}");
            
            try
            {
                // 1. Purge all detected conflicts first
                var conflicts = await _conflictManager.DetectConflictsAsync();
                foreach (var c in conflicts)
                {
                    Logger.Info($"Purging conflict before install: {c.Name}");
                    await _conflictManager.PurgeConflictAsync(c);
                }

                // 2. Run Smart Installer for clean re-deployment
                // Note: SmartDriverInstaller is assumed to be in the Drivers namespace as per other references
                var smartInstaller = new DeepEyeUnlocker.Drivers.SmartDriverInstaller();
                var progress = new Progress<string>(p => {
                    Logger.Info($"[SmartInstall] {p}");
                });

                return await smartInstaller.InstallUniversalDriversAsync(progress);
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to execute professional driver repair.");
                return false;
            }
        }
    }
}
