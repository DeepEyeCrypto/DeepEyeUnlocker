using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.CloudSync.Models;
using DeepEyeUnlocker.Features.CloudSync.Services;
using System.Collections.ObjectModel;
using System.Threading.Tasks;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class CloudSyncViewModel : CenterViewModelBase
    {
        private readonly DeviceContext? _device;
        private readonly CloudSyncService _syncService;

        public override string Title => "SECURE CLOUD VAULT";

        [ObservableProperty] private bool _isSyncing = false;
        [ObservableProperty] private string _syncStatus = "Logged in as Technician. Cloud Vault Active.";
        
        public ObservableCollection<CloudBackupStatus> RemoteBackups { get; } = new();

        public CloudSyncViewModel(DeviceContext? device)
        {
            _device = device;
            _syncService = new CloudSyncService(new CloudSyncSettings());
            LoadRemoteBackups();
        }

        private async void LoadRemoteBackups()
        {
            if (_device == null) return;
            
            var backups = await _syncService.GetRemoteBackupsAsync(_device.Serial ?? "UNKNOWN");
            RemoteBackups.Clear();
            foreach (var b in backups)
            {
                RemoteBackups.Add(b);
            }
        }

        [RelayCommand]
        private async Task TriggerUpload()
        {
            if (_device == null) return;

            IsSyncing = true;
            SyncStatus = "Preparing local partition for cloud offload...";

            // Simulate local file path from recent backup
            string dummyPath = $"/backups/{_device.Serial}/persist.debk";
            
            await _syncService.UploadBackupAsync(dummyPath, _device.Serial ?? "UNK", "persist", new Progress<ProgressUpdate>(p => {
                SyncStatus = p.Message;
            }));

            IsSyncing = false;
            SyncStatus = "Cloud Sync Complete. Partition Secured.";
            LoadRemoteBackups();
        }
    }
}
