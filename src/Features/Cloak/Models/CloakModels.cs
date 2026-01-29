using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Features.Cloak.Models
{
    public enum RootingTool
    {
        None,
        Magisk,
        KernelSu,
        APatch,
        KitsuneMask
    }

    public enum HidingModuleType
    {
        Zygisk,
        Shamiko,
        PlayIntegrityFix,
        LSPosed,
        HideMyApplist,
        BootloaderSpoofer,
        Custom
    }

    public enum Difficulty
    {
        Easy,
        Medium,
        Hard
    }

    public class RootEnvironment
    {
        public RootingTool Tool { get; set; } = RootingTool.None;
        public string? Version { get; set; }
        public bool ZygiskEnabled { get; set; }
        public bool IsRooted { get; set; }
        public List<string> InstalledModules { get; set; } = new();
        public List<string> DenylistedPackages { get; set; } = new();
        public bool EnforceDenyListEnabled { get; set; }
    }

    public class HidingModule
    {
        public string Id { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public HidingModuleType Type { get; set; }
        public List<RootingTool> CompatibleTools { get; set; } = new();
        public List<string> Dependencies { get; set; } = new();
        public bool IsCritical { get; set; }
        public string DownloadUrl { get; set; } = string.Empty;
    }

    public class BankingAppProfile
    {
        public string PackageName { get; set; } = string.Empty;
        public string AppName { get; set; } = string.Empty;
        public Difficulty DetectionDifficulty { get; set; }
        public List<HidingModuleType> RecommendedModules { get; set; } = new();
        public List<string> AdditionalDenylist { get; set; } = new();
        public bool RequiresHMA { get; set; }
    }

    public class CloakProfile
    {
        public string Id { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public List<RootingTool> RecommendedTools { get; set; } = new();
        public List<HidingModuleType> RequiredModules { get; set; } = new();
        public List<string> RecommendedDenylist { get; set; } = new();
        public List<BankingAppProfile> TargetApps { get; set; } = new();
    }
}
