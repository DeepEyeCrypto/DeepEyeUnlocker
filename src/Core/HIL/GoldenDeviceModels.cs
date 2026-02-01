using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.HIL
{
    public class GoldenDeviceInfo
    {
        public string DeviceId { get; set; } = string.Empty;
        public string Manufacturer { get; set; } = string.Empty;
        public string Model { get; set; } = string.Empty;
        public string FirmwareVersion { get; set; } = string.Empty;
        public Dictionary<string, string> Scenarios { get; set; } = new(); // Protocol -> FilePath
        public DateTime RegisteredAt { get; set; }
        public string? Chipset { get; set; }
    }

    public class GoldenRegistryRoot
    {
        public List<GoldenDeviceInfo> Devices { get; set; } = new();
        public int Version { get; set; } = 1;
    }
}
