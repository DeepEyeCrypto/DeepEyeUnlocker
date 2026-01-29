using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.Models
{
    /// <summary>
    /// Represents a comprehensive health and security audit of a mobile device.
    /// </summary>
    public class DeviceHealthReport
    {
        // Identification
        public string SerialNumber { get; set; } = "";
        public string Imei1 { get; set; } = "";
        public string Imei2 { get; set; } = "";
        public string MacAddress { get; set; } = "";
        public string BluetoothAddress { get; set; } = "";

        // Hardware Status
        public int BatteryLevel { get; set; }
        public int BatteryHealth { get; set; } // 0-100 percentage
        public double BatteryTemperature { get; set; }
        public string BatteryStatus { get; set; } = "Unknown";
        public long StorageTotalBytes { get; set; }
        public long StorageFreeBytes { get; set; }

        // OS & Security
        public string AndroidVersion { get; set; } = "";
        public string SecurityPatchLevel { get; set; } = "";
        public string KernelVersion { get; set; } = "";
        public string BuildNumber { get; set; } = "";
        public DateTime BuildDate { get; set; }
        public string BasebandVersion { get; set; } = "";

        // Status & Audit
        public bool IsBootloaderUnlocked { get; set; }
        public bool IsRooted { get; set; }
        public string RootMethod { get; set; } = "None";
        public bool IsDevOptionsEnabled { get; set; }
        public bool IsUsbDebuggingEnabled { get; set; }
        public bool IsOemUnlockEnabled { get; set; }
        public bool IsFrpActive { get; set; }
        
        // System Integrity
        public bool IsSelinuxEnforcing { get; set; }
        public string VerifiedBootState { get; set; } = "Unknown";
        public bool HasSuspiciousApps { get; set; }
        public List<string> AuditFindings { get; set; } = new();

        // Metadata
        public DateTime ScanTimestamp { get; set; } = DateTime.UtcNow;
        public string ToolVersion { get; set; } = "1.3.0";

        public override string ToString()
        {
            return $"Health Report for {SerialNumber} ({AndroidVersion}) - Root: {IsRooted}, BL: {IsBootloaderUnlocked}";
        }
    }
}
