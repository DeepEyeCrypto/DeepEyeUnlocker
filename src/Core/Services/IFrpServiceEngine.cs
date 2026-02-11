using System.Collections.Generic;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Services
{
    public interface IFrpServiceEngine
    {
        /// <summary>
        /// Check if this engine supports FRP service for the given context (device + detected mode)
        /// </summary>
        bool IsSupported(FrpServiceContext ctx);

        /// <summary>
        /// Safely check lock status (Read-Only)
        /// Returns "LOCKED", "UNLOCKED", "UNKNOWN"
        /// </summary>
        string CheckLockStatus(FrpServiceContext ctx); 

        /// <summary>
        /// Get instructions for official removal (e.g. Settings menu)
        /// </summary>
        string GetOfficialInstructions(FrpServiceContext ctx);

        /// <summary>
        /// Execute the service clear operation. MUST verify ownership first.
        /// Throws AccessViolationException if ownership is Unverified.
        /// </summary>
        FrpResult ExecuteServiceClear(FrpServiceContext ctx);
    }

    public class FrpResult
    {
        public bool Success { get; set; }
        public string Message { get; set; } = string.Empty;
        public bool RequiresReboot { get; set; }
        public Dictionary<string, string> Logs { get; set; } = new();

        public static FrpResult Fail(string msg) => new FrpResult { Success = false, Message = msg };
        public static FrpResult Ok(string msg, bool reboot = true) => new FrpResult { Success = true, Message = msg, RequiresReboot = reboot };
    }
}
