using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Newtonsoft.Json;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core
{
    /// <summary>
    /// Provides EDL capability profiles for device models
    /// </summary>
    public class EdlProfileProvider
    {
        private readonly List<EdlProfile> _profiles = new();
        private readonly Dictionary<string, TestPointInfo> _testPoints = new();
        private static readonly string ProfilePath;
        
        static EdlProfileProvider()
        {
            ProfilePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "assets", "EdlProfiles.json");
        }

        public EdlProfileProvider()
        {
            LoadProfiles();
            InitializeBuiltInProfiles();
        }

        private void LoadProfiles()
        {
            try
            {
                if (File.Exists(ProfilePath))
                {
                    string json = File.ReadAllText(ProfilePath);
                    var data = JsonConvert.DeserializeObject<EdlProfileData>(json);
                    if (data?.Profiles != null)
                        _profiles.AddRange(data.Profiles);
                    if (data?.TestPoints != null)
                    {
                        foreach (var tp in data.TestPoints)
                            _testPoints[tp.DeviceModel.ToLower()] = tp;
                    }
                    Logger.Info($"Loaded {_profiles.Count} EDL profiles from disk", "EDL");
                }
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to load EDL profiles from disk");
            }
        }

        private void InitializeBuiltInProfiles()
        {
            // Built-in profiles for common devices
            var builtIn = new List<EdlProfile>
            {
                // Xiaomi - Generally good EDL support on older MIUI
                new EdlProfile
                {
                    Brand = "Xiaomi", Model = "Redmi Note 10 Pro", Codename = "sweet", SoC = "SM7150",
                    SupportsAdbRebootEdl = true, SupportsFastbootOemEdl = true,
                    Notes = "Works on MIUI 12.x, may require unlock for MIUI 13+"
                },
                new EdlProfile
                {
                    Brand = "Xiaomi", Model = "Redmi Note 11", Codename = "spes", SoC = "SM6225",
                    SupportsAdbRebootEdl = true, SupportsFastbootOemEdl = true,
                    Notes = "MIUI 13 may block EDL. Consider Mi Unlock tool."
                },
                new EdlProfile
                {
                    Brand = "Xiaomi", Model = "POCO X3 NFC", Codename = "surya", SoC = "SM7150",
                    SupportsAdbRebootEdl = true, SupportsFastbootOemEdl = true
                },
                new EdlProfile
                {
                    Brand = "Xiaomi", Model = "Mi 11", Codename = "venus", SoC = "SM8350",
                    SupportsAdbRebootEdl = false, SupportsFastbootOemEdl = false,
                    RequiresAuthTool = true, AuthToolName = "Mi Flash Pro (Authorized)",
                    Notes = "SDM888+ requires signed firehose via Xiaomi Auth"
                },
                
                // OnePlus - Mixed support
                new EdlProfile
                {
                    Brand = "OnePlus", Model = "OnePlus 6", Codename = "enchilada", SoC = "SDM845",
                    SupportsAdbRebootEdl = true, SupportsFastbootOemEdl = true,
                    Notes = "Good EDL support on OxygenOS 9-10"
                },
                new EdlProfile
                {
                    Brand = "OnePlus", Model = "OnePlus 8", Codename = "instantnoodle", SoC = "SM8250",
                    SupportsAdbRebootEdl = false, SupportsFastbootOemEdl = false,
                    RequiresTestPoint = true,
                    Notes = "OxygenOS 11+ blocks software EDL"
                },
                
                // Samsung - Never supports software EDL
                new EdlProfile
                {
                    Brand = "Samsung", Model = "Galaxy S21", Codename = "o1s", SoC = "SM8350",
                    SupportsAdbRebootEdl = false, SupportsFastbootOemEdl = false,
                    RequiresTestPoint = true,
                    Notes = "Samsung uses Download Mode (Odin), not EDL. Test-point for true EDL."
                },
                new EdlProfile
                {
                    Brand = "Samsung", Model = "Galaxy A52", Codename = "a52q", SoC = "SM7125",
                    SupportsAdbRebootEdl = false, SupportsFastbootOemEdl = false,
                    RequiresTestPoint = true
                },
                
                // Google Pixel - Blocked
                new EdlProfile
                {
                    Brand = "Google", Model = "Pixel 6", Codename = "oriole", SoC = "Tensor",
                    SupportsAdbRebootEdl = false, SupportsFastbootOemEdl = false,
                    RequiresTestPoint = true,
                    Notes = "Tensor SoC is not Qualcomm. EDL N/A."
                },
                new EdlProfile
                {
                    Brand = "Google", Model = "Pixel 4a", Codename = "sunfish", SoC = "SM7150",
                    SupportsAdbRebootEdl = false, SupportsFastbootOemEdl = false,
                    RequiresTestPoint = true,
                    Notes = "Google blocks EDL commands at aboot level"
                },
                
                // Realme/Oppo - Generally blocked
                new EdlProfile
                {
                    Brand = "Realme", Model = "Realme 8 Pro", Codename = "RMX3081", SoC = "SM7125",
                    SupportsAdbRebootEdl = false, SupportsFastbootOemEdl = false,
                    RequiresAuthTool = true, AuthToolName = "MSMDownloadTool",
                    Notes = "Requires Realme/Oppo authorized service tool"
                },
                new EdlProfile
                {
                    Brand = "Oppo", Model = "Find X3 Pro", Codename = "PEEM00", SoC = "SM8350",
                    SupportsAdbRebootEdl = false, SupportsFastbootOemEdl = false,
                    RequiresAuthTool = true, AuthToolName = "MSMDownloadTool"
                },
                
                // Motorola - Variable support
                new EdlProfile
                {
                    Brand = "Motorola", Model = "Moto G Power 2021", Codename = "borneo", SoC = "SM6115",
                    SupportsAdbRebootEdl = true, SupportsFastbootOemEdl = true,
                    Notes = "Most Moto G series support EDL"
                }
            };

            // Add built-in profiles that aren't already loaded from disk
            foreach (var profile in builtIn)
            {
                if (!_profiles.Any(p => 
                    p.Brand.Equals(profile.Brand, StringComparison.OrdinalIgnoreCase) &&
                    p.Codename.Equals(profile.Codename, StringComparison.OrdinalIgnoreCase)))
                {
                    _profiles.Add(profile);
                }
            }
        }

        /// <summary>
        /// Get EDL profile for a specific device
        /// </summary>
        public EdlProfile? GetProfileFor(string brand, string model)
        {
            return _profiles.FirstOrDefault(p =>
                p.Brand.Equals(brand, StringComparison.OrdinalIgnoreCase) &&
                (p.Model.Equals(model, StringComparison.OrdinalIgnoreCase) ||
                 p.Codename.Equals(model, StringComparison.OrdinalIgnoreCase)));
        }

        /// <summary>
        /// Get EDL profile by codename (internal name)
        /// </summary>
        public EdlProfile? GetProfileByCodename(string codename)
        {
            return _profiles.FirstOrDefault(p =>
                p.Codename.Equals(codename, StringComparison.OrdinalIgnoreCase));
        }

        /// <summary>
        /// Get all profiles for a brand
        /// </summary>
        public IEnumerable<EdlProfile> GetProfilesForBrand(string brand)
        {
            return _profiles.Where(p => 
                p.Brand.Equals(brand, StringComparison.OrdinalIgnoreCase));
        }

        /// <summary>
        /// Get test point info for a device
        /// </summary>
        public TestPointInfo? GetTestPointInfo(string model)
        {
            return _testPoints.GetValueOrDefault(model.ToLower());
        }

        /// <summary>
        /// Get count of loaded profiles
        /// </summary>
        public int ProfileCount => _profiles.Count;
    }

    internal class EdlProfileData
    {
        public List<EdlProfile> Profiles { get; set; } = new();
        public List<TestPointInfo> TestPoints { get; set; } = new();
    }
}
