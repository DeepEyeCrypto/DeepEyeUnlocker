using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Architecture
{
    /// <summary>
    /// Represents a "Miracle Mode" universal plugin that can execute operations 
    /// directly on a chipset family without model-specific handlers.
    /// </summary>
    public interface IUniversalPlugin : IProtocolPlugin
    {
        /// <summary>
        /// Executes a universal operation based on chipset capability.
        /// </summary>
        Task<OperationResult> ExecuteOperationAsync(
            string operation,
            Dictionary<string, object> parameters,
            DeviceProfile device
        );

        /// <summary>
        /// Executes legacy operations for keypad/feature phones.
        /// </summary>
        Task<OperationResult> ExecuteKeypadOperationAsync(
            string operation,
            DeviceProfile device
        );
    }
}
