using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.DeviceHealth.Interfaces;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Features.DeviceHealth.Readers
{
    public class MacReader : IDeviceHealthReader
    {
        public string Name => "Network ID Auditor";

        public async Task ReadAsync(DeviceHealthReport report, CancellationToken ct = default)
        {
            Logger.Debug("Extracting MAC addresses...");
            await Task.Delay(300, ct); // Simulation

            report.MacAddress = "BC:D0:74:AA:BB:CC";
            report.BluetoothAddress = "BC:D0:74:11:22:33";
        }
    }
}
