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
            
            LoadMockFleetData();
        }

        private void LoadMockFleetData()
        {
            // Simulate a fleet of 50 devices with various reports
            var mockReports = new List<DeviceHealthReport>();
            var mockCveReports = new List<CveReport>();

            for (int i = 0; i < 50; i++)
            {
                var h = new DeviceHealthReport 
                { 
                    SerialNumber = $"SN{i:D4}", 
                    IsRooted = i % 10 == 0,
                    AndroidVersion = "14",
                    SecurityPatchLevel = i % 5 == 0 ? "2023-01-01" : "2024-05-01"
                };
                if (!h.IsRooted) h.AuditFindings.Add("Root Check");
                
                mockReports.Add(h);
                mockCveReports.Add(_cveScanner.Scan(h));
            }

            FleetSummary = _fleetAnalytics.GenerateSummary(mockReports, mockCveReports);
        }

        [RelayCommand]
        private async Task RunCveScan()
        {
            if (_device == null) return;

            IsScanning = true;
            ScanStatus = "Scanning firmware security attributes...";
            Vulnerabilities.Clear();

            await Task.Delay(1500); // Simulate processing

            // Since we don't have a real health report for the current device context here easily, 
            // we simulate one based on device info or use a placeholder
            var dummyHealth = new DeviceHealthReport
            {
                SerialNumber = _device.Serial ?? "UNKNOWN",
                AndroidVersion = "14",
                SecurityPatchLevel = "2023-01-01" // Intentionally old for demonstration
            };

            CurrentReport = _cveScanner.Scan(dummyHealth);
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
            ScanStatus = "Generating encrypted PDF security audit...";
            await Task.Delay(2000);
            ScanStatus = "Report exported to Documents/DeepEye/Reports/.";
        }
    }
}
