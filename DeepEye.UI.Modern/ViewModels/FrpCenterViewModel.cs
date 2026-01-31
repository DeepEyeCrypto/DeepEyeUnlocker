using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.FrpBypass;
using DeepEyeUnlocker.Features.FrpBypass.Models;
using DeepEyeUnlocker.Features.FrpBypass.Protocols;
using Newtonsoft.Json;
using System;
using System.Collections.ObjectModel;
using System.IO;
using System.Linq;
using System.Threading.Tasks;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class FrpCenterViewModel : CenterViewModelBase
    {
        private readonly DeviceContext? _device;
        public override string Title => "FRP BYPASS & UNLOCK";

        [ObservableProperty] private bool _isBypassing = false;
        [ObservableProperty] private FrpBrandProfile? _selectedProfile;
        
        public ObservableCollection<FrpBrandProfile> Profiles { get; } = new();

        public FrpCenterViewModel(DeviceContext? device)
        {
            _device = device;
            LoadProfiles();
        }

        private void LoadProfiles()
        {
            try
            {
                // In a real app, this would be in a resource or app data folder
                string jsonPath = "src/Features/FrpBypass/Profiles/FrpProfiles.json";
                if (File.Exists(jsonPath))
                {
                    var profiles = JsonConvert.DeserializeObject<ObservableCollection<FrpBrandProfile>>(File.ReadAllText(jsonPath));
                    if (profiles != null)
                    {
                        foreach (var p in profiles) Profiles.Add(p);
                        SelectedProfile = Profiles.FirstOrDefault();
                    }
                }
            }
            catch (Exception ex)
            {
                Logger.Error($"Failed to load FRP profiles: {ex.Message}");
            }
        }

        [RelayCommand]
        private async Task StartBypass()
        {
            if (_device == null || SelectedProfile == null)
            {
                Logger.Warn("Device or Profile not selected.");
                return;
            }

            IsBypassing = true;

            // In a real implementation, we would detect the protocol from the DeviceContext
            var adapter = new FrpProtocolAdapter(_device); 
            
            var engine = new FrpEngineCore(adapter, SelectedProfile);
            bool success = await engine.ExecuteBypassAsync(_device);

            if (success)
            {
                Logger.Success($"[FRP] Bypass completed successfully for {_device.Brand}!");
            }
            else
            {
                Logger.Error($"[FRP] Bypass failed for {_device.Brand}. Check logs.");
            }

            IsBypassing = false;
        }
    }
}
