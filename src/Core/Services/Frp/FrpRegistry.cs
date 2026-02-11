using System;
using System.Collections.Generic;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Services.Frp
{
    public static class FrpRegistry
    {
        public static FrpCapabilities GetDefaultCapabilities(string brand, string chipset)
        {
            var brandNorm = brand?.ToUpper() ?? "GENERIC";
            var chipNorm = chipset?.ToUpper() ?? "GENERIC";

            if (brandNorm == "SAMSUNG")
            {
                return new FrpCapabilities
                {
                    Type = FrpType.SamsungKnox,
                    FrpPartitionName = "persistent",
                    SupportedProtocols = new List<string> { "ODIN" },
                    OfficialServiceMethod = "KNOX_DEPLOYMENT_APP",
                    RiskLevel = RiskLevel.Medium
                };
            }

            if (chipNorm.Contains("QUALCOMM") || chipNorm.Contains("SNAPDRAGON") || chipNorm.StartsWith("MSM") || chipNorm.StartsWith("SDM"))
            {
                return new FrpCapabilities
                {
                    Type = FrpType.GoogleStandard,
                    FrpPartitionName = "frp",
                    SupportedProtocols = new List<string> { "EDL" },
                    OfficialServiceMethod = "EDL_WIPE_FRP_PARTITION",
                    RiskLevel = RiskLevel.Low
                };
            }

            if (chipNorm.Contains("MEDIATEK") || chipNorm.Contains("MTK") || chipNorm.StartsWith("MT"))
            {
                return new FrpCapabilities
                {
                    Type = FrpType.GoogleStandard,
                    FrpPartitionName = brandNorm == "SAMSUNG" ? "persistent" : "frp",
                    SupportedProtocols = new List<string> { "BROM", "PRELOADER" },
                    OfficialServiceMethod = "BROM_FORMAT_PARTITION",
                    RiskLevel = RiskLevel.Low
                };
            }

            return new FrpCapabilities
            {
                Type = FrpType.GoogleStandard,
                FrpPartitionName = "frp",
                SupportedProtocols = new List<string> { "FASTBOOT" },
                OfficialServiceMethod = "FASTBOOT_ERASE_FRP"
            };
        }
    }
}
