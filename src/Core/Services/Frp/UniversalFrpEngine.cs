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
            };
        }

        public bool IsSupported(FrpServiceContext ctx)
        {
            if (ctx?.Profile == null) return false;
            foreach (var strategy in _strategies)
            {
                if (strategy.CanHandle(ctx)) return true;
            }
            return false;
        }

        public Task<string> CheckLockStatusAsync(FrpServiceContext ctx)
        {
            if (ctx.Profile.FrpInfo.Type == FrpType.SamsungKnox) return Task.FromResult("LOCKED (Server-Side)");
            if (ctx.Profile.FrpInfo.Type == FrpType.GoogleStandard) return Task.FromResult("UNKNOWN (Requires Read Access)");
            return Task.FromResult("UNSUPPORTED");
        }

        public string GetOfficialInstructions(FrpServiceContext ctx)
        {
            if (ctx.Profile.FrpInfo.OfficialServiceMethod != "")
                return $"Official Method ID: {ctx.Profile.FrpInfo.OfficialServiceMethod}";

            if (ctx.Profile.FrpInfo.Type == FrpType.SamsungKnox)
                return "1. Connect device to Samsung Smart Switch.\n2. Sign in with original Samsung/Google account.";
            
            if (ctx.Profile.FrpInfo.Type == FrpType.GoogleStandard)
                return "1. Perform Factory Reset via Settings if possible.\n2. Or use original Google account credentials.";

            return "Refer to OEM documentation for official removal.";
        }

        public async Task<FrpResult> ExecuteServiceClearAsync(FrpServiceContext ctx)
        {
            // CORE GUARDRAIL: Ownership Verification
            if (ctx.Ownership == OwnershipStatus.Unverified || ctx.Ownership == OwnershipStatus.Unknown)
            {
                return FrpResult.Fail("Operation Refused: Ownership verification is mandatory for FRP services.");
            }

            foreach (var strategy in _strategies)
            {
                if (strategy.CanHandle(ctx))
                {
                    return await strategy.ExecuteAsync(ctx);
                }
            }

            return FrpResult.Fail($"No supported strategy found for Protocol: {ctx.Protocol} on {ctx.Profile.Brand} {ctx.Profile.ModelNumber}.");
        }
    }
}
