using System;
using System.Diagnostics;
using System.Threading.Tasks;
using NLog;

namespace DeepEyeUnlocker.Protocols.Android
{
    public class FastbootEngine
    {
        private static readonly Logger Logger = LogManager.GetCurrentClassLogger();
        private readonly string _fastbootPath = "fastboot.exe";

        public async Task<string> ExecuteCommandAsync(string arguments)
        {
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo
                {
                    FileName = _fastbootPath,
                    Arguments = arguments,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };

                using var process = Process.Start(psi);
                if (process == null) return "Error: Could not start Fastboot.";

                string output = await process.StandardOutput.ReadToEndAsync();
                string error = await process.StandardError.ReadToEndAsync();
                await process.WaitForExitAsync();

                return string.IsNullOrEmpty(error) ? output : error;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Fastboot execution failed.");
                return $"Exception: {ex.Message}";
            }
        }

        public async Task<bool> UnlockBootloaderAsync()
        {
            Logger.Info("Attempting bootloader unlock via Fastboot...");
            string output = await ExecuteCommandAsync("flashing unlock");
            return output.Contains("OKAY");
        }
    }
}
