using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Features.Analytics.Models;

namespace DeepEyeUnlocker.Features.Analytics.Services
{
    public class AnalyticsProService
    {
        private readonly List<ChipsetSuccessRate> _heatmapCache = new();
        private readonly List<CveAlert> _activeAlerts = new();

        public AnalyticsProService()
        {
            // Seed with mock data for MVP
            _heatmapCache.Add(new ChipsetSuccessRate { Chipset = "MT6765", Attempts = 1500, Successes = 1420, AverageLatencyMs = 450 });
            _heatmapCache.Add(new ChipsetSuccessRate { Chipset = "MT6893", Attempts = 800, Successes = 750, AverageLatencyMs = 380 });
            _heatmapCache.Add(new ChipsetSuccessRate { Chipset = "T610", Attempts = 2000, Successes = 1850, AverageLatencyMs = 520 });
            _heatmapCache.Add(new ChipsetSuccessRate { Chipset = "SC9863A", Attempts = 3000, Successes = 2100, AverageLatencyMs = 610 });
        }

        public Task<FleetHeatmap> GetGlobalHeatmapAsync()
        {
            Logger.Info("[ANALYTICS] Generating success heatmap for global fleet...");
            var heatmap = new FleetHeatmap
            {
                SuccessRates = _heatmapCache,
                GeneratedAt = DateTime.UtcNow
            };
            return Task.FromResult(heatmap);
        }

        public Task<List<CveAlert>> ScanForNewAlertsAsync(string connectedChipset)
        {
            Logger.Info($"[ANALYTICS] Scanning CVE database for {connectedChipset}...");
            var alerts = new List<CveAlert>();

            if (connectedChipset.Contains("MT6765"))
            {
                alerts.Add(new CveAlert 
                { 
                    CveId = "CVE-2024-5501", 
                    Description = "BROM Buffer Overflow allows arbitrary code execution.",
                    Severity = 9.8,
                    AffectedChipset = "MT6765"
                });
            }

            if (connectedChipset.Contains("T610"))
            {
                alerts.Add(new CveAlert 
                { 
                    CveId = "CVE-2025-1011", 
                    Description = "FDL Payload Injection Vulnerability via UART.",
                    Severity = 8.5,
                    AffectedChipset = "T610"
                });
            }

            _activeAlerts.AddRange(alerts);
            return Task.FromResult(alerts);
        }
    }
}
