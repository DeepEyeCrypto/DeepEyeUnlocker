using System;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure;
using DeepEyeUnlocker.Features.DeviceHealth.Interfaces;

namespace DeepEyeUnlocker.Features.DeviceHealth.Readers
{
    public class BatteryReader : IDeviceHealthReader
    {
        private readonly IAdbClient _adb;
        public string Name => "Battery Health Reader";

        public BatteryReader(IAdbClient adb)
        {
            _adb = adb;
        }

        public async Task ReadAsync(DeviceHealthReport report, CancellationToken ct = default)
        {
            var output = await _adb.ExecuteShellAsync("dumpsys battery", ct);
            if (string.IsNullOrEmpty(output)) return;

            report.BatteryLevel = ParseMatch(output, @"level: (\d+)", 0);
            report.BatteryTemperature = ParseMatch(output, @"temperature: (\d+)", 0) / 10.0;
            
            int healthCode = ParseMatch(output, @"health: (\d+)", 1);
            report.BatteryHealth = healthCode switch
            {
                2 => 100, // Good
                3 => 60,  // Overheat
                4 => 30,  // Dead
                5 => 80,  // Over voltage
                _ => 50
            };

            report.BatteryStatus = ParseMatch(output, @"status: (\d+)", 1) switch
            {
                2 => "Charging",
                3 => "Discharging",
                4 => "Not charging",
                5 => "Full",
                _ => "Unknown"
            };

            // Enhanced audit for battery
            if (report.BatteryTemperature > 45.0)
                report.AuditFindings.Add($"[Battery] Critical Temperature: {report.BatteryTemperature}°C");

            if (report.BatteryHealth < 70)
                report.AuditFindings.Add($"[Battery] Degraded Health Status: {healthCode}");
        }

        private int ParseMatch(string input, string pattern, int defaultValue)
        {
            var match = Regex.Match(input, pattern);
            return (match.Success && int.TryParse(match.Groups[1].Value, out int val)) ? val : defaultValue;
        }
    }
}
