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
        public string ModelNumber { get; set; } = string.Empty; // "RMX3941"
        public string MarketingName { get; set; } = string.Empty; // "Realme C75"
        public string Codename { get; set; } = string.Empty;
        public string CpuVariant { get; set; } = string.Empty; // "MT6789 (Dimensity 1080)"

        // Classification
        public string Brand { get; set; } = string.Empty;
        public string Series { get; set; } = string.Empty;
        public string Region { get; set; } = "Global";

        // Hardware & Modes (v2)
        public ChipsetInfo Chipset { get; set; } = new();
        public ServiceModeDescriptor ServiceModes { get; set; } = new();
        public LoaderRequirement Loaders { get; set; } = new();

        // Operations
        public List<OperationSupport> SupportedOperations { get; set; } = new();

        // Security
        public SecurityProfile Security { get; set; } = new();
        public FrpCapabilities FrpInfo { get; set; } = new();

        // Status
        public TestStatus ValidationStatus { get; set; } = TestStatus.Untested;
    }

    public class ServiceModeDescriptor
    {
        public PreloaderConfig Preloader { get; set; } = new();
        public bool SupportsBromAuthBypass { get; set; }
        public bool SupportsMetaMode { get; set; }
        public bool SupportsEdl { get; set; }
        public bool SupportsOdin { get; set; }
    }

    public class PreloaderConfig
    {
        public bool Supported { get; set; }
        public string ConnectionMethod { get; set; } = "VOLUME_KEYS"; // VOLUME_KEYS, TEST_POINT
        public TestPointInfo? TestPoint { get; set; }
    }

    public class TestPointInfo
    {
        public bool Required { get; set; }
        public string DiagramId { get; set; } = string.Empty;
        public string PinDescription { get; set; } = string.Empty;
    }

    public class LoaderRequirement
    {
        public string? CustomDaId { get; set; }   // Reference to LoaderRepository ID
        public string? CustomEmiId { get; set; }  // Reference to LoaderRepository ID
        public string? FirehoseProgrammerId { get; set; }
    }

    public class ChipsetInfo
    {
        public string Manufacturer { get; set; } = string.Empty;
        public string Model { get; set; } = string.Empty;
        public string Platform { get; set; } = string.Empty;
        public string Architecture { get; set; } = "ARM64";
    }

    public enum RiskLevel { Safe, Low, Medium, High, Critical }
    public enum TestStatus { Untested, VerifiedAlpha, VerifiedBeta, Stable }

    public class SecurityProfile
    {
        public string PatchLevel { get; set; } = string.Empty;
        public bool SecureBoot { get; set; }
        public bool EncryptedUserData { get; set; }
    }

    public class OperationSupport
    {
        public string OperationName { get; set; } = string.Empty;
        public RiskLevel RiskLevel { get; set; }
        public bool RequiresAuth { get; set; }
    }
}
