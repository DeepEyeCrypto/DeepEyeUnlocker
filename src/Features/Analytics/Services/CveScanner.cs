using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.Analytics.Models;

namespace DeepEyeUnlocker.Features.Analytics.Services
{
    public class CveScanner
    {
        private static readonly List<CveInfo> _vulnerabilityDb = new()
        {
            new CveInfo { Id = "CVE-2023-21250", Severity = "Critical", AffectedComponent = "System", FixedInPatch = "2023-06-01", Description = "Remote code execution in System component." },
            new CveInfo { Id = "CVE-2023-21144", Severity = "High", AffectedComponent = "Kernel", FixedInPatch = "2023-05-01", Description = "Privilege escalation in the kernel." },
            new CveInfo { Id = "CVE-2022-20412", Severity = "High", AffectedComponent = "Framework", FixedInPatch = "2022-11-01", Description = "Information disclosure in Framework." },
            new CveInfo { Id = "CVE-2021-0309", Severity = "Medium", AffectedComponent = "Media Framework", FixedInPatch = "2021-03-01", Description = "Denial of service in Media Framework." },
            new CveInfo { Id = "CVE-2024-0012", Severity = "Critical", AffectedComponent = "System", FixedInPatch = "2024-01-01", Description = "Critical vulnerability in system service." },
            new CveInfo { Id = "CVE-2024-31317", Severity = "High", AffectedComponent = "Framework", FixedInPatch = "2024-06-01", Description = "Information disclosure in Framework due to out of bounds read." },
            new CveInfo { Id = "CVE-2023-40088", Severity = "Critical", AffectedComponent = "System", FixedInPatch = "2023-12-01", Description = "RCE in system service." }
        };

        public CveReport Scan(DeviceHealthReport health)
        {
            var report = new CveReport { DeviceSerial = health.SerialNumber };
            
            if (DateTime.TryParse(health.SecurityPatchLevel, out DateTime patchDate))
            {
                foreach (var v in _vulnerabilityDb)
                {
                    if (DateTime.TryParse(v.FixedInPatch, out DateTime fixDate))
                    {
                        if (patchDate < fixDate)
                        {
                            report.Vulnerabilities.Add(v);
                        }
                    }
                }
            }

            return report;
        }
    }
}
