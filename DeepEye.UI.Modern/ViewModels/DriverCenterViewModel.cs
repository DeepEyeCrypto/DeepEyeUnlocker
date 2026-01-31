using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.DriverCenter.Models;
using DeepEyeUnlocker.Features.DriverCenter.Services;
using Newtonsoft.Json;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class DriverCenterViewModel : CenterViewModelBase
    {
        private readonly DriverDetectorService _detector;
        private readonly DriverInstallerService _installer;
        private List<DriverProfile> _allProfiles = new();

        public override string Title => "DRIVER CENTER PRO";

        [ObservableProperty] private bool _isScanning = false;
        [ObservableProperty] private bool _isInstalling = false;
        [ObservableProperty] private string _statusMessage = "Ready to audit system driver health.";
        
        public ObservableCollection<DriverStatus> InstalledDrivers { get; } = new();
        public ObservableCollection<DeviceConnectivityReport> ConnectedDevices { get; } = new();

        public DriverCenterViewModel()
        {
            _detector = new DriverDetectorService();
            _installer = new DriverInstallerService();
            LoadProfiles();
            RunAuditCommand.Execute(null);
        }

        private void LoadProfiles()
        {
            try
            {
                // In production, this would be an absolute path or embedded resource
                string jsonPath = "/Users/enayat/Documents/DeepEyeUnlocker/src/Features/DriverCenter/Profiles/DriverProfiles.json";
                if (System.IO.File.Exists(jsonPath))
                {
                    var json = System.IO.File.ReadAllText(jsonPath);
                    _allProfiles = JsonConvert.DeserializeObject<List<DriverProfile>>(json) ?? new();
                }
            }
            catch (Exception ex)
            {
                Logger.Error($"[DRIVER-VM] Failed to load profiles: {ex.Message}");
            }
        }

        [RelayCommand]
        private async Task RunAudit()
        {
            IsScanning = true;
            StatusMessage = "Auditing Windows Device Manager & System Registry...";

            var devices = await _detector.ScanConnectedDevicesAsync();
            ConnectedDevices.Clear();
            foreach (var d in devices) ConnectedDevices.Add(d);

            var health = await _detector.CheckDriverHealthAsync(_allProfiles);
            InstalledDrivers.Clear();
            foreach (var h in health) InstalledDrivers.Add(h);

            StatusMessage = $"Audit Complete: {ConnectedDevices.Count} devices, {InstalledDrivers.Count(x => !x.IsInstalled)} drivers missing.";
            IsScanning = false;
        }

        [RelayCommand]
        private async Task InstallAll()
        {
            IsInstalling = true;
            StatusMessage = "Initializing bulk driver deployment...";

            var missing = InstalledDrivers.Where(x => !x.IsInstalled).ToList();
            if (!missing.Any())
            {
                StatusMessage = "System is already healthy. No missing drivers detected.";
                IsInstalling = false;
                return;
            }

            foreach (var status in missing)
            {
                var profile = _allProfiles.FirstOrDefault(p => p.BrandId == status.BrandId);
                if (profile == null) continue;

                foreach (var package in profile.Packages)
                {
                    StatusMessage = $"Deploying {package.Name}...";
                    bool success = await _installer.InstallPackageAsync(package, new Progress<ProgressUpdate>(p => {
                        StatusMessage = p.Message;
                    }));

                    if (!success)
                    {
                        Logger.Error($"[DRIVER-VM] Failed to install {package.Name}");
                    }
                }
            }

            await RunAudit();
            StatusMessage = "System Driver Refresh Complete.";
            IsInstalling = false;
        }
    }
}
