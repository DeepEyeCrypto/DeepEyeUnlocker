using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEye.UI.Modern.Infrastructure;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class MainViewModel : ObservableObject
    {
        public static MainViewModel? Instance { get; private set; }
        private readonly DeviceManager _deviceManager;

        [ObservableProperty]
        private string _logContent = "";

        [ObservableProperty]
        private DeviceContext? _selectedDevice;

        [ObservableProperty]
        private CenterViewModelBase? _currentCenter;

        [ObservableProperty]
        private bool _isExpertMode = false;

        [ObservableProperty]
        private string _statusText = "Ready";

        [ObservableProperty]
        private string _riskLevel = "STABLE";

        [ObservableProperty]
        private double _progressValue = 0;

        public ObservableCollection<DeviceContext> ConnectedDevices { get; } = new();

        public MainViewModel()
        {
            Instance = this;
            // Logging Bridge
            Logger.AddSink(new UILogSink(line => {
                App.Current.Dispatcher.Invoke(() => {
                    LogContent += line + "\n";
                });
            }));

            _deviceManager = new DeviceManager();
            _deviceManager.OnDevicesChanged += devices =>
            {
                App.Current.Dispatcher.Invoke(() =>
                {
                    ConnectedDevices.Clear();
                    foreach (var device in devices)
                    {
                        ConnectedDevices.Add(device);
                    }
                    if (SelectedDevice == null && ConnectedDevices.Any())
                    {
                        SelectedDevice = ConnectedDevices.First();
                    }
                    StatusText = devices.Any() ? $"{devices.Count()} device(s) connected" : "Waiting for device...";
                });
            };

            // Initial scan
            var initialDevices = _deviceManager.EnumerateDevices();
            foreach (var d in initialDevices) ConnectedDevices.Add(d);
            if (ConnectedDevices.Any()) SelectedDevice = ConnectedDevices.First();
            StatusText = initialDevices.Any() ? $"{initialDevices.Count} device(s) connected" : "Waiting for device...";

            // Default view
            NavigateToCenter("INFO");
        }

        [RelayCommand]
        private void NavigateToCenter(string center)
        {
            CurrentCenter = center.ToUpper() switch
            {
                "INFO" => new DeviceInfoViewModel(SelectedDevice),
                "SECURITY" => new UnlockCenterViewModel(SelectedDevice),
                "FLASH" => new FlashCenterViewModel(),
                "CLOAK" => new CloakCenterViewModel(SelectedDevice),
                "EXPERT" => new PartitionCenterViewModel(SelectedDevice),
                "FLEET" => new FleetCenterViewModel(),
                _ => CurrentCenter
            };
        }

        [RelayCommand]
        private void ToggleExpertMode()
        {
            IsExpertMode = !IsExpertMode;
            RiskLevel = IsExpertMode ? "CRITICAL (EXPERT)" : "STABLE";
        }

        [RelayCommand]
        private void SelectOEM(string oem)
        {
            StatusText = $"Selected OEM: {oem}";
        }
    }
}
