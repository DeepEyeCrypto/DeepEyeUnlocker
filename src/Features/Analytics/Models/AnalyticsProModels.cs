using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Features.Analytics.Models
{
    public class ChipsetSuccessRate
    {
        public string Chipset { get; set; } = string.Empty;
        public int Attempts { get; set; }
        public int Successes { get; set; }
        public double SuccessRate => Attempts == 0 ? 0 : (double)Successes / Attempts * 100.0;
        public double AverageLatencyMs { get; set; }
    }

    public class FleetHeatmap
    {
        public string Region { get; set; } = "Global";
        public List<ChipsetSuccessRate> SuccessRates { get; set; } = new();
        public DateTime GeneratedAt { get; set; } = DateTime.UtcNow;
    }

    public class CveAlert
    {
        public string CveId { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public string AffectedChipset { get; set; } = string.Empty;
        public double Severity { get; set; }
        public DateTime DetectedAt { get; set; } = DateTime.UtcNow;
    }
}
