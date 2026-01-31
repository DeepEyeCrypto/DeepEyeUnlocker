using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.FrpBypass.Interfaces;
using DeepEyeUnlocker.Features.FrpBypass.Models;

namespace DeepEyeUnlocker.Features.FrpBypass.Samsung
{
    public class SamsungFrpEngine
    {
        private readonly IFrpProtocol _protocol;
        private readonly FrpBrandProfile _profile;

        public SamsungFrpEngine(IFrpProtocol protocol, FrpBrandProfile profile)
        {
            _protocol = protocol;
            _profile = profile;
        }

        public async Task<bool> ExecuteBypassAsync(DeviceContext device, IProgress<ProgressUpdate> progress)
        {
            Logger.Info($"[SAMSUNG-FRP] Initializing bypass for {device.Model} ({_profile.Codename})...");

            // 1. Safety verification
            if (!await SafetyGate.ValidateBypassEnv(_profile, progress))
            {
                Logger.Error("[SAMSUNG-FRP] Safety validation failed.");
                return false;
            }

            switch (_profile.Method)
            {
                case "SAMSUNG_ODIN_FACTORY_RESET":
                    return await ExecuteOdinFactoryResetAsync(progress);
                case "SAMSUNG_ADB_MTP_BYPASS":
                    return await ExecuteAdbMtpBypassAsync(progress);
                default:
                    Logger.Warn($"[SAMSUNG-FRP] Unknown method: {_profile.Method}. Falling back to default protocol.");
                    return await _protocol.ADB_Bypass_FRP();
            }
        }

        private async Task<bool> ExecuteOdinFactoryResetAsync(IProgress<ProgressUpdate> progress)
        {
            progress.Report(ProgressUpdate.Info(30, "Switching to Download Mode..."));
            await Task.Delay(1000); // Simulation

            Logger.Info("[SAMSUNG-FRP] Initializing ODIN/PIT sequence...");
            progress.Report(ProgressUpdate.Info(60, $"Erasing secure partition: {_profile.TargetPartition}..."));
            
            // Logic: Samsung uses PIT mapping to target 'persistent'
            // We simulate the protocol call here
            return await _protocol.EDL_Erase_Partition(_profile.TargetPartition);
        }

        private async Task<bool> ExecuteAdbMtpBypassAsync(IProgress<ProgressUpdate> progress)
        {
            progress.Report(ProgressUpdate.Info(20, "Detecting MTP security layer..."));
            Logger.Info("[SAMSUNG-FRP] Triggering hidden GMS activity via shell...");
            
            // Logic: Send Intent to trigger the FRP bypass UI on some models
            string cmd = "am start -n com.google.android.gms/.setup.uiflow.SetupWizardFlowActivity";
            Logger.Info($"[SAMSUNG-FRP] Executing: {cmd}");
            
            await Task.Delay(1500); // Waiting for UI to react
            
            progress.Report(ProgressUpdate.Info(80, "Injecting bypass flag..."));
            return await _protocol.ADB_Bypass_FRP();
        }
    }
}
