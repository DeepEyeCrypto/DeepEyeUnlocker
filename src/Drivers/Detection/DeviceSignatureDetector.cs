using System;
using System.Collections.Generic;
using System.Management;
using System.Text.RegularExpressions;
using DeepEyeUnlocker.Drivers.Models;

namespace DeepEyeUnlocker.Drivers.Detection
{
    public interface IDeviceDetector
    {
        List<ConnectedDevice> GetConnectedDevices();
    }

    public class DeviceSignatureDetector : IDeviceDetector
    {
        private static readonly Regex VidPidRegex = new Regex(@"VID_([0-9A-F]{4})&PID_([0-9A-F]{4})", RegexOptions.IgnoreCase);

        public List<ConnectedDevice> GetConnectedDevices()
        {
            var devices = new List<ConnectedDevice>();
            try
            {
                using var searcher = new ManagementObjectSearcher(@"SELECT * FROM Win32_PnPEntity WHERE DeviceID LIKE 'USB%'");
                foreach (ManagementObject entity in searcher.Get())
                {
                    var device = new ConnectedDevice
                    {
                        Description = entity["Description"]?.ToString() ?? string.Empty,
                        Manufacturer = entity["Manufacturer"]?.ToString() ?? string.Empty,
                        HardwareId = GetHardwareId(entity),
                        ConfigManagerErrorCode = Convert.ToInt32(entity["ConfigManagerErrorCode"] ?? 0),
                        IsProblemDevice = Convert.ToInt32(entity["ConfigManagerErrorCode"] ?? 0) != 0
                    };

                    var match = VidPidRegex.Match(device.HardwareId);
                    if (match.Success)
                    {
                        device.Vid = match.Groups[1].Value.ToUpper();
                        device.Pid = match.Groups[2].Value.ToUpper();
                    }

                    // Try to get driver info
                    try
                    {
                        // This involves more complex queries or SetupAPI, keeping it simple for now
                        // In real implementation, we'd query Win32_PnPSignedDriver
                    } catch { }

                    devices.Add(device);
                }
            }
            catch (Exception ex)
            {
                Core.Logger.Error(ex, "Failed to enumerate USB devices via WMI.");
            }
            return devices;
        }

        private string GetHardwareId(ManagementObject entity)
        {
            var ids = entity["HardwareID"] as string[];
            if (ids != null && ids.Length > 0)
                return ids[0];
            return entity["DeviceID"]?.ToString() ?? string.Empty;
        }
    }
}
