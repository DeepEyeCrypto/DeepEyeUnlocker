using System;
using System.IO;
using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Protocols;

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

        public override async Task<bool> ExecuteAsync(Device device)
        {
            try
            {
                Logger.Info($"Starting unified backup using {_protocol.Name}...");
                var partitions = await _protocol.GetPartitionTableAsync();
                
                string backupDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "backups", DateTime.Now.ToString("yyyyMMdd_HHmmss"));
                if (!Directory.Exists(backupDir)) Directory.CreateDirectory(backupDir);

                int completed = 0;
                foreach (var part in partitions)
                {
                    Logger.Info($"Backing up {part.Name} ({part.Size} bytes)...");
                    
                    try 
                    {
                        var data = await _protocol.ReadPartitionAsync(part.Name);
                        string filePath = Path.Combine(backupDir, $"{part.Name}.img");
                        if (data != null && data.Length > 0)
                        {
                            await File.WriteAllBytesAsync(filePath, data);
                        }
                    }
                    catch (Exception ex)
                    {
                        Logger.Error($"Critical failure backing up {part.Name}: {ex.Message}");
                    }

                    completed++;
                    int progress = (int)((float)completed / partitions.Count * 100);
                    ReportProgress(progress, $"Progress: {completed}/{partitions.Count} ({part.Name})");
                }

                Logger.Info($"Backup complete. Saved to: {backupDir}");
                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Full Backup Operation failed.");
                return false;
            }
        }
    }
}
