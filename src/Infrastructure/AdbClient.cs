using System;
using System.Diagnostics;
using System.Threading;
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

        public async Task<string> ExecuteShellAsync(string command, CancellationToken ct = default)
        {
            return await RunAdbCommandAsync($"shell \"{command}\"", ct);
        }

        public Task<System.IO.Stream> OpenShellStreamAsync(string command, CancellationToken ct = default)
        {
            string args = string.IsNullOrEmpty(TargetSerial) ? $"shell \"{command}\"" : $"-s {TargetSerial} shell \"{command}\"";
            
            var psi = new ProcessStartInfo
            {
                FileName = _adbPath,
                Arguments = args,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };

            var process = new Process { StartInfo = psi };
            process.Start();

            // Note: Caller is responsible for disposing the stream and ensuring process exit
            return Task.FromResult(process.StandardOutput.BaseStream);
        }

        public Task<System.IO.Stream> OpenShellWritableStreamAsync(string command, CancellationToken ct = default)
        {
            string args = string.IsNullOrEmpty(TargetSerial) ? $"shell \"{command}\"" : $"-s {TargetSerial} shell \"{command}\"";
            
            var psi = new ProcessStartInfo
            {
                FileName = _adbPath,
                Arguments = args,
                RedirectStandardInput = true,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };

            var process = new Process { StartInfo = psi };
            process.Start();

            return Task.FromResult(process.StandardInput.BaseStream);
        }

        public async Task<bool> PushFileAsync(string localPath, string remotePath, CancellationToken ct = default)
        {
            var result = await RunAdbCommandAsync($"push \"{localPath}\" \"{remotePath}\"", ct);
            return !result.ToLower().Contains("failed") && !result.ToLower().Contains("error");
        }

        public async Task<bool> PullFileAsync(string remotePath, string localPath, CancellationToken ct = default)
        {
            var result = await RunAdbCommandAsync($"pull \"{remotePath}\" \"{localPath}\"", ct);
            return !result.ToLower().Contains("failed") && !result.ToLower().Contains("error");
        }

        public async Task<bool> InstallPackageAsync(string apkPath, CancellationToken ct = default)
        {
            var result = await RunAdbCommandAsync($"install -r \"{apkPath}\"", ct);
            return result.ToLower().Contains("success");
        }

        public async Task RebootAsync(CancellationToken ct = default)
        {
            await RunAdbCommandAsync("reboot", ct);
        }

        public async Task<bool> HasRootAsync(CancellationToken ct = default)
        {
            var result = await ExecuteShellAsync("id", ct);
            return result.Contains("uid=0(root)");
        }

        public bool IsConnected()
        {
            var result = RunAdbCommandAsync("get-state").GetAwaiter().GetResult();
            return result.Trim().ToLower() == "device";
        }

        public async Task<string> RunAdbCommandAsync(string args, CancellationToken ct = default)
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

                string output = await process.StandardOutput.ReadToEndAsync(ct);
                string error = await process.StandardError.ReadToEndAsync(ct);
                await process.WaitForExitAsync(ct);

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
