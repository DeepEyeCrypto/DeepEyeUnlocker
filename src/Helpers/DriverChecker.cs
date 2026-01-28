using System;
using System.Management;
using System.Collections.Generic;
using NLog;

namespace DeepEyeUnlocker.Helpers
{
    public class DriverStatus
    {
        public string Name { get; set; } = "";
        public bool IsInstalled { get; set; }
        public string Version { get; set; } = "N/A";
    }

    public static class DriverChecker
    {
        private static readonly Logger Logger = LogManager.GetCurrentClassLogger();

        public static List<DriverStatus> CheckDrivers()
        {
            var results = new List<DriverStatus>();
            
            // Driver names to look for in the system
            var searchTargets = new Dictionary<string, string>
            {
                { "Qualcomm", "Qualcomm HS-USB QDLoader 9008" },
                { "MediaTek", "MediaTek USB Port" },
                { "Samsung", "SAMSUNG Mobile USB Connectivity" }
            };

            try
            {
                // This is a simplified check via WMI (Windows Management Instrumentation)
                // In a real environment, this scans the PNP entities
                using var searcher = new ManagementObjectSearcher("SELECT * FROM Win32_PnPEntity");
                var devices = searcher.Get();

                foreach (var target in searchTargets)
                {
                    bool found = false;
                    foreach (var device in devices)
                    {
                        string name = device["Name"]?.ToString() ?? "";
                        if (name.Contains(target.Value, StringComparison.OrdinalIgnoreCase))
                        {
                            results.Add(new DriverStatus { Name = target.Key, IsInstalled = true });
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                    {
                        results.Add(new DriverStatus { Name = target.Key, IsInstalled = false });
                    }
                }
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to perform WMI driver check.");
            }

            return results;
        }
    }
}
