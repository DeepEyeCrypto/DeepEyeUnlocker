using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols;
using DeepEyeUnlocker.Core.Services;
using DeepEyeUnlocker.Core.Services.Frp;

namespace DeepEyeUnlocker.Operations
{
    /// <summary>
    /// Professional FRP Bypass Operation using the Universal Engine (Parity Stage 1-8).
    /// </summary>
    public class FrpBypassOperation : Operation
    {
        private readonly IProtocol _protocol;
        private readonly UniversalFrpEngine _frpEngine;

        public FrpBypassOperation(IProtocol protocol)
        {
            _protocol = protocol;
            _frpEngine = new UniversalFrpEngine();
            Name = "Sentinel Pro: FRP Service";
        }

        public override async Task<bool> ExecuteAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Report(progress, 5, $"Starting Universal FRP Service Engine...");

            // 1. Establish Service Context
            // We use the new DeviceProfile mapping from V2 schema
            var profile = new DeviceProfile
            {
                Brand = device.Brand ?? "Generic",
                ModelNumber = device.Model ?? "Unknown",
                Chipset = new ChipsetInfo { Manufacturer = device.Chipset ?? "MediaTek" }
            };

            var ctx = new FrpServiceContext
            {
                Profile = profile,
                ActiveConnection = _protocol,
                Protocol = _protocol.Name,
                Ownership = device.IsAuthorized ? OwnershipStatus.VerifiedIndividual : OwnershipStatus.Unverified,
                UserReason = "Factory Reset Maintenance"
            };

            // 2. Compatibility Check
            if (!_frpEngine.IsSupported(ctx))
            {
                Report(progress, 0, "Operation Blocked: No compliant strategy for this device hardware.", LogLevel.Error);
                return false;
            }

            // 3. Safety Disclosure (Instructions)
            string instructions = _frpEngine.GetOfficialInstructions(ctx);
            Logger.Debug($"Service Instructions: {instructions}");

            // 4. Check Current Lock Status
            Report(progress, 20, "Analyzing security state...");
            string status = await _frpEngine.CheckLockStatusAsync(ctx);
            Report(progress, 30, $"Current State: {status}");

            // 5. Execution (High level strategy execution)
            Report(progress, 50, "Engaging Service Channel...");
            var result = await _frpEngine.ExecuteServiceClearAsync(ctx);

            if (result.Success)
            {
                Report(progress, 100, $"SUCCESS: {result.Message}");
                Logger.Success($"FRP bypass completed successfully for {device.Model}");
                
                // Track internally for auditing (Epic B)
                DeepEyeUnlocker.Features.Analytics.Services.FleetManager.Instance.RegisterOperation(
                    device.Brand ?? "UNKNOWN", "Universal FRP", true);
                
                await _protocol.RebootAsync();
                return true;
            }
            else
            {
                Report(progress, 0, $"FAILED: {result.Message}", LogLevel.Error);
                Logger.Error($"Universal FRP Engine failed: {result.Message}");
                
                DeepEyeUnlocker.Features.Analytics.Services.FleetManager.Instance.RegisterOperation(
                    device.Brand ?? "UNKNOWN", "Universal FRP", false, result.Message);
                
                return false;
            }
        }
    }
}
