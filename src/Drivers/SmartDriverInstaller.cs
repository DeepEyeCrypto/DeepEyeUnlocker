using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Infrastructure.Drivers;
using DeepEyeUnlocker.Infrastructure.Native;


namespace DeepEyeUnlocker.Drivers
{
    /// <summary>
    /// V4.0 Smart Driver Installer.
    /// Handles architecture detection, conflict purging, proper setupapi installation, and LibUSB filtering.
    /// </summary>
    public class SmartDriverInstaller
    {
        private readonly DriverStoreManager _storeManager;
        private readonly FilterDriverManager _filterManager;
        private readonly DriverConflictManager _conflictManager;

        public SmartDriverInstaller()
        {
            _storeManager = new DriverStoreManager();
            _filterManager = new FilterDriverManager();
            _conflictManager = new DriverConflictManager();
        }

        public async Task<bool> InstallUniversalDriversAsync(IProgress<string>? progress = null)
        {
            try
            {
                progress?.Report("Detecting system architecture...");
                string arch = ArchitectureHelper.GetDriverArchitecture();
                Logger.Info($"Target architecture for drivers: {arch}");

                progress?.Report("Checking for driver conflicts...");
                var conflicts = await _conflictManager.DetectConflictsAsync();
                foreach (var conflict in conflicts)
                {
                    progress?.Report($"Found conflict: {conflict.Name}. Purging...");
                    await _conflictManager.PurgeConflictAsync(conflict);
                }

                // Define driver paths based on architecture
                string driverBaseDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "drivers", arch);
                
                // 1. Install MediaTek Drivers
                await InstallComponentAsync("MediaTek USB VCOM", 
                    Path.Combine(driverBaseDir, "mtk", "mtk_usb.inf"), progress);

                // 2. Install Qualcomm Drivers
                await InstallComponentAsync("Qualcomm QDLoader 9008", 
                    Path.Combine(driverBaseDir, "qualcomm", "qcser.inf"), progress);

                // 3. Register LibUSB Filters for BROM/EDL
                progress?.Report("Registering LibUSB filters for BROM stability...");
                _filterManager.RegisterUpperFilter("{36FC9E60-C465-11CF-8056-444553540000}"); // USB Class
                _filterManager.RegisterUpperFilter("{4D36E978-E325-11CE-BFC1-08002BE10318}"); // Ports Class

                progress?.Report("Driver installation and optimization complete.");
                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Smart Driver Installation failed.");
                progress?.Report($"Fatal Error: {ex.Message}");
                return false;
            }
        }

        private async Task<bool> InstallComponentAsync(string name, string infPath, IProgress<string>? progress)
        {
            if (!File.Exists(infPath))
            {
                Logger.Warning($"Driver INF not found for {name}: {infPath}");
                return false;
            }

            progress?.Report($"Installing {name} to Driver Store...");
            
            // Check signature
            if (!await _storeManager.IsDigitallySignedAsync(infPath))
            {
                Logger.Warning($"{name} driver is not digitally signed. Installation might fail on Win10/11.");
            }

            bool success = await _storeManager.InstallToDriverStoreAsync(infPath);
            if (success)
            {
                Logger.Success($"{name} installed successfully.");
            }
            else
            {
                Logger.Error($"{name} installation failed via SetupAPI.");
            }

            return success;
        }
    }
}
