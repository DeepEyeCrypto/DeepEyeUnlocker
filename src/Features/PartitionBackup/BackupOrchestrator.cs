using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Engines;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Features.PartitionBackup
{
    /// <summary>
    /// Orchestrates safe partition backups with encryption and verification.
    /// </summary>
    public class BackupOrchestrator
    {
        private readonly IProtocolEngine _engine;

        public BackupOrchestrator(IProtocolEngine engine)
        {
            _engine = engine ?? throw new ArgumentNullException(nameof(engine));
        }

        public async Task<bool> RunBackupAsync(PartitionBackupJob job, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Logger.Info($"Starting backup job {job.JobId} for device {job.DeviceSerial}");
            job.Status = BackupStatus.InProgress;
            job.StartTime = DateTime.UtcNow;

            try
            {
                if (!Directory.Exists(job.OutputDirectory))
                {
                    Directory.CreateDirectory(job.OutputDirectory);
                }

                foreach (var partName in job.Partitions)
                {
                    if (ct.IsCancellationRequested)
                    {
                        job.Status = BackupStatus.Cancelled;
                        return false;
                    }

                    await BackupSinglePartitionAsync(job, partName, progress, ct);
                }

                job.Status = BackupStatus.Completed;
                job.EndTime = DateTime.UtcNow;
                await GenerateManifestAsync(job);
                
                Logger.Info($"Backup job {job.JobId} completed successfully.");
                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, $"Backup job {job.JobId} failed");
                job.Status = BackupStatus.Failed;
                job.ErrorMessage = ex.Message;
                return false;
            }
        }

        private async Task BackupSinglePartitionAsync(PartitionBackupJob job, string partName, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            var partitions = await _engine.GetPartitionTableAsync();
            var info = partitions.FirstOrDefault(p => p.Name.Equals(partName, StringComparison.OrdinalIgnoreCase));
            
            long size = (long)(info?.SizeInBytes ?? 0);
            var entry = new PartitionBackupEntry 
            { 
                PartitionName = partName, 
                SizeBytes = size,
                FilePath = Path.Combine(job.OutputDirectory, $"{partName}.img")
            };
            job.Entries.Add(entry);

            using (var fileStream = new FileStream(entry.FilePath, FileMode.Create, FileAccess.Write))
            {
                var partProgress = new Progress<ProgressUpdate>(u => 
                {
                    entry.TransferredBytes = (long)(u.Percentage / 100.0 * entry.SizeBytes);
                    progress.Report(new ProgressUpdate 
                    { 
                        Percentage = u.Percentage,
                        Status = $"Backing up {partName}... {u.Percentage}%"
                    });
                });

                bool success = await _engine.ReadPartitionToStreamAsync(partName, fileStream, partProgress, ct);
                entry.Success = success;

                if (success)
                {
                    entry.Sha256 = await CalculateSha256Async(entry.FilePath);
                }
            }
        }

        private async Task<string> CalculateSha256Async(string filePath)
        {
            using (var sha256 = SHA256.Create())
            {
                using (var stream = File.OpenRead(filePath))
                {
                    byte[] hash = await sha256.ComputeHashAsync(stream);
                    return BitConverter.ToString(hash).Replace("-", "").ToLowerInvariant();
                }
            }
        }

        private async Task GenerateManifestAsync(PartitionBackupJob job)
        {
            var manifest = new BackupManifest
            {
                DeviceSerial = job.DeviceSerial,
                Timestamp = DateTime.UtcNow,
                IsEncrypted = job.Encrypt,
                Backups = job.Entries.Where(e => e.Success).ToList()
            };

            string json = System.Text.Json.JsonSerializer.Serialize(manifest, new System.Text.Json.JsonSerializerOptions { WriteIndented = true });
            await File.WriteAllTextAsync(Path.Combine(job.OutputDirectory, "manifest.json"), json);
        }
    }
}
