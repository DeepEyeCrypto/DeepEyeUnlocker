using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.Models
{
    public enum ConnectionMode 
    { 
        None, 
        ADB, 
        Fastboot, 
        EDL, 
        BROM, 
        Preloader, 
        DownloadMode, 
        MTP,
        Diagnostics,
        Sideload,
        Recovery
    }

    public class DeviceContext
    {
        public string Serial { get; set; } = string.Empty;
        public string Brand { get; set; } = "Unknown";
        public string Model { get; set; } = "Unknown";
        public string Chipset { get; set; } = "Unknown";
        public string SoC { get; set; } = "Unknown"; // e.g. SM8450
        public ConnectionMode Mode { get; set; } = ConnectionMode.None;
        public int Vid { get; set; }
        public int Pid { get; set; }
        public bool IsAuthorized { get; set; }
        public int BatteryLevel { get; set; } = -1;

        // Metadata Store for brand-specific properties (HWID, ARB Version, etc.)
        public Dictionary<string, string> Properties { get; } = new(StringComparer.OrdinalIgnoreCase);

        public override string ToString() => $"{Brand} {Model} [{Mode}] ({Vid:X4}:{Pid:X4})";
    }
}
