using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Newtonsoft.Json;

namespace DeepEyeUnlocker.Core.Services.Repositories
{
    public class DiagramMetadata
    {
        public string DiagramId { get; set; } = string.Empty;
        public string ImagePath { get; set; } = string.Empty;
        public string Title { get; set; } = string.Empty;
        public List<string> Instructions { get; set; } = new();
        public List<DiagramAnnotation> Annotations { get; set; } = new();
    }

    public class DiagramAnnotation
    {
        public string Label { get; set; } = string.Empty;
        public double X { get; set; } // Percentage based coordinates
        public double Y { get; set; }
        public string Color { get; set; } = "Red";
    }

    public class DiagramRepository
    {
        private static readonly string RepoPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "assets", "diagrams");
        private static readonly string ManifestPath = Path.Combine(RepoPath, "diagrams.json");
        private List<DiagramMetadata> _diagrams = new();

        public DiagramRepository()
        {
            LoadManifest();
        }

        private void LoadManifest()
        {
            if (File.Exists(ManifestPath))
            {
                var json = File.ReadAllText(ManifestPath);
                _diagrams = JsonConvert.DeserializeObject<List<DiagramMetadata>>(json) ?? new();
            }
        }

        public DiagramMetadata? GetDiagram(string id)
        {
            return _diagrams.FirstOrDefault(d => d.DiagramId.Equals(id, StringComparison.OrdinalIgnoreCase));
        }

        public string GetFullImagePath(string id)
        {
            var diag = GetDiagram(id);
            if (diag == null) return string.Empty;
            return Path.Combine(RepoPath, diag.ImagePath);
        }
    }
}
