using System;
using System.Diagnostics;
using System.Threading.Tasks;
using NLog;

namespace DeepEyeUnlocker.Protocols.Android
{
    public class AdbEngine
    {
        private static readonly Logger Logger = LogManager.GetCurrentClassLogger();
        private readonly string _adbPath = "adb.exe"; // Assumes adb is in path or Resources

        public async Task<string> ExecuteCommandAsync(string arguments)
        {
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo
                {
                    FileName = _adbPath,
                    Arguments = arguments,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };

                using var process = Process.Start(psi);
                if (process == null) return "Error: Could not start ADB.";

                string output = await process.StandardOutput.ReadToEndAsync();
                string error = await process.StandardError.ReadToEndAsync();
                await process.WaitForExitAsync();

                if (!string.IsNullOrEmpty(error))
                {
                    Logger.Warn($"ADB Error: {error}");
                    return error;
                }
                return output;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "ADB execution failed.");
                return $"Exception: {ex.Message}";
            }
        }

        public async Task<bool> RebootEdlAsync()
        {
            Logger.Info("Rebooting device to EDL mode via ADB...");
            string output = await ExecuteCommandAsync("reboot edl");
            return !output.Contains("error");
        }

        public async Task<string> GetDevicesAsync()
        {
            return await ExecuteCommandAsync("devices");
        }
    }
}
