using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Features.ModelDiscovery.Models
{
    public class SupportedModel
    {
        public int Id { get; set; }
        public string Tool { get; set; } = string.Empty; // UnlockTool, Hydra, Miracle, Chimera
        public string? ToolVersion { get; set; }
        public string Brand { get; set; } = string.Empty;
        public string MarketingName { get; set; } = string.Empty;
        public string? ModelNumber { get; set; }
        public string? Codename { get; set; }
        public string? ChipsetFamily { get; set; } // Qualcomm, MediaTek, UNISOC, etc.
        public string? ChipsetModel { get; set; }
        public string OperationsJson { get; set; } = "[]"; // Serialized string array
        public string ModesJson { get; set; } = "[]"; // Serialized string array
        public string SourceUrl { get; set; } = string.Empty;
        public string? SourceSection { get; set; }
        public DateTime FirstSeen { get; set; } = DateTime.UtcNow;
        public DateTime LastSeen { get; set; } = DateTime.UtcNow;

        // Helper properties for easy access
        public string[] GetOperations() => System.Text.Json.JsonSerializer.Deserialize<string[]>(OperationsJson) ?? Array.Empty<string>();
        public void SetOperations(IEnumerable<string> ops) => OperationsJson = System.Text.Json.JsonSerializer.Serialize(ops);
        
        public string[] GetModes() => System.Text.Json.JsonSerializer.Deserialize<string[]>(ModesJson) ?? Array.Empty<string>();
        public void SetModes(IEnumerable<string> modes) => ModesJson = System.Text.Json.JsonSerializer.Serialize(modes);
    }
}
