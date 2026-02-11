using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Services.Frp;
using DeepEyeUnlocker.Core.Services.Repositories;

namespace DeepEyeUnlocker.Core.Scenarios
{
    public class FrpEraseOneClickScenario : OperationScenario
    {
        private readonly LoaderRepository _loaders;
        private readonly DiagramRepository _diagrams;

        public FrpEraseOneClickScenario()
        {
            _loaders = new LoaderRepository();
            _diagrams = new DiagramRepository();
            Name = "One-Click FRP Erase";
            Description = "Automated FRP clearing using the best available protocol and loaders.";
        }

        public override async Task<ScenarioResult> RunAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            // 1. Safety Checklist (Stage 8 Compatibility)
            Report(progress, 5, "Verifying legal compliance and ownership...");
            if (!device.IsAuthorized)
            {
                return ScenarioResult.Fail("Security Interlock: Ownership verification required for this operation.");
            }

            // 2. Fetch Profile from DB (Assume we have one or create ad-hoc)
            // In a real app, we'd lookup by VID/PID/Model
            var profile = new DeviceProfile { Brand = device.Brand, ModelNumber = device.Model };
            
            // 3. Asset Loading
            if (!string.IsNullOrEmpty(profile.Loaders.CustomDaId))
            {
                Report(progress, 15, $"Loading custom DA: {profile.Loaders.CustomDaId}...");
                var daPath = _loaders.GetFullPath(profile.Loaders.CustomDaId);
                // logic to feed to MTK protocol...
            }

            // 4. Test-Point Awareness
            if (profile.ServiceModes.Preloader.TestPoint?.Required == true)
            {
                Report(progress, 25, "Waiting for Test-Point connection...");
                var diagram = _diagrams.GetDiagram(profile.ServiceModes.Preloader.TestPoint.DiagramId);
                // logic to show UI diagram (handled by view binder)
            }

            // 5. Execution via Universal Engine
            var engine = new UniversalFrpEngine();
            var ctx = new FrpServiceContext
            {
                Profile = profile,
                ActiveConnection = null, // Set by the caller wrapper
                Ownership = OwnershipStatus.VerifiedIndividual
            };

            Report(progress, 50, "Engaging FRP Service Engine...");
            var result = await engine.ExecuteServiceClearAsync(ctx);

            if (result.Success)
            {
                Report(progress, 100, "Operation successful.");
                return ScenarioResult.Ok(result.Message);
            }

        return ScenarioResult.Fail(result.Message);
        }
    }
}
