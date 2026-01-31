using System;
using System.Collections.Generic;
using System.Management;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Features.DriverCenter.Models;
using System.Linq;

namespace DeepEyeUnlocker.Features.DriverCenter.Services
{
    public class DriverDetectorService
    {
        public async Task<List<DeviceConnectivityReport>> ScanConnectedDevicesAsync()
        {
            return await Task.Run(() =>
            {
                var reports = new List<DeviceConnectivityReport>();
                Logger.Info("[DRIVER-DETECTOR] Scanning USB bus for Android-compatible hardware...");

                try
                {
                    // WMI query for all PnP entities
                    using var searcher = new ManagementObjectSearcher("SELECT * FROM Win32_PnPEntity WHERE DeviceID LIKE 'USB\\\\%'");
                    foreach (var device in searcher.Get())
                    {
                        string deviceId = device["DeviceID"]?.ToString() ?? "";
                        string name = device["Name"]?.ToString() ?? "Unknown USB Device";
                        string status = device["Status"]?.ToString() ?? "";
                        uint configManagerErrorCode = (uint)(device["ConfigManagerErrorCode"] ?? 0);

                        // Extract VID/PID
                        if (deviceId.Contains("VID_") && deviceId.Contains("PID_"))
                        {
                            reports.Add(new DeviceConnectivityReport
                            {
                                DeviceName = name,
                                InstancePath = deviceId,
                                HasDriverIssue = configManagerErrorCode != 0,
                                VendorId = ExtractHex(deviceId, "VID_"),
                                ProductId = ExtractHex(deviceId, "PID_")
                            });
                        }
                    }
                }
                catch (Exception ex)
                {
                    Logger.Error($"[DRIVER-DETECTOR] WMI Scan failed: {ex.Message}");
                }

                return reports;
            });
        }

        public async Task<List<DriverStatus>> CheckDriverHealthAsync(List<DriverProfile> profiles)
        {
            return await Task.Run(() =>
            {
                var healthReports = new List<DriverStatus>();
                
                // 1. Check for Platform Tools in PATH
                bool adbInPath = IsInPath("adb.exe");
                healthReports.Add(new DriverStatus
                {
                    Name = "Android Platform-Tools (ADB)",
                    BrandId = "google",
                    IsInstalled = adbInPath,
                    StatusMessage = adbInPath ? "System Path Verified" : "Binary Missing in System PATH"
                });

                // 2. Check for Specific Brand Drivers via Registry/WMI
                foreach (var profile in profiles)
                {
                    bool found = false;
                    try 
                    {
                        // Check if any device with this brand's VID exists and has a working driver
                        using var searcher = new ManagementObjectSearcher($"SELECT * FROM Win32_PnPEntity WHERE DeviceID LIKE 'USB\\\\VID_{profile.Vids.FirstOrDefault()?.Replace("0x", "")}%'");
                        foreach (var device in searcher.Get())
                        {
                            uint errCode = (uint)(device["ConfigManagerErrorCode"] ?? 0);
                            if (errCode == 0)
                            {
                                found = true;
                                break;
                            }
                        }
                    }
                    catch { /* Fallback */ }

                    healthReports.Add(new DriverStatus
                    {
                        Name = $"{profile.BrandName} USB Driver",
                        BrandId = profile.BrandId,
                        IsInstalled = found,
                        StatusMessage = found ? "Driver Operational" : "Driver Not Detected or Erroneous"
                    });
                }

                return healthReports;
            });
        }

        private string ExtractHex(string source, string key)
        {
            int start = source.IndexOf(key);
            if (start == -1) return "0000";
            return "0x" + source.Substring(start + key.Length, 4);
        }

        private bool IsInPath(string fileName)
        {
            var paths = Environment.GetEnvironmentVariable("PATH")?.Split(';');
            if (paths == null) return false;

            foreach (var path in paths)
            {
                try { if (System.IO.File.Exists(System.IO.Path.Combine(path, fileName))) return true; } catch { }
            }
            return false;
        }
    }
}
