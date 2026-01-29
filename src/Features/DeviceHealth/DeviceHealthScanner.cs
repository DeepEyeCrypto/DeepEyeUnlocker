using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure;
using DeepEyeUnlocker.Features.DeviceHealth.Interfaces;
using DeepEyeUnlocker.Features.DeviceHealth.Readers;

namespace DeepEyeUnlocker.Features.DeviceHealth
{
    public class DeviceHealthScanner
    {
        private readonly IAdbClient _adb;
        private readonly List<IDeviceHealthReader> _readers = new();

        public DeviceHealthScanner(IAdbClient adb)
        {
            _adb = adb ?? throw new ArgumentNullException(nameof(adb));
            
            // Register Phase 1 Readers (Architecture Tier 1)
            _readers.Add(new ImeiReader(_adb));
            _readers.Add(new MacReader(_adb));
            _readers.Add(new BatteryReader(_adb));
            _readers.Add(new KernelAudit(_adb));
        }

        public async Task<DeviceHealthReport> ScanAsync(CancellationToken ct = default)
        {
            Logger.Info("Initializing Deep Device Health Audit...");
            var report = new DeviceHealthReport
            {
                ScanTimestamp = DateTime.UtcNow,
                ToolVersion = "1.5.0-proto" // Advancement toward Phase 1 goals
            };

            // Basic Identification (Phase 0/1)
            report.SerialNumber = (await _adb.ExecuteShellAsync("getprop ro.serialno", ct)).Trim();
            report.AndroidVersion = (await _adb.ExecuteShellAsync("getprop ro.build.version.release", ct)).Trim();
            report.BuildNumber = (await _adb.ExecuteShellAsync("getprop ro.build.display.id", ct)).Trim();

            // Run specialized readers (Architecture Tier 1)
            foreach (var reader in _readers)
            {
                try
                {
                    Logger.Debug($"Running {reader.Name}...");
                    await reader.ReadAsync(report, ct);
                }
                catch (Exception ex)
                {
                    Logger.Error(ex, $"Reader {reader.Name} failed");
                    report.AuditFindings.Add($"[Error] {reader.Name} failed: {ex.Message}");
                }
            }

            // Final Root/Security Checks
            await FinalSecurityChecks(report, ct);

            Logger.Info($"Health Audit finished for {report.SerialNumber}. Findings: {report.AuditFindings.Count}");
            return report;
        }

        private async Task FinalSecurityChecks(DeviceHealthReport report, CancellationToken ct)
        {
            var suCheck = await _adb.ExecuteShellAsync("which su", ct);
            report.IsRooted = !string.IsNullOrEmpty(suCheck) && suCheck.Contains("/su");
            
            if (report.IsRooted)
            {
                var magisk = await _adb.ExecuteShellAsync("magisk -v", ct);
                report.RootMethod = !string.IsNullOrEmpty(magisk) ? $"Magisk {magisk.Trim()}" : "Generic SU";
            }

            report.IsBootloaderUnlocked = (await _adb.ExecuteShellAsync("getprop ro.boot.flash.locked", ct)).Trim() == "0";
            report.IsOemUnlockEnabled = (await _adb.ExecuteShellAsync("getprop ro.oem_unlock_supported", ct)).Trim() == "1";
        }
    }
}
