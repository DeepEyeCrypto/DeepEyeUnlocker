using DeepEyeUnlocker.Core.Models;
using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.Services.Frp
{
    /// <summary>
    /// Static registry that provides default FRP configurations based on Brand and Chipset patterns.
    /// Used when a specific DeviceProfile doesn't have explicit FrpInfo set.
    /// </summary>
    public static class FrpRegistry
    {
        public static FrpCapabilities GetDefaultCapabilities(string brand, string chipset)
        {
            var brandNorm = brand?.ToUpper() ?? "GENERIC";
            var chipNorm = chipset?.ToUpper() ?? "GENERIC";

            // 1. Samsung Knox Pattern
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

            // 2. Qualcomm EDL Pattern
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

            // 3. MediaTek BROM Pattern
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

            // 4. Spreadtrum / Unisoc
            if (chipNorm.Contains("SPREADTRUM") || chipNorm.Contains("UNISOC") || chipNorm.StartsWith("SC"))
            {
                return new FrpCapabilities
                {
                    Type = FrpType.GoogleStandard,
                    FrpPartitionName = "frp",
                    SupportedProtocols = new List<string> { "SPD_DIAG" },
                    RiskLevel = RiskLevel.Medium
                };
            }

            // Default fallback
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
