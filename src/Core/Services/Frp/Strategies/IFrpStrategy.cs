using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Services;

namespace DeepEyeUnlocker.Core.Services.Frp.Strategies
{
    public interface IFrpStrategy
    {
        bool CanHandle(FrpServiceContext ctx);
        Task<FrpResult> ExecuteAsync(FrpServiceContext ctx);
    }
}
