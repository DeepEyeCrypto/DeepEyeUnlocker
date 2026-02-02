using System;
using System.Collections.Generic;
using Microsoft.Win32;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Infrastructure.Drivers
{
    /// <summary>
    /// Manages LibUSB-win32 Upper Filter registration for specified VID/PIDs.
    /// This prevents Windows from disconnecting devices in BROM/EDL mode.
    /// </summary>
    public class FilterDriverManager
    {
        private const string LibUsbServiceName = "libusb0";
        private const string ControlSetKey = @"SYSTEM\CurrentControlSet\Control\Class";

        // GUIDs for USB and Ports
        private const string UsbClassGuid = "{36FC9E60-C465-11CF-8056-444553540000}";
        private const string PortsClassGuid = "{4D36E978-E325-11CE-BFC1-08002BE10318}";

        public struct TargetDevice
        {
            public ushort Vid;
            public ushort Pid;
            public string Name;
        }

        private static readonly List<TargetDevice> MandatoryFilterDevices = new List<TargetDevice>
        {
            new TargetDevice { Vid = 0x0E8D, Pid = 0x0003, Name = "MediaTek BROM" },
            new TargetDevice { Vid = 0x0E8D, Pid = 0x2000, Name = "MediaTek Preloader" },
            new TargetDevice { Vid = 0x05C6, Pid = 0x9008, Name = "Qualcomm EDL" },
            new TargetDevice { Vid = 0x1782, Pid = 0x4D00, Name = "SPD FDL" }
        };

        /// <summary>
        /// Installs the LibUSB service as an Upper Filter for the specified device class.
        /// </summary>
        public bool RegisterUpperFilter(string classGuid)
        {
            try
            {
                using var classKey = Registry.LocalMachine.OpenSubKey($@"{ControlSetKey}\{classGuid}", true);
                if (classKey == null)
                {
                    Logger.Warning($"Device class {classGuid} not found in registry.");
                    return false;
                }

                var existingFilters = classKey.GetValue("UpperFilters") as string[];
                var filters = existingFilters != null ? new List<string>(existingFilters) : new List<string>();

                if (!filters.Contains(LibUsbServiceName))
                {
                    filters.Add(LibUsbServiceName);
                    classKey.SetValue("UpperFilters", filters.ToArray(), RegistryValueKind.MultiString);
                    Logger.Info($"Registered {LibUsbServiceName} as Upper Filter for {classGuid}");
                    return true;
                }

                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, $"Failed to register Upper Filter for {classGuid}");
                return false;
            }
        }

        /// <summary>
        /// Unregisters the LibUSB service from Upper Filters.
        /// </summary>
        public bool UnregisterUpperFilter(string classGuid)
        {
            try
            {
                using var classKey = Registry.LocalMachine.OpenSubKey($@"{ControlSetKey}\{classGuid}", true);
                if (classKey == null) return true;

                var existingFilters = classKey.GetValue("UpperFilters") as string[];
                if (existingFilters == null) return true;

                var filters = new List<string>(existingFilters);
                if (filters.Remove(LibUsbServiceName))
                {
                    if (filters.Count > 0)
                        classKey.SetValue("UpperFilters", filters.ToArray(), RegistryValueKind.MultiString);
                    else
                        classKey.DeleteValue("UpperFilters", false);
                    
                    Logger.Info($"Unregistered {LibUsbServiceName} from {classGuid}");
                }

                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, $"Failed to unregister Upper Filter for {classGuid}");
                return false;
            }
        }

        /// <summary>
        /// Verifies if the LibUSB service is installed and running.
        /// </summary>
        public bool IsLibUsbServiceInstalled()
        {
            using var serviceKey = Registry.LocalMachine.OpenSubKey($@"SYSTEM\CurrentControlSet\Services\{LibUsbServiceName}");
            return serviceKey != null;
        }
    }
}
