using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.Models
{
    public class BrandProfile
    {
        public string BrandName { get; set; } = string.Empty;
        public List<string> CommonVids { get; set; } = new();
        public List<string> CommonPids { get; set; } = new();
        
        // Brand-specific features
        public bool SupportsBypassAuth { get; set; }
        public bool RequiresCredit { get; set; }
        public string AuthServerUrl { get; set; } = string.Empty;

        // Custom properties for specific chipset handlers
        // e.g., "EDL_Programmer_Prefix": "prog_emmc_firehose_8953"
        public Dictionary<string, string> Configs { get; set; } = new(StringComparer.OrdinalIgnoreCase);

        // Map Chipset -> Default Protocol
        public Dictionary<string, string> ProtocolOverrides { get; set; } = new();

        public override string ToString() => BrandName;
    }

    public class ProfileDatabase
    {
        public List<BrandProfile> Profiles { get; set; } = new();
        public DateTime LastUpdated { get; set; } = DateTime.Now;
        public string Version { get; set; } = "1.0.0";
    }
}
