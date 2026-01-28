using System;

namespace DeepEyeUnlocker.Drivers.Models
{
    public class DriverProfile
    {
        public string Id { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string Vendor { get; set; } = string.Empty;
        public string Mode { get; set; } = string.Empty; // ADB, Fastboot, EDL, BROM, etc.
        public string[] HardwareIdPatterns { get; set; } = Array.Empty<string>();
        public string InfPath { get; set; } = string.Empty;
        public string Version { get; set; } = string.Empty;
        public bool RequiresTestMode { get; set; }
        public string Source { get; set; } = string.Empty;
        public string Notes { get; set; } = string.Empty;
    }

    public class ConnectedDevice
    {
        public string HardwareId { get; set; } = string.Empty;
        public string Vid { get; set; } = string.Empty;
        public string Pid { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public string Manufacturer { get; set; } = string.Empty;
        public string DriverProvider { get; set; } = string.Empty;
        public string DriverVersion { get; set; } = string.Empty;
        public bool IsProblemDevice { get; set; }
        public int ConfigManagerErrorCode { get; set; }
    }

    public class DriverInstallResult
    {
        public bool Success { get; set; }
        public string ErrorMessage { get; set; } = string.Empty;
        public string Log { get; set; } = string.Empty;
        public bool RebootRequired { get; set; }
    }
}
