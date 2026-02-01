using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;

namespace DeepEyeUnlocker.Core.Models
{
    public class DeviceProfile
    {
        [Key]
        public string ProfileId { get; set; } = Guid.NewGuid().ToString();

        // Identity
        [Required]
        public string ModelNumber { get; set; } = string.Empty; // "SM-A546E", "22041216I"
        public string MarketingName { get; set; } = string.Empty; // "Galaxy A54 5G"
        public string Codename { get; set; } = string.Empty; // "a54x"

        // Classification
        public string Brand { get; set; } = string.Empty;
        public string Series { get; set; } = string.Empty;
        public string Region { get; set; } = "Global";

        // Hardware
        public ChipsetInfo Chipset { get; set; } = new();
        public List<string> SupportedBootModes { get; set; } = new();

        // USB Identification
        public List<UsbIdentifier> UsbIds { get; set; } = new();
        public List<string> InterfaceClassGuids { get; set; } = new();

        // Operations Matrix
        public List<OperationSupport> SupportedOperations { get; set; } = new();

        // Security Characteristics
        public SecurityProfile Security { get; set; } = new();

        // Metadata
        public List<FirmwareInfo> KnownFirmwares { get; set; } = new();
        public TestStatus ValidationStatus { get; set; } = TestStatus.Untested;
    }

    public class ChipsetInfo
    {
        public string Manufacturer { get; set; } = string.Empty; // Qualcomm, MediaTek
        public string Model { get; set; } = string.Empty; // SM-A7150
        public string Platform { get; set; } = string.Empty; // Snapdragon 7 Gen 1
        public string Architecture { get; set; } = "ARM64";
        public List<UniversalMethod> SupportedUniversalMethods { get; set; } = new();
    }

    public class UsbIdentifier
    {
        public int Vid { get; set; }
        public int Pid { get; set; }
    }

    public class OperationSupport
    {
        public string OperationName { get; set; } = string.Empty; // "FrpRemove"
        public string ProtocolPlugin { get; set; } = string.Empty;
        public string HandlerName { get; set; } = string.Empty;
        public bool RequiresAuth { get; set; }
        public RiskLevel RiskLevel { get; set; }
        public List<string> Prerequisites { get; set; } = new();
        public DateTime? LastVerified { get; set; }
    }

    public class SecurityProfile
    {
        public string PatchLevel { get; set; } = string.Empty;
        public bool SecureBoot { get; set; }
        public bool EncryptedUserData { get; set; }
    }

    public class FirmwareInfo
    {
        public string Version { get; set; } = string.Empty;
        public string AndroidVersion { get; set; } = string.Empty;
        public string ReleaseDate { get; set; } = string.Empty;
    }

    public enum UniversalMethod
    {
        None,
        MtkBromAuthBypass,
        QualcommEdlFirehose,
        SpdDiagMode
    }

    public enum RiskLevel
    {
        Low,     // Read Info
        Medium,  // FRP Bypass
        High,    // Flashing
        Critical // Bootloader Unlock / IMEI
    }

    public enum TestStatus
    {
        Untested,
        VerifiedAlpha,
        VerifiedBeta,
        Stable
    }
}
