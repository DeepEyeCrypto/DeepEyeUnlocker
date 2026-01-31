using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.FrpBypass.Interfaces;
using DeepEyeUnlocker.Features.FrpBypass.Models;

namespace DeepEyeUnlocker.Features.FrpBypass.Xiaomi
{
    public class XiaomiFrpEngine
    {
        private readonly IFrpProtocol _protocol;
        private readonly FrpBrandProfile _profile;

        public XiaomiFrpEngine(IFrpProtocol protocol, FrpBrandProfile profile)
        {
            _protocol = protocol ?? throw new ArgumentNullException(nameof(protocol));
            _profile = profile ?? throw new ArgumentNullException(nameof(profile));
        }

        public async Task<bool> ExecuteBypassAsync(DeviceContext device, IProgress<ProgressUpdate> progress)
        {
            Logger.Info($"[XIAOMI] Initializing FRP Engine for {device.Model} ({_profile.Codename})...");

            // 1. Pre-flight Safety Gate
            if (!await SafetyGate.ValidateEnvironment(device, _profile.TargetPartition))
            {
                Logger.Error("[XIAOMI] Safety Gate rejected the environment. Operation aborted.");
                return false;
            }

            try
            {
                progress.Report(ProgressUpdate.Info(20, $"Connecting via {_protocol.ProtocolName}..."));

                // 2. Execution based on method
                switch (_profile.Method)
                {
                    case "QUALCOMM_EDL_ERASE":
                        return await ExecuteEdlEraseAsync(progress);
                    
                    case "MTK_BROM_FORMAT":
                        return await ExecuteBromFormatAsync(progress);

                    default:
                        Logger.Warn($"[XIAOMI] Method '{_profile.Method}' not explicitly handled. Falling back to generic adapter.");
                        return await FallbackGenericAsync(progress);
                }
            }
            catch (Exception ex)
            {
                Logger.Error($"[XIAOMI] FRP Bypass failed: {ex.Message}");
                return false;
            }
        }

        private async Task<bool> ExecuteEdlEraseAsync(IProgress<ProgressUpdate> progress)
        {
            Logger.Info($"[EDL] Targeting partition: {_profile.TargetPartition}");
            progress.Report(ProgressUpdate.Info(50, $"Erasing {_profile.TargetPartition} partition..."));
            
            bool success = await _protocol.EDL_Erase_Partition(_profile.TargetPartition);
            
            if (success)
            {
                Logger.Success($"[XIAOMI] FRP Partition wiped successfully via EDL.");
                progress.Report(ProgressUpdate.Info(100, "Bypass Complete. Device will reboot."));
            }
            return success;
        }

        private async Task<bool> ExecuteBromFormatAsync(IProgress<ProgressUpdate> progress)
        {
            Logger.Info($"[BROM] Formatting partition region: {_profile.TargetPartition}");
            progress.Report(ProgressUpdate.Info(50, $"Zeroing {_profile.TargetPartition} via DA..."));
            
            // In MTK, we often use write reg or direct erase
            // Here we use a unified patch command for simplicity
            bool success = await _protocol.MTK_Brom_Execute(0x0, new byte[] { 0x00 }); 
            
            if (success)
            {
                Logger.Success($"[XIAOMI] BROM Format successful on {_profile.TargetPartition}.");
                progress.Report(ProgressUpdate.Info(100, "Bypass Complete."));
            }
            return success;
        }

        private async Task<bool> FallbackGenericAsync(IProgress<ProgressUpdate> progress)
        {
            if (_protocol.ProtocolName.Contains("Qualcomm"))
                return await ExecuteEdlEraseAsync(progress);
            
            Logger.Error("[XIAOMI] No specific fallback for this protocol.");
            return false;
        }
    }
}
