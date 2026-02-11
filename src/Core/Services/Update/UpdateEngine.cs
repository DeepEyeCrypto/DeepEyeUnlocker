using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace DeepEyeUnlocker.Core.Services.Update
{
    public class UpdateManifest
    {
        public string Version { get; set; } = "1.0.0";
        public List<UpdateItem> Items { get; set; } = new();
    }

    public class UpdateItem
    {
        public string Type { get; set; } = string.Empty; // "LOADER", "PROFILE", "DIAGRAM"
        public string Id { get; set; } = string.Empty;
        public string DownloadUrl { get; set; } = string.Empty;
        public string Sha256 { get; set; } = string.Empty;
    }

    public class UpdateEngine
    {
        private readonly HttpClient _client = new();
        private const string UpdateUrl = "https://updates.deep-eye.io/v1/manifest.json";

        public async Task<List<UpdateItem>> CheckForUpdatesAsync()
        {
            try
            {
                var json = await _client.GetStringAsync(UpdateUrl);
                var manifest = JsonConvert.DeserializeObject<UpdateManifest>(json);
                return manifest?.Items ?? new List<UpdateItem>();
            }
            catch
            {
                // Return empty list if offline or server down
                return new List<UpdateItem>();
            }
        }

        public async Task<bool> DownloadItemAsync(UpdateItem item, string storagePath)
        {
            try
            {
                var data = await _client.GetByteArrayAsync(item.DownloadUrl);
                // logic to verify SHA256
                await System.IO.File.WriteAllBytesAsync(storagePath, data);
                return true;
            }
            catch
            {
                return false;
            }
        }
    }
}
