using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Services;
using DeepEyeUnlocker.Core.Services.Frp.Strategies;

namespace DeepEyeUnlocker.Core.Services.Frp
{
    public class UniversalFrpEngine : IFrpServiceEngine
    {
        private readonly List<IFrpStrategy> _strategies;

        public UniversalFrpEngine()
        {
            _strategies = new List<IFrpStrategy>
            {
                new SamsungKnoxStrategy(),
                new QualcommEdlFrpStrategy(),
                new MtkBromFrpStrategy(),
            };
        }

        public bool IsSupported(FrpServiceContext ctx)
        {
            if (ctx?.Profile == null) return false;

            // Fill missing info from Registry if needed
            EnsureFrpCapabilities(ctx);
            
            foreach (var strategy in _strategies)
            {
                if (strategy.CanHandle(ctx)) return true;
            }
            return false;
        }

        public async Task<string> CheckLockStatusAsync(FrpServiceContext ctx)
        {
            EnsureFrpCapabilities(ctx);
            if (ctx.Profile.FrpInfo.Type == FrpType.SamsungKnox) return "LOCKED (Knox Check Required)";
            return "UNKNOWN (Check Partition)";
        }

        public string GetOfficialInstructions(FrpServiceContext ctx)
        {
            EnsureFrpCapabilities(ctx);
            if (!string.IsNullOrEmpty(ctx.Profile.FrpInfo.OfficialServiceMethod))
                return $"Official Mode Required: {ctx.Profile.FrpInfo.OfficialServiceMethod}";

            return "1. Sign in with Google Account.\n2. Or use Android Enterprise MDM console.";
        }

        public async Task<FrpResult> ExecuteServiceClearAsync(FrpServiceContext ctx)
        {
            EnsureFrpCapabilities(ctx);

            // 1. Ownership Guardrail
            if (ctx.Ownership == OwnershipStatus.Unverified || ctx.Ownership == OwnershipStatus.Unknown)
            {
                var fail = FrpResult.Fail("Operation Blocking: Ownership verification is mandatory for FRP services.");
                FrpAuditLogger.LogOperation(ctx, fail);
                return fail;
            }

            // 2. Strategy Execution
            FrpResult result = FrpResult.Fail("No suitable strategy found for this device/mode.");
            
            foreach (var strategy in _strategies)
            {
                if (strategy.CanHandle(ctx))
                {
                    result = await strategy.ExecuteAsync(ctx);
                    break;
                }
            }

            // 3. Compliance Logging
            FrpAuditLogger.LogOperation(ctx, result);
            
            return result;
        }

        private void EnsureFrpCapabilities(FrpServiceContext ctx)
        {
            if (ctx.Profile != null && (ctx.Profile.FrpInfo == null || ctx.Profile.FrpInfo.Type == FrpType.Unknown))
            {
                ctx.Profile.FrpInfo = FrpRegistry.GetDefaultCapabilities(ctx.Profile.Brand, ctx.Profile.Chipset?.Manufacturer ?? "Generic");
            }
        }
    }
}
