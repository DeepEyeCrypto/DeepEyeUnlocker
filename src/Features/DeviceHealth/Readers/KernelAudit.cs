using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure;
using DeepEyeUnlocker.Features.DeviceHealth.Interfaces;

namespace DeepEyeUnlocker.Features.DeviceHealth.Readers
{
    public class KernelAudit : IDeviceHealthReader
    {
        private readonly IAdbClient _adb;
        public string Name => "Kernel & Security Audit";

        public KernelAudit(IAdbClient adb)
        {
            _adb = adb;
        }

        public async Task ReadAsync(DeviceHealthReport report, CancellationToken ct = default)
        {
            report.KernelVersion = (await _adb.ExecuteShellAsync("uname -rs", ct)).Trim();
            report.SecurityPatchLevel = (await _adb.ExecuteShellAsync("getprop ro.build.version.security_patch", ct)).Trim();
            report.VerifiedBootState = (await _adb.ExecuteShellAsync("getprop ro.boot.verifiedbootstate", ct)).Trim();
            
            var selinux = await _adb.ExecuteShellAsync("getenforce", ct);
            report.IsSelinuxEnforcing = selinux.Contains("Enforcing");

            // Audit
            if (string.Compare(report.SecurityPatchLevel, "2024-01-01") < 0)
                report.AuditFindings.Add($"[Security] Outdated Patch Level: {report.SecurityPatchLevel}");

            if (!report.IsSelinuxEnforcing)
                report.AuditFindings.Add("[Security] SELinux is NOT Enforcing (Permissive/Disabled)");

            if (report.VerifiedBootState == "orange" || report.VerifiedBootState == "red")
                report.AuditFindings.Add($"[Security] Verified Boot State is compromised: {report.VerifiedBootState}");
        }
    }
}
