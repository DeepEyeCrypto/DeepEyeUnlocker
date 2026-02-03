using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.Analytics.Models;
using DeepEyeUnlocker.Features.Analytics.Services;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading.Tasks;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class AnalyticsCenterViewModel : CenterViewModelBase
    {
        private readonly DeviceContext? _device;
        private readonly CveScanner _cveScanner;
        private readonly FleetAnalytics _fleetAnalytics;

        public override string Title => "ADVANCED ANALYTICS & CVE SCANNER";

        [ObservableProperty] private bool _isScanning = false;
        [ObservableProperty] private string _scanStatus = "Idle - Select device to scan vulnerabilities";
        
        // Individual Report
        [ObservableProperty] private CveReport? _currentReport;
        public ObservableCollection<CveInfo> Vulnerabilities { get; } = new();

        // Fleet Analytics
        [ObservableProperty] private FleetAnalytics.FleetSummary? _fleetSummary;

        public AnalyticsCenterViewModel(DeviceContext? device)
        {
            _device = device;
            _cveScanner = new CveScanner();
            _fleetAnalytics = new FleetAnalytics();
            
            FleetManager.Instance.OnDataUpdated += RefreshFleetData;
            RefreshFleetData();
        }

        private void RefreshFleetData()
        {
            App.Current.Dispatcher.Invoke(() => {
                FleetSummary = FleetManager.Instance.GetSummary();
            });
        }

        [RelayCommand]
        private async Task RunCveScan()
        {
            if (_device == null) return;
            await Task.Yield();

            IsScanning = true;
            ScanStatus = "Scanning firmware security attributes...";
            Vulnerabilities.Clear();

            // Try to find an existing health report for this device
            var health = FleetManager.Instance.GetReports().FirstOrDefault(r => r.SerialNumber == _device.Serial);
            
            if (health == null)
            {
                // Fallback to dummy if no health check was run yet
                health = new DeviceHealthReport
                {
                    SerialNumber = _device.Serial ?? "UNKNOWN",
                    AndroidVersion = "14",
                    SecurityPatchLevel = "2023-01-01" 
                };
            }

            CurrentReport = _cveScanner.Scan(health);
            foreach (var v in CurrentReport.Vulnerabilities)
            {
                Vulnerabilities.Add(v);
            }

            ScanStatus = $"Scan complete. {Vulnerabilities.Count} vulnerabilities found.";
            IsScanning = false;
        }

        [RelayCommand]
        private async Task ExportReport()
        {
            if (CurrentReport == null || _device == null) return;

            ScanStatus = "Generating enterprise security audit...";
            
            // Find corresponding health report
            var health = FleetManager.Instance.GetReports().FirstOrDefault(r => r.SerialNumber == _device.Serial)
                        ?? new DeviceHealthReport { SerialNumber = _device.Serial ?? "UNK" };

            string desktopPath = Environment.GetFolderPath(Environment.SpecialFolder.Desktop);
            string reportsDir = System.IO.Path.Combine(desktopPath, "DeepEye_Reports");
            if (!System.IO.Directory.Exists(reportsDir)) System.IO.Directory.CreateDirectory(reportsDir);
            
            string filePath = System.IO.Path.Combine(reportsDir, $"SecurityReport_{_device.Serial}_{DateTime.Now:yyyyMMdd}.md");
            
            await ReportGenerator.ExportPdfReportAsync(health, CurrentReport, filePath);
            
            ScanStatus = $"Report exported to: {filePath}";
        }
    }
}
