using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Newtonsoft.Json;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Services.Repositories
{
    public class LoaderMetadata
    {
        public string Id { get; set; } = string.Empty;
        public string Type { get; set; } = "MTK_DA"; // MTK_DA, MTK_EMI, QCOM_FIREHOSE
        public string FilePath { get; set; } = string.Empty;
        public string Sha256 { get; set; } = string.Empty;
        public List<string> CompatibleCpus { get; set; } = new();
        public string Version { get; set; } = "1.0.0";
    }

    public class LoaderRepository
    {
        private static readonly string RepoPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "assets", "loaders");
        private static readonly string ManifestPath = Path.Combine(RepoPath, "loaders.json");
        private List<LoaderMetadata> _loaders = new();

        public LoaderRepository()
        {
            LoadManifest();
        }

        private void LoadManifest()
        {
            if (File.Exists(ManifestPath))
            {
                var json = File.ReadAllText(ManifestPath);
                _loaders = JsonConvert.DeserializeObject<List<LoaderMetadata>>(json) ?? new();
            }
        }

        public LoaderMetadata? GetLoader(string id)
        {
            return _loaders.FirstOrDefault(l => l.Id.Equals(id, StringComparison.OrdinalIgnoreCase));
        }

        public string GetFullPath(string id)
        {
            var loader = GetLoader(id);
            if (loader == null) return string.Empty;
            return Path.Combine(RepoPath, loader.FilePath);
        }

        public List<LoaderMetadata> FindCompatible(string cpu)
        {
            return _loaders.Where(l => l.CompatibleCpus.Contains(cpu, StringComparer.OrdinalIgnoreCase)).ToList();
        }
    }
}
