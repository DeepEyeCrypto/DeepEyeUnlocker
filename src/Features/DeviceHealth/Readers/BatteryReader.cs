using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.DeviceHealth.Interfaces;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Features.DeviceHealth.Readers
{
    public class BatteryReader : IDeviceHealthReader
    {
        public string Name => "Battery Analyst";

        public async Task ReadAsync(DeviceHealthReport report, CancellationToken ct = default)
        {
            Logger.Debug("Extracting Battery Telemetry...");
            await Task.Delay(400, ct); // Simulation

            report.BatteryLevel = 92;
            report.BatteryHealth = 88;
            report.BatteryStatus = "Discharging";
            report.BatteryTemperature = 32.5;

            if (report.BatteryHealth < 80)
            {
                report.AuditFindings.Add($"[WARNING] Battery health is at {report.BatteryHealth}%. Replacement advised.");
            }
        }
    }
}
