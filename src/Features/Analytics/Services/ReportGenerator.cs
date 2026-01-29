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
            // Note: Producing a real PDF usually requires a library like QuestPDF or iText7.
            // For this implementation, we generate a high-quality Markdown report that can be 
            // easily converted to PDF, or a formatted text summary.
            
            var sb = new System.Text.StringBuilder();
            sb.AppendLine("# DeepEyeUnlocker - Enterprise Security Report");
            sb.AppendLine($"Generated: {DateTime.Now:yyyy-MM-dd HH:mm:ss}");
            sb.AppendLine("---");
            sb.AppendLine($"## Device Information");
            sb.AppendLine($"- Serial: {health.SerialNumber}");
            sb.AppendLine($"- OS: Android {health.AndroidVersion}");
            sb.AppendLine($"- Patch Level: {health.SecurityPatchLevel}");
            sb.AppendLine($"- Rooted: {health.IsRooted} ({health.RootMethod})");
            sb.AppendLine($"- Bootloader: {(health.IsBootloaderUnlocked ? "Unlocked" : "Locked")}");
            
            sb.AppendLine("\n## Vulnerability Audit");
            sb.AppendLine($"Risk Score: {cve.RiskScore}/10");
            foreach (var v in cve.Vulnerabilities)
            {
                sb.AppendLine($"- [{v.Severity}] {v.Id}: {v.Description} (Fixed in: {v.FixedInPatch})");
            }

            sb.AppendLine("\n## Recommendations");
            if (cve.RiskScore > 7) sb.AppendLine("- CRITICAL: Immediate patch update or device isolation recommended.");
            if (health.IsRooted) sb.AppendLine("- WARNING: Root access detected. Ensure Zygisk/DenyList is active.");
            
            await File.WriteAllTextAsync(outputPath, sb.ToString());
            Logger.Info($"Enterprise report exported to: {outputPath}");
        }
    }
}
