using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.Features.DeviceHealth
{
    /// <summary>
    /// Core scanner for gathering device health and security data via ADB.
    /// </summary>
    public class DeviceHealthScanner
    {
        private readonly IAdbClient _adb;

        public DeviceHealthScanner(IAdbClient adb)
        {
            _adb = adb ?? throw new ArgumentNullException(nameof(adb));
        }

        public async Task<DeviceHealthReport> ScanAsync(CancellationToken ct = default)
        {
            Logger.Info("Starting deep device health scan...");
            var report = new DeviceHealthReport();

            // 1. Identification & OS
            report.SerialNumber = await GetPropAsync("ro.serialno");
            report.AndroidVersion = await GetPropAsync("ro.build.version.release");
            report.SecurityPatchLevel = await GetPropAsync("ro.build.version.security_patch");
            report.BuildNumber = await GetPropAsync("ro.build.display.id");
            report.KernelVersion = await GetKernelVersionAsync();
            report.BasebandVersion = await GetPropAsync("gsm.version.baseband");
            
            // 2. Hardware Status (Battery)
            await PopulateBatteryInfoAsync(report);
            await PopulateStorageInfoAsync(report);

            // 3. IMEIs & Connectvity
            report.Imei1 = await GetImeiAsync(0);
            report.Imei2 = await GetImeiAsync(1);
            report.MacAddress = await GetPropAsync("ro.boot.mac") ?? await GetPropAsync("wlan.driver.macaddr");
            report.BluetoothAddress = await GetPropAsync("ro.boot.btmacaddr");

            // 4. Security & Bootloader
            report.IsBootloaderUnlocked = (await GetPropAsync("ro.boot.flash.locked")) == "0";
            report.IsOemUnlockEnabled = (await GetPropAsync("ro.oem_unlock_supported")) == "1";
            report.IsDevOptionsEnabled = (await GetPropAsync("persist.sys.usb.config")).Contains("adb");
            report.IsSelinuxEnforcing = (await _adb.ExecuteShellAsync("getenforce")).Contains("Enforcing");
            
            // 5. Root Detection
            await DetectRootStatusAsync(report);

            Logger.Info($"Scan complete for {report.SerialNumber}. Root: {report.IsRooted}");
            return report;
        }

        private async Task<string> GetPropAsync(string prop)
        {
            var result = await _adb.ExecuteShellAsync($"getprop {prop}");
            return result?.Trim() ?? "";
        }

        private async Task<string> GetKernelVersionAsync()
        {
            var result = await _adb.ExecuteShellAsync("uname -a");
            return result?.Trim() ?? "Unknown";
        }

        private async Task PopulateBatteryInfoAsync(DeviceHealthReport report)
        {
            var dumpsys = await _adb.ExecuteShellAsync("dumpsys battery");
            if (string.IsNullOrEmpty(dumpsys)) return;

            report.BatteryLevel = ParseIntMatch(dumpsys, @"level: (\d+)", 0);
            report.BatteryTemperature = ParseIntMatch(dumpsys, @"temperature: (\d+)", 0) / 10.0;
            report.BatteryStatus = ParseStringMatch(dumpsys, @"status: (\d+)") switch
            {
                "2" => "Charging",
                "3" => "Discharging",
                "4" => "Not charging",
                "5" => "Full",
                _ => "Unknown"
            };
            
            // Heuristic for health if not directly exposed
            report.BatteryHealth = ParseIntMatch(dumpsys, @"health: (\d+)", 0) switch
            {
                2 => 100, // Good
                3 => 70,  // Overheat
                4 => 30,  // Dead
                _ => 50
            };
        }

        private async Task PopulateStorageInfoAsync(DeviceHealthReport report)
        {
            var df = await _adb.ExecuteShellAsync("df /data");
            if (string.IsNullOrEmpty(df)) return;

            // Simple parsing of df output
            var matches = Regex.Matches(df, @"(\d+)");
            if (matches.Count >= 3)
            {
                // df output usually: size, used, free
                if (long.TryParse(matches[0].Value, out long total)) report.StorageTotalBytes = total * 1024;
                if (long.TryParse(matches[2].Value, out long free)) report.StorageFreeBytes = free * 1024;
            }
        }

        private async Task<string> GetImeiAsync(int slot)
        {
            // Note: This often requires root or service-mode access on modern Android
            // We bait with service queries, fallback to empty
            var result = await _adb.ExecuteShellAsync($"service call iphonesubinfo {slot + 1}");
            if (string.IsNullOrEmpty(result) || result.Contains("Permission denied"))
            {
                return ""; // Expected failure on non-root in standard ADB
            }
            return ""; // Placeholder for hex-to-string IMEI parsing if enabled
        }

        private async Task DetectRootStatusAsync(DeviceHealthReport report)
        {
            var whichSu = await _adb.ExecuteShellAsync("which su");
            report.IsRooted = !string.IsNullOrEmpty(whichSu) && whichSu.Contains("/su");
            
            if (report.IsRooted)
            {
                var magisk = await _adb.ExecuteShellAsync("magisk -v");
                if (!string.IsNullOrEmpty(magisk)) report.RootMethod = "Magisk (" + magisk.Trim() + ")";
                else report.RootMethod = "Unknown / Legacy";
            }
        }

        private int ParseIntMatch(string input, string pattern, int defaultValue)
        {
            var match = Regex.Match(input, pattern);
            return match.Success && int.TryParse(match.Groups[1].Value, out int val) ? val : defaultValue;
        }

        private string ParseStringMatch(string input, string pattern)
        {
            var match = Regex.Match(input, pattern);
            return match.Success ? match.Groups[1].Value : "";
        }
    }
}
