using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Infrastructure.Logging
{
    public static class ErrorHintEngine
    {
        private static readonly Dictionary<string, string> Hints = new()
        {
            { "0x00000005", "ACCESS DENIED: Try running as Administrator or check if another tool (Odin/MTKClient) is holding the port." },
            { "Sahara Timeout", "CONNECTION TIMEOUT: Check your cable or try a USB 2.0 port. Some USB 3.0 controllers are incompatible with EDL." },
            { "BROM Handshake Failed", "HANDSHAKE ERROR: Device failed to enter BROM mode. Try a different USB cable or check test points." },
            { "Code 28", "DRIVER MISSING: Device drivers (MTP/EDL) are not installed. Go to 'Driver Center' to fix." },
            { "Code 10", "DRIVER CONFLICT: Another device driver is conflicting. Try uninstalling existing drivers in Device Manager." }
        };

        public static string GetHint(string errorMessage)
        {
            foreach (var hint in Hints)
            {
                if (errorMessage.Contains(hint.Key, StringComparison.OrdinalIgnoreCase))
                {
                    return hint.Value;
                }
            }
            return "UNKNOWN ERROR: Please check the verbose logs for more details.";
        }

        public static string GetHint(Exception ex) => GetHint(ex.Message);
    }
}
