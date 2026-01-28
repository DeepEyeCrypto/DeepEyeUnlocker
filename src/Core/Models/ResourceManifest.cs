using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.Models
{
    public class ResourceItem
    {
        public string Id { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string Type { get; set; } = "Programmer"; // Programmer, DA, Auth, Loader
        public string Brand { get; set; } = string.Empty;
        public string Chipset { get; set; } = string.Empty;
        public List<string> SupportedModels { get; set; } = new();
        public string FileName { get; set; } = string.Empty;
        public string Checksum { get; set; } = string.Empty;
        public long Size { get; set; }
        public string RemoteUrl { get; set; } = string.Empty;
    }

    public class ResourceManifest
    {
        public string Version { get; set; } = "1.0.0";
        public DateTime GeneratedAt { get; set; } = DateTime.Now;
        public List<ResourceItem> Items { get; set; } = new();
    }
}
