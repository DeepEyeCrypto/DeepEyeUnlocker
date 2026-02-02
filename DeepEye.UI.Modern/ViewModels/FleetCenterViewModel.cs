using CommunityToolkit.Mvvm.ComponentModel;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using System;
using System.Collections.ObjectModel;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class FleetCenterViewModel : CenterViewModelBase
    {
        private readonly DeepEyeUnlocker.Features.Analytics.Services.FleetManager _manager;

        public override string Title => "FLEET ANALYTICS & REPORTS";

        [ObservableProperty] private int _managedDevices;
        [ObservableProperty] private int _healthAlerts;
        [ObservableProperty] private int _criticalThreats;
        [ObservableProperty] private double _avgRisk;
        [ObservableProperty] private string _topThreats = "None detected";

        public ObservableCollection<JobReportViewModel> JobHistory { get; } = new();

        public FleetCenterViewModel()
        {
            _manager = DeepEyeUnlocker.Features.Analytics.Services.FleetManager.Instance;
            _manager.OnDataUpdated += RefreshData;
            RefreshData();
        }

        private void RefreshData()
        {
            App.Current.Dispatcher.Invoke(() =>
            {
                var summary = _manager.GetSummary();
                ManagedDevices = summary.ManagedDevicesCount;
                HealthAlerts = summary.HealthAlertsCount;
                CriticalThreats = summary.CriticalCvesCount;
                AvgRisk = summary.AverageRiskScore;
                TopThreats = summary.CommonVulnerabilities.Any() 
                    ? string.Join(", ", summary.CommonVulnerabilities) 
                    : "None detected";

                JobHistory.Clear();
                foreach (var op in _manager.GetOperationHistory().OrderByDescending(x => x.Timestamp))
                {
                    JobHistory.Add(new JobReportViewModel
                    {
                        Timestamp = op.Timestamp.ToString("HH:mm:ss"),
                        Brand = op.Brand,
                        Operation = op.Operation,
                        Status = op.IsSuccess ? "SUCCESS" : "FAILED",
                        Error = op.Error ?? "--"
                    });
                }
            });
        }
    }

    public partial class JobReportViewModel : ObservableObject
    {
        [ObservableProperty] private string _timestamp = "";
        [ObservableProperty] private string _brand = "";
        [ObservableProperty] private string _operation = "";
        [ObservableProperty] private string _status = "";
        [ObservableProperty] private string _error = "--";
    }
}
