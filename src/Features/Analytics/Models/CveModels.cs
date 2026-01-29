using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Features.Analytics.Models
{
    public class CveInfo
    {
        public string Id { get; set; } = "";
        public string Description { get; set; } = "";
        public string Severity { get; set; } = "Unknown"; // Low, Medium, High, Critical
        public string AffectedComponent { get; set; } = "";
        public string FixedInPatch { get; set; } = "";
    }

    public class CveReport
    {
        public string DeviceSerial { get; set; } = "";
        public DateTime ScanTime { get; set; } = DateTime.Now;
        public List<CveInfo> Vulnerabilities { get; set; } = new();
        public int RiskScore => Vulnerabilities.Count == 0 ? 0 : Vulnerabilities.Max(v => GetSeverityScore(v.Severity));

        private int GetSeverityScore(string severity) => severity switch {
            "Critical" => 10,
            "High" => 7,
            "Medium" => 4,
            "Low" => 2,
            _ => 1
        };
    }
}
