using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols.Samsung;
using DeepEyeUnlocker.Core.Services;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Core.Services.Frp.Strategies
{
    public class SamsungKnoxStrategy : IFrpStrategy
    {
        public bool CanHandle(FrpServiceContext ctx)
        {
            return ctx.Profile?.FrpInfo?.Type == FrpType.SamsungKnox || 
                   (ctx.ActiveConnection != null && ctx.ActiveConnection.GetType().Name == "SamsungEngine");
        }

        public async Task<FrpResult> ExecuteAsync(FrpServiceContext ctx)
        {
            await Task.Yield(); // Async simulation
            if (ctx.Ownership == OwnershipStatus.Unverified || ctx.Ownership == OwnershipStatus.Unknown)
            {
                return FrpResult.Fail("Samsung Knox operations require verified ownership.");
            }

            if (ctx.Profile.FrpInfo.OfficialServiceMethod == "KNOX_DEPLOYMENT_APP")
            {
                return FrpResult.Fail("This device is managed by Enterprise Knox. Please contact IT administrator.");
            }

            return FrpResult.Fail("Direct memory write to Persistent partition is blocked on this security patch level. Use OEM Service Cable.");
        }
    }
}
