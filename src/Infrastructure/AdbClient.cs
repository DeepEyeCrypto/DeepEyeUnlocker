using System;
using System.Diagnostics;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Infrastructure
{
    public class AdbClient : IAdbClient
    {
        private readonly string _adbPath;

        public string? TargetSerial { get; set; }

        public AdbClient(string adbPath = "adb")
        {
            _adbPath = adbPath;
        }

        public async Task<string> ExecuteShellAsync(string command)
        {
            return await RunCommandAsync($"shell \"{command}\"");
        }

        public async Task<bool> PushFileAsync(string localPath, string remotePath)
        {
            var result = await RunCommandAsync($"push \"{localPath}\" \"{remotePath}\"");
            return !result.ToLower().Contains("failed") && !result.ToLower().Contains("error");
        }

        public async Task<bool> PullFileAsync(string remotePath, string localPath)
        {
            var result = await RunCommandAsync($"pull \"{remotePath}\" \"{localPath}\"");
            return !result.ToLower().Contains("failed") && !result.ToLower().Contains("error");
        }

        public async Task<bool> InstallPackageAsync(string apkPath)
        {
            var result = await RunCommandAsync($"install -r \"{apkPath}\"");
            return result.ToLower().Contains("success");
        }

        public async Task RebootAsync()
        {
            await RunCommandAsync("reboot");
        }

        public bool IsConnected()
        {
            var result = RunCommandAsync("get-state").GetAwaiter().GetResult();
            return result.Trim().ToLower() == "device";
        }

        private async Task<string> RunCommandAsync(string args)
        {
            string finalArgs = string.IsNullOrEmpty(TargetSerial) ? args : $"-s {TargetSerial} {args}";
            try
            {
                var psi = new ProcessStartInfo
                {
                    FileName = _adbPath,
                    Arguments = finalArgs,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };

                using var process = new Process { StartInfo = psi };
                process.Start();

                string output = await process.StandardOutput.ReadToEndAsync();
                string error = await process.StandardError.ReadToEndAsync();
                await process.WaitForExitAsync();

                if (!string.IsNullOrEmpty(error) && !output.Contains(error))
                {
                    Logger.Debug($"ADB Error: {error}");
                    return error;
                }

                return output;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, $"Failed to run ADB command: {args}");
                return string.Empty;
            }
        }
    }
}
