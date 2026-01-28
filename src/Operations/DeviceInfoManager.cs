using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Operations
{
    /// <summary>
    /// Comprehensive device information and diagnostics manager
    /// </summary>
    public class DeviceInfoManager
    {
        #region Device Info Collection

        public async Task<DeviceInfo> GetFullInfoAsync(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress = null,
            CancellationToken ct = default)
        {
            var info = new DeviceInfo
            {
                CollectedAt = DateTime.UtcNow,
                ConnectionMode = device.Mode
            };

            if (device.Mode != ConnectionMode.ADB)
            {
                info.Errors.Add($"Limited info available in {device.Mode} mode");
                await GetFastbootInfo(device, info, ct);
                return info;
            }

            progress?.Report(ProgressUpdate.Info(5, "Collecting device information..."));

            // Basic Info
            progress?.Report(ProgressUpdate.Info(10, "Reading device properties..."));
            await GetBasicInfo(info, ct);

            // Hardware Info
            progress?.Report(ProgressUpdate.Info(25, "Reading hardware info..."));
            await GetHardwareInfo(info, ct);

            // Storage Info
            progress?.Report(ProgressUpdate.Info(40, "Reading storage info..."));
            await GetStorageInfo(info, ct);

            // Battery Info
            progress?.Report(ProgressUpdate.Info(55, "Reading battery status..."));
            await GetBatteryInfo(info, ct);

            // Network Info
            progress?.Report(ProgressUpdate.Info(70, "Reading network info..."));
            await GetNetworkInfo(info, ct);

            // Security Info
            progress?.Report(ProgressUpdate.Info(85, "Reading security status..."));
            await GetSecurityInfo(info, ct);

            progress?.Report(ProgressUpdate.Info(100, "Device info collection complete"));
            return info;
        }

        private async Task GetBasicInfo(DeviceInfo info, CancellationToken ct)
        {
            info.Brand = await GetProp("ro.product.brand", ct);
            info.Model = await GetProp("ro.product.model", ct);
            info.Device = await GetProp("ro.product.device", ct);
            info.Manufacturer = await GetProp("ro.product.manufacturer", ct);
            info.ProductName = await GetProp("ro.product.name", ct);
            
            info.AndroidVersion = await GetProp("ro.build.version.release", ct);
            info.SdkVersion = await GetProp("ro.build.version.sdk", ct);
            info.BuildId = await GetProp("ro.build.id", ct);
            info.BuildFingerprint = await GetProp("ro.build.fingerprint", ct);
            info.BuildDate = await GetProp("ro.build.date", ct);
            info.SecurityPatch = await GetProp("ro.build.version.security_patch", ct);
            
            info.Serial = await RunAdb("shell getprop ro.serialno", ct);
            if (string.IsNullOrEmpty(info.Serial))
            {
                info.Serial = await RunAdb("get-serialno", ct);
            }
        }

        private async Task GetHardwareInfo(DeviceInfo info, CancellationToken ct)
        {
            info.Hardware = new HardwareInfo
            {
                Board = await GetProp("ro.product.board", ct),
                Platform = await GetProp("ro.board.platform", ct),
                Chipset = await GetProp("ro.hardware", ct),
                CpuAbi = await GetProp("ro.product.cpu.abi", ct),
                CpuAbiList = await GetProp("ro.product.cpu.abilist", ct)
            };

            // CPU info
            var cpuInfo = await RunAdb("shell cat /proc/cpuinfo | head -30", ct);
            info.Hardware.CpuInfo = cpuInfo.Trim();

            // Get CPU cores and frequency
            var coreCount = await RunAdb("shell cat /sys/devices/system/cpu/present", ct);
            info.Hardware.CpuCores = coreCount.Trim();

            // Memory info
            var memInfo = await RunAdb("shell cat /proc/meminfo | head -5", ct);
            var lines = memInfo.Split('\n');
            foreach (var line in lines)
            {
                if (line.StartsWith("MemTotal:"))
                {
                    var parts = line.Split(new[] { ' ' }, StringSplitOptions.RemoveEmptyEntries);
                    if (parts.Length >= 2 && long.TryParse(parts[1], out var kb))
                    {
                        info.Hardware.TotalRamMB = kb / 1024;
                    }
                }
                else if (line.StartsWith("MemAvailable:"))
                {
                    var parts = line.Split(new[] { ' ' }, StringSplitOptions.RemoveEmptyEntries);
                    if (parts.Length >= 2 && long.TryParse(parts[1], out var kb))
                    {
                        info.Hardware.AvailableRamMB = kb / 1024;
                    }
                }
            }

            // Screen resolution
            var wmSize = await RunAdb("shell wm size", ct);
            if (wmSize.Contains("Physical size:"))
            {
                var size = wmSize.Split(':').LastOrDefault()?.Trim();
                info.Hardware.ScreenResolution = size;
            }

            var wmDensity = await RunAdb("shell wm density", ct);
            if (wmDensity.Contains("Physical density:"))
            {
                var density = wmDensity.Split(':').LastOrDefault()?.Trim();
                info.Hardware.ScreenDensity = density;
            }
        }

        private async Task GetStorageInfo(DeviceInfo info, CancellationToken ct)
        {
            info.Storage = new StorageInfo();

            // Internal storage
            var dfOutput = await RunAdb("shell df /data", ct);
            var dfLines = dfOutput.Split('\n');
            foreach (var line in dfLines)
            {
                if (line.Contains("/data"))
                {
                    var parts = line.Split(new[] { ' ' }, StringSplitOptions.RemoveEmptyEntries);
                    if (parts.Length >= 4)
                    {
                        if (long.TryParse(parts[1], out var total))
                            info.Storage.InternalTotalKB = total;
                        if (long.TryParse(parts[2], out var used))
                            info.Storage.InternalUsedKB = used;
                        if (long.TryParse(parts[3], out var avail))
                            info.Storage.InternalAvailableKB = avail;
                    }
                }
            }

            // External storage check
            var externalPath = await RunAdb("shell echo $EXTERNAL_STORAGE", ct);
            if (!string.IsNullOrEmpty(externalPath.Trim()))
            {
                var extDf = await RunAdb($"shell df {externalPath.Trim()}", ct);
                // Parse similar to above if needed
            }

            // Partition list
            var partitions = await RunAdb("shell ls -la /dev/block/by-name/ 2>/dev/null | head -50", ct);
            info.Storage.PartitionList = partitions.Trim();
        }

        private async Task GetBatteryInfo(DeviceInfo info, CancellationToken ct)
        {
            info.Battery = new BatteryInfo();

            var batteryDump = await RunAdb("shell dumpsys battery", ct);
            var lines = batteryDump.Split('\n');

            foreach (var line in lines)
            {
                var trimmed = line.Trim();
                if (trimmed.StartsWith("level:"))
                    int.TryParse(trimmed.Split(':').LastOrDefault()?.Trim(), out info.Battery.Level);
                else if (trimmed.StartsWith("status:"))
                    info.Battery.Status = trimmed.Split(':').LastOrDefault()?.Trim() ?? "";
                else if (trimmed.StartsWith("health:"))
                    info.Battery.Health = trimmed.Split(':').LastOrDefault()?.Trim() ?? "";
                else if (trimmed.StartsWith("temperature:"))
                {
                    if (int.TryParse(trimmed.Split(':').LastOrDefault()?.Trim(), out var temp))
                        info.Battery.Temperature = temp / 10.0; // Usually in tenths of degree
                }
                else if (trimmed.StartsWith("voltage:"))
                    int.TryParse(trimmed.Split(':').LastOrDefault()?.Trim(), out info.Battery.Voltage);
                else if (trimmed.StartsWith("technology:"))
                    info.Battery.Technology = trimmed.Split(':').LastOrDefault()?.Trim() ?? "";
            }
        }

        private async Task GetNetworkInfo(DeviceInfo info, CancellationToken ct)
        {
            info.Network = new NetworkInfo();

            // WiFi info
            var wifiInfo = await RunAdb("shell dumpsys wifi | grep 'mWifiInfo'", ct);
            if (wifiInfo.Contains("SSID:"))
            {
                var ssidMatch = System.Text.RegularExpressions.Regex.Match(wifiInfo, @"SSID: ([^,]+)");
                if (ssidMatch.Success)
                    info.Network.WifiSsid = ssidMatch.Groups[1].Value.Trim();
            }

            // IP address
            var ipAddr = await RunAdb("shell ip addr show wlan0 2>/dev/null | grep 'inet '", ct);
            if (!string.IsNullOrEmpty(ipAddr))
            {
                var match = System.Text.RegularExpressions.Regex.Match(ipAddr, @"inet (\d+\.\d+\.\d+\.\d+)");
                if (match.Success)
                    info.Network.WifiIp = match.Groups[1].Value;
            }

            // IMEI (requires permissions, may fail)
            try
            {
                var imei = await RunAdb("shell service call iphonesubinfo 1 2>/dev/null | grep -o '[0-9.]'", ct);
                // IMEI parsing is complex, just note if accessible
                info.Network.ImeiAccessible = !string.IsNullOrEmpty(imei);
            }
            catch { info.Network.ImeiAccessible = false; }

            // SIM info
            var simInfo = await RunAdb("shell getprop gsm.sim.state", ct);
            info.Network.SimState = simInfo.Trim();

            var operator_ = await RunAdb("shell getprop gsm.operator.alpha", ct);
            info.Network.Operator = operator_.Trim();
        }

        private async Task GetSecurityInfo(DeviceInfo info, CancellationToken ct)
        {
            info.Security = new SecurityInfo();

            // Bootloader state
            var bootloaderState = await GetProp("ro.boot.flash.locked", ct);
            info.Security.BootloaderLocked = bootloaderState == "1";

            var verifiedBoot = await GetProp("ro.boot.verifiedbootstate", ct);
            info.Security.VerifiedBootState = verifiedBoot;

            // SELinux
            var selinux = await RunAdb("shell getenforce 2>/dev/null", ct);
            info.Security.SelinuxMode = selinux.Trim();

            // Encryption
            var cryptState = await GetProp("ro.crypto.state", ct);
            info.Security.EncryptionState = cryptState;

            // dm-verity
            var verity = await GetProp("ro.boot.veritymode", ct);
            info.Security.VerityMode = verity;

            // Check for common root indicators (non-invasive)
            var suWhich = await RunAdb("shell which su 2>/dev/null", ct);
            info.Security.SuBinaryFound = !string.IsNullOrEmpty(suWhich.Trim());

            var magiskProps = await GetProp("ro.build.flavor", ct);
            // Just collecting props, not determining root status
        }

        private async Task GetFastbootInfo(DeviceContext device, DeviceInfo info, CancellationToken ct)
        {
            if (device.Mode != ConnectionMode.Fastboot) return;

            try
            {
                var vars = await RunFastboot("getvar all 2>&1", ct);
                info.FastbootVars = vars;

                // Parse some common vars
                var lines = vars.Split('\n');
                foreach (var line in lines)
                {
                    if (line.StartsWith("(bootloader) ") || line.StartsWith("INFO"))
                    {
                        var trimmed = line.Replace("(bootloader) ", "").Replace("INFO", "").Trim();
                        if (trimmed.StartsWith("product:"))
                            info.Model = trimmed.Split(':').LastOrDefault()?.Trim();
                        else if (trimmed.StartsWith("serialno:"))
                            info.Serial = trimmed.Split(':').LastOrDefault()?.Trim();
                        else if (trimmed.StartsWith("unlocked:"))
                            info.Security = new SecurityInfo { BootloaderLocked = trimmed.Contains("no") };
                    }
                }
            }
            catch { }
        }

        #endregion

        #region IMEI Tools

        public async Task<ImeiInfo> GetImeiInfoAsync(CancellationToken ct = default)
        {
            var imeiInfo = new ImeiInfo();

            try
            {
                // Try multiple methods to get IMEI
                // Method 1: Service call (requires phone permission)
                var imei1 = await RunAdb("shell service call iphonesubinfo 1", ct);
                if (!string.IsNullOrEmpty(imei1) && !imei1.Contains("Security exception"))
                {
                    imeiInfo.Slot1 = ParseServiceCallImei(imei1);
                }

                // Method 2: Dump telephony (works on some devices)
                var telephony = await RunAdb("shell dumpsys telephony.registry | grep mDeviceId", ct);
                if (!string.IsNullOrEmpty(telephony))
                {
                    var match = System.Text.RegularExpressions.Regex.Match(telephony, @"mDeviceId=(\d+)");
                    if (match.Success && string.IsNullOrEmpty(imeiInfo.Slot1))
                    {
                        imeiInfo.Slot1 = match.Groups[1].Value;
                    }
                }

                // Get MEID (for CDMA)
                var meid = await GetProp("ro.ril.oem.meid", ct);
                imeiInfo.Meid = meid;

                imeiInfo.Accessible = !string.IsNullOrEmpty(imeiInfo.Slot1);
            }
            catch
            {
                imeiInfo.Accessible = false;
                imeiInfo.Error = "Failed to retrieve IMEI - may require special permissions";
            }

            return imeiInfo;
        }

        private string? ParseServiceCallImei(string output)
        {
            // Service call returns hex-encoded string
            var sb = new StringBuilder();
            var matches = System.Text.RegularExpressions.Regex.Matches(output, @"'([^']+)'");
            foreach (System.Text.RegularExpressions.Match match in matches)
            {
                var hex = match.Groups[1].Value;
                sb.Append(hex.Replace(".", ""));
            }
            var result = sb.ToString();
            return result.Length >= 15 ? result[..15] : null;
        }

        #endregion

        #region Export

        public string GenerateReport(DeviceInfo info)
        {
            var sb = new StringBuilder();
            sb.AppendLine("═══════════════════════════════════════════════════════════════");
            sb.AppendLine("                    DEVICE INFORMATION REPORT                   ");
            sb.AppendLine($"                    Generated: {info.CollectedAt:yyyy-MM-dd HH:mm:ss}");
            sb.AppendLine("═══════════════════════════════════════════════════════════════");
            sb.AppendLine();

            // Basic Info
            sb.AppendLine("── DEVICE ──────────────────────────────────────────────────────");
            sb.AppendLine($"  Brand:          {info.Brand}");
            sb.AppendLine($"  Model:          {info.Model}");
            sb.AppendLine($"  Device:         {info.Device}");
            sb.AppendLine($"  Manufacturer:   {info.Manufacturer}");
            sb.AppendLine($"  Serial:         {info.Serial}");
            sb.AppendLine();

            // Software
            sb.AppendLine("── SOFTWARE ────────────────────────────────────────────────────");
            sb.AppendLine($"  Android:        {info.AndroidVersion} (SDK {info.SdkVersion})");
            sb.AppendLine($"  Build ID:       {info.BuildId}");
            sb.AppendLine($"  Security Patch: {info.SecurityPatch}");
            sb.AppendLine($"  Build Date:     {info.BuildDate}");
            sb.AppendLine();

            // Hardware
            if (info.Hardware != null)
            {
                sb.AppendLine("── HARDWARE ────────────────────────────────────────────────────");
                sb.AppendLine($"  Platform:       {info.Hardware.Platform}");
                sb.AppendLine($"  Chipset:        {info.Hardware.Chipset}");
                sb.AppendLine($"  Board:          {info.Hardware.Board}");
                sb.AppendLine($"  CPU ABI:        {info.Hardware.CpuAbi}");
                sb.AppendLine($"  CPU Cores:      {info.Hardware.CpuCores}");
                sb.AppendLine($"  RAM:            {info.Hardware.TotalRamMB} MB ({info.Hardware.AvailableRamMB} MB available)");
                sb.AppendLine($"  Screen:         {info.Hardware.ScreenResolution} @ {info.Hardware.ScreenDensity} dpi");
                sb.AppendLine();
            }

            // Storage
            if (info.Storage != null)
            {
                var totalGB = info.Storage.InternalTotalKB / 1024.0 / 1024.0;
                var usedGB = info.Storage.InternalUsedKB / 1024.0 / 1024.0;
                var availGB = info.Storage.InternalAvailableKB / 1024.0 / 1024.0;
                var usedPct = totalGB > 0 ? (usedGB / totalGB * 100) : 0;

                sb.AppendLine("── STORAGE ─────────────────────────────────────────────────────");
                sb.AppendLine($"  Total:          {totalGB:F1} GB");
                sb.AppendLine($"  Used:           {usedGB:F1} GB ({usedPct:F0}%)");
                sb.AppendLine($"  Available:      {availGB:F1} GB");
                sb.AppendLine();
            }

            // Battery
            if (info.Battery != null)
            {
                sb.AppendLine("── BATTERY ─────────────────────────────────────────────────────");
                sb.AppendLine($"  Level:          {info.Battery.Level}%");
                sb.AppendLine($"  Status:         {info.Battery.Status}");
                sb.AppendLine($"  Health:         {info.Battery.Health}");
                sb.AppendLine($"  Temperature:    {info.Battery.Temperature:F1}°C");
                sb.AppendLine($"  Voltage:        {info.Battery.Voltage} mV");
                sb.AppendLine($"  Technology:     {info.Battery.Technology}");
                sb.AppendLine();
            }

            // Security
            if (info.Security != null)
            {
                sb.AppendLine("── SECURITY ────────────────────────────────────────────────────");
                sb.AppendLine($"  Bootloader:     {(info.Security.BootloaderLocked ? "LOCKED" : "UNLOCKED")}");
                sb.AppendLine($"  Verified Boot:  {info.Security.VerifiedBootState}");
                sb.AppendLine($"  SELinux:        {info.Security.SelinuxMode}");
                sb.AppendLine($"  Encryption:     {info.Security.EncryptionState}");
                sb.AppendLine($"  dm-verity:      {info.Security.VerityMode}");
                sb.AppendLine($"  SU Binary:      {(info.Security.SuBinaryFound ? "FOUND" : "Not found")}");
                sb.AppendLine();
            }

            // Network
            if (info.Network != null)
            {
                sb.AppendLine("── NETWORK ─────────────────────────────────────────────────────");
                sb.AppendLine($"  WiFi SSID:      {info.Network.WifiSsid}");
                sb.AppendLine($"  WiFi IP:        {info.Network.WifiIp}");
                sb.AppendLine($"  SIM State:      {info.Network.SimState}");
                sb.AppendLine($"  Operator:       {info.Network.Operator}");
                sb.AppendLine();
            }

            sb.AppendLine("═══════════════════════════════════════════════════════════════");

            return sb.ToString();
        }

        public async Task<string> ExportJsonAsync(DeviceInfo info, string path, CancellationToken ct = default)
        {
            var json = JsonSerializer.Serialize(info, new JsonSerializerOptions { WriteIndented = true });
            await System.IO.File.WriteAllTextAsync(path, json, ct);
            return path;
        }

        #endregion

        #region Helpers

        private async Task<string> GetProp(string prop, CancellationToken ct)
        {
            return (await RunAdb($"shell getprop {prop}", ct)).Trim();
        }

        private async Task<string> RunAdb(string args, CancellationToken ct)
        {
            var psi = new ProcessStartInfo("adb", args)
            {
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };

            using var proc = Process.Start(psi);
            if (proc == null) return "";

            var output = await proc.StandardOutput.ReadToEndAsync(ct);
            await proc.WaitForExitAsync(ct);
            return output;
        }

        private async Task<string> RunFastboot(string args, CancellationToken ct)
        {
            var psi = new ProcessStartInfo("fastboot", args)
            {
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };

            using var proc = Process.Start(psi);
            if (proc == null) return "";

            var stdout = await proc.StandardOutput.ReadToEndAsync(ct);
            var stderr = await proc.StandardError.ReadToEndAsync(ct);
            await proc.WaitForExitAsync(ct);
            return stdout + stderr;
        }

        #endregion
    }

    #region DTOs

    public class DeviceInfo
    {
        public DateTime CollectedAt { get; set; }
        public ConnectionMode ConnectionMode { get; set; }
        public List<string> Errors { get; set; } = new();

        // Basic
        public string? Brand { get; set; }
        public string? Model { get; set; }
        public string? Device { get; set; }
        public string? Manufacturer { get; set; }
        public string? ProductName { get; set; }
        public string? Serial { get; set; }

        // Software
        public string? AndroidVersion { get; set; }
        public string? SdkVersion { get; set; }
        public string? BuildId { get; set; }
        public string? BuildFingerprint { get; set; }
        public string? BuildDate { get; set; }
        public string? SecurityPatch { get; set; }

        // Sub-sections
        public HardwareInfo? Hardware { get; set; }
        public StorageInfo? Storage { get; set; }
        public BatteryInfo? Battery { get; set; }
        public NetworkInfo? Network { get; set; }
        public SecurityInfo? Security { get; set; }

        // Fastboot mode
        public string? FastbootVars { get; set; }
    }

    public class HardwareInfo
    {
        public string? Board { get; set; }
        public string? Platform { get; set; }
        public string? Chipset { get; set; }
        public string? CpuAbi { get; set; }
        public string? CpuAbiList { get; set; }
        public string? CpuInfo { get; set; }
        public string? CpuCores { get; set; }
        public long TotalRamMB { get; set; }
        public long AvailableRamMB { get; set; }
        public string? ScreenResolution { get; set; }
        public string? ScreenDensity { get; set; }
    }

    public class StorageInfo
    {
        public long InternalTotalKB { get; set; }
        public long InternalUsedKB { get; set; }
        public long InternalAvailableKB { get; set; }
        public string? PartitionList { get; set; }
    }

    public class BatteryInfo
    {
        public int Level { get; set; }
        public string? Status { get; set; }
        public string? Health { get; set; }
        public double Temperature { get; set; }
        public int Voltage { get; set; }
        public string? Technology { get; set; }
    }

    public class NetworkInfo
    {
        public string? WifiSsid { get; set; }
        public string? WifiIp { get; set; }
        public bool ImeiAccessible { get; set; }
        public string? SimState { get; set; }
        public string? Operator { get; set; }
    }

    public class SecurityInfo
    {
        public bool BootloaderLocked { get; set; }
        public string? VerifiedBootState { get; set; }
        public string? SelinuxMode { get; set; }
        public string? EncryptionState { get; set; }
        public string? VerityMode { get; set; }
        public bool SuBinaryFound { get; set; }
    }

    public class ImeiInfo
    {
        public bool Accessible { get; set; }
        public string? Slot1 { get; set; }
        public string? Slot2 { get; set; }
        public string? Meid { get; set; }
        public string? Error { get; set; }
    }

    #endregion
}
