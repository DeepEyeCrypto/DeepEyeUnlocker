using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.Win32;

namespace DeepEyeUnlocker.Infrastructure.Drivers
{
    public class UsbDeviceId
    {
        public ushort Vid { get; }
        public ushort Pid { get; }
        public string Name { get; }

        public UsbDeviceId(ushort vid, ushort pid, string name)
        {
            Vid = vid;
            Pid = pid;
            Name = name;
        }
    }

    public class LibUsbFilterManager
    {
        private const string LibUsbServiceKey = @"HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\libusb0";
        private const string UsbEnumKey = @"SYSTEM\CurrentControlSet\Enum\USB";

        public async Task<bool> InstallFiltersAsync()
        {
            try
            {
                // 1. Ensure libusb service exists
                EnsureLibUsbService();

                // 2. Attach filters for critical VID/PIDs
                var targets = GetCriticalTargets();
                foreach (var device in targets)
                {
                    AttachFilter(device);
                }

                await Task.Delay(100); // UI breathing room
                return true;
            }
            catch (Exception ex)
            {
                // In production, log to telemetry
                System.Diagnostics.Debug.WriteLine($"Filter Install Error: {ex.Message}");
                return false;
            }
        }

        private void EnsureLibUsbService()
        {
            // Registry intervention for LibUSB-win32 service
            using (var key = Registry.LocalMachine.CreateSubKey(@"SYSTEM\CurrentControlSet\Services\libusb0"))
            {
                key.SetValue("Type", 1, RegistryValueKind.DWord);
                key.SetValue("Start", 3, RegistryValueKind.DWord); // Manual start, PnP will trigger
                key.SetValue("ErrorControl", 1, RegistryValueKind.DWord);
                key.SetValue("DisplayName", "LibUSB-win32 Filter Driver", RegistryValueKind.String);
            }
        }

        private void AttachFilter(UsbDeviceId device)
        {
            string hwid = $"VID_{device.Vid:X4}&PID_{device.Pid:X4}";
            string deviceKeyPath = $@"{UsbEnumKey}\{hwid}";

            // Note: On Windows, accessing Enum key requires SYSTEM or TrustedInstaller usually
            // but for PnP management via SetupAPI, we typically register the INF which does this.
            // Direct registry write is for "Force Filter" scenarios like Chimera.
            
            using (var usbKey = Registry.LocalMachine.OpenSubKey(deviceKeyPath, true))
            {
                if (usbKey != null)
                {
                    foreach (var subKeyName in usbKey.GetSubKeyNames())
                    {
                        using (var instanceKey = usbKey.OpenSubKey(subKeyName, true))
                        {
                            if (instanceKey == null) continue;
                            
                            // Set LowerFilters for BROM stability
                            instanceKey.SetValue("LowerFilters", new[] { "libusb0" }, RegistryValueKind.MultiString);
                            instanceKey.SetValue("ClassGUID", "{78A1C341-4539-11D3-B88D-00C04FAD5171}", RegistryValueKind.String);
                        }
                    }
                }
            }

            // Also register in service parameters
            string paramPath = $@"SYSTEM\CurrentControlSet\Services\libusb0\Parameters\Devices\{device.Vid:X4}_{device.Pid:X4}";
            using (var paramKey = Registry.LocalMachine.CreateSubKey(paramPath))
            {
                paramKey.SetValue("Location", "Standard", RegistryValueKind.String);
            }
        }

        public List<UsbDeviceId> GetCriticalTargets()
        {
            return new List<UsbDeviceId>
            {
                new UsbDeviceId(0x0E8D, 0x0003, "MTK_BROM"),
                new UsbDeviceId(0x0E8D, 0x2000, "MTK_Preloader"),
                new UsbDeviceId(0x05C6, 0x9008, "Qualcomm_EDL"),
                new UsbDeviceId(0x1782, 0x4D00, "SPD_FDL")
            };
        }
    }
}
