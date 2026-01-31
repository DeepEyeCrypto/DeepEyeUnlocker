using System.Collections.Generic;

namespace DeepEyeUnlocker.Protocols.MTK
{
    public static class MTKChipsetDatabase
    {
        private static readonly Dictionary<uint, string> Chipsets = new Dictionary<uint, string>
        {
            { 0x6261, "MT6261" },
            { 0x6572, "MT6572" },
            { 0x6582, "MT6582" },
            { 0x6735, "MT6735" },
            { 0x6737, "MT6737" },
            { 0x6739, "MT6739" },
            { 0x6750, "MT6750" },
            { 0x6755, "MT6755 (Helio P10)" },
            { 0x6761, "MT6761 (Helio A22)" },
            { 0x6763, "MT6763 (Helio P23)" },
            { 0x6765, "MT6765 (Helio P35)" },
            { 0x6768, "MT6768 (Helio G80/G85)" },
            { 0x6771, "MT6771 (Helio P60)" },
            { 0x6779, "MT6779 (Helio P90)" },
            { 0x6781, "MT6781 (Helio G96)" },
            { 0x6785, "MT6785 (Helio G90)" },
            { 0x6833, "MT6833 (Dimensity 700)" },
            { 0x6853, "MT6853 (Dimensity 720/800U)" },
            { 0x6873, "MT6873 (Dimensity 800)" },
            { 0x6877, "MT6877 (Dimensity 900)" },
            { 0x6885, "MT6885 (Dimensity 1000L)" },
            { 0x6893, "MT6893 (Dimensity 1200)" }
        };

        public static string GetName(uint hwCode)
        {
            if (Chipsets.TryGetValue(hwCode, out string name))
                return name;
            return $"MT{hwCode:X4} (Unknown)";
        }
    }
}
