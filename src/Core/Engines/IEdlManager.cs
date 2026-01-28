using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Engines
{
    /// <summary>
    /// Interface for managing EDL (Emergency Download Mode) operations on Qualcomm devices
    /// </summary>
    public interface IEdlManager
    {
        /// <summary>
        /// Attempt to reboot the device into EDL mode using available software methods
        /// </summary>
        /// <param name="device">Device context with current connection info</param>
        /// <param name="ct">Cancellation token</param>
        /// <returns>Result indicating success/failure and method used</returns>
        Task<EdlResult> RebootToEdlAsync(DeviceContext device, CancellationToken ct);

        /// <summary>
        /// Check if any device is currently connected in EDL mode (VID:PID 05C6:9008)
        /// </summary>
        Task<bool> IsInEdlModeAsync(CancellationToken ct);

        /// <summary>
        /// Get EDL capability classification for a specific device
        /// </summary>
        EdlCapability GetCapabilityFor(DeviceContext device);

        /// <summary>
        /// Get the full EDL profile for a device if available
        /// </summary>
        EdlProfile? GetProfileFor(DeviceContext device);

        /// <summary>
        /// Get test point information for hardware EDL entry
        /// </summary>
        TestPointInfo? GetTestPointInfo(DeviceContext device);

        /// <summary>
        /// Wait for a device to appear in EDL mode after reboot command
        /// </summary>
        Task<bool> WaitForEdlModeAsync(CancellationToken ct, int timeoutSeconds = 15);
    }
}
