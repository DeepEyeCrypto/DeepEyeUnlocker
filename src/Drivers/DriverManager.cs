using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Win32;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Drivers
{
    /// <summary>
    /// Driver status enumeration
    /// </summary>
    public enum DriverStatus
    {
        NotInstalled,
        Installed,
        UpdateAvailable,
        Error,
        Unknown
    }

    /// <summary>
    /// Driver information DTO
    /// </summary>
    public class DriverInfo
    {
        public string Name { get; set; } = "";
        public string Version { get; set; } = "";
        public string Manufacturer { get; set; } = "";
        public DriverStatus Status { get; set; }
        public string? Path { get; set; }
        public string? Description { get; set; }
    }

    /// <summary>
    /// Manages device driver detection and installation
    /// </summary>
    public class DriverManager
    {
        private static readonly string DriversPath;
        
        // Known Qualcomm driver identifiers
        private static readonly string[] QualcommDriverNames = 
        {
            "Qualcomm HS-USB QDLoader 9008",
            "Qualcomm USB Driver",
            "QHSUSB_BULK",
            "Android Bootloader Interface"
        };

        // Known VID/PID for Qualcomm EDL
        private static readonly (int Vid, int Pid, string Name)[] QualcommDevices =
        {
            (0x05C6, 0x9008, "Qualcomm HS-USB QDLoader 9008"),
            (0x05C6, 0x9006, "Qualcomm HS-USB QDLoader 9006"),
            (0x05C6, 0x900E, "Qualcomm HS-USB Diagnostics 900E"),
            (0x05C6, 0xF006, "Qualcomm HS-USB Legacy"),
        };

        static DriverManager()
        {
            DriversPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "drivers");
        }

        /// <summary>
        /// Check if Qualcomm QDLoader driver is installed
        /// </summary>
        public async Task<DriverInfo> CheckQualcommDriverAsync(CancellationToken ct = default)
        {
            return await Task.Run(() =>
            {
                try
                {
                    // Method 1: Check Windows Device Manager via Registry
                    var registryDriver = CheckRegistryForDriver();
                    if (registryDriver != null)
                        return registryDriver;

                    // Method 2: Check for known driver files
                    var fileDriver = CheckDriverFiles();
                    if (fileDriver != null)
                        return fileDriver;

                    // Method 3: Use pnputil to list drivers
                    var pnpDriver = CheckPnpUtil();
                    if (pnpDriver != null)
                        return pnpDriver;

                    return new DriverInfo
                    {
                        Name = "Qualcomm USB Driver",
                        Status = DriverStatus.NotInstalled,
                        Description = "Qualcomm QDLoader 9008 driver not found"
                    };
                }
                catch (Exception ex)
                {
                    Core.Logger.Error($"Error checking Qualcomm driver: {ex.Message}", "DRIVER");
                    return new DriverInfo
                    {
                        Name = "Qualcomm USB Driver",
                        Status = DriverStatus.Error,
                        Description = ex.Message
                    };
                }
            }, ct);
        }

        /// <summary>
        /// Check if MediaTek USB driver is installed
        /// </summary>
        public async Task<DriverInfo> CheckMediaTekDriverAsync(CancellationToken ct = default)
        {
            return await Task.Run(() =>
            {
                try
                {
                    // Check for MTK VCOM driver
                    using var key = Registry.LocalMachine.OpenSubKey(@"SYSTEM\CurrentControlSet\Services\mtk_usb");
                    if (key != null)
                    {
                        return new DriverInfo
                        {
                            Name = "MediaTek USB VCOM",
                            Status = DriverStatus.Installed,
                            Manufacturer = "MediaTek Inc.",
                            Description = "MediaTek Preloader USB Driver"
                        };
                    }

                    // Check for alternative MTK driver
                    using var altKey = Registry.LocalMachine.OpenSubKey(@"SYSTEM\CurrentControlSet\Services\MediaTekDevices");
                    if (altKey != null)
                    {
                        return new DriverInfo
                        {
                            Name = "MediaTek Device Driver",
                            Status = DriverStatus.Installed,
                            Manufacturer = "MediaTek Inc."
                        };
                    }

                    return new DriverInfo
                    {
                        Name = "MediaTek USB Driver",
                        Status = DriverStatus.NotInstalled,
                        Description = "MediaTek VCOM driver not found"
                    };
                }
                catch (Exception ex)
                {
                    return new DriverInfo
                    {
                        Name = "MediaTek USB Driver",
                        Status = DriverStatus.Error,
                        Description = ex.Message
                    };
                }
            }, ct);
        }

        /// <summary>
        /// Check if Samsung USB driver is installed
        /// </summary>
        public async Task<DriverInfo> CheckSamsungDriverAsync(CancellationToken ct = default)
        {
            return await Task.Run(() =>
            {
                try
                {
                    // Check for Samsung Mobile MTP Device
                    using var key = Registry.LocalMachine.OpenSubKey(@"SYSTEM\CurrentControlSet\Services\ssudbus");
                    if (key != null)
                    {
                        return new DriverInfo
                        {
                            Name = "Samsung USB Driver",
                            Status = DriverStatus.Installed,
                            Manufacturer = "Samsung Electronics Co., Ltd.",
                            Description = "Samsung Mobile USB Composite Device"
                        };
                    }

                    return new DriverInfo
                    {
                        Name = "Samsung USB Driver",
                        Status = DriverStatus.NotInstalled,
                        Description = "Samsung USB driver not found. Install via Samsung Smart Switch."
                    };
                }
                catch (Exception ex)
                {
                    return new DriverInfo
                    {
                        Name = "Samsung USB Driver",
                        Status = DriverStatus.Error,
                        Description = ex.Message
                    };
                }
            }, ct);
        }

        /// <summary>
        /// Get all driver statuses for current system
        /// </summary>
        public async Task<Dictionary<string, DriverInfo>> GetAllDriverStatusesAsync(CancellationToken ct = default)
        {
            var results = new Dictionary<string, DriverInfo>();

            var qcTask = CheckQualcommDriverAsync(ct);
            var mtkTask = CheckMediaTekDriverAsync(ct);
            var samsungTask = CheckSamsungDriverAsync(ct);

            await Task.WhenAll(qcTask, mtkTask, samsungTask);

            results["Qualcomm"] = await qcTask;
            results["MediaTek"] = await mtkTask;
            results["Samsung"] = await samsungTask;

            return results;
        }

        /// <summary>
        /// Install driver from bundled package
        /// </summary>
        public async Task<bool> InstallDriverAsync(string driverName, IProgress<string>? progress = null, CancellationToken ct = default)
        {
            return await Task.Run(async () =>
            {
                try
                {
                    string driverPath = driverName.ToLower() switch
                    {
                        "qualcomm" => Path.Combine(DriversPath, "qualcomm", "qcser.inf"),
                        "mediatek" => Path.Combine(DriversPath, "mediatek", "mtk_usb.inf"),
                        "samsung" => Path.Combine(DriversPath, "samsung", "ssudbus.inf"),
                        _ => throw new ArgumentException($"Unknown driver: {driverName}")
                    };

                    if (!File.Exists(driverPath))
                    {
                        progress?.Report($"Driver package not found: {driverPath}");
                        Core.Logger.Error($"Driver INF not found: {driverPath}", "DRIVER");
                        return false;
                    }

                    progress?.Report($"Installing {driverName} driver...");

                    // Use pnputil to install driver
                    var psi = new ProcessStartInfo
                    {
                        FileName = "pnputil",
                        Arguments = $"/add-driver \"{driverPath}\" /install",
                        UseShellExecute = true,
                        Verb = "runas", // Request admin elevation
                        CreateNoWindow = true
                    };

                    using var process = Process.Start(psi);
                    if (process != null)
                    {
                        await process.WaitForExitAsync(ct);
                        
                        if (process.ExitCode == 0)
                        {
                            progress?.Report($"{driverName} driver installed successfully!");
                            Core.Logger.Success($"{driverName} driver installed", "DRIVER");
                            return true;
                        }
                        else
                        {
                            progress?.Report($"Driver installation failed with code: {process.ExitCode}");
                            Core.Logger.Error($"Driver install failed: exit code {process.ExitCode}", "DRIVER");
                            return false;
                        }
                    }

                    return false;
                }
                catch (Exception ex)
                {
                    progress?.Report($"Installation error: {ex.Message}");
                    Core.Logger.Error(ex, "Driver installation failed");
                    return false;
                }
            }, ct);
        }

        /// <summary>
        /// Open Windows Device Manager
        /// </summary>
        public void OpenDeviceManager()
        {
            try
            {
                Process.Start(new ProcessStartInfo
                {
                    FileName = "devmgmt.msc",
                    UseShellExecute = true
                });
            }
            catch (Exception ex)
            {
                Core.Logger.Error($"Failed to open Device Manager: {ex.Message}", "DRIVER");
            }
        }

        /// <summary>
        /// Get driver recommendation based on device
        /// </summary>
        public string GetDriverRecommendation(DeviceContext device)
        {
            var chipset = device.Chipset?.ToLower() ?? "";
            var brand = device.Brand?.ToLower() ?? "";

            if (chipset.Contains("qualcomm") || device.Mode == ConnectionMode.EDL)
            {
                return "Qualcomm QDLoader 9008 driver is required for EDL communication. " +
                       "Install via Qualcomm USB Driver package or use the bundled driver.";
            }

            if (chipset.Contains("mediatek") || device.Mode == ConnectionMode.BROM)
            {
                return "MediaTek USB VCOM driver is required for BROM/Preloader communication. " +
                       "Install via SP Flash Tool or MTK USB drivers package.";
            }

            if (brand.Contains("samsung"))
            {
                return "Samsung USB drivers are required for Download mode. " +
                       "Install via Samsung Smart Switch or Odin package.";
            }

            return "Ensure proper USB drivers are installed for your device chipset.";
        }

        #region Private Helpers

        private DriverInfo? CheckRegistryForDriver()
        {
            try
            {
                // Check USB device enumeration in registry
                using var usbKey = Registry.LocalMachine.OpenSubKey(@"SYSTEM\CurrentControlSet\Enum\USB");
                if (usbKey == null) return null;

                foreach (var deviceId in usbKey.GetSubKeyNames())
                {
                    // Check for Qualcomm VID (05C6)
                    if (deviceId.StartsWith("VID_05C6", StringComparison.OrdinalIgnoreCase))
                    {
                        using var deviceKey = usbKey.OpenSubKey(deviceId);
                        if (deviceKey == null) continue;

                        foreach (var instanceId in deviceKey.GetSubKeyNames())
                        {
                            using var instanceKey = deviceKey.OpenSubKey(instanceId);
                            var service = instanceKey?.GetValue("Service")?.ToString();
                            
                            if (!string.IsNullOrEmpty(service))
                            {
                                return new DriverInfo
                                {
                                    Name = "Qualcomm USB Driver",
                                    Status = DriverStatus.Installed,
                                    Manufacturer = "Qualcomm Inc.",
                                    Description = $"Service: {service}"
                                };
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Core.Logger.Debug($"Registry driver check error: {ex.Message}", "DRIVER");
            }

            return null;
        }

        private DriverInfo? CheckDriverFiles()
        {
            try
            {
                // Check Windows\System32\drivers for known Qualcomm drivers
                var driversDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System), "drivers");
                
                var qualcommDriverFiles = new[] { "QCUSB.sys", "qcser.sys", "qcusbser.sys", "QHSUSB.sys" };
                
                foreach (var driverFile in qualcommDriverFiles)
                {
                    var fullPath = Path.Combine(driversDir, driverFile);
                    if (File.Exists(fullPath))
                    {
                        var version = FileVersionInfo.GetVersionInfo(fullPath);
                        return new DriverInfo
                        {
                            Name = "Qualcomm USB Driver",
                            Status = DriverStatus.Installed,
                            Version = version.FileVersion ?? "Unknown",
                            Path = fullPath,
                            Manufacturer = version.CompanyName ?? "Qualcomm"
                        };
                    }
                }
            }
            catch (Exception ex)
            {
                Core.Logger.Debug($"Driver file check error: {ex.Message}", "DRIVER");
            }

            return null;
        }

        private DriverInfo? CheckPnpUtil()
        {
            try
            {
                var psi = new ProcessStartInfo
                {
                    FileName = "pnputil",
                    Arguments = "/enum-drivers",
                    RedirectStandardOutput = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };

                using var process = Process.Start(psi);
                if (process == null) return null;

                var output = process.StandardOutput.ReadToEnd();
                process.WaitForExit();

                // Look for Qualcomm driver entries
                if (output.Contains("Qualcomm", StringComparison.OrdinalIgnoreCase) ||
                    output.Contains("QDLoader", StringComparison.OrdinalIgnoreCase) ||
                    output.Contains("QHSUSB", StringComparison.OrdinalIgnoreCase))
                {
                    return new DriverInfo
                    {
                        Name = "Qualcomm USB Driver",
                        Status = DriverStatus.Installed,
                        Description = "Detected via PnP enumeration"
                    };
                }
            }
            catch (Exception ex)
            {
                Core.Logger.Debug($"PnPUtil check error: {ex.Message}", "DRIVER");
            }

            return null;
        }

        #endregion
    }
}
