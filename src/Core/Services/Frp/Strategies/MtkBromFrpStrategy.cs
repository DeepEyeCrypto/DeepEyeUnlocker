using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Services;
using DeepEyeUnlocker.Protocols.MTK;

namespace DeepEyeUnlocker.Core.Services.Frp.Strategies
{
    public class MtkBromFrpStrategy : IFrpStrategy
    {
        public bool CanHandle(FrpServiceContext ctx)
        {
            // Handle if device is MediaTek and engine is MTKEngine
            return (ctx.ActiveConnection != null && ctx.ActiveConnection.GetType().Name == "MTKEngine");
        }

        public async Task<FrpResult> ExecuteAsync(FrpServiceContext ctx)
        {
            // Guardrail
            if (ctx.Ownership == OwnershipStatus.Unverified || ctx.Ownership == OwnershipStatus.Unknown)
            {
                 return FrpResult.Fail("Security: MediaTek BROM operations require ownership verification.");
            }

            var engine = ctx.ActiveConnection as MTKEngine;
            if (engine == null) return FrpResult.Fail("Connection mismatch: Expected MTKEngine.");

            // MTK FRP partition is usually 'frp' or 'persistent'
            var partition = !string.IsNullOrEmpty(ctx.Profile.FrpInfo.FrpPartitionName) 
                            ? ctx.Profile.FrpInfo.FrpPartitionName 
                            : "frp"; 

            try
            {
                // In MTK BROM, FRP is typically removed by formatting the specific partition
                bool success = await engine.ErasePartitionAsync(partition, null, System.Threading.CancellationToken.None);
                
                if (success)
                {
                    return FrpResult.Ok($"Successfully cleared FRP partition ({partition}) via MTK BROM protocol.", reboot: true);
                }
                else
                {
                    return FrpResult.Fail($"MTK DA failed to erase '{partition}'. Device potentially Auth-Locked.");
                }
            }
            catch (Exception ex)
            {
                return FrpResult.Fail($"MTK Error: {ex.Message}");
            }
        }
    }
}
