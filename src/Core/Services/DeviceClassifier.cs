using System;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Services
{
    public enum DeviceCategory
    {
        Unknown,
        Legacy_Keypad,      // Feature phones (Nokia, Jio)
        Budget_WhiteLabel,  // Unbranded Chinese Androids
        Legacy_Android,     // Android 5-10
        Mid_Range,          // Modern mid-range (A54, Redmi Note 13)
        Modern_Flagship     // S24, Xiaomi 14
    }

    public class RecommendedStrategy
    {
        public StrategyType Type { get; set; }
        public string Reason { get; set; } = string.Empty;
        public RiskLevel Risk { get; set; }

        public RecommendedStrategy(StrategyType type, string reason, RiskLevel risk)
        {
            Type = type;
            Reason = reason;
            Risk = risk;
        }
    }

    public enum StrategyType
    {
        Miracle_Universal,
        Model_Specific,
        Hybrid_Optimized,
        Universal_Fallback
    }

    public enum RiskLevel
    {
        Safe,
        Low,
        Medium,
        High
    }

    public class DeviceClassifier
    {
        public DeviceCategory Classify(DeviceProfile device)
        {
            // 1. Keypad phones
            // Assuming we added a 'FormFactor' or similar property, or infer from chipset
            if (device.Chipset.Model.Contains("MT62") || device.Chipset.Model.Contains("SC65"))
                return DeviceCategory.Legacy_Keypad;

            // 2. Modern Flagship
            // Price > $600 or specific high-end chips
            if (device.Chipset.Model.Contains("Snapdragon 8 Gen") || device.Chipset.Model.Contains("Dimensity 9"))
                return DeviceCategory.Modern_Flagship;

            // 3. Legacy Android
            // In a real app we'd use release date. Here we approximate by checking "Legacy" in name or chipset.
            if (device.Chipset.Model.Contains("MT65") || device.Chipset.Model.Contains("MSM89"))
                return DeviceCategory.Legacy_Android;

            return DeviceCategory.Mid_Range; // Default
        }

        public RecommendedStrategy GetRecommendedStrategy(DeviceCategory category)
        {
            return category switch
            {
                DeviceCategory.Legacy_Keypad =>
                    new RecommendedStrategy(StrategyType.Miracle_Universal,
                        "Use MTK/SPD universal methods (BROM/Diag) for maximum compatibility.",
                        RiskLevel.Medium),

                DeviceCategory.Budget_WhiteLabel =>
                    new RecommendedStrategy(StrategyType.Miracle_Universal,
                        "Universal methods are most reliable for unbranded phones.",
                        RiskLevel.Medium),

                DeviceCategory.Modern_Flagship =>
                    new RecommendedStrategy(StrategyType.Model_Specific,
                        "High security device. Use exact model-specific profile to avoid tripping Knox/Bootloader checks.",
                        RiskLevel.Safe),

                DeviceCategory.Legacy_Android =>
                    new RecommendedStrategy(StrategyType.Miracle_Universal,
                        "Older Android security is vulnerable to generic BROM/EDL methods.",
                        RiskLevel.Low),

                _ => new RecommendedStrategy(StrategyType.Hybrid_Optimized,
                        "Try Model-Specific first, fall back to Universal if unavailable.",
                        RiskLevel.Low)
            };
        }
    }
}
