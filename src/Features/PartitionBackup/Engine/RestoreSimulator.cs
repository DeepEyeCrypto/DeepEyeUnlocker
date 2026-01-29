using System;
using System.IO;
using System.IO.Compression;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Features.PartitionBackup.Models;
using Newtonsoft.Json;

namespace DeepEyeUnlocker.Features.PartitionBackup.Engine
{
    public class RestoreSimulator
    {
        private const int BufferSize = 1024 * 1024;

        public async Task<bool> ValidateBackupAsync(string manifestPath, string serial, CancellationToken ct)
        {
            try
            {
                var manifestText = await File.ReadAllTextAsync(manifestPath, ct);
                var manifest = JsonConvert.DeserializeObject<BackupManifest>(manifestText);
                if (manifest == null) return false;

                if (manifest.DeviceSerial != serial)
                {
                    Core.Logger.Warning($"Backup serial mismatch: {manifest.DeviceSerial} vs {serial}");
                    return false;
                }

                string dir = Path.GetDirectoryName(manifestPath)!;
                foreach (var entry in manifest.Entries)
                {
                    string fullPath = Path.Combine(dir, entry.FileName);
                    if (!await VerifyEntryAsync(entry, fullPath, serial, ct))
                    {
                        Core.Logger.Error($"Integrity check failed for {entry.PartitionName}");
                        return false;
                    }
                }

                return true;
            }
            catch (Exception ex)
            {
                Core.Logger.Error(ex, "Restore simulation failed");
                return false;
            }
        }

        private async Task<bool> VerifyEntryAsync(PartitionBackupEntry entry, string path, string serial, CancellationToken ct)
        {
            if (!File.Exists(path)) return false;

            using var fileStream = File.OpenRead(path);
            using var sha256 = SHA256.Create();

            Stream sourceStream = fileStream;

            // 1. Decryption Layer
            if (entry.IsEncrypted)
            {
                byte[] key = DeriveKey(serial);
                byte[] iv = new byte[16];
                await fileStream.ReadAsync(iv, 0, 16, ct);

                var aes = Aes.Create();
                aes.Key = key;
                aes.IV = iv;

                sourceStream = new CryptoStream(fileStream, aes.CreateDecryptor(), CryptoStreamMode.Read);
            }

            // 2. Decompression Layer
            if (entry.IsCompressed)
            {
                sourceStream = new GZipStream(sourceStream, CompressionMode.Decompress);
            }

            // 3. Verification hashing (Read everything)
            byte[] buffer = new byte[BufferSize];
            int read;
            ulong totalRead = 0;

            while ((read = await sourceStream.ReadAsync(buffer, 0, buffer.Length, ct)) > 0)
            {
                sha256.TransformBlock(buffer, 0, read, null, 0);
                totalRead += (ulong)read;
            }

            sha256.TransformFinalBlock(Array.Empty<byte>(), 0, 0);
            string calculatedHash = BitConverter.ToString(sha256.Hash!).Replace("-", "").ToLower();

            return calculatedHash == entry.Sha256Hash;
        }

        private byte[] DeriveKey(string serial)
        {
            using var rfc = new Rfc2898DeriveBytes(serial, Encoding.UTF8.GetBytes("DeepEyeSalt"), 1000, HashAlgorithmName.SHA256);
            return rfc.GetBytes(32);
        }
    }
}
