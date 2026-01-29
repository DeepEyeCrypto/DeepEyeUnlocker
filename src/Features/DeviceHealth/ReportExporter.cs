using System;
using System.IO;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Features.DeviceHealth
{
    public enum ExportFormat
    {
        Markdown,
        Json,
        PlainText
    }

    public static class ReportExporter
    {
        public static async Task ExportAsync(DeviceHealthReport report, string filePath, ExportFormat format)
        {
            string content = format switch
            {
                ExportFormat.Json => JsonSerializer.Serialize(report, new JsonSerializerOptions { WriteIndented = true }),
                ExportFormat.Markdown => GenerateMarkdown(report),
                ExportFormat.PlainText => GeneratePlainText(report),
                _ => throw new ArgumentException("Unsupported format", nameof(format))
            };

            await File.WriteAllTextAsync(filePath, content);
        }

        private static string GenerateMarkdown(DeviceHealthReport report)
        {
            var sb = new StringBuilder();
            sb.AppendLine($"# DeepEye Device Health Audit: {report.SerialNumber}");
            sb.AppendLine($"**Generated:** {report.ScanTimestamp:yyyy-MM-dd HH:mm:ss} UTC");
            sb.AppendLine($"**Tool Version:** {report.ToolVersion}");
            sb.AppendLine();

            sb.AppendLine("## 📱 Device Identification");
            sb.AppendLine($"| Property | Value |");
            sb.AppendLine($"|---|---|");
            sb.AppendLine($"| Serial Number | `{report.SerialNumber}` |");
            sb.AppendLine($"| Android Version | {report.AndroidVersion} |");
            sb.AppendLine($"| Security Patch | {report.SecurityPatchLevel} |");
            sb.AppendLine($"| Build Number | {report.BuildNumber} |");
            sb.AppendLine();

            sb.AppendLine("## 🔋 Hardware & Battery");
            sb.AppendLine($"| Property | Value |");
            sb.AppendLine($"|---|---|");
            sb.AppendLine($"| Battery Level | {report.BatteryLevel}% |");
            sb.AppendLine($"| Battery Health | {report.BatteryHealth}% |");
            sb.AppendLine($"| Temperature | {report.BatteryTemperature}°C |");
            sb.AppendLine($"| Status | {report.BatteryStatus} |");
            sb.AppendLine();

            sb.AppendLine("## 🔒 Security Posture");
            sb.AppendLine($"| Property | Value |");
            sb.AppendLine($"|---|---|");
            sb.AppendLine($"| Bootloader | {(report.IsBootloaderUnlocked ? "🔓 Unlocked" : "🔒 Locked")} |");
            sb.AppendLine($"| Root Status | {(report.IsRooted ? $"🔴 Rooted ({report.RootMethod})" : "🟢 Not Rooted")} |");
            sb.AppendLine($"| SELinux | {(report.IsSelinuxEnforcing ? "✅ Enforcing" : "⚠️ Permissive")} |");
            sb.AppendLine($"| Dev Options | {(report.IsDevOptionsEnabled ? "⚠️ Enabled" : "✅ Disabled")} |");
            sb.AppendLine();

            if (report.AuditFindings.Count > 0)
            {
                sb.AppendLine("## 🚩 Audit Findings");
                foreach (var finding in report.AuditFindings)
                {
                    sb.AppendLine($"- {finding}");
                }
            }

            return sb.ToString();
        }

        private static string GeneratePlainText(DeviceHealthReport report)
        {
            var sb = new StringBuilder();
            sb.AppendLine("========================================");
            sb.AppendLine(" DEEPEYE DEVICE HEALTH REPORT ");
            sb.AppendLine("========================================");
            sb.AppendLine($"Time: {report.ScanTimestamp}");
            sb.AppendLine($"Serial: {report.SerialNumber}");
            sb.AppendLine($"Android: {report.AndroidVersion}");
            sb.AppendLine($"Rooted: {report.IsRooted} ({report.RootMethod})");
            sb.AppendLine($"Bootloader: {(report.IsBootloaderUnlocked ? "Unlocked" : "Locked")}");
            sb.AppendLine($"Battery: {report.BatteryLevel}% ({report.BatteryStatus})");
            sb.AppendLine("========================================");
            return sb.ToString();
        }
    }
}
