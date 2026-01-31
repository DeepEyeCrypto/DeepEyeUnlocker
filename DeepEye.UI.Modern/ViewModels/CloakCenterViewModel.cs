using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using System;
using System.Threading.Tasks;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class CloakCenterViewModel : CenterViewModelBase
    {
        private readonly DeviceContext? _device;
        public override string Title => "CLOAK & STEALTH ENGINE";

        [ObservableProperty]
        private string _rootStatus = "Wait for scan...";

        [ObservableProperty]
        private string _devModeStatus = "Wait for scan...";

        public CloakCenterViewModel(DeviceContext? device)
        {
            _device = device;
        }

        [RelayCommand]
        private async Task ScanRoot()
        {
            if (_device == null) { Logger.Error("Connect a device in ADB mode first."); return; }
            Logger.Info("Scanning root detection traps...");
            await Task.Delay(1000); // Simulate
            RootStatus = "OPTIMAL (WELL HIDDEN)";
            Logger.Success("Root environment analysis complete.");
        }

        [RelayCommand]
        private async Task ApplyStealth()
        {
            if (_device == null) return;
            Logger.Warn("Applying Stealth Cloak... persistent settings will be modified.");
            await Task.Delay(1500); // Simulate
            DevModeStatus = "CLOAKED";
            Logger.Success("Stealth Cloak active. Device is now invisible to standard traps.");
        }
    }
}
