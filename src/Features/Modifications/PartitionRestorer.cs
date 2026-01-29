using System;
using System.IO;
using System.IO.Compression;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure;
using DeepEyeUnlocker.Features.PartitionBackup.Models;
using DeepEyeUnlocker.Features.PartitionBackup.Engine;
using Newtonsoft.Json;

namespace DeepEyeUnlocker.Features.Modifications
{
    public class PartitionRestorer
    {
        private readonly IAdbClient _adb;
        private readonly RestoreSimulator _simulator;
        private const int BufferSize = 1024 * 1024;

        public PartitionRestorer(IAdbClient adb)
        {
            _adb = adb;
            _simulator = new RestoreSimulator();
        }

        public async Task RestoreAsync(string manifestPath, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            progress.Report(ProgressUpdate.Info(0, "Starting Verification..."));
            
            var manifestText = await File.ReadAllTextAsync(manifestPath, ct);
            var manifest = JsonConvert.DeserializeObject<DeepEyeUnlocker.Features.PartitionBackup.Models.BackupManifest>(manifestText);
            if (manifest == null) throw new Exception("Invalid manifest");

            // 1. MUST verify integrity first
            bool valid = await _simulator.ValidateBackupAsync(manifestPath, _adb.TargetSerial ?? "", ct);
            if (!valid) throw new Exception("Backup integrity validation failed. Restore aborted for safety.");

            progress.Report(ProgressUpdate.Info(10, "Verification Passed. Starting Restore..."));

            string dir = Path.GetDirectoryName(manifestPath)!;
            int finished = 0;

            foreach (var entry in manifest.Entries)
            {
                if (ct.IsCancellationRequested) break;

                progress.Report(ProgressUpdate.Info((int)((float)finished / manifest.Entries.Count * 100), $"Restoring {entry.PartitionName}..."));
                
                string filePath = Path.Combine(dir, entry.FileName);
                await RestoreSinglePartitionAsync(entry, filePath, _adb.TargetSerial ?? "", progress, ct);
                
                finished++;
            }

            progress.Report(ProgressUpdate.Info(100, "Restore Operation Completed."));
        }

        private async Task RestoreSinglePartitionAsync(
            DeepEyeUnlocker.Features.PartitionBackup.Models.PartitionBackupEntry entry, 
            string localPath, 
            string serial,
            IProgress<ProgressUpdate> progress,
            CancellationToken ct)
        {
            // Command to write to partition
            string cmd = $"su -c 'dd of=/dev/block/by-name/{entry.PartitionName} bs=1M status=none'";
            
            using var destStream = await _adb.OpenShellWritableStreamAsync(cmd, ct);
            using var fileStream = File.OpenRead(localPath);
            
            Stream sourceStream = fileStream;

            // 1. Decryption Layer
            if (entry.IsEncrypted)
            {
                byte[] key = DeriveKey(serial);
                byte[] iv = new byte[16];
                await fileStream.ReadAsync(iv, 0, 16, ct);

                using var aes = Aes.Create();
                aes.Key = key;
                aes.IV = iv;

                sourceStream = new CryptoStream(fileStream, aes.CreateDecryptor(), CryptoStreamMode.Read);
            }

            // 2. Decompression Layer
            if (entry.IsCompressed)
            {
                sourceStream = new GZipStream(sourceStream, CompressionMode.Decompress);
            }

            // 3. Streaming to Device
            byte[] buffer = new byte[BufferSize];
            int read;
            while ((read = await sourceStream.ReadAsync(buffer, 0, buffer.Length, ct)) > 0)
            {
                await destStream.WriteAsync(buffer, 0, read, ct);
            }

            await destStream.FlushAsync(ct);
            destStream.Close(); // Close to signal EOF to DD
        }

        private byte[] DeriveKey(string serial)
        {
            using var rfc = new Rfc2898DeriveBytes(serial, Encoding.UTF8.GetBytes("DeepEyeSalt"), 1000, HashAlgorithmName.SHA256);
            return rfc.GetBytes(32);
        }
    }
}
