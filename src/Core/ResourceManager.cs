using System;
using System.Net.Http;
using System.IO;
using System.Security.Cryptography;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Generic;
using System.Linq;
using Newtonsoft.Json;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core
{
    public class ResourceManager
    {
        private readonly HttpClient _client = new HttpClient();
        private const string ManifestUrl = "https://repo.deepeyeunlocker.io/manifest.json";
        private const string DownloadBaseUrl = "https://repo.deepeyeunlocker.io/resource/";
        private ResourceManifest? _cachedManifest;

        public async Task<ResourceItem?> FindResourceAsync(DeviceContext device, string type = "Programmer")
        {
            var manifest = await GetManifestAsync();
            if (manifest == null) return null;

            // Hierarchical search: 
            // 1. Match specific model + chipset
            // 2. Match brand + chipset
            // 3. Match chipset generic
            return manifest.Items.FirstOrDefault(i => 
                       i.Type == type && 
                       i.Brand.Equals(device.Brand, StringComparison.OrdinalIgnoreCase) && 
                       i.SupportedModels.Contains(device.Model, StringComparer.OrdinalIgnoreCase))
                ?? manifest.Items.FirstOrDefault(i => 
                       i.Type == type && 
                       i.Brand.Equals(device.Brand, StringComparison.OrdinalIgnoreCase) && 
                       i.Chipset.Equals(device.Chipset, StringComparison.OrdinalIgnoreCase))
                ?? manifest.Items.FirstOrDefault(i => 
                       i.Type == type && 
                       i.Chipset.Equals(device.Chipset, StringComparison.OrdinalIgnoreCase));
        }

        public async Task<string?> EnsureResourceAsync(ResourceItem item, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            string localDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Resources", item.Type + "s");
            string localPath = Path.Combine(localDir, item.FileName);

            if (File.Exists(localPath))
            {
                if (VerifyChecksum(localPath, item.Checksum))
                {
                    Logger.Info($"Found validated resource: {item.FileName}", "RESOURCES");
                    return localPath;
                }
                Logger.Warn($"Checksum mismatch for {item.FileName}, re-downloading...", "RESOURCES");
            }

            return await DownloadResourceAsync(item, localPath, progress, ct);
        }

        private async Task<string?> DownloadResourceAsync(ResourceItem item, string localPath, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            try
            {
                string? directory = Path.GetDirectoryName(localPath);
                if (directory != null && !Directory.Exists(directory)) Directory.CreateDirectory(directory);

                string url = string.IsNullOrEmpty(item.RemoteUrl) ? $"{DownloadBaseUrl}{item.FileName}" : item.RemoteUrl;
                
                using var response = await _client.GetAsync(url, HttpCompletionOption.ResponseHeadersRead, ct);
                response.EnsureSuccessStatusCode();

                var totalBytes = response.Content.Headers.ContentLength ?? -1L;
                using var contentStream = await response.Content.ReadAsStreamAsync(ct);
                using var fileStream = new FileStream(localPath, FileMode.Create, FileAccess.Write, FileShare.None, 8192, true);

                var buffer = new byte[8192];
                long totalRead = 0;
                int read;

                while ((read = await contentStream.ReadAsync(buffer, 0, buffer.Length, ct)) > 0)
                {
                    await fileStream.WriteAsync(buffer.AsMemory(0, read), ct);
                    totalRead += read;

                    if (totalBytes != -1)
                    {
                        int pct = (int)((totalRead * 100) / totalBytes);
                        progress?.Report(new ProgressUpdate { Percentage = pct, Status = $"Downloading {item.Name}...", Category = "RESOURCES" });
                    }
                }

                fileStream.Close();
                if (VerifyChecksum(localPath, item.Checksum))
                {
                    Logger.Success($"Resource {item.Name} downloaded and verified.", "RESOURCES");
                    return localPath;
                }
                
                Logger.Error($"Downloaded file {item.Name} failed integrity check.");
                return null;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, $"Failed to download resource: {item.Name}");
                return null;
            }
        }

        private bool VerifyChecksum(string path, string expectedChecksum)
        {
            if (string.IsNullOrEmpty(expectedChecksum)) return true; // Skip if no checksum provided
            try
            {
                using var sha = SHA256.Create();
                using var stream = File.OpenRead(path);
                var hash = sha.ComputeHash(stream);
                string actual = BitConverter.ToString(hash).Replace("-", "").ToLower();
                return actual.Equals(expectedChecksum.ToLower(), StringComparison.OrdinalIgnoreCase);
            }
            catch { return false; }
        }

        private async Task<ResourceManifest?> GetManifestAsync()
        {
            if (_cachedManifest != null) return _cachedManifest;
            try
            {
                // Note: In development we might want to load a local manifest if offline
                string json = await _client.GetStringAsync(ManifestUrl);
                _cachedManifest = JsonConvert.DeserializeObject<ResourceManifest>(json);
                return _cachedManifest;
            }
            catch
            {
                Logger.Warn("Unable to reach cloud manifest. Using local cache if available.", "RESOURCES");
                return null;
            }
        }
    }
}
