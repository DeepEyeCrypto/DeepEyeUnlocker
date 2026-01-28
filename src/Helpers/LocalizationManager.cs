using System.Collections.Generic;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Helpers
{
    public static class LocalizationManager
    {
        public enum Language { English, Hindi }
        public static Language CurrentLanguage { get; set; } = Language.English;

        private static readonly Dictionary<string, Dictionary<Language, string>> Resources = new()
        {
            ["AppTitle"] = new() { [Language.English] = "DeepEyeUnlocker Pro v1.1", [Language.Hindi] = "डीपआई अनलॉकर प्रो v1.1" },
            ["Ready"] = new() { [Language.English] = "Ready for operation...", [Language.Hindi] = "ऑपरेशन के लिए तैयार..." },
            ["Scanning"] = new() { [Language.English] = "Scanning for devices...", [Language.Hindi] = "डिवाइस की खोज कर रहे हैं..." },
            ["NoDevice"] = new() { [Language.English] = "No device detected.", [Language.Hindi] = "कोई डिवाइस नहीं मिला।" },
            ["OperationStarted"] = new() { [Language.English] = "Operation started:", [Language.Hindi] = "ऑपरेशन शुरू हुआ:" },
            ["OperationFinished"] = new() { [Language.English] = "Completed!", [Language.Hindi] = "पूरा हुआ!" },
            ["Refresh"] = new() { [Language.English] = "Refresh", [Language.Hindi] = "रिफ्रेश" },
            ["HeaderTitle"] = new() { [Language.English] = "DEEPEYE DASHBOARD", [Language.Hindi] = "डीपआई डैशबोर्ड" }
        };

        public static string GetString(string key)
        {
            if (Resources.ContainsKey(key))
                return Resources[key][CurrentLanguage];
            return key;
        }
    }
}
