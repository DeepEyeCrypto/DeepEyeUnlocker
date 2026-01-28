using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Operations
{
    /// <summary>
    /// Common ADB operations and utilities
    /// </summary>
    public class AdbToolsManager
    {
        #region Device Control

        public async Task<bool> RebootDevice(RebootMode mode, CancellationToken ct = default)
        {
            var arg = mode switch
            {
                RebootMode.System => "",
                RebootMode.Recovery => "recovery",
                RebootMode.Bootloader => "bootloader",
                RebootMode.Fastboot => "bootloader",
                RebootMode.Download => "download", // Samsung ODIN mode
                RebootMode.EDL => "edl",
                _ => ""
            };

            var result = await RunAdb($"reboot {arg}".Trim(), ct);
            return !result.Contains("error");
        }

        public async Task<bool> ShutdownDevice(CancellationToken ct = default)
        {
            await RunAdb("shell reboot -p", ct);
            return true;
        }

        #endregion

        #region App Management

        public async Task<List<PackageInfo>> ListPackages(
            PackageFilter filter = PackageFilter.All,
            CancellationToken ct = default)
        {
            var packages = new List<PackageInfo>();

            var arg = filter switch
            {
                PackageFilter.System => "-s",
                PackageFilter.ThirdParty => "-3",
                PackageFilter.Disabled => "-d",
                PackageFilter.Enabled => "-e",
                _ => ""
            };

            var output = await RunAdb($"shell pm list packages {arg} -f", ct);
            var lines = output.Split('\n', StringSplitOptions.RemoveEmptyEntries);

            foreach (var line in lines)
            {
                // Format: package:/path/to/app.apk=com.package.name
                if (line.StartsWith("package:"))
                {
                    var text = line["package:".Length..];
                    var eqIdx = text.LastIndexOf('=');
                    if (eqIdx > 0)
                    {
                        packages.Add(new PackageInfo
                        {
                            PackageName = text[(eqIdx + 1)..].Trim(),
                            ApkPath = text[..eqIdx].Trim()
                        });
                    }
                }
            }

            return packages;
        }

        public async Task<bool> UninstallPackage(string packageName, bool keepData = false, CancellationToken ct = default)
        {
            var arg = keepData ? "-k" : "";
            var result = await RunAdb($"uninstall {arg} {packageName}".Trim(), ct);
            return result.Contains("Success");
        }

        public async Task<bool> InstallApk(string apkPath, CancellationToken ct = default)
        {
            var result = await RunAdb($"install -r \"{apkPath}\"", ct);
            return result.Contains("Success");
        }

        public async Task<bool> ClearAppData(string packageName, CancellationToken ct = default)
        {
            var result = await RunAdb($"shell pm clear {packageName}", ct);
            return result.Contains("Success");
        }

        public async Task<bool> ForceStopApp(string packageName, CancellationToken ct = default)
        {
            await RunAdb($"shell am force-stop {packageName}", ct);
            return true;
        }

        public async Task<bool> DisableApp(string packageName, CancellationToken ct = default)
        {
            var result = await RunAdb($"shell pm disable-user --user 0 {packageName}", ct);
            return !result.Contains("Error");
        }

        public async Task<bool> EnableApp(string packageName, CancellationToken ct = default)
        {
            var result = await RunAdb($"shell pm enable {packageName}", ct);
            return !result.Contains("Error");
        }

        #endregion

        #region File Operations

        public async Task<bool> PushFile(string localPath, string remotePath, CancellationToken ct = default)
        {
            var result = await RunAdb($"push \"{localPath}\" \"{remotePath}\"", ct);
            return result.Contains("pushed") || result.Contains("1 file");
        }

        public async Task<bool> PullFile(string remotePath, string localPath, CancellationToken ct = default)
        {
            var result = await RunAdb($"pull \"{remotePath}\" \"{localPath}\"", ct);
            return result.Contains("pulled") || result.Contains("1 file");
        }

        public async Task<string> ListDirectory(string remotePath, CancellationToken ct = default)
        {
            return await RunAdb($"shell ls -la \"{remotePath}\"", ct);
        }

        public async Task<bool> DeleteFile(string remotePath, CancellationToken ct = default)
        {
            await RunAdb($"shell rm -f \"{remotePath}\"", ct);
            return true;
        }

        public async Task<bool> CreateDirectory(string remotePath, CancellationToken ct = default)
        {
            await RunAdb($"shell mkdir -p \"{remotePath}\"", ct);
            return true;
        }

        #endregion

        #region Screen & Input

        public async Task<byte[]?> CaptureScreenshot(CancellationToken ct = default)
        {
            var remotePath = "/sdcard/screenshot_temp.png";
            await RunAdb($"shell screencap -p {remotePath}", ct);
            
            var localPath = System.IO.Path.GetTempFileName() + ".png";
            await PullFile(remotePath, localPath, ct);
            await DeleteFile(remotePath, ct);

            if (System.IO.File.Exists(localPath))
            {
                var bytes = await System.IO.File.ReadAllBytesAsync(localPath, ct);
                System.IO.File.Delete(localPath);
                return bytes;
            }

            return null;
        }

        public async Task<bool> InputText(string text, CancellationToken ct = default)
        {
            // Escape special characters
            var escaped = text.Replace(" ", "%s").Replace("\"", "\\\"");
            await RunAdb($"shell input text \"{escaped}\"", ct);
            return true;
        }

        public async Task<bool> InputKeyEvent(int keyCode, CancellationToken ct = default)
        {
            await RunAdb($"shell input keyevent {keyCode}", ct);
            return true;
        }

        public async Task<bool> InputTap(int x, int y, CancellationToken ct = default)
        {
            await RunAdb($"shell input tap {x} {y}", ct);
            return true;
        }

        public async Task<bool> InputSwipe(int x1, int y1, int x2, int y2, int durationMs = 300, CancellationToken ct = default)
        {
            await RunAdb($"shell input swipe {x1} {y1} {x2} {y2} {durationMs}", ct);
            return true;
        }

        // Common key codes
        public static class KeyCodes
        {
            public const int Home = 3;
            public const int Back = 4;
            public const int Menu = 82;
            public const int Power = 26;
            public const int VolumeUp = 24;
            public const int VolumeDown = 25;
            public const int Enter = 66;
            public const int Tab = 61;
            public const int Escape = 111;
        }

        #endregion

        #region System Operations

        public async Task<bool> RemountSystem(CancellationToken ct = default)
        {
            await RunAdb("root", ct);
            await Task.Delay(1000, ct);
            var result = await RunAdb("remount", ct);
            return result.Contains("succeeded") || result.Contains("remount");
        }

        public async Task<bool> WaitForDevice(int timeoutSeconds = 30, CancellationToken ct = default)
        {
            using var cts = CancellationTokenSource.CreateLinkedTokenSource(ct);
            cts.CancelAfter(TimeSpan.FromSeconds(timeoutSeconds));

            try
            {
                await RunAdbAsync("wait-for-device", cts.Token);
                return true;
            }
            catch (OperationCanceledException)
            {
                return false;
            }
        }

        public async Task<string> GetDeviceState(CancellationToken ct = default)
        {
            return (await RunAdb("get-state", ct)).Trim();
        }

        public async Task<List<string>> GetConnectedDevices(CancellationToken ct = default)
        {
            var devices = new List<string>();
            var output = await RunAdb("devices", ct);
            var lines = output.Split('\n', StringSplitOptions.RemoveEmptyEntries);

            foreach (var line in lines)
            {
                if (line.Contains("\tdevice") || line.Contains("\trecovery") || line.Contains("\tbootloader"))
                {
                    var serial = line.Split('\t')[0];
                    devices.Add(serial);
                }
            }

            return devices;
        }

        #endregion

        #region Logcat

        public async Task<string> GetLogcat(int lines = 100, string? filter = null, CancellationToken ct = default)
        {
            var filterArg = string.IsNullOrEmpty(filter) ? "" : $"-s {filter}";
            return await RunAdb($"logcat -d -t {lines} {filterArg}".Trim(), ct);
        }

        public async Task ClearLogcat(CancellationToken ct = default)
        {
            await RunAdb("logcat -c", ct);
        }

        #endregion

        #region Helpers

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
            var error = await proc.StandardError.ReadToEndAsync(ct);
            await proc.WaitForExitAsync(ct);
            
            return string.IsNullOrEmpty(error) ? output : output + error;
        }

        private async Task RunAdbAsync(string args, CancellationToken ct)
        {
            var psi = new ProcessStartInfo("adb", args)
            {
                UseShellExecute = false,
                CreateNoWindow = true
            };

            using var proc = Process.Start(psi);
            if (proc != null)
            {
                await proc.WaitForExitAsync(ct);
            }
        }

        #endregion
    }

    #region Enums and DTOs

    public enum RebootMode
    {
        System,
        Recovery,
        Bootloader,
        Fastboot,
        Download,
        EDL
    }

    public enum PackageFilter
    {
        All,
        System,
        ThirdParty,
        Disabled,
        Enabled
    }

    public class PackageInfo
    {
        public string PackageName { get; set; } = "";
        public string ApkPath { get; set; } = "";
    }

    #endregion
}
