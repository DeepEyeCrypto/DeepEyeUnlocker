using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.Models
{
    #region Root Cloak Models

    public enum CloakReadiness
    {
        NotRooted,
        RootExposed,
        PartiallyHidden,
        WellHidden,
        OptimalSetup
    }

    public class RootCloakStatus
    {
        // Root Detection
        public bool IsRooted { get; set; }
        public bool HasMagisk { get; set; }
        public string? MagiskVersion { get; set; }
        public string? MagiskPackage { get; set; }

        // Zygisk
        public bool ZygiskEnabled { get; set; }
        public bool ZygiskSupported { get; set; }

        // Shamiko
        public bool ShamikoInstalled { get; set; }
        public bool ShamikoActive { get; set; }
        public string? ShamikoVersion { get; set; }

        // DenyList
        public bool DenyListConfigured { get; set; }
        public bool EnforceDenyListEnabled { get; set; }
        public List<string> DenyListedPackages { get; set; } = new();

        // Play Integrity
        public bool? PlayIntegrityBasicPass { get; set; }
        public bool? PlayIntegrityDevicePass { get; set; }
        public bool? PlayIntegrityStrongPass { get; set; }

        // Other Modules
        public List<string> InstalledModules { get; set; } = new();
        public bool PlayIntegrityFixInstalled { get; set; }
        public bool PropsConfigInstalled { get; set; }

        // Analysis
        public CloakReadiness Readiness { get; set; }
        public List<string> Issues { get; set; } = new();
        public List<string> Recommendations { get; set; } = new();
        public DateTime ScannedAt { get; set; } = DateTime.UtcNow;
    }

    #endregion

    #region Dev Mode Cloak Models

    public class DevModeStatus
    {
        public bool DeveloperOptionsEnabled { get; set; }
        public bool UsbDebuggingEnabled { get; set; }
        public bool WirelessDebuggingEnabled { get; set; }
        public bool SystemDebuggable { get; set; }
        public bool OemUnlockAllowed { get; set; }
        public bool MockLocationEnabled { get; set; }
        public string? UsbConfig { get; set; }
        public string? AndroidVersion { get; set; }
        public List<string> DetectionRisks { get; set; } = new();
        public List<string> Recommendations { get; set; } = new();
    }

    public class StealthProfile
    {
        public string Name { get; set; } = "";
        public string Description { get; set; } = "";
        public bool HideDeveloperOptions { get; set; }
        public bool HideUsbDebugging { get; set; }
        public bool SpoofDebuggableProp { get; set; }
        public Dictionary<string, string> SettingsOverrides { get; set; } = new();
        public Dictionary<string, string> PropsOverrides { get; set; } = new();
    }

    #endregion

    #region Cloak Profiles

    public class CloakProfile
    {
        public string Name { get; set; } = "";
        public string Description { get; set; } = "";
        public ProfileType Type { get; set; }
        public List<string> TargetPackages { get; set; } = new();
        public bool RecommendShamiko { get; set; }
        public bool RecommendPlayIntegrityFix { get; set; }
        public bool HideDevOptions { get; set; }
    }

    public enum ProfileType
    {
        Banking,
        UPI,
        Gaming,
        Enterprise,
        Streaming,
        Custom
    }

    public static class CloakProfiles
    {
        public static readonly CloakProfile Banking = new()
        {
            Name = "Banking Apps",
            Description = "Hide root from banking and financial apps",
            Type = ProfileType.Banking,
            TargetPackages = new List<string>
            {
                // Indian Banks
                "com.sbi.SBIFreedomPlus",
                "com.csam.icici.bank.imobile",
                "com.axis.mobile",
                "com.msf.kbank.mobile",
                "net.one97.paytm",
                "com.phonepe.app",
                "in.org.npci.upiapp",
                "com.google.android.apps.nbu.paisa.user",
                // International
                "com.chase.sig.android",
                "com.wf.wellsfargomobile",
                "com.infonow.bofa",
                "com.usaa.mobile.android.usaa",
                "com.citi.citimobile"
            },
            RecommendShamiko = true,
            RecommendPlayIntegrityFix = true,
            HideDevOptions = true
        };

        public static readonly CloakProfile Gaming = new()
        {
            Name = "Gaming Apps",
            Description = "Hide root from games with anti-cheat",
            Type = ProfileType.Gaming,
            TargetPackages = new List<string>
            {
                "com.tencent.ig",
                "com.pubg.imobile",
                "com.activision.callofduty.shooter",
                "com.garena.game.codm",
                "com.supercell.clashofclans",
                "com.supercell.clashroyale",
                "com.nianticlabs.pokemongo",
                "com.riotgames.league.wildrift",
                "jp.pokemon.pokemonunite"
            },
            RecommendShamiko = true,
            RecommendPlayIntegrityFix = true,
            HideDevOptions = false
        };

        public static readonly CloakProfile Streaming = new()
        {
            Name = "Streaming Apps",
            Description = "Hide root from DRM-protected streaming apps",
            Type = ProfileType.Streaming,
            TargetPackages = new List<string>
            {
                "com.netflix.mediaclient",
                "com.amazon.avod.thirdpartyclient",
                "com.disney.disneyplus",
                "in.startv.hotstar",
                "com.jio.jioplay.tv"
            },
            RecommendShamiko = true,
            RecommendPlayIntegrityFix = true,
            HideDevOptions = false
        };

        public static readonly CloakProfile Enterprise = new()
        {
            Name = "Enterprise/Work Apps",
            Description = "Hide root from corporate MDM and work apps",
            Type = ProfileType.Enterprise,
            TargetPackages = new List<string>
            {
                "com.microsoft.intune.mam",
                "com.airwatch.androidagent",
                "com.good.gd",
                "com.mobileiron",
                "com.microsoft.windowsintune.companyportal"
            },
            RecommendShamiko = true,
            RecommendPlayIntegrityFix = false,
            HideDevOptions = true
        };
    }

    #endregion
}
