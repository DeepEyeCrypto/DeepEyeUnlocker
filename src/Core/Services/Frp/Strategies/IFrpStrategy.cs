using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Services;

namespace DeepEyeUnlocker.Core.Services.Frp.Strategies
{
    public interface IFrpStrategy
    {
        /// <summary>
        /// Returns true if this strategy can handle the device state described in context.
        /// </summary>
        bool CanHandle(FrpServiceContext ctx);

        /// <summary>
        /// Executes the FRP service operation.
        /// Guaranteed to be called only if CanHandle returned true.
        /// </summary>
        FrpResult Execute(FrpServiceContext ctx);
    }
}
