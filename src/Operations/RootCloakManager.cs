using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Operations
{
    /// <summary>
    /// Manages root detection bypass (Magisk, Zygisk, Shamiko) diagnostics and configuration
    /// </summary>
    public class RootCloakManager
    {
        private const string MagiskDataPath = "/data/adb";
        private const string ModulesPath = "/data/adb/modules";

        // Known Magisk package patterns
        private static readonly string[] MagiskPackages = new[]
        {
            "com.topjohnwu.magisk",
            "io.github.huskydg.magisk", // Magisk Delta
            "com.topjohnwu.magisk.zy",  // Repackaged variants
        };

        // Known root-related packages
        private static readonly string[] RootIndicatorPackages = new[]
        {
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.noshufou.android.su",
            "me.phh.superuser",
            "com.kingroot.kinguser",
            "de.robv.android.xposed.installer",
            "org.lsposed.manager"
        };

        #region Inspection

        public async Task<RootCloakStatus> InspectAsync(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress = null,
            CancellationToken ct = default)
        {
            var status = new RootCloakStatus();
            progress?.Report(ProgressUpdate.Info(5, "Scanning for root environment..."));

            if (device.Mode != ConnectionMode.ADB)
            {
                status.Issues.Add("Device must be in ADB mode for root inspection");
                return status;
            }

            // Check for Magisk
            progress?.Report(ProgressUpdate.Info(15, "Checking for Magisk..."));
            await DetectMagisk(status, ct);

            // Check Zygisk status
            progress?.Report(ProgressUpdate.Info(30, "Checking Zygisk status..."));
            await DetectZygisk(status, ct);

            // Check installed modules
            progress?.Report(ProgressUpdate.Info(45, "Scanning installed modules..."));
            await DetectModules(status, ct);

            // Check DenyList
            progress?.Report(ProgressUpdate.Info(60, "Checking DenyList configuration..."));
            await DetectDenyList(status, ct);

            // Check basic root indicators
            progress?.Report(ProgressUpdate.Info(75, "Checking root indicators..."));
            await DetectRootIndicators(status, ct);

            // Analyze and generate recommendations
            progress?.Report(ProgressUpdate.Info(90, "Analyzing configuration..."));
            AnalyzeStatus(status);

            progress?.Report(ProgressUpdate.Info(100, $"Root cloak scan complete: {status.Readiness}"));
            return status;
        }

        private async Task DetectMagisk(RootCloakStatus status, CancellationToken ct)
        {
            // Check for Magisk package
            var packages = await RunAdb("shell pm list packages", ct);
            foreach (var pkg in MagiskPackages)
            {
                if (packages.Contains(pkg))
                {
                    status.HasMagisk = true;
                    status.MagiskPackage = pkg;
                    break;
                }
            }

            // Check for randomized/hidden Magisk package
            if (!status.HasMagisk)
            {
                // Look for stub-like packages with Magisk characteristics
                var stubCheck = await RunAdb("shell pm list packages | grep -E '^package:[a-z]{8,12}$'", ct);
                // This is heuristic - may have false positives
            }

            // Try to get Magisk version via su
            try
            {
                var version = await RunAdb("shell su -c 'magisk -v'", ct);
                if (!string.IsNullOrEmpty(version) && !version.Contains("not found"))
                {
                    status.HasMagisk = true;
                    status.IsRooted = true;
                    status.MagiskVersion = version.Trim();
                }
            }
            catch { /* su not available or denied */ }

            // Check /data/adb existence (requires root)
            try
            {
                var adbPath = await RunAdb($"shell su -c 'ls {MagiskDataPath}'", ct);
                if (adbPath.Contains("magisk"))
                {
                    status.HasMagisk = true;
                    status.IsRooted = true;
                }
            }
            catch { }
        }

        private async Task DetectZygisk(RootCloakStatus status, CancellationToken ct)
        {
            if (!status.HasMagisk) return;

            try
            {
                // Check Zygisk enabled flag
                var zygiskCheck = await RunAdb(
                    "shell su -c 'cat /data/adb/magisk/zygisk_enabled 2>/dev/null || echo 0'", ct);
                status.ZygiskEnabled = zygiskCheck.Trim() == "1";

                // Check if Zygisk is supported (presence of lib)
                var libCheck = await RunAdb(
                    "shell su -c 'ls /data/adb/magisk/zygisk 2>/dev/null'", ct);
                status.ZygiskSupported = !string.IsNullOrEmpty(libCheck) && !libCheck.Contains("No such file");
            }
            catch { }
        }

        private async Task DetectModules(RootCloakStatus status, CancellationToken ct)
        {
            if (!status.IsRooted) return;

            try
            {
                var modules = await RunAdb($"shell su -c 'ls {ModulesPath}'", ct);
                if (!string.IsNullOrEmpty(modules))
                {
                    var moduleList = modules.Split('\n', StringSplitOptions.RemoveEmptyEntries)
                        .Select(m => m.Trim())
                        .Where(m => !string.IsNullOrEmpty(m))
                        .ToList();

                    status.InstalledModules = moduleList;

                    // Check for specific important modules
                    status.ShamikoInstalled = moduleList.Any(m => 
                        m.Contains("shamiko", StringComparison.OrdinalIgnoreCase) ||
                        m.Contains("zygisk-assistant", StringComparison.OrdinalIgnoreCase));

                    status.PlayIntegrityFixInstalled = moduleList.Any(m =>
                        m.Contains("playintegrityfix", StringComparison.OrdinalIgnoreCase) ||
                        m.Contains("play-integrity", StringComparison.OrdinalIgnoreCase) ||
                        m.Contains("safetynet-fix", StringComparison.OrdinalIgnoreCase));

                    status.PropsConfigInstalled = moduleList.Any(m =>
                        m.Contains("MagiskHidePropsConf", StringComparison.OrdinalIgnoreCase) ||
                        m.Contains("props", StringComparison.OrdinalIgnoreCase));
                }
            }
            catch { }

            // Check if Shamiko is active
            if (status.ShamikoInstalled)
            {
                try
                {
                    var shamikoDisable = await RunAdb(
                        "shell su -c 'cat /data/adb/modules/zygisk_shamiko/disable 2>/dev/null'", ct);
                    status.ShamikoActive = string.IsNullOrEmpty(shamikoDisable) || shamikoDisable.Contains("No such file");
                }
                catch { status.ShamikoActive = status.ShamikoInstalled; }
            }
        }

        private async Task DetectDenyList(RootCloakStatus status, CancellationToken ct)
        {
            if (!status.HasMagisk) return;

            try
            {
                // Check if Enforce DenyList is enabled
                var enforce = await RunAdb(
                    "shell su -c 'magisk --denylist status 2>/dev/null'", ct);
                status.EnforceDenyListEnabled = enforce.Contains("enabled") || enforce.Trim() == "1";

                // Get DenyList packages
                var denyList = await RunAdb(
                    "shell su -c 'magisk --denylist ls 2>/dev/null'", ct);
                if (!string.IsNullOrEmpty(denyList) && !denyList.Contains("error"))
                {
                    status.DenyListedPackages = denyList
                        .Split('\n', StringSplitOptions.RemoveEmptyEntries)
                        .Select(p => p.Trim().Split('|')[0]) // Format: package|process
                        .Where(p => !string.IsNullOrEmpty(p))
                        .Distinct()
                        .ToList();

                    status.DenyListConfigured = status.DenyListedPackages.Count > 0;
                }
            }
            catch { }
        }

        private async Task DetectRootIndicators(RootCloakStatus status, CancellationToken ct)
        {
            // Check for root packages
            var packages = await RunAdb("shell pm list packages", ct);
            foreach (var pkg in RootIndicatorPackages)
            {
                if (packages.Contains(pkg))
                {
                    status.IsRooted = true;
                    break;
                }
            }

            // Check su binary accessibility
            try
            {
                var suCheck = await RunAdb("shell which su", ct);
                if (!string.IsNullOrEmpty(suCheck) && suCheck.Contains("/su"))
                {
                    status.IsRooted = true;
                }
            }
            catch { }
        }

        private void AnalyzeStatus(RootCloakStatus status)
        {
            // Determine readiness level
            if (!status.IsRooted && !status.HasMagisk)
            {
                status.Readiness = CloakReadiness.NotRooted;
                return;
            }

            // Check for optimal setup
            if (status.HasMagisk && 
                status.ZygiskEnabled && 
                status.ShamikoInstalled && 
                status.ShamikoActive &&
                !status.EnforceDenyListEnabled &&
                status.DenyListConfigured &&
                status.PlayIntegrityFixInstalled)
            {
                status.Readiness = CloakReadiness.OptimalSetup;
                return;
            }

            // Check for well-hidden setup
            if (status.HasMagisk && status.ZygiskEnabled && 
                (status.ShamikoInstalled || !status.EnforceDenyListEnabled))
            {
                status.Readiness = CloakReadiness.WellHidden;
            }
            else if (status.HasMagisk)
            {
                status.Readiness = CloakReadiness.PartiallyHidden;
            }
            else
            {
                status.Readiness = CloakReadiness.RootExposed;
            }

            // Generate issues and recommendations
            GenerateRecommendations(status);
        }

        private void GenerateRecommendations(RootCloakStatus status)
        {
            if (!status.ZygiskEnabled && status.ZygiskSupported)
            {
                status.Issues.Add("Zygisk is not enabled");
                status.Recommendations.Add("Enable Zygisk in Magisk settings");
            }

            if (!status.ShamikoInstalled)
            {
                status.Issues.Add("Shamiko module not installed");
                status.Recommendations.Add("Install Shamiko from: github.com/LSPosed/LSPosed.github.io/releases");
            }

            if (status.ShamikoInstalled && !status.ShamikoActive)
            {
                status.Issues.Add("Shamiko is installed but disabled");
                status.Recommendations.Add("Enable Shamiko module in Magisk");
            }

            if (status.EnforceDenyListEnabled)
            {
                status.Issues.Add("Enforce DenyList is ON (should be OFF for Shamiko)");
                status.Recommendations.Add("Disable 'Enforce DenyList' in Magisk settings - Shamiko handles hiding");
            }

            if (!status.DenyListConfigured)
            {
                status.Issues.Add("DenyList has no apps configured");
                status.Recommendations.Add("Add sensitive apps to DenyList via Magisk → Configure DenyList");
            }

            if (!status.PlayIntegrityFixInstalled)
            {
                status.Recommendations.Add("Consider installing Play Integrity Fix module for banking apps");
            }

            if (status.MagiskPackage == "com.topjohnwu.magisk")
            {
                status.Issues.Add("Magisk app is using default package name (detectable)");
                status.Recommendations.Add("Hide Magisk app: Settings → Hide the Magisk app");
            }
        }

        #endregion

        #region Profile Management

        public CloakProfile GetProfile(ProfileType type)
        {
            return type switch
            {
                ProfileType.Banking => CloakProfiles.Banking,
                ProfileType.Gaming => CloakProfiles.Gaming,
                ProfileType.Streaming => CloakProfiles.Streaming,
                ProfileType.Enterprise => CloakProfiles.Enterprise,
                _ => new CloakProfile { Name = "Custom", Type = ProfileType.Custom }
            };
        }

        public async Task<bool> ApplyDenyListProfile(
            CloakProfile profile,
            IProgress<ProgressUpdate>? progress = null,
            CancellationToken ct = default)
        {
            progress?.Report(ProgressUpdate.Info(10, $"Applying {profile.Name} profile..."));

            foreach (var pkg in profile.TargetPackages)
            {
                try
                {
                    await RunAdb($"shell su -c 'magisk --denylist add {pkg}'", ct);
                    progress?.Report(ProgressUpdate.Info(50, $"Added: {pkg}"));
                }
                catch { }
            }

            progress?.Report(ProgressUpdate.Info(100, "Profile applied"));
            return true;
        }

        public string GenerateSetupInstructions(RootCloakStatus status)
        {
            var sb = new StringBuilder();
            sb.AppendLine("═══════════════════════════════════════════════════════════════");
            sb.AppendLine("                 MAGISK ROOT HIDING SETUP GUIDE                 ");
            sb.AppendLine("═══════════════════════════════════════════════════════════════");
            sb.AppendLine();

            if (!status.HasMagisk)
            {
                sb.AppendLine("❌ Magisk not detected. Install Magisk first.");
                return sb.ToString();
            }

            int step = 1;

            if (!status.ZygiskEnabled)
            {
                sb.AppendLine($"STEP {step++}: Enable Zygisk");
                sb.AppendLine("   1. Open Magisk app");
                sb.AppendLine("   2. Go to Settings (⚙️)");
                sb.AppendLine("   3. Enable 'Zygisk'");
                sb.AppendLine("   4. Reboot device");
                sb.AppendLine();
            }

            if (status.EnforceDenyListEnabled)
            {
                sb.AppendLine($"STEP {step++}: Disable Enforce DenyList");
                sb.AppendLine("   1. Open Magisk app");
                sb.AppendLine("   2. Go to Settings (⚙️)");
                sb.AppendLine("   3. Disable 'Enforce DenyList'");
                sb.AppendLine("   (Shamiko will handle hiding instead)");
                sb.AppendLine();
            }

            if (!status.ShamikoInstalled)
            {
                sb.AppendLine($"STEP {step++}: Install Shamiko");
                sb.AppendLine("   1. Download Shamiko from:");
                sb.AppendLine("      github.com/LSPosed/LSPosed.github.io/releases");
                sb.AppendLine("   2. Open Magisk → Modules");
                sb.AppendLine("   3. Install from storage → select Shamiko.zip");
                sb.AppendLine("   4. Reboot device");
                sb.AppendLine();
            }

            if (!status.PlayIntegrityFixInstalled)
            {
                sb.AppendLine($"STEP {step++}: Install Play Integrity Fix (Recommended)");
                sb.AppendLine("   1. Download from:");
                sb.AppendLine("      github.com/chiteroman/PlayIntegrityFix/releases");
                sb.AppendLine("   2. Install via Magisk → Modules");
                sb.AppendLine("   3. Reboot device");
                sb.AppendLine();
            }

            sb.AppendLine($"STEP {step++}: Configure DenyList");
            sb.AppendLine("   1. Open Magisk → Configure DenyList");
            sb.AppendLine("   2. Add these apps (tap to expand, check all processes):");
            sb.AppendLine("      • Google Play Services (com.google.android.gms)");
            sb.AppendLine("      • Google Play Store");
            sb.AppendLine("      • Your banking/sensitive apps");
            sb.AppendLine();

            if (status.MagiskPackage == "com.topjohnwu.magisk")
            {
                sb.AppendLine($"STEP {step++}: Hide Magisk App");
                sb.AppendLine("   1. Open Magisk → Settings");
                sb.AppendLine("   2. Tap 'Hide the Magisk app'");
                sb.AppendLine("   3. Enter a random name");
                sb.AppendLine();
            }

            sb.AppendLine("═══════════════════════════════════════════════════════════════");
            sb.AppendLine("After completing all steps, verify with a Play Integrity check app.");
            sb.AppendLine("═══════════════════════════════════════════════════════════════");

            return sb.ToString();
        }

        #endregion

        #region Export

        public async Task<string> ExportStatusAsync(
            RootCloakStatus status,
            string outputPath,
            CancellationToken ct = default)
        {
            var json = JsonSerializer.Serialize(status, new JsonSerializerOptions { WriteIndented = true });
            await File.WriteAllTextAsync(outputPath, json, ct);
            return outputPath;
        }

        #endregion

        #region Helpers

        private async Task<string> RunAdb(string args, CancellationToken ct)
        {
            var psi = new ProcessStartInfo("adb", args)
            {
                RedirectStandardOutput = true,
                RedirectStandardError = true,
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
}
