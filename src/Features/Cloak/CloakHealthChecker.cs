using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.Features.Cloak
{
    public class TrapDetectionResult
    {
        public string TrapName { get; set; } = string.Empty;
        public bool Detected { get; set; }
        public string Description { get; set; } = string.Empty;
        public string FixSuggestion { get; set; } = string.Empty;
        public string Severity { get; set; } = "Low"; // Low, Medium, High
    }

    public class CloakHealthChecker
    {
        private readonly IAdbClient _adb;

        public CloakHealthChecker(IAdbClient adb)
        {
            _adb = adb;
        }

        public async Task<List<TrapDetectionResult>> RunFullHealthCheckAsync(CancellationToken ct = default)
        {
            var results = new List<TrapDetectionResult>();

            // 1. Developer Options
            results.Add(await CheckDeveloperOptions(ct));

            // 2. USB Debugging
            results.Add(await CheckUsbDebugging(ct));

            // 3. Accessibility Services
            results.Add(await CheckAccessibilityServices(ct));

            // 4. ADB Shell Root Visibility
            results.Add(await CheckAdbRootVisibility(ct));

            // 5. Build Props (Safe Check)
            results.Add(await CheckBuildProps(ct));

            return results;
        }

        private async Task<TrapDetectionResult> CheckDeveloperOptions(CancellationToken ct)
        {
            // Settings.Secure.development_settings_enabled
            var output = await _adb.ExecuteShellAsync("settings get global development_settings_enabled", ct);
            bool isEnabled = output.Trim() == "1";

            return new TrapDetectionResult
            {
                TrapName = "Developer Options",
                Detected = isEnabled,
                Severity = "High",
                Description = "Developer options are enabled. Many banking apps check this flag directly.",
                FixSuggestion = "Turn OFF 'Developer Options' in System Settings."
            };
        }

        private async Task<TrapDetectionResult> CheckUsbDebugging(CancellationToken ct)
        {
            var output = await _adb.ExecuteShellAsync("settings get global adb_enabled", ct);
            bool isEnabled = output.Trim() == "1";

            return new TrapDetectionResult
            {
                TrapName = "USB Debugging",
                Detected = isEnabled,
                Severity = "Medium",
                Description = "USB Debugging is active. This is a common detection vector.",
                FixSuggestion = "Disable 'USB Debugging' in Settings after setup is complete."
            };
        }

        private async Task<TrapDetectionResult> CheckAccessibilityServices(CancellationToken ct)
        {
            var output = await _adb.ExecuteShellAsync("settings get secure enabled_accessibility_services", ct);
            bool hasServices = !string.IsNullOrWhiteSpace(output) && output.Trim() != "null";

            return new TrapDetectionResult
            {
                TrapName = "Accessibility Services",
                Detected = hasServices,
                Severity = "High",
                Description = "Active accessibility services detected. Banking apps block these to prevent overlay attacks.",
                FixSuggestion = "Disable all non-essential Accessibility services."
            };
        }

        private async Task<TrapDetectionResult> CheckAdbRootVisibility(CancellationToken ct)
        {
            var output = await _adb.ExecuteShellAsync("id", ct);
            bool isRoot = output.Contains("uid=0(root)");

            return new TrapDetectionResult
            {
                TrapName = "ADB Shell Root",
                Detected = isRoot,
                Severity = "High",
                Description = "ADB shell is running as root. This indicates an extremely exposed environment.",
                FixSuggestion = "Ensure ADB root is disabled in your rooting tool when not in use."
            };
        }

        private async Task<TrapDetectionResult> CheckBuildProps(CancellationToken ct)
        {
            var debuggable = await _adb.ExecuteShellAsync("getprop ro.debuggable", ct);
            bool isDebuggable = debuggable.Trim() == "1";

            return new TrapDetectionResult
            {
                TrapName = "Debuggable Kernel",
                Detected = isDebuggable,
                Severity = "High",
                Description = "System is marked as debuggable (ro.debuggable=1).",
                FixSuggestion = "Use Cloak Stealth Mode to spoof this property."
            };
        }
    }
}
