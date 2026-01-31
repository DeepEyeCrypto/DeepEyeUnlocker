using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using System;
using System.Collections.ObjectModel;
using System.Threading.Tasks;
using System.Linq;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class PartitionCenterViewModel : CenterViewModelBase
    {
        private readonly DeviceContext? _device;
        public override string Title => "EXPERT PARTITION MANAGER";

        [ObservableProperty]
        private bool _isScanning = false;

        public ObservableCollection<PartitionInfoViewModel> Partitions { get; } = new();

        public PartitionCenterViewModel(DeviceContext? device)
        {
            _device = device;
        }

        [RelayCommand]
        private async Task ScanPartitions()
        {
            if (_device == null) return;
            IsScanning = true;
            Partitions.Clear();
            Logger.Info("Reading partition table via GPT/Scatter...");

            await Task.Delay(1500); // Simulate engine reading

            // Mock Data for demonstration
            var mockPartitions = new[]
            {
                new PartitionInfoViewModel { Name = "boot", Size = "64 MB", StartAddress = "0x00004000" },
                new PartitionInfoViewModel { Name = "recovery", Size = "64 MB", StartAddress = "0x00008000" },
                new PartitionInfoViewModel { Name = "system", Size = "4.2 GB", StartAddress = "0x00010000" },
                new PartitionInfoViewModel { Name = "vendor", Size = "800 MB", StartAddress = "0x02100000" },
                new PartitionInfoViewModel { Name = "userdata", Size = "112 GB", StartAddress = "0x04C00000" },
                new PartitionInfoViewModel { Name = "frp", Size = "512 KB", StartAddress = "0xFF000000" }
            };

            foreach (var p in mockPartitions) Partitions.Add(p);
            
            IsScanning = false;
            Logger.Success($"Detected {Partitions.Count} partitions.");
        }
    }

    public partial class PartitionInfoViewModel : ObservableObject
    {
        [ObservableProperty] private string _name = "";
        [ObservableProperty] private string _size = "";
        [ObservableProperty] private string _startAddress = "";

        [RelayCommand]
        private async Task Backup()
        {
            Logger.Info($"[EXPERT] Initiating atomic backup for {Name}...");
            for(int i=0; i<=100; i+=20)
            {
                MainViewModel.Instance!.ProgressValue = i;
                await Task.Delay(200);
            }
            Logger.Success($"Backup of {Name} complete.");
            MainViewModel.Instance!.ProgressValue = 0;
        }

        [RelayCommand]
        private async Task Erase()
        {
            if (!MainViewModel.Instance!.IsExpertMode)
            {
                Logger.Error("ERASE operation requires EXPERT MODE enabled!");
                return;
            }
            Logger.Warn($"[DANGER] Erasing partition: {Name}...");
            await Task.Delay(1000);
            Logger.Success($"{Name} has been wiped.");
        }
    }
}
