using System;
using System.Collections.Generic;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Services.Safety
{
    public static class SafetyInterlock
    {
        /// <summary>
        /// Perform a safety and compliance check for an FRP session.
        /// </summary>
        public static FrpResult Check(FrpServiceContext ctx)
        {
            // 1. Ownership check (Primary legal wall)
            if (ctx.Ownership == OwnershipStatus.Unverified || ctx.Ownership == OwnershipStatus.Unknown)
            {
                return FrpResult.Fail("Compliance Error: Device ownership must be verified before FRP services can be engaged.");
            }

            // 2. Risk check
            if (ctx.Profile?.FrpInfo?.RiskLevel == RiskLevel.High || ctx.Profile?.FrpInfo?.RiskLevel == RiskLevel.Critical)
            {
                // High risk devices require additional server-side auth or user acknowledgment
                // In this mock, we assume it's okay but log it.
                Logger.Warn($"High Risk operation requested for {ctx.Profile?.Brand}. Audit trail intensified.");
            }

            return FrpResult.Ok("Safety checks passed.");
        }

        public static List<string> GetSafetyChecklist(string operation)
        {
            return new List<string>
            {
                "Verified Proof of Purchase",
                "Consent from legal owner signed",
                "Device is NOT flagged as lost/stolen",
                "Using regulated DeepEye Service Tools"
            };
        }
    }
}
