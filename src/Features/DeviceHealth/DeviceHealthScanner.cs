using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Features.DeviceHealth.Interfaces;
using DeepEyeUnlocker.Features.DeviceHealth.Readers;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.Features.DeviceHealth
{
    public class DeviceHealthScanner
    {
        private readonly List<IDeviceHealthReader> _readers = new();

        public DeviceHealthScanner()
        {
            _readers.Add(new ImeiReader());
            _readers.Add(new MacReader());
            _readers.Add(new BatteryReader());
            _readers.Add(new KernelAudit());
        }

        public DeviceHealthScanner(IAdbClient adb) : this() { }

        public async Task<DeviceHealthReport> ScanAsync(DeviceContext? context = null, CancellationToken ct = default)
        {
            var serial = context?.Serial ?? "UNKNOWN";
            Logger.Info($"[HEALTH] Initializing deep audit for {serial}...");
            
            var report = new DeviceHealthReport
            {
                SerialNumber = serial,
                ScanTimestamp = DateTime.UtcNow,
                ToolVersion = "2.0.0-pro"
            };

            foreach (var reader in _readers)
            {
                if (ct.IsCancellationRequested) break;
                
                try
                {
                    await reader.ReadAsync(report, ct);
                }
                catch (Exception ex)
                {
                    Logger.Warn($"[HEALTH] {reader.Name} failed: {ex.Message}");
                    report.AuditFindings.Add($"[Error] {reader.Name}: {ex.Message}");
                }
            }

            return report;
        }
    }
}
