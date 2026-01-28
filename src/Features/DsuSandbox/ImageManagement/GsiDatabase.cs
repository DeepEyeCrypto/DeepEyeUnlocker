using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Features.DsuSandbox.Models;

namespace DeepEyeUnlocker.Features.DsuSandbox.ImageManagement
{
    /// <summary>
    /// Manages GSI/ROM image database and downloads
    /// </summary>
    public class GsiDatabase
    {
        private readonly string _databasePath;
        private readonly string _imageCacheDir;
        private readonly HttpClient _httpClient;
        private List<DsuImage> _images = new();

        public IReadOnlyList<DsuImage> Images => _images.AsReadOnly();

        public GsiDatabase()
        {
            var appData = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
            var deepEyeDir = Path.Combine(appData, "DeepEyeUnlocker");
            
            _databasePath = Path.Combine(deepEyeDir, "gsi_database.json");
            _imageCacheDir = Path.Combine(deepEyeDir, "ImageCache");

            Directory.CreateDirectory(deepEyeDir);
            Directory.CreateDirectory(_imageCacheDir);

            _httpClient = new HttpClient();
            _httpClient.DefaultRequestHeaders.Add("User-Agent", "DeepEyeUnlocker/1.0");
        }

        /// <summary>
        /// Load database from disk or initialize with defaults
        /// </summary>
        public async Task LoadAsync()
        {
            if (File.Exists(_databasePath))
            {
                try
                {
                    var json = await File.ReadAllTextAsync(_databasePath);
                    _images = JsonSerializer.Deserialize<List<DsuImage>>(json) ?? new List<DsuImage>();
                    
                    // Update download status
                    foreach (var img in _images)
                    {
                        var localPath = GetLocalPath(img.Id);
                        img.IsDownloaded = File.Exists(localPath);
                        img.LocalPath = localPath;
                    }
                    
                    Logger.Info($"Loaded {_images.Count} GSI images from database");
                }
                catch (Exception ex)
                {
                    Logger.Error(ex, "Failed to load GSI database");
                    _images = GetDefaultImages();
                }
            }
            else
            {
                _images = GetDefaultImages();
                await SaveAsync();
            }
        }

        /// <summary>
        /// Save database to disk
        /// </summary>
        public async Task SaveAsync()
        {
            try
            {
                var json = JsonSerializer.Serialize(_images, new JsonSerializerOptions { WriteIndented = true });
                await File.WriteAllTextAsync(_databasePath, json);
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to save GSI database");
            }
        }

        /// <summary>
        /// Get images compatible with a chipset
        /// </summary>
        public IEnumerable<DsuImage> GetCompatibleImages(string chipset)
        {
            var chipsetLower = chipset?.ToLower() ?? "";
            
            return _images.Where(img =>
            {
                if (!img.Compatibility.Any()) return true;

                // Check chipset-specific compatibility
                foreach (var compat in img.Compatibility)
                {
                    if (chipsetLower.Contains(compat.Key.ToLower()) &&
                        compat.Value.ToLower() != "unknown" &&
                        compat.Value.ToLower() != "broken")
                    {
                        return true;
                    }
                }
                return true; // Include if no specific incompatibility noted
            });
        }

        /// <summary>
        /// Download an image
        /// </summary>
        public async Task<string> DownloadImageAsync(string imageId, IProgress<int>? progress, CancellationToken ct)
        {
            var image = _images.FirstOrDefault(i => i.Id == imageId);
            if (image == null)
                throw new ArgumentException($"Image not found: {imageId}");

            var localPath = GetLocalPath(imageId);
            
            Logger.Info($"Downloading {image.Name} from {image.DownloadUrl}");

            try
            {
                using var response = await _httpClient.GetAsync(image.DownloadUrl, 
                    HttpCompletionOption.ResponseHeadersRead, ct);
                response.EnsureSuccessStatusCode();

                var totalBytes = response.Content.Headers.ContentLength ?? 0;
                using var contentStream = await response.Content.ReadAsStreamAsync(ct);
                using var fileStream = File.Create(localPath);

                var buffer = new byte[8192];
                long totalRead = 0;
                int bytesRead;

                while ((bytesRead = await contentStream.ReadAsync(buffer, 0, buffer.Length, ct)) > 0)
                {
                    await fileStream.WriteAsync(buffer, 0, bytesRead, ct);
                    totalRead += bytesRead;

                    if (totalBytes > 0)
                    {
                        progress?.Report((int)((totalRead * 100) / totalBytes));
                    }
                }

                // Update image status
                image.IsDownloaded = true;
                image.LocalPath = localPath;
                await SaveAsync();

                Logger.Info($"Downloaded {image.Name} ({totalRead / (1024 * 1024)} MB)");
                return localPath;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, $"Failed to download {image.Name}");
                if (File.Exists(localPath))
                    File.Delete(localPath);
                throw;
            }
        }

        /// <summary>
        /// Add a custom image
        /// </summary>
        public async Task AddCustomImageAsync(string name, string localPath, string? sha256 = null)
        {
            var id = $"custom_{Guid.NewGuid():N}";
            var fileInfo = new FileInfo(localPath);

            var image = new DsuImage
            {
                Id = id,
                Name = name,
                OsName = "Custom",
                Version = "Unknown",
                SizeMb = (int)(fileInfo.Length / (1024 * 1024)),
                Sha256Checksum = sha256 ?? "",
                Source = "user",
                Notes = $"Custom image added by user from {localPath}",
                IsDownloaded = true,
                LocalPath = localPath
            };

            _images.Add(image);
            await SaveAsync();
        }

