using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Features.Cloak.Models;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.Features.Cloak
{
    public class CloakSetupWizard
    {
        private readonly IAdbClient _adb;

        public CloakSetupWizard(IAdbClient adb)
        {
            _adb = adb;
        }

        public async Task<RootEnvironment> DetectEnvironmentAsync(CancellationToken ct = default)
        {
            var env = new RootEnvironment();
            
            // 1. Check su availability
            var suCheck = await _adb.ExecuteShellAsync("which su", ct);
            env.IsRooted = !string.IsNullOrEmpty(suCheck) && suCheck.Contains("/su");

            // 2. Identify Rooting Tool
            var packages = await _adb.ExecuteShellAsync("pm list packages", ct);
            
            if (packages.Contains("com.topjohnwu.magisk")) env.Tool = RootingTool.Magisk;
            else if (packages.Contains("io.github.huskydg.magisk")) env.Tool = RootingTool.KitsuneMask;
            // KSU/APatch don't always have a package unless Manager is installed
            
            // 3. Check Zygisk
            var zygiskCheck = await _adb.ExecuteShellAsync("getprop | grep zygisk", ct);
            env.ZygiskEnabled = zygiskCheck.Contains("1") || zygiskCheck.Contains("running");

            // 4. List Modules (requires root)
            if (env.IsRooted)
            {
                var modules = await _adb.ExecuteShellAsync("ls /data/adb/modules", ct);
                if (!modules.Contains("Permission denied"))
                {
                    env.InstalledModules = modules.Split('\n', StringSplitOptions.RemoveEmptyEntries)
                        .Select(m => m.Trim()).ToList();
                }
            }

            return env;
        }

        public string GenerateSetupGuide(RootEnvironment env, List<BankingAppProfile> targetApps)
        {
            var sb = new StringBuilder();
            sb.AppendLine("🚀 DEEPEYE CLOAK SETUP WIZARD - 2025 GOLDEN STANDARD");
            sb.AppendLine("======================================================");
            sb.AppendLine();

            sb.AppendLine($"[+] Detected Environment: {env.Tool} (Rooted: {env.IsRooted})");
            sb.AppendLine($"[+] Zygisk Status: {(env.ZygiskEnabled ? "ENABLED" : "DISABLED")}");
            sb.AppendLine();

            int step = 1;

            // Step 1: Tool selection
            if (env.Tool == RootingTool.None)
            {
                sb.AppendLine($"{step++}. RECOMMENDATION: Use KernelSU or APatch for best stealth on GKI devices.");
            }

            // Step 2: Zygisk
            if (!env.ZygiskEnabled)
            {
                sb.AppendLine($"{step++}. ACTION: Enable Zygisk in your root manager settings.");
            }

            // Step 3: Modules
            var missingModules = new List<string>();
            if (!env.InstalledModules.Any(m => m.ToLower().Contains("shamiko"))) missingModules.Add("Shamiko");
            if (!env.InstalledModules.Any(m => m.ToLower().Contains("playintegrity"))) missingModules.Add("Play Integrity Fix");

            if (missingModules.Any())
            {
                sb.AppendLine($"{step++}. INSTALL MODULES:");
                foreach (var mod in missingModules)
                {
                    var registryMod = CloakProfiles.ModuleRegistry.FirstOrDefault(m => m.Name == mod);
                    sb.AppendLine($"   - {mod}: {registryMod?.DownloadUrl}");
                }
            }

            // Step 4: App specific
            if (targetApps.Any())
            {
                sb.AppendLine($"{step++}. CONFIGURE DENYLIST FOR:");
                foreach (var app in targetApps)
                {
                    sb.AppendLine($"   - {app.AppName} ({app.PackageName})");
                    if (app.RequiresHMA)
                    {
                        sb.AppendLine("     * NOTE: This app requires 'Hide My Applist' + LSPosed for full stealth.");
                    }
                }
            }

            sb.AppendLine();
            sb.AppendLine("⚠️ IMPORTANT: Do NOT enable 'Enforce DenyList' in Magisk if using Shamiko.");
            sb.AppendLine("🛠️ FINAL ACTION: Clear data for Play Store & Play Services and REBOOT.");

            return sb.ToString();
        }
    }
}
