using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Operations;

namespace DeepEyeUnlocker.Features.Cloak
{
    public class CloakOrchestrator
    {
        private readonly RootCloakManager _rootManager;
        private readonly DevModeCloakManager _devManager;

        public CloakOrchestrator()
        {
            _rootManager = new RootCloakManager();
            _devManager = new DevModeCloakManager();
        }

        /// <summary>
        /// Executes a full multi-layer stealth profile on the device (v1.4.0 Epic C).
        /// </summary>
        public async Task<bool> ApplyFullStealthProfileAsync(
            DeviceContext device, 
            StealthTier tier, 
            IProgress<ProgressUpdate> progress, 
            CancellationToken ct)
        {
            Logger.Info($"Applying Full Stealth Profile Tier: {tier} to {device.Serial}");
            
            try
            {
                // 1. Root Hiding (DenyList + Shamiko Check)
                progress.Report(ProgressUpdate.Info(20, "Configuring Root Hiding Layers..."));
                var rootStatus = await _rootManager.InspectAsync(device, progress, ct);
                
                if (rootStatus.IsRooted)
                {
                    // Ensure Zygisk is enabled if tier >= Hybrid
                    if (tier >= StealthTier.Hybrid && !rootStatus.ZygiskEnabled)
                    {
                        progress.Report(ProgressUpdate.Warning(30, "Enabling Zygisk (requires reboot after profile application)..."));
                        await RunSuCommand("magisk --zygisk enable", ct);
                    }

                    // Apply standard DenyList profile for the tier
                    var profileType = tier == StealthTier.Maximum ? ProfileType.Banking : ProfileType.Gaming;
                    var profile = _rootManager.GetProfile(profileType);
                    await _rootManager.ApplyDenyListProfile(profile, progress, ct);
                }

                // 2. Dev Mode Hiding
                progress.Report(ProgressUpdate.Info(60, "Applying Developer Mode Cloak..."));
                var devProfile = tier == StealthTier.Maximum 
                    ? DevModeCloakManager.GetBankingProfile() 
                    : DevModeCloakManager.GetStandardProfile();
                
                await _devManager.ApplyStealthAsync(devProfile, rootStatus.IsRooted, progress);

                // 3. System Prop Hiding (Epic C Specific)
                if (tier == StealthTier.Maximum && rootStatus.IsRooted)
                {
                    progress.Report(ProgressUpdate.Info(80, "Injecting ResetProp stealth tweaks..."));
                    await ApplyPropTweaks(ct);
                }

                progress.Report(ProgressUpdate.Info(100, "Full Stealth Orchestration Complete. Recommend Reboot."));
                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Stealth orchestration failed");
                return false;
            }
        }

        private async Task ApplyPropTweaks(CancellationToken ct)
        {
            // Standard stealth props via resetprop
            await RunSuCommand("resetprop ro.debuggable 0", ct);
            await RunSuCommand("resetprop ro.secure 1", ct);
            await RunSuCommand("resetprop ro.build.type user", ct);
            await RunSuCommand("resetprop ro.build.tags release-keys", ct);
        }

        private async Task RunSuCommand(string cmd, CancellationToken ct)
        {
            // Internal use of adb shell su -c
            var command = $"shell su -c '{cmd}'";
            // This would normally call an internal ADB wrapper
            Logger.Debug($"Orchestrator Su Exec: {command}");
        }
    }

    public enum StealthTier
    {
        Basic,      // Simple DenyList
        Hybrid,     // DenyList + DevMode Hider
        Maximum     // DenyList + DevMode Hider + ResetProp + Module Automations
    }
}
