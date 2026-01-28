using System;
using System.Diagnostics;
using System.IO;
using System.Threading.Tasks;
using NLog;

namespace DeepEyeUnlocker.Drivers
{
    public class DriverInstaller
    {
        private static readonly Logger Logger = LogManager.GetCurrentClassLogger();

        public static async Task<bool> InstallMtkDriversAsync()
        {
            Logger.Info("Installing MediaTek USB Drivers...");
            return await RunDriverProcessAsync("Drivers/mtk_usb_drivers.inf");
        }

        public static async Task<bool> InstallQualcommDriversAsync()
        {
            Logger.Info("Installing Qualcomm HS-USB QDLoader 9008 Drivers...");
            return await RunDriverProcessAsync("Drivers/qualcomm_drivers.inf");
        }

        public static async Task<bool> InstallSamsungDriversAsync()
        {
            Logger.Info("Installing Samsung Mobile USB Drivers...");
            return await RunDriverProcessAsync("Drivers/samsung_drivers.exe");
        }

        private static async Task<bool> RunDriverProcessAsync(string relativePath)
        {
            try
            {
                string fullPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, relativePath);
                if (!File.Exists(fullPath))
                {
                    Logger.Error($"Driver file not found: {fullPath}");
                    return false;
                }

                ProcessStartInfo psi = new ProcessStartInfo
                {
                    FileName = "pnputil.exe",
                    Arguments = $"/add-driver \"{fullPath}\" /install",
                    Verb = "runas", // Require admin
                    UseShellExecute = true
                };

                using var process = Process.Start(psi);
                if (process != null)
                {
                    await process.WaitForExitAsync();
                    Logger.Info($"Driver installation finished for: {Path.GetFileName(relativePath)}");
                    return true;
                }
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Driver installation failed.");
            }
            return false;
        }
    }
}
