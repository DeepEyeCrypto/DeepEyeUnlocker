using System;
using System.Collections.Generic;
using DeepEyeUnlocker.Features.Cloak.Models;

namespace DeepEyeUnlocker.Features.Cloak
{
    public static class CloakProfiles
    {
        public static readonly List<BankingAppProfile> PopularBankingApps = new()
        {
            new BankingAppProfile 
            { 
                AppName = "Google Wallet / Pay", 
                PackageName = "com.google.android.apps.walletnfcrel",
                DetectionDifficulty = Difficulty.Medium,
                RecommendedModules = new() { HidingModuleType.Shamiko, HidingModuleType.PlayIntegrityFix }
            },
            new BankingAppProfile 
            { 
                AppName = "Yono SBI", 
                PackageName = "com.sbi.lotusintouch",
                DetectionDifficulty = Difficulty.Hard,
                RequiresHMA = true,
                RecommendedModules = new() { HidingModuleType.Shamiko, HidingModuleType.HideMyApplist }
            },
            new BankingAppProfile 
            { 
                AppName = "PhonePe", 
                PackageName = "com.phonepe.app",
                DetectionDifficulty = Difficulty.Medium,
                RecommendedModules = new() { HidingModuleType.Shamiko }
            }
        };

        public static readonly CloakProfile BankingGoldStandard = new()
        {
            Id = "banking_2025_gold",
            Name = "Banking Golden Standard (2025)",
            Description = "The consensus best-practice setup for bypassing high-security banking app detection.",
            RecommendedTools = new() { RootingTool.KernelSu, RootingTool.APatch, RootingTool.Magisk },
            RequiredModules = new() 
            { 
                HidingModuleType.Zygisk, 
                HidingModuleType.Shamiko, 
                HidingModuleType.PlayIntegrityFix 
            },
            RecommendedDenylist = new() 
            { 
                "com.google.android.gms", 
                "com.android.vending" 
            }
        };

        public static readonly List<HidingModule> ModuleRegistry = new()
        {
            new HidingModule
            {
                Id = "shamiko",
                Name = "Shamiko",
                Type = HidingModuleType.Shamiko,
                Description = "The most powerful root hider. Works with DenyList to hide all root traces.",
                DownloadUrl = "https://github.com/LSPosed/LSPosed.github.io/releases"
            },
            new HidingModule
            {
                Id = "pifix",
                Name = "Play Integrity Fix",
                Type = HidingModuleType.PlayIntegrityFix,
                Description = "Spoofs device info to pass Google Play Integrity API checks.",
                DownloadUrl = "https://github.com/chiteroman/PlayIntegrityFix/releases"
            },
            new HidingModule
            {
                Id = "hma",
                Name = "Hide My Applist",
                Type = HidingModuleType.HideMyApplist,
                Description = "Xposed module to hide root apps (Magisk, Termux) from specific apps.",
                Dependencies = new() { "lsposed" },
                DownloadUrl = "https://github.com/Dr-TSNG/HideMyApplist/releases"
            }
        };
    }
}
