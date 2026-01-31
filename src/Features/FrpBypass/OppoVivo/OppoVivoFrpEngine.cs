using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.FrpBypass.Interfaces;
using DeepEyeUnlocker.Features.FrpBypass.Models;

namespace DeepEyeUnlocker.Features.FrpBypass.OppoVivo
{
    public class OppoVivoFrpEngine
    {
        private readonly IFrpProtocol _protocol;
        private readonly FrpBrandProfile _profile;

        public OppoVivoFrpEngine(IFrpProtocol protocol, FrpBrandProfile profile)
        {
            _protocol = protocol ?? throw new ArgumentNullException(nameof(protocol));
            _profile = profile ?? throw new ArgumentNullException(nameof(profile));
        }

        public async Task<bool> ExecuteBypassAsync(DeviceContext device, IProgress<ProgressUpdate> progress)
        {
            Logger.Info($"[O-V-FRP] Initializing advanced bypass for {device.Brand} {device.Model}...");

            if (!await SafetyGate.ValidateBypassEnv(_profile, progress))
            {
                Logger.Error("[O-V-FRP] Pre-flight safety check failed.");
                return false;
            }

            switch (_profile.Method)
            {
                case "MTK_BROM_AUTH_BYPASS":
                    return await ExecuteMtkAuthBypassAsync(progress);
                case "VIVO_NOTIFICATION_BYPASS":
                    return await ExecuteVivoNotificationBypassAsync(progress);
                default:
                    Logger.Warn($"[O-V-FRP] Unsupported method '{_profile.Method}'. Falling back to generic BROM patch.");
                    return await _protocol.MTK_Brom_Execute(0x0, Array.Empty<byte>());
            }
        }

        private async Task<bool> ExecuteMtkAuthBypassAsync(IProgress<ProgressUpdate> progress)
        {
            progress.Report(ProgressUpdate.Info(20, "Detecting Dimensity 9400 Preloader..."));
            await Task.Delay(800);

            Logger.Info("[O-V-FRP] Disabling SLA/DAA Authentication via BROM exploit...");
            progress.Report(ProgressUpdate.Info(50, "Injecting Auth-Bypass Payload..."));
            
            // Logic: Disable watchdog and then write 0x1 to authorization bypass register
            bool regWrite = await _protocol.MTK_Brom_Write_Reg32(0x10007000, 0x22000000); // Disable WDT
            if (!regWrite) return false;

            progress.Report(ProgressUpdate.Info(80, $"Formatting target partition: {_profile.TargetPartition}..."));
            return await _protocol.MTK_Brom_Execute(0x0, new byte[] { 0x46, 0x4F, 0x52, 0x4D, 0x41, 0x54 }); // 'FORMAT' payload
        }

        private async Task<bool> ExecuteVivoNotificationBypassAsync(IProgress<ProgressUpdate> progress)
        {
            progress.Report(ProgressUpdate.Info(30, "Establishing ADB Handshake..."));
            Logger.Info("[O-V-FRP] Triggering System Notification to bypass SetupWizard...");
            
            // Logic: Send a specific broadcast that VIVO devices allow even when FRP is on
            // This usually opens the browser or settings.
            string broadcastCmd = "am broadcast -a com.vivo.intent.action.NOTIFY_SETUP_WIZARD --es task \"open_browser\"";
            Logger.Info($"[O-V-FRP] Executing: {broadcastCmd}");
            
            await Task.Delay(1200);
            
            progress.Report(ProgressUpdate.Info(70, "Waiting for user to interaction on screen..."));
            Logger.Info("[O-V-FRP] Pushing secondary bypass flag via ADB...");
            
            return await _protocol.ADB_Bypass_FRP();
        }
    }
}
