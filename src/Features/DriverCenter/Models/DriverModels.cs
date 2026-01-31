using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Features.DriverCenter.Models
{
    public class DriverProfile
    {
        public string BrandId { get; set; } = string.Empty;
        public string BrandName { get; set; } = string.Empty;
        public List<string> Vids { get; set; } = new();
        public List<DriverPackage> Packages { get; set; } = new();
        public string Notes { get; set; } = string.Empty;
    }

    public class DriverPackage
    {
        public string Id { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string Url { get; set; } = string.Empty;
        public string Checksum { get; set; } = string.Empty;
        public string InstallMethod { get; set; } = "EXE"; // EXE, INF, ZIP
        public string SilentFlags { get; set; } = "";
    }

    public class DriverStatus
    {
        public string Name { get; set; } = string.Empty;
        public string BrandId { get; set; } = string.Empty;
        public bool IsInstalled { get; set; }
        public string Version { get; set; } = "N/A";
        public string ProblemCode { get; set; } = "0"; // Windows Device Problem Code
        public string StatusMessage { get; set; } = "Idle";
    }

    public class DeviceConnectivityReport
    {
        public string DeviceName { get; set; } = string.Empty;
        public string VendorId { get; set; } = string.Empty;
        public string ProductId { get; set; } = string.Empty;
        public string InstancePath { get; set; } = string.Empty;
        public bool HasDriverIssue { get; set; }
    }
}
