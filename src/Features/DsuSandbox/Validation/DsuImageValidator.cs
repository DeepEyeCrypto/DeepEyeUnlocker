using System;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.DsuSandbox.Models;

namespace DeepEyeUnlocker.Features.DsuSandbox.Validation
{
    /// <summary>
    /// Validates DSU/GSI images before flashing
    /// </summary>
    public class DsuImageValidator
    {
        // Known magic bytes for Android images
        private static readonly byte[] ANDROID_BOOT_MAGIC = { 0x41, 0x4E, 0x44, 0x52, 0x4F, 0x49, 0x44, 0x21 }; // "ANDROID!"
        private static readonly byte[] SPARSE_MAGIC = { 0x3A, 0xFF, 0x26, 0xED }; // Sparse image
        private static readonly byte[] EXT4_MAGIC_OFFSET = { 0x53, 0xEF }; // EXT4 at offset 0x438

        /// <summary>
        /// Validate an image file for DSU flashing
        /// </summary>
        public async Task<DsuValidationResult> ValidateAsync(string imagePath, DeviceContext device, DsuCapability capability)
        {
            var result = new DsuValidationResult();

            // Check file exists
            if (!File.Exists(imagePath))
            {
                result.Errors.Add($"Image file not found: {imagePath}");
                return result;
            }

            var fileInfo = new FileInfo(imagePath);

            // Validate file format
            result.ImageFormatValid = await ValidateImageFormat(imagePath);
            if (!result.ImageFormatValid)
            {
                result.Errors.Add("Invalid image format. Must be a valid Android system image (sparse or raw ext4).");
            }

            // Check file size (typical system images are 1-5 GB)
            var sizeMb = fileInfo.Length / (1024 * 1024);
            if (sizeMb < 500)
            {
                result.Warnings.Add($"Image size ({sizeMb} MB) is unusually small for a system image.");
            }
            if (sizeMb > 8192)
            {
                result.Warnings.Add($"Image size ({sizeMb} MB) is very large. Ensure sufficient storage.");
            }

            // Check device storage space (need 2.5x image size)
            var requiredSpace = (long)(fileInfo.Length * 2.5);
            if (capability.FreeSpaceBytes < requiredSpace)
            {
                result.Errors.Add($"Insufficient storage. Need {FormatSize(requiredSpace)}, " +
                                 $"available: {FormatSize(capability.FreeSpaceBytes)}");
                result.SpaceAvailable = false;
            }
            else
            {
                result.SpaceAvailable = true;
            }

            // Check Android version compatibility
            if (device.AndroidVersion != null && int.TryParse(device.AndroidVersion.Split('.')[0], out int androidVer))
            {
                if (androidVer < 10)
                {
                    result.Errors.Add("DSU requires Android 10 or higher.");
                }
            }

            // Check architecture compatibility (basic heuristic)
            var deviceArch = device.Properties.GetValueOrDefault("ro.product.cpu.abi", "arm64-v8a");
            // Assume arm64 GSI works on most modern devices
            if (!deviceArch.Contains("arm64") && !deviceArch.Contains("x86_64"))
            {
                result.Warnings.Add($"Device architecture ({deviceArch}) may have GSI compatibility issues.");
            }

            result.CompatibilityOk = result.Errors.Count == 0;
            return result;
        }

        /// <summary>
        /// Validate checksum against known GSI checksums
        /// </summary>
        public async Task<bool> ValidateChecksumAsync(string imagePath, string expectedSha256)
        {
            if (string.IsNullOrEmpty(expectedSha256))
                return true; // No checksum to validate

            try
            {
                using var sha256 = SHA256.Create();
                using var stream = File.OpenRead(imagePath);
                
                var hash = await Task.Run(() => sha256.ComputeHash(stream));
                var hashString = BitConverter.ToString(hash).Replace("-", "").ToLowerInvariant();
                
                return hashString.Equals(expectedSha256.ToLowerInvariant());
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to compute checksum");
                return false;
            }
        }

        /// <summary>
        /// Compute SHA256 checksum of an image
        /// </summary>
        public async Task<string> ComputeChecksumAsync(string imagePath, IProgress<int>? progress = null)
        {
            using var sha256 = SHA256.Create();
            using var stream = File.OpenRead(imagePath);
            
            var totalSize = stream.Length;
            var buffer = new byte[1024 * 1024]; // 1MB buffer
            long totalRead = 0;
            int bytesRead;

            while ((bytesRead = await stream.ReadAsync(buffer, 0, buffer.Length)) > 0)
            {
                sha256.TransformBlock(buffer, 0, bytesRead, buffer, 0);
                totalRead += bytesRead;
                progress?.Report((int)((totalRead * 100) / totalSize));
            }

            sha256.TransformFinalBlock(Array.Empty<byte>(), 0, 0);
            return BitConverter.ToString(sha256.Hash!).Replace("-", "").ToLowerInvariant();
        }

        /// <summary>
        /// Validate the image format by checking magic bytes
        /// </summary>
        private async Task<bool> ValidateImageFormat(string imagePath)
        {
            try
            {
                using var stream = File.OpenRead(imagePath);
                var header = new byte[1024];
                await stream.ReadAsync(header, 0, header.Length);

                // Check for sparse image format
                if (header.Take(4).SequenceEqual(SPARSE_MAGIC))
                {
                    Logger.Debug("Image format: Android sparse image");
                    return true;
                }

                // Check for raw ext4 (magic at offset 0x438)
                if (stream.Length > 0x43A)
                {
                    stream.Seek(0x438, SeekOrigin.Begin);
                    var ext4Magic = new byte[2];
                    await stream.ReadAsync(ext4Magic, 0, 2);
                    if (ext4Magic.SequenceEqual(EXT4_MAGIC_OFFSET))
                    {
                        Logger.Debug("Image format: Raw ext4 filesystem");
                        return true;
                    }
                }

                // Check for ANDROID! boot image (not typically used for system, but valid)
                if (header.Take(8).SequenceEqual(ANDROID_BOOT_MAGIC))
                {
                    Logger.Debug("Image format: Android boot image");
                    return true;
                }

                Logger.Warn("Unknown image format - proceeding with caution");
                return true; // Allow unknown formats but warn
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to validate image format");
                return false;
            }
        }

        private static string FormatSize(long bytes)
        {
            string[] sizes = { "B", "KB", "MB", "GB", "TB" };
            double len = bytes;
            int order = 0;
            while (len >= 1024 && order < sizes.Length - 1)
            {
                order++;
                len = len / 1024;
            }
            return $"{len:0.##} {sizes[order]}";
        }
    }
}
