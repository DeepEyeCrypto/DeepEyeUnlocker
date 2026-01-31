using CommunityToolkit.Mvvm.ComponentModel;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using System;
using System.Collections.ObjectModel;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class FleetCenterViewModel : CenterViewModelBase
    {
        public override string Title => "FLEET ANALYTICS & REPORTS";

        public ObservableCollection<JobReportViewModel> JobHistory { get; } = new();

        public FleetCenterViewModel()
        {
            // Seed some mock data
            JobHistory.Add(new JobReportViewModel { Timestamp = "10:45 AM", Brand = "SAMSUNG", Operation = "FRP ERASE", Status = "SUCCESS" });
            JobHistory.Add(new JobReportViewModel { Timestamp = "11:12 AM", Brand = "XIAOMI", Operation = "BOOTLOADER UNLOCK", Status = "FAILED", Error = "0x8004" });
            JobHistory.Add(new JobReportViewModel { Timestamp = "11:30 AM", Brand = "QUALCOMM", Operation = "FORMAT DATA", Status = "SUCCESS" });
            JobHistory.Add(new JobReportViewModel { Timestamp = "12:05 PM", Brand = "OPPO", Operation = "FRP ERASE", Status = "SUCCESS" });
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
