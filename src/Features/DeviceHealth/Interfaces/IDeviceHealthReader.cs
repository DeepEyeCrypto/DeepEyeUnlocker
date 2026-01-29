using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Features.DeviceHealth.Interfaces
{
    public interface IDeviceHealthReader
    {
        string Name { get; }
        Task ReadAsync(DeviceHealthReport report, CancellationToken ct = default);
    }
}
