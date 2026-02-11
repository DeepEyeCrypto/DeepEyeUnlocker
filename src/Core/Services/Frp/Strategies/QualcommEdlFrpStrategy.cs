using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Services;
using DeepEyeUnlocker.Protocols.Qualcomm;

namespace DeepEyeUnlocker.Core.Services.Frp.Strategies
{
    public class QualcommEdlFrpStrategy : IFrpStrategy
    {
        public bool CanHandle(FrpServiceContext ctx)
        {
            // Only handle if active connection is QualcommEngine (EDL Mode)
            return (ctx.ActiveConnection != null && ctx.ActiveConnection.GetType().Name == "QualcommEngine");
        }

        public async Task<FrpResult> ExecuteAsync(FrpServiceContext ctx)
        {
            // Guardrail: Strict Ownership Verification
            if (ctx.Ownership == OwnershipStatus.Unverified || ctx.Ownership == OwnershipStatus.Unknown)
            {
                 return FrpResult.Fail("Qualcomm EDL operations require verified ownership.");
            }

            // Safe Cast (Using dynamic/reflection or direct if reference valid)
            // Assuming direct reference is safe as per file structure
            var engine = ctx.ActiveConnection as QualcommEngine;
            if (engine == null) return FrpResult.Fail("Active connection is not a valid Qualcomm Engine instance.");

            var partition = !string.IsNullOrEmpty(ctx.Profile.FrpInfo.FrpPartitionName) 
                            ? ctx.Profile.FrpInfo.FrpPartitionName 
                            : "frp"; // Default fallback (Xiaomi/Motorola usually use 'frp' or 'config')

            try
            {
                // Execute standard erase command using Firehose
                bool success = await engine.ErasePartitionAsync(partition, null, System.Threading.CancellationToken.None);
                
                if (success)
                {
                    return FrpResult.Ok($"Successfully erased '{partition}' partition. FRP request prevents reuse.", reboot: true);
                }
                else
                {
                    return FrpResult.Fail($"Failed to erase partition '{partition}'. Access Denied by Loader.");
                }
            }
            catch (Exception ex)
            {
                return FrpResult.Fail($"EDL Operation Error: {ex.Message}");
            }
        }
    }
}
