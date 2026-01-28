using System;
using System.Diagnostics;
using System.Threading.Tasks;
using DeepEyeUnlocker.Drivers.Models;

namespace DeepEyeUnlocker.Drivers.Installer
{
    public interface IUsbDriverInstaller
    {
        Task<DriverInstallResult> InstallAsync(DriverProfile profile);
        Task<bool> UninstallAsync(string oemInfName);
    }

    public class PnputilInstaller : IUsbDriverInstaller
    {
        public async Task<DriverInstallResult> InstallAsync(DriverProfile profile)
        {
            var result = new DriverInstallResult();
            try
            {
                var psi = new ProcessStartInfo
                {
                    FileName = "pnputil.exe",
                    Arguments = $"/add-driver \"{profile.InfPath}\" /install",
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true,
                    Verb = "runas" // Request elevation
                };

                using var process = new Process { StartInfo = psi };
                process.Start();

                string output = await process.StandardOutput.ReadToEndAsync();
                string error = await process.StandardError.ReadToEndAsync();
                await process.WaitForExitAsync();

                result.Log = output + "\n" + error;
                result.Success = process.ExitCode == 0 || output.Contains("successfully");
                
                if (!result.Success)
                {
                    result.ErrorMessage = string.IsNullOrEmpty(error) ? "Process exited with code " + process.ExitCode : error;
                }
                
                result.RebootRequired = output.Contains("reboot") || output.Contains("restart");
            }
            catch (Exception ex)
            {
                result.Success = false;
                result.ErrorMessage = ex.Message;
                Core.Logger.Error(ex, "Failed to run pnputil installer.");
            }
            return result;
        }

        public async Task<bool> UninstallAsync(string oemInfName)
        {
            try
            {
                var psi = new ProcessStartInfo
                {
                    FileName = "pnputil.exe",
                    Arguments = $"/delete-driver {oemInfName} /force",
                    UseShellExecute = false,
                    CreateNoWindow = true
                };
                using var process = Process.Start(psi);
                if (process != null)
                {
                    await process.WaitForExitAsync();
                    return process.ExitCode == 0;
                }
            } catch { }
            return false;
        }
    }
}
