using System;

namespace DeepEyeUnlocker.Cloak.Dev
{
    public class DevModeStatus
    {
        public bool DeveloperOptionsEnabled { get; set; }
        public bool UsbDebuggingEnabled { get; set; }
        public bool WirelessDebuggingEnabled { get; set; }
        public bool SystemDebuggable { get; set; } // ro.debuggable
        public string Notes { get; set; } = string.Empty;

        public bool IsStealth => !DeveloperOptionsEnabled && !UsbDebuggingEnabled;
    }
}
