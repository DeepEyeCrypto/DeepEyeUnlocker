using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using DeepEyeUnlocker.Drivers.Models;
using DeepEyeUnlocker.Drivers.Detection;
using DeepEyeUnlocker.Drivers.Installer;

namespace DeepEyeUnlocker.Drivers
{
    public class UsbDriverManager
    {
        private readonly List<DriverProfile> _profiles;
        private readonly IDeviceDetector _detector;
        private readonly IUsbDriverInstaller _installer;

        public UsbDriverManager(IDeviceDetector detector, IUsbDriverInstaller installer)
        {
            _detector = detector;
            _installer = installer;
            _profiles = LoadDefaultProfiles();
        }

        public List<DriverProfile> GetAllProfiles() => _profiles;

        public List<ConnectedDevice> GetActiveDevices() => _detector.GetConnectedDevices();

        public DriverProfile? GetRecommendedProfile(ConnectedDevice device)
        {
            if (string.IsNullOrEmpty(device.Vid)) return null;

            return _profiles.FirstOrDefault(p => p.HardwareIdPatterns.Any(pattern => 
                device.HardwareId.Contains(pattern, StringComparison.OrdinalIgnoreCase) ||
                (device.Vid == GetVidPart(pattern) && device.Pid == GetPidPart(pattern))
            ));
        }

        public async Task<DriverInstallResult> InstallProfileAsync(DriverProfile profile)
        {
            return await _installer.InstallAsync(profile);
        }

        private string? GetVidPart(string pattern) 
        {
            var parts = pattern.Split('&');
            return parts.FirstOrDefault(p => p.StartsWith("VID_"))?.Replace("VID_", "");
        }

        private string? GetPidPart(string pattern) 
        {
            var parts = pattern.Split('&');
            return parts.FirstOrDefault(p => p.StartsWith("PID_"))?.Replace("PID_", "");
        }

        private List<DriverProfile> LoadDefaultProfiles()
        {
            // In a real app, this would be loaded from JSON or Embedded Resources
            return new List<DriverProfile>
            {
                new DriverProfile 
                { 
                    Id = "google_adb", 
                    Name = "Google ADB Driver", 
                    Vendor = "Google", 
                    Mode = "ADB", 
                    HardwareIdPatterns = new[] { "VID_18D1&PID_4EE7" },
                    InfPath = "drivers/google/android_winusb.inf",
                    Version = "13.0.0.0"
                },
                new DriverProfile 
                { 
                    Id = "qualcomm_edl", 
                    Name = "Qualcomm HS-USB QDLoader 9008", 
                    Vendor = "Qualcomm", 
                    Mode = "EDL", 
                    HardwareIdPatterns = new[] { "VID_05C6&PID_9008" },
                    InfPath = "drivers/qualcomm/qcser.inf",
                    Version = "2.1.2.2"
                },
                new DriverProfile 
                { 
                    Id = "samsung_modem", 
                    Name = "Samsung Mobile USB Modem", 
                    Vendor = "Samsung", 
                    Mode = "Download", 
                    HardwareIdPatterns = new[] { "VID_04E8&PID_685D" },
                    InfPath = "drivers/samsung/ssudmdm.inf",
                    Version = "2.12.0.0"
                },
                new DriverProfile 
                { 
                    Id = "mtk_vcom", 
                    Name = "MediaTek USB VCOM (Android)", 
                    Vendor = "MediaTek", 
                    Mode = "BROM/Preloader", 
                    HardwareIdPatterns = new[] { "VID_0E8D&PID_2000", "VID_0E8D&PID_0003" },
                    InfPath = "drivers/mediatek/usb_vcom.inf",
                    Version = "3.0.1504"
                }
            };
        }
    }
}
