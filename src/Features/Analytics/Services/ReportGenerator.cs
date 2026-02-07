using System;
using System.IO;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.Analytics.Models;

namespace DeepEyeUnlocker.Features.Analytics.Services
{
    public class ReportGenerator
    {
        public static async Task ExportPdfReportAsync(DeviceHealthReport health, CveReport cve, string outputPath)
        {
            var sb = new System.Text.StringBuilder();
            sb.AppendLine("=================================================");
            sb.AppendLine("      DEEPEYE UNLOCKER - SENTINEL PRO           ");
            sb.AppendLine("         ENTERPRISE SECURITY AUDIT");
            sb.AppendLine("=================================================");
            sb.AppendLine($"Report UUID: {Guid.NewGuid()}");
            sb.AppendLine($"Timestamp:   {DateTime.Now:yyyy-MM-dd HH:mm:ss}");
            sb.AppendLine("-------------------------------------------------");
            
            sb.AppendLine("\n### [ 📱 DEVICE IDENTITY ]");
            sb.AppendLine($"  %-15s: {health.SerialNumber}".Replace("%-15s", "SERIAL_NO"));
            sb.AppendLine($"  %-15s: Android {health.AndroidVersion}".Replace("%-15s", "OS_PLATFORM"));
            sb.AppendLine($"  %-15s: {health.SecurityPatchLevel}".Replace("%-15s", "PATCH_LEVEL"));
            sb.AppendLine($"  %-15s: {(health.IsRooted ? "YES (PROhibited)" : "NO")}".Replace("%-15s", "ROOT_ACCESS"));
            sb.AppendLine($"  %-15s: {(health.IsBootloaderUnlocked ? "UNLOCKED" : "LOCKED")}".Replace("%-15s", "BOOTLOADER"));
            
            sb.AppendLine("\n### [ 🛡️ VULNERABILITY ANALYSIS ]");
            int filled = (int)Math.Round(cve.RiskScore);
            string scoreBar = "[" + new string('█', filled) + new string('░', 10 - filled) + "]";
            sb.AppendLine($"  THREAT_LVL : {scoreBar} {cve.RiskScore}/10");
            
            foreach (var v in cve.Vulnerabilities)
            {
                sb.AppendLine($"  [ {v.Severity.ToUpper()} ] {v.Id}");
                sb.AppendLine($"    - Desc: {v.Description}");
            }

            sb.AppendLine("\n### [ 📋 DIRECTIVES ]");
            if (cve.RiskScore >= 7) sb.AppendLine("  ! CRITICAL: This device pose an IMMEDIATE security threat.");
            if (health.IsRooted) sb.AppendLine("  * WARNING: System integrity compromised by su binary.");
            
            sb.AppendLine("\n=================================================");
            sb.AppendLine("      DIGITALLY SIGNED BY DEEPEYE ENGINE");
            sb.AppendLine("=================================================");
            
            await File.WriteAllTextAsync(outputPath, sb.ToString());
            Logger.Info($"Enterprise report exported to: {outputPath}");
        }

        public static async Task ExportFleetReportAsync(FleetAnalytics.FleetSummary fleet, string outputPath)
        {
            var sb = new System.Text.StringBuilder();
            sb.AppendLine("=================================================");
            sb.AppendLine("      DEEPEYE UNLOCKER - FLEET HQ              ");
            sb.AppendLine("         GLOBAL INFRASTRUCTURE SUMMARY");
            sb.AppendLine("=================================================");
            sb.AppendLine($"Date: {DateTime.Now:yyyy-MM-dd}");
            sb.AppendLine($"Managed Nodes: {fleet.ManagedDevicesCount}");
            sb.AppendLine("-------------------------------------------------");

            sb.AppendLine("\n## GLOBAL RISK PROFILE");
            int filled = (int)Math.Round(fleet.AverageRiskScore);
            string scoreBar = "[" + new string('█', filled) + new string('░', 10 - filled) + "]";
            sb.AppendLine($"AVG RISK SCORE: {scoreBar} {fleet.AverageRiskScore:F1}/10");

            sb.AppendLine($"\n## ALERT STATISTICS");
            sb.AppendLine($"- Health Alerts   : {fleet.HealthAlertsCount}");
            sb.AppendLine($"- Critical CVEs   : {fleet.CriticalCvesCount}");

            sb.AppendLine($"\n## TOP SECURITY EXPOSURES");
            foreach (var cveId in fleet.CommonVulnerabilities)
            {
                sb.AppendLine($"- {cveId}");
            }

            sb.AppendLine("\n=================================================");
            sb.AppendLine("      (C) 2026 DEEPEYE LABS ENTERPRISE");
            sb.AppendLine("=================================================");

            await File.WriteAllTextAsync(outputPath, sb.ToString());
            Logger.Info($"Fleet report exported: {outputPath}");
        }
    }
}
