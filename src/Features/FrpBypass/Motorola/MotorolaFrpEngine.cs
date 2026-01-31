using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.FrpBypass.Interfaces;
using DeepEyeUnlocker.Features.FrpBypass.Models;

namespace DeepEyeUnlocker.Features.FrpBypass.Motorola
{
    public class MotorolaFrpEngine
    {
        private readonly IFrpProtocol _protocol;
        private readonly FrpBrandProfile _profile;

        public MotorolaFrpEngine(IFrpProtocol protocol, FrpBrandProfile profile)
        {
            _protocol = protocol ?? throw new ArgumentNullException(nameof(protocol));
            _profile = profile ?? throw new ArgumentNullException(nameof(profile));
        }

        public async Task<bool> ExecuteBypassAsync(DeviceContext device, IProgress<ProgressUpdate> progress)
        {
            Logger.Info($"[MOTO-FRP] Initializing Motorola-specific bypass sequence for {device.Model}...");

            if (!await SafetyGate.ValidateBypassEnv(_profile, progress))
            {
                Logger.Error("[MOTO-FRP] Pre-flight safety check failed.");
                return false;
            }

            switch (_profile.Method)
            {
                case "FASTBOOT_OEM_FORMAT":
                    return await ExecuteFastbootOemFormatAsync(progress);
                case "MOTO_CONFIG_ERASE":
                    return await ExecuteMotoConfigEraseAsync(progress);
                default:
                    Logger.Warn($"[MOTO-FRP] Unsupported method '{_profile.Method}'. Falling back to generic Fastboot unlock.");
                    return await _protocol.Fastboot_Oem_Unlock();
            }
        }

        private async Task<bool> ExecuteFastbootOemFormatAsync(IProgress<ProgressUpdate> progress)
        {
            progress.Report(ProgressUpdate.Info(30, "Entering Fastboot Bootloader Mode..."));
            await Task.Delay(1000);

            Logger.Info("[MOTO-FRP] Executing OEM partition format sequence...");
            progress.Report(ProgressUpdate.Info(60, $"Formatting target: {_profile.TargetPartition}..."));
            
            // Logic: Motorola specific format command
            return await _protocol.Fastboot_Format_Partition(_profile.TargetPartition);
        }

        private async Task<bool> ExecuteMotoConfigEraseAsync(IProgress<ProgressUpdate> progress)
        {
            progress.Report(ProgressUpdate.Info(20, "Auditing bootloader security flags..."));
            Logger.Info("[MOTO-FRP] Injecting config-erase sequence via OEM-protected command...");
            
            // Logic: Many Motorola devices allow 'oem config frp erase'
            string rawCmd = "oem config frp erase";
            byte[] cmdBytes = System.Text.Encoding.ASCII.GetBytes(rawCmd);
            
            progress.Report(ProgressUpdate.Info(70, "Neutralizing FRP bit in persistent storage..."));
            return await _protocol.Execute_Raw_Command(cmdBytes);
        }
    }
}
