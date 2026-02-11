using System;
using System.IO;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Services.Frp
{
    public static class FrpAuditLogger
    {
        private static readonly string AuditLogPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "logs", "frp_audit.log");

        public static void LogOperation(FrpServiceContext ctx, FrpResult result)
        {
            try
            {
                var directory = Path.GetDirectoryName(AuditLogPath);
                if (!Directory.Exists(directory)) Directory.CreateDirectory(directory);

                var logEntry = $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] " +
                               $"VERIFIED: {ctx.Ownership} | " +
                               $"DEVICE: {ctx.Profile?.Brand} {ctx.Profile?.ModelNumber} | " +
                               $"ACTION: ServiceClearFRP | " +
                               $"REASON: {ctx.UserReason} | " +
                               $"RESULT: {(result.Success ? "SUCCESS" : "FAILED")} | " +
                               $"MSG: {result.Message}";

                File.AppendAllLines(AuditLogPath, new[] { logEntry });
            }
            catch (Exception ex)
            {
                // Fallback to standard logger if file IO fails
                Logger.Error(ex, "Failed to write FRP audit log.");
            }
        }
    }
}
