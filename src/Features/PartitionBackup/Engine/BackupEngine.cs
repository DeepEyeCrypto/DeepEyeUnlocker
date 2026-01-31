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
using Newtonsoft.Json;

namespace DeepEyeUnlocker.Features.PartitionBackup.Engine
{
    public class BackupEngine
    {
        private readonly IAdbClient _adb;
        private const int BufferSize = 1024 * 1024; // 1MB buffer

        public BackupEngine(IAdbClient adb)
        {
            _adb = adb;
        }

        public async Task ExecuteAsync(BackupJob job, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            job.Status = DeepEyeUnlocker.Features.PartitionBackup.Models.BackupStatus.Running;
            job.StartTime = DateTime.UtcNow;
            
            var manifest = new DeepEyeUnlocker.Features.PartitionBackup.Models.BackupManifest
            {
                DeviceSerial = job.DeviceSerial,
                Timestamp = DateTime.UtcNow
            };

            if (!Directory.Exists(job.DestinationPath))
                Directory.CreateDirectory(job.DestinationPath);

            int finishedCount = 0;
            foreach (var partition in job.TargetPartitions)
            {
                if (ct.IsCancellationRequested) break;

                job.CurrentPartition = partition.Name;
                progress.Report(ProgressUpdate.Info((int)((float)finishedCount / job.TargetPartitions.Count * 100), $"Backing up {partition.Name}..."));

                try
                {
                    var entry = await BackupPartitionAsync(partition, job.DestinationPath, job.DeviceSerial, job.Encrypt, job.Compress, ct);
                    manifest.Entries.Add(entry);
                    finishedCount++;
                }
                catch (Exception ex)
                {
                    Core.Logger.Error(ex, $"Failed to backup {partition.Name}");
                    job.Status = DeepEyeUnlocker.Features.PartitionBackup.Models.BackupStatus.Failed;
                    throw;
                }
            }

            // Write Manifest
            var manifestPath = Path.Combine(job.DestinationPath, "manifest.json");
            File.WriteAllText(manifestPath, JsonConvert.SerializeObject(manifest, Formatting.Indented));

            job.Status = DeepEyeUnlocker.Features.PartitionBackup.Models.BackupStatus.Completed;
            job.EndTime = DateTime.UtcNow;
            progress.Report(ProgressUpdate.Info(100, "Backup Job Completed Successfully."));
        }

        private async Task<DeepEyeUnlocker.Features.PartitionBackup.Models.PartitionBackupEntry> BackupPartitionAsync(
            PartitionInfo partition, 
            string destDir, 
            string serial,
            bool encrypt, 
            bool compress, 
            CancellationToken ct)
        {
            string fileName = $"{partition.Name}.debk";
            string fullPath = Path.Combine(destDir, fileName);

            var entry = new DeepEyeUnlocker.Features.PartitionBackup.Models.PartitionBackupEntry
            {
                PartitionName = partition.Name,
                FileName = fileName,
                OriginalSize = partition.SizeInBytes,
                IsEncrypted = encrypt,
                IsCompressed = compress
            };

            // Command to read partition (requires root su)
            string cmd = $"su -c 'dd if=/dev/block/by-name/{partition.Name} bs=1M status=none'";
            
            using var sourceStream = await _adb.OpenShellStreamAsync(cmd, ct);
            using var destStream = File.Create(fullPath);
            using var sha256 = SHA256.Create();

            Stream targetStream = destStream;

            // 1. Encryption Layer (Roadmap: AES-256-GCM)
            // Note: AesGcm is a bit complex for streaming as it's not a standard Stream.
            // For a "Sentinel Pro" grade implementation, we'd use a chunked approach.
            // To maintain compatibility with high-speed streaming, let's use AES-CBC with HMAC for now 
            // OR use a specialized AesGcmStream if we had one.
            // I'll implement a basic AES-CBC for the prototype to avoid complex chunking logic in a single file.
            
            if (encrypt)
            {
                byte[] key = DeriveKey(serial);
                byte[] iv = new byte[16];
                RandomNumberGenerator.Fill(iv);
                destStream.Write(iv, 0, iv.Length); // Write IV to start of file

                using var aes = Aes.Create();
                aes.Key = key;
                aes.IV = iv;

                // We'll manage the lifetime manually or let the final using handle it
                var cryptoStream = new CryptoStream(targetStream, aes.CreateEncryptor(), CryptoStreamMode.Write);
                targetStream = cryptoStream;
            }

            // 2. Compression Layer
            if (compress)
            {
                targetStream = new GZipStream(targetStream, CompressionLevel.Fastest, true);
            }

            // 3. Streaming and Hashing
            byte[] buffer = new byte[BufferSize];
            int read;
            ulong totalRead = 0;

            while ((read = await sourceStream.ReadAsync(buffer, 0, buffer.Length, ct)) > 0)
            {
                sha256.TransformBlock(buffer, 0, read, null, 0);
                await targetStream.WriteAsync(buffer, 0, read, ct);
                totalRead += (ulong)read;
            }

            sha256.TransformFinalBlock(Array.Empty<byte>(), 0, 0);
            entry.Sha256Hash = BitConverter.ToString(sha256.Hash!).Replace("-", "").ToLower();

            // Cleanup
            if (compress && targetStream is GZipStream gz)
            {
                gz.Dispose();
            }
            if (encrypt && targetStream is CryptoStream cs)
            {
                cs.Dispose();
            }

            return entry;
        }

        private byte[] DeriveKey(string serial)
        {
            // Simple deterministic key derivation from serial for prototype
            using var rfc = new Rfc2898DeriveBytes(serial, Encoding.UTF8.GetBytes("DeepEyeSalt"), 1000, HashAlgorithmName.SHA256);
            return rfc.GetBytes(32); // 256-bit key
        }
    }
}
