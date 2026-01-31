using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.DeviceHealth;
using DeepEyeUnlocker.Features.DeviceHealth.Models;
using System.Collections.ObjectModel;
using System.Threading.Tasks;
using System.Linq;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class HealthCenterViewModel : CenterViewModelBase
    {
        private readonly DeviceContext? _device;
        private readonly DeviceHealthScanner _healthService;

        public override string Title => "DEVICE HEALTH & AUDIT";

        [ObservableProperty] private bool _isAuditing = false;

        public ObservableCollection<HealthMetric> Metrics { get; } = new();

        public HealthCenterViewModel(DeviceContext? device)
        {
            _device = device;
            _healthService = new DeviceHealthScanner();
        }

        [RelayCommand]
        private async Task RunAudit()
        {
            if (_device == null)
            {
                Logger.Warn("No device connected to audit.");
                return;
            }

            IsAuditing = true;
            Metrics.Clear();
            Logger.Info("[HEALTH] Initializing deep component audit...");

            var report = await _healthService.ScanAsync(_device);
            
            // Map the complex report to UI metrics
            Metrics.Add(new HealthMetric { 
                Label = "IMEI", 
                Value = report.Imei1, 
                Status = report.AuditFindings.Any(f => f.Contains("IMEI")) ? HealthStatus.Warning : HealthStatus.Healthy,
                Details = "Standard hardware identifier check."
            });

            Metrics.Add(new HealthMetric { 
                Label = "BATTERY", 
                Value = $"{report.BatteryHealth}%", 
                Status = report.BatteryHealth > 80 ? HealthStatus.Healthy : HealthStatus.Warning,
                Details = $"Status: {report.BatteryStatus}. Temp: {report.BatteryTemperature:F1}°C."
            });

            Metrics.Add(new HealthMetric { 
                Label = "SECURITY PATCH", 
                Value = report.SecurityPatchLevel, 
                Status = report.AuditFindings.Any(f => f.Contains("patch")) ? HealthStatus.Warning : HealthStatus.Healthy,
                Details = $"Kernel: {report.KernelVersion}"
            });

            Metrics.Add(new HealthMetric { 
                Label = "NETWORK ID", 
                Value = report.MacAddress, 
                Status = HealthStatus.Healthy,
                Details = $"BT: {report.BluetoothAddress}"
            });

            // Add any specific findings
            foreach (var finding in report.AuditFindings)
            {
                Metrics.Add(new HealthMetric {
                    Label = "AUDIT FINDING",
                    Value = "ALERT",
                    Status = finding.Contains("CRITICAL") ? HealthStatus.Critical : HealthStatus.Warning,
                    Details = finding
                });
            }

            IsAuditing = false;
            Logger.Success("Hardware audit cycle complete.");
        }
    }
}