        /// <summary>
        /// Delete cached image
        /// </summary>
        public async Task DeleteCachedImageAsync(string imageId)
        {
            var image = _images.FirstOrDefault(i => i.Id == imageId);
            if (image == null) return;

            var localPath = GetLocalPath(imageId);
            if (File.Exists(localPath))
            {
                File.Delete(localPath);
            }

            image.IsDownloaded = false;
            image.LocalPath = "";
            await SaveAsync();
        }

        /// <summary>
        /// Get total cache size
        /// </summary>
        public long GetCacheSizeBytes()
        {
            if (!Directory.Exists(_imageCacheDir))
                return 0;

            return new DirectoryInfo(_imageCacheDir)
                .EnumerateFiles("*", SearchOption.AllDirectories)
                .Sum(f => f.Length);
        }

        /// <summary>
        /// Clear entire cache
        /// </summary>
        public async Task ClearCacheAsync()
        {
            if (Directory.Exists(_imageCacheDir))
            {
                Directory.Delete(_imageCacheDir, true);
                Directory.CreateDirectory(_imageCacheDir);
            }

            foreach (var img in _images)
            {
                img.IsDownloaded = false;
                img.LocalPath = "";
            }

            await SaveAsync();
            Logger.Info("GSI image cache cleared");
        }

        private string GetLocalPath(string imageId)
        {
            return Path.Combine(_imageCacheDir, $"{imageId}.img");
        }

        private static List<DsuImage> GetDefaultImages()
        {
            return new List<DsuImage>
            {
                new DsuImage
                {
                    Id = "gsi-aosp-arm64-14",
                    Name = "AOSP Android 14 (ARM64)",
                    OsName = "AOSP",
                    Version = "14",
                    Architecture = "arm64",
                    SizeMb = 2048,
                    DownloadUrl = "https://dl.google.com/developers/android/gsi/gsi_arm64-userdebug.img.xz",
                    MinAndroidVersion = 10,
                    Source = "google",
                    Notes = "Official Google AOSP GSI. Best compatibility. May lack some device features (camera, fingerprint).",
                    Compatibility = new Dictionary<string, string>
                    {
                        { "snapdragon", "excellent" },
                        { "mediatek", "good" },
                        { "exynos", "fair" },
                        { "kirin", "partial" }
                    }
                },
                new DsuImage
                {
                    Id = "gsi-aosp-arm64-13",
                    Name = "AOSP Android 13 (ARM64)",
                    OsName = "AOSP",
                    Version = "13",
                    Architecture = "arm64",
                    SizeMb = 1920,
                    DownloadUrl = "https://dl.google.com/developers/android/gsi/gsi-13-arm64.img.xz",
                    MinAndroidVersion = 10,
                    Source = "google",
                    Notes = "Stable AOSP Android 13. Recommended for compatibility testing.",
                    Compatibility = new Dictionary<string, string>
                    {
                        { "snapdragon", "excellent" },
                        { "mediatek", "good" },
                        { "exynos", "good" }
                    }
                },
                new DsuImage
                {
                    Id = "lineage-gsi-21-arm64",
                    Name = "LineageOS 21 GSI (ARM64)",
                    OsName = "LineageOS",
                    Version = "21",
                    Architecture = "arm64",
                    SizeMb = 2560,
                    DownloadUrl = "https://sourceforge.net/projects/andyyan-gsi/files/lineage-21-arm64-bvN.img.xz/download",
                    MinAndroidVersion = 10,
                    Source = "lineageos",
                    Notes = "LineageOS 21 GSI by AndyYan. Good alternative to AOSP with extra features.",
                    Compatibility = new Dictionary<string, string>
                    {
                        { "snapdragon", "excellent" },
                        { "mediatek", "good" }
                    }
                },
                new DsuImage
                {
                    Id = "lineage-gsi-20-arm64",
                    Name = "LineageOS 20 GSI (ARM64)",
                    OsName = "LineageOS",
                    Version = "20",
                    Architecture = "arm64",
                    SizeMb = 2304,
                    DownloadUrl = "https://sourceforge.net/projects/andyyan-gsi/files/lineage-20-arm64-bvN.img.xz/download",
                    MinAndroidVersion = 10,
                    Source = "lineageos",
                    Notes = "Stable LineageOS 20 GSI. Well-tested with many devices."
                },
                new DsuImage
                {
                    Id = "pixel-experience-14-arm64",
                    Name = "Pixel Experience 14 GSI (ARM64)",
                    OsName = "PixelExperience",
                    Version = "14",
                    Architecture = "arm64",
                    SizeMb = 2816,
                    DownloadUrl = "https://example.com/pe14.img", // Placeholder
                    MinAndroidVersion = 11,
                    Source = "pixelexperience",
                    Notes = "Pixel Experience GSI. Includes Google apps and Pixel features."
                },
                new DsuImage
                {
                    Id = "crdroid-arm64-10",
                    Name = "crDroid 10 GSI (ARM64)",
                    OsName = "crDroid",
                    Version = "10",
                    Architecture = "arm64",
                    SizeMb = 2400,
                    DownloadUrl = "https://example.com/crdroid10.img", // Placeholder
                    MinAndroidVersion = 10,
                    Source = "crdroid",
                    Notes = "crDroid 10 with extensive customization options."
                }
            };
        }
    }
}
