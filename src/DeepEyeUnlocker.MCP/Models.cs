using System.Collections.Generic;

namespace DeepEyeUnlocker.MCP.Models
{
    public class TestDiscoveryResult
    {
        public List<string> Tests { get; set; } = new();
    }

    public class TestResult
    {
        public bool Success { get; set; }
        public string Message { get; set; } = string.Empty;
        public string Log { get; set; } = string.Empty;
    }

    public class CoverageReport
    {
        public double CoveragePercent { get; set; }
        public List<string> UncoveredPaths { get; set; } = new();
    }
}
