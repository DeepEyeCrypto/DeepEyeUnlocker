using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Helpers
{
    public static class LocalizationManager
    {
        public enum Language { English, Hindi }
        public static Language CurrentLanguage { get; set; } = Language.English;

        private static readonly Dictionary<string, string> EnStrings = new()
        {
            { "AppTitle", "DeepEyeUnlocker v1.0 - Professional Mobile Repair" },
            { "HeaderTitle", "🔷 DEEPEYE UNLOCKER" },
            { "TargetDevice", "Target Device:" },
            { "Refresh", "Refresh" },
            { "Format", "Format" },
            { "FrpBypass", "FRP Bypass" },
            { "PatternClear", "Pattern Clear" },
            { "Backup", "Backup" },
            { "Flash", "Flash" },
            { "Bootloader", "Bootloader" },
            { "DeviceInfo", "Device Info" },
            { "Scanning", "Scanning for devices..." },
            { "Ready", "Ready for operation..." },
            { "NoDevice", "No supported devices found." },
            { "OperationStarted", "Starting operation:" },
            { "OperationFinished", "Operation finished." }
        };

        private static readonly Dictionary<string, string> HiStrings = new()
        {
            { "AppTitle", "DeepEyeUnlocker v1.0 - प्रोफेशनल मोबाइल रिपेयर" },
            { "HeaderTitle", "🔷 डीपआई अनलॉकर" },
            { "TargetDevice", "लक्ष्य डिवाइस:" },
            { "Refresh", "ताज़ा करें" },
            { "Format", "फॉर्मेट" },
            { "FrpBypass", "FRP बाईपास" },
            { "PatternClear", "पैटर्न साफ़ करें" },
            { "Backup", "बैकअप" },
            { "Flash", "फ्लैश" },
            { "Bootloader", "बूटलोडर" },
            { "DeviceInfo", "डिवाइस जानकारी" },
            { "Scanning", "डिवाइस की तलाश की जा रही है..." },
            { "Ready", "ऑपरेशन के लिए तैयार..." },
            { "NoDevice", "कोई समर्थित डिवाइस नहीं मिला।" },
            { "OperationStarted", "ऑपरेशन शुरू हो रहा है:" },
            { "OperationFinished", "ऑपरेशन पूरा हुआ।" }
        };

        public static string GetString(string key)
        {
            var dict = CurrentLanguage == Language.Hindi ? HiStrings : EnStrings;
            return dict.ContainsKey(key) ? dict[key] : key;
        }
    }
}
