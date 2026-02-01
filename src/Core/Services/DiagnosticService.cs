using System;
using System.Text;
using System.Runtime.InteropServices;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Core.Services
{
    public class DiagnosticService
    {
        public string GenerateSystemReport()
        {
            var sb = new StringBuilder();
            sb.AppendLine("=== DEEPEYEUNLOCKER DIAGNOSTIC REPORT ===");
            sb.AppendLine($"Timestamp: {DateTime.Now:yyyy-MM-dd HH:mm:ss}");
            sb.AppendLine($"App Version: {VersionManager.FullVersionDisplay}");
            sb.AppendLine("-----------------------------------------");
            
            sb.AppendLine($"OS: {RuntimeInformation.OSDescription}");
            sb.AppendLine($"Arch: {RuntimeInformation.OSArchitecture}");
            sb.AppendLine($"Framework: {RuntimeInformation.FrameworkDescription}");
            
            sb.AppendLine("--- ENVIRONMENT ---");
            sb.AppendLine($"Machine Name: {Environment.MachineName}");
            sb.AppendLine($"Processor Count: {Environment.ProcessorCount}");
            
            sb.AppendLine("--- DRIVER STATUS (STUB) ---");
            sb.AppendLine("LibUsb: Present");
            // Future: Add WMI calls to check for specific mobile drivers (Samsung/ADB/MTK)
            
            sb.AppendLine("-----------------------------------------");
            sb.AppendLine("END REPORT");
            
            return sb.ToString();
        }

        public void ExportReport(string path)
        {
            var report = GenerateSystemReport();
            System.IO.File.WriteAllText(path, report);
        }
    }
}
