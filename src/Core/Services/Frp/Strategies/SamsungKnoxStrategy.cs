using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols.Samsung;
using DeepEyeUnlocker.Core.Services;

namespace DeepEyeUnlocker.Core.Services.Frp.Strategies
{
    public class SamsungKnoxStrategy : IFrpStrategy
    {
        public bool CanHandle(FrpServiceContext ctx)
        {
            // Check if profile explicitly is Samsung Knox type OR if the engine is SamsungEngine
            return ctx.Profile?.FrpInfo?.Type == FrpType.SamsungKnox || 
                   (ctx.ActiveConnection != null && ctx.ActiveConnection.GetType().Name == "SamsungEngine");
        }

        public FrpResult Execute(FrpServiceContext ctx)
        {
            // STRICT GUARDRAIL: Ownership Verification
            if (ctx.Ownership == OwnershipStatus.Unverified || ctx.Ownership == OwnershipStatus.Unknown)
            {
                return FrpResult.Fail("Security Alert: Samsung Knox operations require verified ownership context.");
            }

            // Cast connection safely
            // Using dynamic or configured interface since SamsungEngine might not be directly referencable if circular dependency
            // But since they are likely in same assembly, we try via name check above
            // Here we assume ctx.ActiveConnection is usable via reflection or interface if needed.
            
            if (ctx.Profile.FrpInfo.OfficialServiceMethod == "KNOX_DEPLOYMENT_APP")
            {
                return FrpResult.Fail("This device is managed by Enterprise Knox. Please contact IT administrator.");
            }

            return FrpResult.Fail("Direct memory write to Persistent partition is blocked on this security patch level. Use OEM Service Cable.");
        }
    }
}
