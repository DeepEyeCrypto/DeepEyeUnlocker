using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.PartitionBackup.Engine;
using DeepEyeUnlocker.Features.PartitionBackup.Models;
using System;
using System.Collections.ObjectModel;
using System.IO;
using System.Linq;
using System.Threading.Tasks;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class PartitionCenterViewModel : CenterViewModelBase
    {
        private readonly DeviceContext? _device;
        private readonly PartitionMetadataCollector _collector;
        private readonly BackupEngine _engine;

        public override string Title => "PARTITION BACKUP CENTER";

        [ObservableProperty] private bool _isLoading = false;
        [ObservableProperty] private bool _isBusy = false;
        [ObservableProperty] private string _statusMessage = "Ready";

        public ObservableCollection<PartitionInfo> Partitions { get; } = new();

        public PartitionCenterViewModel(DeviceContext? device)
        {
            _device = device;
            _collector = new PartitionMetadataCollector();
            _engine = new BackupEngine();
            
            if (_device != null)
            {
                _ = RefreshPartitions();
            }
        }

        [RelayCommand]
        private async Task RefreshPartitions()
        {
            if (_device == null) return;

            IsLoading = true;
            Partitions.Clear();
            
            var list = await _collector.GetPartitionsAsync(_device);
            foreach (var p in list) Partitions.Add(p);
            
            IsLoading = false;
        }

        [RelayCommand]
        private async Task BackupPartition(PartitionInfo? partition)
        {
            if (partition == null || _device == null) return;

            IsBusy = true;
            StatusMessage = $"Backing up {partition.Name}...";

            string backupDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Backups", _device.Serial);
            if (!Directory.Exists(backupDir)) Directory.CreateDirectory(backupDir);

            string fileName = $"{partition.Name}_{DateTime.Now:yyyyMMdd_HHmmss}.debk";
            string filePath = Path.Combine(backupDir, fileName);

            using (var fs = new FileStream(filePath, FileMode.Create))
            {
                var progress = new Progress<ProgressUpdate>(p => {
                    StatusMessage = $"({p.Percentage}%) {p.Message}";
                });

                bool success = await _engine.StartBackupAsync(_device, partition.Name, fs, progress, default);
                
                if (success)
                {
                    Logger.Success($"[UI] Backup saved: {fileName}");
                }
            }

            IsBusy = false;
            StatusMessage = "Ready";
        }
    }
}
