using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEye.UI.Modern.Infrastructure;
using DeepEyeUnlocker.Core.Services;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class MainViewModel : ObservableObject
    {
        public static MainViewModel? Instance { get; private set; }
        private readonly DeviceManager _deviceManager;
        private readonly DiagnosticService _diagnosticService;
        private readonly DeepEyeUnlocker.Services.Nexus.SentinelBridgeClient _bridge = new();
        private readonly DeepEyeUnlocker.Features.Analytics.Services.AnalyticsProService _analyticsPro = new();

        [ObservableProperty]
        private string _logContent = "";

        [ObservableProperty]
        private DeviceContext? _selectedDevice;

        [ObservableProperty]
        private CenterViewModelBase? _currentCenter;

        [ObservableProperty]
        private GlobalHudViewModel _globalHud = new();

        [ObservableProperty]
        private bool _isExpertMode = false;

        [ObservableProperty]
        private string _statusText = "Ready";

        [ObservableProperty]
        private string _riskLevel = "STABLE";

        [ObservableProperty]
        private double _progressValue = 0;

        public string VersionDisplay => VersionManager.FullVersionDisplay;

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
            _diagnosticService = new DiagnosticService();
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
            if (ConnectedDevices.Any()) 
            {
                SelectedDevice = ConnectedDevices.First();
                _ = Task.Run(async () => {
                     var alerts = await _analyticsPro.ScanForNewAlertsAsync(SelectedDevice.Chipset);
                     foreach(var alert in alerts)
                     {
                         Logger.Warning($"[CVE-ALERT] Critical Security Exposure: {alert.CveId} (Severity {alert.Severity})");
                         App.Current.Dispatcher.Invoke(() => StatusText = $"SECURITY ALERT: {alert.CveId}");
                     }
                });
            }
            StatusText = initialDevices.Any() ? $"{initialDevices.Count} device(s) connected" : "Waiting for device...";

            // Start Sentinel Bridge (v5.1)
            _bridge.CommandReceived += async cmd => {
                await App.Current.Dispatcher.InvokeAsync(async () => {
                    StatusText = $"REMOTE EXECUTION: {cmd.Action}";
                    Logger.Warning($"[NEXUS-RELAY] Executing remote directive: {cmd.Action}");
                    
                    // Simulate processing
                    await Task.Delay(2000);
                    await _bridge.SendResponseAsync(cmd.CommandId, true, "Operation completed via remote relay.");
                    StatusText = "Remote Operation Success";
                });
            };
            _ = _bridge.StartListeningAsync();

            // Default view
            NavigateToCenter("INFO");
        }

        [RelayCommand]
        private void ReportIssue()
        {
            try
            {
                string path = System.IO.Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "DeepEye_Diagnostic.txt");
                _diagnosticService.ExportReport(path);
                StatusText = "Diagnostic report exported to application folder.";
                // In a future update, this could open a web browser to a GitHub Issue page with the text pre-copied
                System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(path) { UseShellExecute = true });
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to export diagnostic report.");
            }
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
                "EXPERT" => new ExpertCenterViewModel(SelectedDevice),
                "FLEET" => new FleetCenterViewModel(),
                "HEALTH" => new HealthCenterViewModel(SelectedDevice),
                "FRP" => new FrpCenterViewModel(SelectedDevice),
                "ANALYTICS" => new AnalyticsCenterViewModel(SelectedDevice),
                "CLOUD" => new CloudSyncViewModel(SelectedDevice),
                "DRIVER" => new DriverCenterViewModel(),
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
