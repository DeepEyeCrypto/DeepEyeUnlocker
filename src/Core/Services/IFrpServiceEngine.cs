using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Models
{
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

namespace DeepEyeUnlocker.Core.Services
{
    public interface IFrpServiceEngine
    {
        bool IsSupported(FrpServiceContext ctx);
        Task<string> CheckLockStatusAsync(FrpServiceContext ctx); 
        string GetOfficialInstructions(FrpServiceContext ctx);
        Task<FrpResult> ExecuteServiceClearAsync(FrpServiceContext ctx);
    }
}
