using System;
using System.IO;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols;
using DeepEyeUnlocker.Infrastructure.Logging;

namespace DeepEyeUnlocker.Operations
{
    public class BackupOperation : Operation
    {
        private readonly IProtocol _protocol;

        public BackupOperation(IProtocol protocol)
        {
            _protocol = protocol;
            Name = "Full Partition Backup";
        }

        public override async Task<bool> ExecuteAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            try
            {
                Logger.Info($"Starting unified backup using {_protocol.Name}...");
                var partitions = await _protocol.GetPartitionTableAsync();
                if (ct.IsCancellationRequested) return false;
                
                string backupDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "backups", DateTime.Now.ToString("yyyyMMdd_HHmmss"));
                if (!Directory.Exists(backupDir)) Directory.CreateDirectory(backupDir);

                int completed = 0;
                foreach (var part in partitions)
                {
                    if (ct.IsCancellationRequested)
                    {
                        Report(progress, 0, "Backup cancelled by user.", LogLevel.Warn);
                        return false;
                    }

                    Logger.Info($"Backing up {part.Name} ({part.SizeInBytes} bytes)...");
                    Report(progress, (int)((float)completed / partitions.Count * 100), $"Backing up {part.Name}...");
                    
                    try 
                    {
                        var data = await _protocol.ReadPartitionAsync(part.Name);
                        string filePath = Path.Combine(backupDir, $"{part.Name}.img");
                        if (data != null && data.Length > 0)
                        {
                            await File.WriteAllBytesAsync(filePath, data, ct);
                        }
                    }
                    catch (Exception ex)
                    {
                        Logger.Error($"Critical failure backing up {part.Name}: {ex.Message}");
                    }

                    completed++;
                    int progressValue = (int)((float)completed / partitions.Count * 100);
                    Report(progress, progressValue, $"Progress: {completed}/{partitions.Count} ({part.Name})");
                }

                Logger.Info($"Backup complete. Saved to: {backupDir}");
                Report(progress, 100, "Backup completed successfully.");
                return true;
            }
            catch (OperationCanceledException)
            {
                return false;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Full Backup Operation failed.");
                Report(progress, 0, "Backup failed: " + ex.Message, LogLevel.Error);
                return false;
            }
        }
    }
}
