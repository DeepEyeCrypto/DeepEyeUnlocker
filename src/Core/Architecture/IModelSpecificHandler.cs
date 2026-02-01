using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Architecture
{
    /// <summary>
    /// Interface for precision handlers tied to specific device models.
    /// Represents the "UnlockTool Mode" where operations are hand-tuned for a specific device.
    /// </summary>
    public interface IModelSpecificHandler
    {
        /// <summary>
        /// The specific model number this handler supports (e.g., "SM-S921B").
        /// </summary>
        string TargetModel { get; }

        /// <summary>
        /// List of operations explicitly supported by this handler.
        /// </summary>
        string[] SupportedOperations { get; }

        /// <summary>
        /// Executes a precise operation on the device.
        /// </summary>
        Task<OperationResult> ExecuteAsync(
            string operation,
            DeviceProfile device,
            Dictionary<string, object> parameters,
            IProtocolPlugin underlyingProtocol // The active connection (ADB, Odin, EDL)
        );
    }
}
