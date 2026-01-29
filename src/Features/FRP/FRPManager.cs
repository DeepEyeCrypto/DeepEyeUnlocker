using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Engines;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Features.FRP
{
    /// <summary>
    /// Orchestrates FRP (Factory Reset Protection) bypass operations across different protocols.
    /// </summary>
    public class FRPManager
    {
        private readonly IProtocolEngine _engine;

        public FRPManager(IProtocolEngine engine)
        {
            _engine = engine ?? throw new ArgumentNullException(nameof(engine));
        }

        public async Task<bool> ExecuteFRPResetAsync(FRPResetPlan plan, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Logger.Info($"Starting FRP Reset for device via {_engine.Name}");
            
            try
            {
                // 1. Mandatory Backup
                progress.Report(ProgressUpdate.Info(10, "Backing up persistent partitions..."));
                foreach (var part in plan.TargetPartitions)
                {
                    await BackupPartitionAsync(part, progress, ct);
                }

                // 2. Erase/Reset execution
                progress.Report(ProgressUpdate.Info(50, "Bypassing FRP protection..."));
                foreach (var part in plan.TargetPartitions)
                {
                    if (ct.IsCancellationRequested) return false;
                    
                    bool erased = await _engine.ErasePartitionAsync(part, progress, ct);
                    if (!erased)
                    {
                        Logger.Error($"Failed to erase FRP partition: {part}");
                        return false;
                    }
                }

                progress.Report(ProgressUpdate.Info(100, "FRP Reset Successful. Rebooting..."));
                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "FRP Reset operation failed");
                return false;
            }
        }

        private async Task BackupPartitionAsync(string partitionName, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            // Simple backup logic to the local 'backups/frp_temp' directory
            string backupDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "backups", "frp_recovery");
            Directory.CreateDirectory(backupDir);
            
            string filePath = Path.Combine(backupDir, $"{partitionName}_{DateTime.Now:yyyyMMdd}.img");
            using var fs = new FileStream(filePath, FileMode.Create);
            
            await _engine.ReadPartitionToStreamAsync(partitionName, fs, progress, ct);
            Logger.Info($"Emergency FRP backup created: {filePath}");
        }
    }

    public class FRPResetPlan
    {
        public string ModelName { get; set; } = string.Empty;
        public List<string> TargetPartitions { get; set; } = new();
        public bool RequiresAuthBypass { get; set; }
    }
}
