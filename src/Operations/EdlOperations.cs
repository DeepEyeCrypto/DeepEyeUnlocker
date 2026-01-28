using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Engines;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure.Logging;

namespace DeepEyeUnlocker.Operations
{
    /// <summary>
    /// Operation to reboot a Qualcomm device into EDL (Emergency Download Mode)
    /// </summary>
    public class RebootToEdlOperation : Operation
    {
        private readonly IEdlManager _edlManager;
        private readonly DeviceContext _targetDevice;

        public RebootToEdlOperation(DeviceContext device) : this(device, new EdlManager()) { }

        public RebootToEdlOperation(DeviceContext device, IEdlManager edlManager)
        {
            _targetDevice = device;
            _edlManager = edlManager;
            Name = "Reboot to EDL (Qualcomm 9008)";
        }

        public override async Task<bool> ExecuteAsync(Device device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Report(progress, 5, "Checking EDL capability...");

            // Step 1: Validate chipset
            if (!IsQualcommDevice(_targetDevice))
            {
                Report(progress, 0, "EDL mode is only available for Qualcomm devices", LogLevel.Error);
                Logger.Error("EDL reboot attempted on non-Qualcomm device", "EDL");
                return false;
            }

            // Step 2: Get capability assessment
            var capability = _edlManager.GetCapabilityFor(_targetDevice);
            var profile = _edlManager.GetProfileFor(_targetDevice);

            Report(progress, 10, $"EDL capability: {GetCapabilityLabel(capability)}");
            Logger.Info($"EDL capability for {_targetDevice.Brand} {_targetDevice.Model}: {capability}", "EDL");

            // Step 3: Handle hardware-only case
            if (capability == EdlCapability.HARDWARE_ONLY)
            {
                var testPoint = _edlManager.GetTestPointInfo(_targetDevice);
                string message = "This device requires hardware test-point EDL entry.";
                
                if (testPoint != null)
                {
                    message += $"\n\nTest-point info: {testPoint.Description}";
                    message += $"\nDifficulty: {testPoint.Difficulty}";
                    message += $"\nTools needed: {testPoint.ToolsNeeded}";
                }
                
                if (profile?.AuthToolName != null)
                {
                    message += $"\n\nAlternative: Use {profile.AuthToolName}";
                }
                
                Report(progress, 0, message, LogLevel.Warn);
                return false;
            }

            // Step 4: Attempt EDL reboot
            Report(progress, 20, "Sending EDL reboot command...");
            Logger.Info("Initiating EDL reboot sequence", "EDL");

            try
            {
                var result = await _edlManager.RebootToEdlAsync(_targetDevice, ct);

                if (result.Success)
                {
                    Report(progress, 90, $"EDL reboot successful via {GetMethodLabel(result.MethodUsed)}");
                    
                    // Wait a moment for USB to stabilize
                    await Task.Delay(1000, ct);
                    
                    Report(progress, 100, "✅ Device is now in EDL mode (Qualcomm 9008)");
                    Logger.Success($"EDL reboot successful: {result.MethodUsed}", "EDL");
                    return true;
                }
                else
                {
                    Report(progress, 0, result.FailureReason ?? "EDL reboot failed", LogLevel.Error);
                    Logger.Error($"EDL reboot failed: {result.FailureReason}", "EDL");
                    
                    // Provide hint based on capability
                    if (capability == EdlCapability.SOFTWARE_RESTRICTED)
                    {
                        Logger.Info("Hint: This device may have EDL blocked on newer firmware versions", "EDL");
                    }
                    
                    return false;
                }
            }
            catch (OperationCanceledException)
            {
                Report(progress, 0, "Operation cancelled", LogLevel.Warn);
                return false;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "EDL reboot operation failed unexpectedly");
                Report(progress, 0, $"Unexpected error: {ex.Message}", LogLevel.Error);
                return false;
            }
        }

        private static bool IsQualcommDevice(DeviceContext device)
        {
            var chipset = device.Chipset?.ToLower() ?? "";
            var soc = device.SoC?.ToLower() ?? "";
            
            // Check for Qualcomm indicators
            return chipset.Contains("qualcomm") ||
                   chipset.Contains("snapdragon") ||
                   soc.StartsWith("sm") ||
                   soc.StartsWith("sdm") ||
                   soc.StartsWith("msm") ||
                   soc.StartsWith("qsd");
        }

        private static string GetCapabilityLabel(EdlCapability capability) => capability switch
        {
            EdlCapability.SOFTWARE_DIRECT_SUPPORTED => "✅ Supported",
            EdlCapability.SOFTWARE_RESTRICTED => "⚠️ Restricted",
            EdlCapability.HARDWARE_ONLY => "🔩 Hardware Only",
            EdlCapability.UNKNOWN => "❓ Unknown",
            _ => "Unknown"
        };

        private static string GetMethodLabel(EdlAttemptMethod method) => method switch
        {
            EdlAttemptMethod.AdbRebootEdl => "ADB",
            EdlAttemptMethod.FastbootOemEdl => "Fastboot OEM",
            EdlAttemptMethod.FastbootRebootEdl => "Fastboot Reboot",
            _ => "Unknown"
        };
    }

    /// <summary>
    /// Operation to check if device is in EDL mode
    /// </summary>
    public class CheckEdlModeOperation : Operation
    {
        private readonly IEdlManager _edlManager;

        public CheckEdlModeOperation() : this(new EdlManager()) { }

        public CheckEdlModeOperation(IEdlManager edlManager)
        {
            _edlManager = edlManager;
            Name = "Check EDL Mode";
        }

        public override async Task<bool> ExecuteAsync(Device device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Report(progress, 50, "Scanning for Qualcomm 9008 devices...");
            
            bool inEdl = await _edlManager.IsInEdlModeAsync(ct);
            
            if (inEdl)
            {
                Report(progress, 100, "✅ Device is in EDL mode");
                Logger.Info("EDL device detected on USB", "EDL");
            }
            else
            {
                Report(progress, 100, "Device is not in EDL mode");
            }
            
            return inEdl;
        }
    }
}
