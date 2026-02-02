using System;
using System.Collections.Generic;
using System.Linq;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.Analytics.Models;
using DeepEyeUnlocker.Features.Analytics.Services;

namespace DeepEyeUnlocker.Features.Analytics.Services
{
    /// <summary>
    /// Coordinates device scans and operations across the application for fleet-wide reporting.
    /// </summary>
    public class FleetManager
    {
        private static readonly Lazy<FleetManager> _instance = new(() => new FleetManager());
        public static FleetManager Instance => _instance.Value;

        private readonly List<DeviceHealthReport> _healthReports = new();
        private readonly List<CveReport> _cveReports = new();
        private readonly List<OperationRecord> _operations = new();

        private readonly CveScanner _cveScanner = new();
        private readonly FleetAnalytics _fleetAnalytics = new();

        public event Action? OnDataUpdated;

        private FleetManager() { }

        public void RegisterHealthReport(DeviceHealthReport report)
        {
            // Update or add report
            var existing = _healthReports.FirstOrDefault(r => r.SerialNumber == report.SerialNumber);
            if (existing != null) _healthReports.Remove(existing);
            _healthReports.Add(report);

            // Trigger automatic CVE scan
            var cveReport = _cveScanner.Scan(report);
            RegisterCveReport(cveReport);

            Logger.Info($"[FLEET] Registered health report for {report.SerialNumber}");
            OnDataUpdated?.Invoke();
        }

        public void RegisterCveReport(CveReport report)
        {
            var existing = _cveReports.FirstOrDefault(r => r.DeviceSerial == report.DeviceSerial);
            if (existing != null) _cveReports.Remove(existing);
            _cveReports.Add(report);
            OnDataUpdated?.Invoke();
        }

        public void RegisterOperation(string brand, string operation, bool success, string? error = null)
        {
            _operations.Add(new OperationRecord
            {
                Timestamp = DateTime.Now,
                Brand = brand,
                Operation = operation,
                IsSuccess = success,
                Error = error
            });
            OnDataUpdated?.Invoke();
        }

        public FleetAnalytics.FleetSummary GetSummary()
        {
            return _fleetAnalytics.GenerateSummary(_healthReports, _cveReports);
        }

        public IEnumerable<OperationRecord> GetOperationHistory() => _operations;
        public IEnumerable<DeviceHealthReport> GetReports() => _healthReports;
    }

    public class OperationRecord
    {
        public DateTime Timestamp { get; set; }
        public string Brand { get; set; } = string.Empty;
        public string Operation { get; set; } = string.Empty;
        public bool IsSuccess { get; set; }
        public string? Error { get; set; }
    }
}
