using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.DeviceHealth.Interfaces;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Features.DeviceHealth.Readers
{
    public class KernelAudit : IDeviceHealthReader
    {
        public string Name => "Kernel & SPL Auditor";

        public async Task ReadAsync(DeviceHealthReport report, CancellationToken ct = default)
        {
            Logger.Debug("Auditing Security Patch Levels...");
            await Task.Delay(500, ct); // Simulation

            report.SecurityPatchLevel = "2023-11-01";
            report.KernelVersion = "5.10.168-android12-9-00001-g8e8e8e8e8e8e";

            DateTime splDate;
            if (DateTime.TryParse(report.SecurityPatchLevel, out splDate))
            {
                if (splDate < DateTime.Now.AddMonths(-12))
                {
                    report.AuditFindings.Add("[CRITICAL] Security patch is older than 1 year. Device is vulnerable.");
                }
            }
        }
    }
}
