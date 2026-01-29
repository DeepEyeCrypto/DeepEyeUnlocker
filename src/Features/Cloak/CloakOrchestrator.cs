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
        private readonly Infrastructure.IAdbClient _adbClient;

        public CloakOrchestrator(Infrastructure.IAdbClient adbClient)
        {
            _adbClient = adbClient;
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
            // Surgical prop injection to fool advanced integrity checks
            await RunSuCommand("resetprop ro.debuggable 0", ct);
            await RunSuCommand("resetprop ro.secure 1", ct);
            await RunSuCommand("resetprop ro.build.type user", ct);
            await RunSuCommand("resetprop ro.build.tags release-keys", ct);
            await RunSuCommand("resetprop ro.boot.verifiedbootstate green", ct);
            await RunSuCommand("resetprop ro.boot.flash.locked 1", ct);
            await RunSuCommand("resetprop ro.boot.veritymode enforcing", ct);
            await RunSuCommand("resetprop sys.usb.config mtp,adb", ct);
        }

        private async Task RunSuCommand(string cmd, CancellationToken ct)
        {
            if (ct.IsCancellationRequested) return;
            
            Logger.Debug($"Orchestrator Su Exec: {cmd}");
            try 
            {
                await _adbClient.ExecuteShellAsync($"su -c '{cmd}'");
            }
            catch (Exception ex)
            {
                Logger.Error($"Su Command Failed: {cmd} - {ex.Message}");
            }
        }
    }

    public enum StealthTier
    {
        Basic,      // Simple DenyList
        Hybrid,     // DenyList + DevMode Hider
        Maximum     // DenyList + DevMode Hider + ResetProp + Module Automations
    }
}
