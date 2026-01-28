using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Operations
{
    /// <summary>
    /// Manages Developer Options and USB Debugging detection bypass
    /// </summary>
    public class DevModeCloakManager
    {
        // Settings keys apps commonly check
        private const string KEY_DEV_OPTIONS = "development_settings_enabled";
        private const string KEY_ADB_ENABLED = "adb_enabled";
        private const string KEY_ADB_WIFI = "adb_wifi_enabled";
        private const string KEY_OEM_UNLOCK = "oem_unlock_enabled";

        // System properties
        private const string PROP_DEBUGGABLE = "ro.debuggable";
        private const string PROP_SECURE = "ro.secure";
        private const string PROP_USB_CONFIG = "persist.sys.usb.config";

        #region Inspection

        public async Task<DevModeStatus> InspectAsync(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress = null,
            CancellationToken ct = default)
        {
            var status = new DevModeStatus
            {
                AndroidVersion = device.AndroidVersion
            };

            if (device.Mode != ConnectionMode.ADB)
            {
                status.DetectionRisks.Add("Cannot inspect dev mode without ADB connection");
                return status;
            }

            progress?.Report(ProgressUpdate.Info(10, "Reading developer options status..."));

            // Check Settings.Secure
            status.DeveloperOptionsEnabled = await GetSecureSetting(KEY_DEV_OPTIONS, ct) == "1";

            // Check Settings.Global
            status.UsbDebuggingEnabled = await GetGlobalSetting(KEY_ADB_ENABLED, ct) == "1";
            status.WirelessDebuggingEnabled = await GetGlobalSetting(KEY_ADB_WIFI, ct) == "1";
            status.OemUnlockAllowed = await GetGlobalSetting(KEY_OEM_UNLOCK, ct) == "1";

            progress?.Report(ProgressUpdate.Info(50, "Reading system properties..."));

            // Check properties
            status.SystemDebuggable = await GetProp(PROP_DEBUGGABLE, ct) == "1";
            status.UsbConfig = await GetProp(PROP_USB_CONFIG, ct);

            progress?.Report(ProgressUpdate.Info(80, "Analyzing detection risks..."));

            // Analyze risks
            AnalyzeRisks(status);

            progress?.Report(ProgressUpdate.Info(100, "Dev mode inspection complete"));
            return status;
        }

        private void AnalyzeRisks(DevModeStatus status)
        {
            if (status.DeveloperOptionsEnabled)
            {
                status.DetectionRisks.Add("Developer Options is ENABLED - many apps detect this");
                status.Recommendations.Add("Consider hiding via hook or temporarily disabling");
            }

            if (status.UsbDebuggingEnabled)
            {
                status.DetectionRisks.Add("USB Debugging is ENABLED - banking apps often check this");
                status.Recommendations.Add("Disable before opening sensitive apps, or use hooks");
            }

            if (status.WirelessDebuggingEnabled)
            {
                status.DetectionRisks.Add("Wireless Debugging is ENABLED");
            }

            if (status.OemUnlockAllowed)
            {
                status.DetectionRisks.Add("OEM Unlock is ALLOWED - indicates unlocked bootloader");
                status.Recommendations.Add("Some apps check this; may need LSPosed hook to spoof");
            }

            if (status.SystemDebuggable)
            {
                status.DetectionRisks.Add("ro.debuggable=1 - system is marked as debuggable");
                status.Recommendations.Add("Use MagiskHide Props Config to spoof this property");
            }

            if (status.DetectionRisks.Count == 0)
            {
                status.Recommendations.Add("Dev mode appears hidden - good for sensitive apps");
            }
        }

        #endregion

        #region Stealth Operations

        /// <summary>
        /// Apply stealth profile to hide developer options (requires root or will warn about ADB loss)
        /// </summary>
        public async Task<StealthResult> ApplyStealthAsync(
            StealthProfile profile,
            bool hasRoot,
            IProgress<ProgressUpdate>? progress = null,
            CancellationToken ct = default)
        {
            var result = new StealthResult();

            progress?.Report(ProgressUpdate.Info(10, $"Applying stealth profile: {profile.Name}"));

            // Warning: disabling ADB will break connection
            if (profile.HideUsbDebugging && !hasRoot)
            {
                result.Warnings.Add("⚠️ Disabling ADB will disconnect this tool! Re-enable manually on device.");
            }

            try
            {
                // Apply settings overrides
                if (profile.HideDeveloperOptions)
                {
                    await SetSecureSetting(KEY_DEV_OPTIONS, "0", ct);
                    result.AppliedChanges.Add($"Set {KEY_DEV_OPTIONS}=0");
                }

                if (profile.HideUsbDebugging)
                {
                    await SetGlobalSetting(KEY_ADB_ENABLED, "0", ct);
                    result.AppliedChanges.Add($"Set {KEY_ADB_ENABLED}=0");
                }

                // Apply any custom overrides
                foreach (var (key, value) in profile.SettingsOverrides)
                {
                    await SetSecureSetting(key, value, ct);
                    result.AppliedChanges.Add($"Set {key}={value}");
                }

                result.Success = true;
                progress?.Report(ProgressUpdate.Info(100, "Stealth profile applied"));
            }
            catch (Exception ex)
            {
                result.Success = false;
                result.Error = ex.Message;
            }

            return result;
        }

        /// <summary>
        /// Restore normal developer mode settings
        /// </summary>
        public async Task<StealthResult> RestoreNormalAsync(
            IProgress<ProgressUpdate>? progress = null,
            CancellationToken ct = default)
        {
            var result = new StealthResult();

            progress?.Report(ProgressUpdate.Info(10, "Restoring normal dev mode settings..."));

            try
            {
                await SetSecureSetting(KEY_DEV_OPTIONS, "1", ct);
                result.AppliedChanges.Add($"Set {KEY_DEV_OPTIONS}=1");

                await SetGlobalSetting(KEY_ADB_ENABLED, "1", ct);
                result.AppliedChanges.Add($"Set {KEY_ADB_ENABLED}=1");

                result.Success = true;
                progress?.Report(ProgressUpdate.Info(100, "Normal settings restored"));
            }
            catch (Exception ex)
            {
                result.Success = false;
                result.Error = ex.Message;
            }

            return result;
        }

        #endregion

        #region Predefined Profiles

        public static StealthProfile GetBankingProfile()
        {
            return new StealthProfile
            {
                Name = "Banking Stealth",
                Description = "Hide developer options for banking apps",
                HideDeveloperOptions = true,
                HideUsbDebugging = false, // Don't break ADB
                SpoofDebuggableProp = true,
                SettingsOverrides = new Dictionary<string, string>
                {
                    ["mock_location"] = "0"
                }
            };
        }

        public static StealthProfile GetMaxStealthProfile()
        {
            return new StealthProfile
            {
                Name = "Maximum Stealth",
                Description = "Hide all developer indicators (will disconnect ADB!)",
                HideDeveloperOptions = true,
                HideUsbDebugging = true,
                SpoofDebuggableProp = true,
                SettingsOverrides = new Dictionary<string, string>
                {
                    ["mock_location"] = "0"
                }
            };
        }

        #endregion

        #region Hook Script Generation

        /// <summary>
        /// Generate LSPosed/Xposed hook script for dev mode hiding
        /// </summary>
        public string GenerateXposedHookScript(string[] targetPackages)
        {
            var sb = new StringBuilder();
            sb.AppendLine("// LSPosed/Xposed Module - Dev Mode Hider");
            sb.AppendLine("// Auto-generated by DeepEyeUnlocker");
            sb.AppendLine("// Install as LSPosed module and enable for target apps");
            sb.AppendLine();
            sb.AppendLine("package com.deepeye.devmodehider;");
            sb.AppendLine();
            sb.AppendLine("import de.robv.android.xposed.*;");
            sb.AppendLine("import de.robv.android.xposed.callbacks.XC_LoadPackage;");
            sb.AppendLine("import android.content.ContentResolver;");
            sb.AppendLine();
            sb.AppendLine("public class DevModeHider implements IXposedHookLoadPackage {");
            sb.AppendLine();
            sb.AppendLine("    private static final String[] TARGET_PACKAGES = {");
            foreach (var pkg in targetPackages)
            {
                sb.AppendLine($"        \"{pkg}\",");
            }
            sb.AppendLine("    };");
            sb.AppendLine();
            sb.AppendLine("    @Override");
            sb.AppendLine("    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {");
            sb.AppendLine("        boolean isTarget = false;");
            sb.AppendLine("        for (String pkg : TARGET_PACKAGES) {");
            sb.AppendLine("            if (lpparam.packageName.equals(pkg)) { isTarget = true; break; }");
            sb.AppendLine("        }");
            sb.AppendLine("        if (!isTarget) return;");
            sb.AppendLine();
            sb.AppendLine("        // Hook Settings.Secure.getInt");
            sb.AppendLine("        XposedHelpers.findAndHookMethod(");
            sb.AppendLine("            \"android.provider.Settings$Secure\",");
            sb.AppendLine("            lpparam.classLoader,");
            sb.AppendLine("            \"getInt\",");
            sb.AppendLine("            ContentResolver.class, String.class, int.class,");
            sb.AppendLine("            new XC_MethodHook() {");
            sb.AppendLine("                @Override");
            sb.AppendLine("                protected void beforeHookedMethod(MethodHookParam param) {");
            sb.AppendLine("                    String key = (String) param.args[1];");
            sb.AppendLine("                    if (\"development_settings_enabled\".equals(key)) {");
            sb.AppendLine("                        param.setResult(0); // Always return OFF");
            sb.AppendLine("                    }");
            sb.AppendLine("                }");
            sb.AppendLine("            });");
            sb.AppendLine();
            sb.AppendLine("        // Hook Settings.Global.getInt");
            sb.AppendLine("        XposedHelpers.findAndHookMethod(");
            sb.AppendLine("            \"android.provider.Settings$Global\",");
            sb.AppendLine("            lpparam.classLoader,");
            sb.AppendLine("            \"getInt\",");
            sb.AppendLine("            ContentResolver.class, String.class, int.class,");
            sb.AppendLine("            new XC_MethodHook() {");
            sb.AppendLine("                @Override");
            sb.AppendLine("                protected void beforeHookedMethod(MethodHookParam param) {");
            sb.AppendLine("                    String key = (String) param.args[1];");
            sb.AppendLine("                    if (\"adb_enabled\".equals(key) ||");
            sb.AppendLine("                        \"adb_wifi_enabled\".equals(key)) {");
            sb.AppendLine("                        param.setResult(0);");
            sb.AppendLine("                    }");
            sb.AppendLine("                }");
            sb.AppendLine("            });");
            sb.AppendLine();
            sb.AppendLine("        // Hook SystemProperties.get for ro.debuggable");
            sb.AppendLine("        XposedHelpers.findAndHookMethod(");
            sb.AppendLine("            \"android.os.SystemProperties\",");
            sb.AppendLine("            lpparam.classLoader,");
            sb.AppendLine("            \"get\", String.class,");
            sb.AppendLine("            new XC_MethodHook() {");
            sb.AppendLine("                @Override");
            sb.AppendLine("                protected void afterHookedMethod(MethodHookParam param) {");
            sb.AppendLine("                    String key = (String) param.args[0];");
            sb.AppendLine("                    if (\"ro.debuggable\".equals(key)) {");
            sb.AppendLine("                        param.setResult(\"0\");");
            sb.AppendLine("                    }");
            sb.AppendLine("                }");
            sb.AppendLine("            });");
            sb.AppendLine("    }");
            sb.AppendLine("}");

            return sb.ToString();
        }

        /// <summary>
        /// Generate Frida script for runtime dev mode hiding
        /// </summary>
        public string GenerateFridaScript()
        {
            var sb = new StringBuilder();
            sb.AppendLine("// Frida Script - Dev Mode Hider");
            sb.AppendLine("// Usage: frida -U -f <package> -l devmode_hider.js");
            sb.AppendLine("// Auto-generated by DeepEyeUnlocker");
            sb.AppendLine();
            sb.AppendLine("Java.perform(function() {");
            sb.AppendLine("    console.log('[*] Dev Mode Hider loaded');");
            sb.AppendLine();
            sb.AppendLine("    // Hook Settings.Secure.getInt");
            sb.AppendLine("    var Secure = Java.use('android.provider.Settings$Secure');");
            sb.AppendLine("    Secure.getInt.overload('android.content.ContentResolver', 'java.lang.String', 'int').implementation = function(resolver, name, def) {");
            sb.AppendLine("        if (name === 'development_settings_enabled') {");
            sb.AppendLine("            console.log('[+] Hiding development_settings_enabled');");
            sb.AppendLine("            return 0;");
            sb.AppendLine("        }");
            sb.AppendLine("        return this.getInt(resolver, name, def);");
            sb.AppendLine("    };");
            sb.AppendLine();
            sb.AppendLine("    // Hook Settings.Global.getInt");
            sb.AppendLine("    var Global = Java.use('android.provider.Settings$Global');");
            sb.AppendLine("    Global.getInt.overload('android.content.ContentResolver', 'java.lang.String', 'int').implementation = function(resolver, name, def) {");
            sb.AppendLine("        if (name === 'adb_enabled' || name === 'adb_wifi_enabled') {");
            sb.AppendLine("            console.log('[+] Hiding ' + name);");
            sb.AppendLine("            return 0;");
            sb.AppendLine("        }");
            sb.AppendLine("        return this.getInt(resolver, name, def);");
            sb.AppendLine("    };");
            sb.AppendLine();
            sb.AppendLine("    // Hook SystemProperties.get");
            sb.AppendLine("    var SystemProperties = Java.use('android.os.SystemProperties');");
            sb.AppendLine("    SystemProperties.get.overload('java.lang.String').implementation = function(key) {");
            sb.AppendLine("        if (key === 'ro.debuggable') {");
            sb.AppendLine("            console.log('[+] Hiding ro.debuggable');");
            sb.AppendLine("            return '0';");
            sb.AppendLine("        }");
            sb.AppendLine("        return this.get(key);");
            sb.AppendLine("    };");
            sb.AppendLine();
            sb.AppendLine("    console.log('[*] All hooks installed');");
            sb.AppendLine("});");

            return sb.ToString();
        }

        #endregion

        #region Export

        public async Task<string> ExportStatusAsync(
            DevModeStatus status,
            string outputPath,
            CancellationToken ct = default)
        {
            var json = JsonSerializer.Serialize(status, new JsonSerializerOptions { WriteIndented = true });
            await File.WriteAllTextAsync(outputPath, json, ct);
            return outputPath;
        }

        public async Task SaveHookScript(
            string script,
            string outputPath,
            CancellationToken ct = default)
        {
            await File.WriteAllTextAsync(outputPath, script, ct);
        }

        #endregion

        #region Helpers

        private async Task<string> GetSecureSetting(string key, CancellationToken ct)
        {
            return (await RunAdb($"shell settings get secure {key}", ct)).Trim();
        }

        private async Task<string> GetGlobalSetting(string key, CancellationToken ct)
        {
            return (await RunAdb($"shell settings get global {key}", ct)).Trim();
        }

        private async Task<string> GetProp(string prop, CancellationToken ct)
        {
            return (await RunAdb($"shell getprop {prop}", ct)).Trim();
        }

        private async Task SetSecureSetting(string key, string value, CancellationToken ct)
        {
            await RunAdb($"shell settings put secure {key} {value}", ct);
        }

        private async Task SetGlobalSetting(string key, string value, CancellationToken ct)
        {
            await RunAdb($"shell settings put global {key} {value}", ct);
        }

        private async Task<string> RunAdb(string args, CancellationToken ct)
        {
            var psi = new ProcessStartInfo("adb", args)
            {
                RedirectStandardOutput = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };

            using var proc = Process.Start(psi);
            if (proc == null) return "";

            var output = await proc.StandardOutput.ReadToEndAsync(ct);
            await proc.WaitForExitAsync(ct);
            return output;
        }

        #endregion
    }

    public class StealthResult
    {
        public bool Success { get; set; }
        public string? Error { get; set; }
        public List<string> AppliedChanges { get; set; } = new();
        public List<string> Warnings { get; set; } = new();
    }
}
