using System;
using System.IO;
using System.Security.Cryptography;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Engines;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Features.PartitionRestore
{
    /// <summary>
    /// Core logic for granular partition restoration with safety validation.
    /// </summary>
    public class RestoreManager
    {
        private readonly IProtocolEngine _engine;

        public RestoreManager(IProtocolEngine engine)
        {
            _engine = engine ?? throw new ArgumentNullException(nameof(engine));
        }

        public async Task<bool> RestorePartitionAsync(RestoreJob job, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Logger.Info($"Starting restoration of {job.PartitionName} using file {job.ImagePath}");

            try
            {
                // 1. Validation: File Existence
                if (!File.Exists(job.ImagePath))
                {
                    Logger.Error($"Restore image not found: {job.ImagePath}");
                    return false;
                }

                var fileInfo = new FileInfo(job.ImagePath);
                long imageSize = fileInfo.Length;

                // 2. Validation: Target Partition Check
                var partitions = await _engine.GetPartitionTableAsync();
                var target = partitions.FirstOrDefault(p => p.Name.Equals(job.PartitionName, StringComparison.OrdinalIgnoreCase));

                if (target == null)
                {
                    Logger.Error($"Partition {job.PartitionName} not found on device.");
                    return false;
                }

                // 3. Size Safeguard
                if ((ulong)imageSize > target.SizeInBytes)
                {
                    Logger.Error($"Size mismatch! Image ({imageSize} bytes) is larger than partition ({target.SizeInBytes} bytes).");
                    return false;
                }

                // 4. Pre-Restore Backup (Mandatory for High-Risk)
                if (IsHighRisk(job.PartitionName))
                {
                    progress.Report(ProgressUpdate.Warning(10, $"Safety: Backing up current {job.PartitionName}..."));
                    await SafetyBackupAsync(job.PartitionName, progress, ct);
                }

                // 5. Execution
                progress.Report(ProgressUpdate.Info(20, $"Flashing {job.PartitionName}..."));
                using (var fs = new FileStream(job.ImagePath, FileMode.Open, FileAccess.Read))
                {
                    bool success = await _engine.WritePartitionFromStreamAsync(job.PartitionName, fs, progress, ct);
                    
                    if (success)
                    {
                        progress.Report(ProgressUpdate.Info(100, $"Successfully restored {job.PartitionName}."));
                        return true;
                    }
                }

                return false;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, $"Critical failure during restore of {job.PartitionName}");
                return false;
            }
        }

        private bool IsHighRisk(string partitionName)
        {
            string p = partitionName.ToLower();
            return p.Contains("efs") || p.Contains("modem") || p.Contains("nv") || p.Contains("persist") || p.Contains("vbmeta");
        }

        private async Task SafetyBackupAsync(string partitionName, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            string backupDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "backups", "safety_pre_restore");
            Directory.CreateDirectory(backupDir);
            
            string filePath = Path.Combine(backupDir, $"{partitionName}_PRE_{DateTime.Now:yyyyMMdd_HHmmss}.img");
            using var fs = new FileStream(filePath, FileMode.Create);
            await _engine.ReadPartitionToStreamAsync(partitionName, fs, progress, ct);
            Logger.Warn($"High-risk safety backup created: {filePath}");
        }
    }

    public class RestoreJob
    {
        public string PartitionName { get; set; } = string.Empty;
        public string ImagePath { get; set; } = string.Empty;
        public string? ExpectedSha256 { get; set; }
    }
}
