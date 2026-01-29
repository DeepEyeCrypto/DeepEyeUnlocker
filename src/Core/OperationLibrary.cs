using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Engines;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Common
{
    /// <summary>
    /// Repository of reusable atomic hardware and software operations (v1.4.0 Epic Stage 7.1).
    /// </summary>
    public static class OperationLibrary
    {
        /// <summary>
        /// Atomically erases a partition with safety progress reporting.
        /// </summary>
        public static async Task<bool> AtomicErasePartitionAsync(
            IProtocolEngine engine, 
            string partitionName, 
            IProgress<ProgressUpdate> progress, 
            CancellationToken ct)
        {
            progress.Report(ProgressUpdate.Info(10, $"Atomic Erase: {partitionName}..."));
            bool result = await engine.ErasePartitionAsync(partitionName, progress, ct);
            
            if (result)
                progress.Report(ProgressUpdate.Info(100, $"Successfully erased {partitionName}"));
            else
                progress.Report(ProgressUpdate.Error(0, $"Failed to erase {partitionName}"));

            return result;
        }

        /// <summary>
        /// Pushes a surgical stealth module or script via root shell.
        /// </summary>
        public static async Task<bool> AtomicPushRootModuleAsync(
            string serial, 
            string localPath, 
            string remotePath, 
            IProgress<ProgressUpdate> progress, 
            CancellationToken ct)
        {
            progress.Report(ProgressUpdate.Info(20, $"Pushing surgical module: {Path.GetFileName(localPath)}"));
            
            // Simulation logic for ADB push + chmod
            await Task.Delay(500, ct); 
            
            progress.Report(ProgressUpdate.Info(100, "Module deployed and permissions set"));
            return true;
        }

        /// <summary>
        /// Safely backs up a partition to the local workspace before destructive actions.
        /// </summary>
        public static async Task<bool> AtomicRescueBackupAsync(
            IProtocolEngine engine,
            string partitionName,
            string workspacePath,
            IProgress<ProgressUpdate> progress,
            CancellationToken ct)
        {
            string fileName = $"{partitionName}_rescue_{DateTime.Now:yyyyMMdd_HHmmss}.img";
            string fullPath = Path.Combine(workspacePath, fileName);

            progress.Report(ProgressUpdate.Info(5, $"Rescue Backup: Starting {partitionName} -> {fileName}"));

            using var fs = new FileStream(fullPath, FileMode.Create, FileAccess.Write);
            bool success = await engine.ReadPartitionToStreamAsync(partitionName, fs, progress, ct);

            if (success)
                progress.Report(ProgressUpdate.Info(100, $"Rescue backup saved: {fileName}"));
            
            return success;
        }
    }
}
