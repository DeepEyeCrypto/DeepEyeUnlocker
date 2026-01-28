using System;

namespace DeepEyeUnlocker.Cloak.Root
{
    public enum IntegrityLevel
    {
        None,
        BasicIntegrity,
        DeviceIntegrity,
        StrongIntegrity
    }

    public class RootCloakStatus
    {
        public bool IsRooted { get; set; }
        public bool IsMagiskInstalled { get; set; }
        public string MagiskVersion { get; set; } = string.Empty;
        public bool ZygiskActive { get; set; }
        public bool ShamikoActive { get; set; }
        public bool EnforceDenyListOff { get; set; }
        public IntegrityLevel PlayIntegrity { get; set; } = IntegrityLevel.None;
        
        public bool IsOptimal => ZygiskActive && ShamikoActive && EnforceDenyListOff;
    }
}
