using System;
using System.Collections.Generic;
using System.Linq;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.Analytics.Models;

namespace DeepEyeUnlocker.Features.Analytics.Services
{
    public class FleetAnalytics
    {
        public class FleetSummary
        {
            public int ManagedDevicesCount { get; set; }
            public int HealthAlertsCount { get; set; }
            public int CriticalCvesCount { get; set; }
            public double AverageRiskScore { get; set; }
            public List<string> CommonVulnerabilities { get; set; } = new();
        }

        public FleetSummary GenerateSummary(IEnumerable<DeviceHealthReport> healthReports, IEnumerable<CveReport> cveReports)
        {
            var summary = new FleetSummary
            {
                ManagedDevicesCount = healthReports.Count()
            };

            if (summary.ManagedDevicesCount == 0) return summary;

            summary.HealthAlertsCount = healthReports.Count(h => !h.IsHealthy);
            
            var allCves = cveReports.SelectMany(r => r.Vulnerabilities).ToList();
            summary.CriticalCvesCount = allCves.Count(v => v.Severity == "Critical");
            
            summary.AverageRiskScore = cveReports.Average(r => r.RiskScore);
            
            summary.CommonVulnerabilities = allCves.GroupBy(v => v.Id)
                                                   .OrderByDescending(g => g.Count())
                                                   .Take(5)
                                                   .Select(g => g.Key)
                                                   .ToList();

            return summary;
        }
    }
}
