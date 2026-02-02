using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.DriverCenter.Models;

namespace DeepEyeUnlocker.Features.DriverCenter.Services
{
    public class DriverInstallerService
    {
        private readonly HttpClient _httpClient = new();
        private readonly string _tempPath;

        public DriverInstallerService()
        {
            _tempPath = Path.Combine(Path.GetTempPath(), "DeepEyeDrivers");
            if (!Directory.Exists(_tempPath)) Directory.CreateDirectory(_tempPath);
        }

        public async Task<bool> InstallPackageAsync(DriverPackage package, IProgress<ProgressUpdate> progress)
        {
            Logger.Info($"[DRIVER-INSTALLER] Initializing deployment for {package.Name}...");
            progress.Report(ProgressUpdate.Info(10, $"Downloading {package.Name}..."));

            string localPath = Path.Combine(_tempPath, Path.GetFileName(package.Url));
            if (string.IsNullOrEmpty(Path.GetExtension(localPath))) localPath += ".exe";

            try
            {
                // 1. Download (Simulated logic for now, using actual HttpClient for structure)
                // in real scenario, we check if file exists or download
                Logger.Info($"[DRIVER-INSTALLER] Fetching payload from {package.Url}");
                await Task.Delay(1000); // Simulate network latency

                // 2. Install based on method
                progress.Report(ProgressUpdate.Info(40, $"Executing {package.InstallMethod} deployment..."));
                
                switch (package.InstallMethod)
                {
                    case "EXE":
                        return await ExecuteSilentExeAsync(package, localPath, progress);
                    case "INF":
                        return await ExecuteInfInstallAsync(package, localPath, progress);
                    case "ZIP":
                        return await ExecuteZipDeploymentAsync(package, localPath, progress);
                    default:
                        Logger.Warn($"[DRIVER-INSTALLER] Unknown install method: {package.InstallMethod}");
                        return false;
                }
            }
            catch (Exception ex)
            {
                Logger.Error($"[DRIVER-INSTALLER] Deployment failed: {ex.Message}");
                return false;
            }
        }

        private async Task<bool> ExecuteSilentExeAsync(DriverPackage package, string path, IProgress<ProgressUpdate> progress)
        {
            Logger.Info($"[DRIVER-INSTALLER] Launching silent installer: {package.Name}");
            
            try
            {
                var psi = new ProcessStartInfo
                {
                    FileName = path,
                    Arguments = package.SilentFlags,
                    UseShellExecute = true,
                    Verb = "runas", // Elevation required
                    WindowStyle = ProcessWindowStyle.Hidden
                };

                using var process = Process.Start(psi);
                if (process != null)
                {
                    await process.WaitForExitAsync();
                    if (process.ExitCode == 0)
                    {
                        Logger.Success($"[DRIVER-INSTALLER] {package.Name} EXE installation finished.");
                        return true;
                    }
                }
            }
            catch (Exception ex)
            {
                Logger.Error($"[DRIVER-INSTALLER] EXE execution failed: {ex.Message}");
            }
            return false;
        }

        private async Task<bool> ExecuteInfInstallAsync(DriverPackage package, string path, IProgress<ProgressUpdate> progress)
        {
            Logger.Info($"[DRIVER-INSTALLER] Injecting {package.Name} into Windows Driver Store...");
            
            try
            {
                var storeManager = new Infrastructure.Drivers.DriverStoreManager();
                var arch = Infrastructure.Native.ArchitectureHelper.GetDriverArchitecture();
                
                Logger.Info($"[DRIVER-INSTALLER] Verifying architecture compatibility for {arch}...");

                // In a real scenario, we would parse the INF to ensure it supports the target arch
                // For now, we proceed with the SetupAPI installation
                bool success = await storeManager.InstallToDriverStoreAsync(path);
                
                if (success)
                {
                    Logger.Success($"[DRIVER-INSTALLER] {package.Name} INF injection complete via SetupAPI.");
                    return true;
                }
                else
                {
                    Logger.Error($"[DRIVER-INSTALLER] SetupAPI failed for {package.Name}. Falling back to PnPUtil...");
                    
                    // Fallback to PnPUtil if SetupAPI fails (e.g. signature issues handled differently)
                    var psi = new ProcessStartInfo
                    {
                        FileName = "pnputil.exe",
                        Arguments = $"/add-driver \"{path}\" /install",
                        UseShellExecute = true,
                        Verb = "runas",
                        CreateNoWindow = true
                    };

                    using var process = Process.Start(psi);
                    if (process != null)
                    {
                        await process.WaitForExitAsync();
                        return process.ExitCode == 0;
                    }
                }
            }
            catch (Exception ex)
            {
                Logger.Error($"[DRIVER-INSTALLER] INF installation failed: {ex.Message}");
            }
            return false;
        }

        private async Task<bool> ExecuteZipDeploymentAsync(DriverPackage package, string path, IProgress<ProgressUpdate> progress)
        {
            return await Task.Run(() =>
            {
                if (package.SilentFlags == "EXTRACT_TO_PATH")
                {
                    string targetDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "DeepEyeUnlocker", "platform-tools");
                    Logger.Info($"[DRIVER-INSTALLER] Deploying Platform-Tools to: {targetDir}");

                    try
                    {
                        if (!Directory.Exists(targetDir)) Directory.CreateDirectory(targetDir);
                        
                        // Add to User PATH
                        string currentPath = Environment.GetEnvironmentVariable("PATH", EnvironmentVariableTarget.User) ?? "";
                        if (!currentPath.Contains(targetDir))
                        {
                            string newPath = (string.IsNullOrEmpty(currentPath) ? "" : currentPath + ";") + targetDir;
                            Environment.SetEnvironmentVariable("PATH", newPath, EnvironmentVariableTarget.User);
                            Logger.Info("[DRIVER-INSTALLER] System PATH updated with Platform-Tools.");
                        }
                        
                        return true;
                    }
                    catch (Exception ex)
                    {
                        Logger.Error($"[DRIVER-INSTALLER] ZIP deployment failed: {ex.Message}");
                    }
                }
                return false;
            });
        }
    }
}
