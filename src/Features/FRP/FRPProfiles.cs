using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Features.FRP
{
    public class FRPProfile
    {
        public string Model { get; set; } = string.Empty;
        public string Chipset { get; set; } = string.Empty;
        public List<string> Partitions { get; set; } = new();
        public bool EraseOnly { get; set; } = true;
        public string? Note { get; set; }
    }

    public static class FRPProfiles
    {
        public static List<FRPProfile> GetStandardProfiles()
        {
            return new List<FRPProfile>
            {
                new FRPProfile 
                { 
                    Model = "Generic Qualcomm", 
                    Chipset = "Qualcomm", 
                    Partitions = new List<string> { "frp", "config" },
                    Note = "Standard reset for Snapdragon devices" 
                },
                new FRPProfile 
                { 
                    Model = "Generic MediaTek", 
                    Chipset = "MTK", 
                    Partitions = new List<string> { "frp" },
                    Note = "Standard reset for Helio/Dimensity devices" 
                },
                new FRPProfile 
                { 
                    Model = "Samsung (Persistent)", 
                    Chipset = "Exynos/QC", 
                    Partitions = new List<string> { "persistent" },
                    Note = "Specific to Samsung account/FRP locks" 
                },
                new FRPProfile 
                { 
                    Model = "Xiaomi (Config)", 
                    Chipset = "Qualcomm/MTK", 
                    Partitions = new List<string> { "config" },
                    Note = "Use with caution - may affect other settings" 
                }
            };
        }
    }
}
