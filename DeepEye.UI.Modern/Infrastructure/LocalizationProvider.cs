using System;
using System.Collections.Generic;

namespace DeepEye.UI.Modern.Infrastructure
{
    public static class LocalizationProvider
    {
        public enum Language { English, Hindi }
        public static Language CurrentLanguage { get; set; } = Language.English;

        private static readonly Dictionary<string, string> HindiDictionary = new()
        {
            { "EXPERT WORKFLOW ENGINE", "एक्स्पर्ट वर्कफ़्लो इंजन" },
            { "Locked - Expert Mode Required", "लॉक - एक्स्पर्ट मोड आवश्यक है" },
            { "Ready - Proceed with caution", "तैयार - सावधानी के साथ आगे बढ़ें" },
            { "Preset Loaded", "प्रिसेट लोड हो गया" },
            { "Workflow Completed Successfully.", "वर्कफ़्लो सफलतापूर्वक पूरा हुआ।" },
            { "Workflow Failed or Aborted.", "वर्कफ़्लो विफल या रद्द हुआ।" },
            { "Critical Engine Error", "गंभीर इंजन त्रुटि" },
            { "Cancellation requested...", "निरस्तीकरण का अनुरोध किया गया..." },
            { "Workflow saved as preset", "वर्कफ़्लो प्रिसेट के रूप में सहेजा गया" },
            { "Safe FRP Wipe (Qualcomm)", "सुरक्षित FRP वाइप (क्वालकॉम)" },
            { "Full Health & Audit", "पूर्ण स्वास्थ्य और ऑडिट" },
            { "EXECUTE", "निष्पादित करें" },
            { "STOP", "रोकें" },
            { "ADD STEP", "चरण जोड़ें" }
        };

        public static string T(string key)
        {
            if (CurrentLanguage == Language.Hindi && HindiDictionary.TryGetValue(key, out var translated))
            {
                return translated;
            }
            return key;
        }
    }
}
