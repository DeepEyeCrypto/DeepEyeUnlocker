using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Newtonsoft.Json;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core
{
    public class ProfileManager
    {
        private static readonly string ProfilePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "assets", "Profiles.json");
        private ProfileDatabase _db = new();

        public ProfileManager()
        {
            LoadProfiles();
        }

        public void LoadProfiles()
        {
            try
            {
                if (File.Exists(ProfilePath))
                {
                    string json = File.ReadAllText(ProfilePath);
                    _db = JsonConvert.DeserializeObject<ProfileDatabase>(json) ?? new ProfileDatabase();
                    Logger.Info($"Loaded {_db.Profiles.Count} brand profiles.", "PROFILES");
                }
                else
                {
                    Logger.Warn("Profiles.json not found. Initializing with defaults.", "PROFILES");
                    InitializeDefaults();
                    SaveProfiles();
                }
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to load profiles system.");
                _db = new ProfileDatabase();
            }
        }

        public BrandProfile GetProfileForDevice(int vid, int pid)
        {
            string vidHex = vid.ToString("X4");
            string pidHex = pid.ToString("X4");

            return _db.Profiles.FirstOrDefault(p => 
                p.CommonVids.Contains(vidHex, StringComparer.OrdinalIgnoreCase) && 
                p.CommonPids.Contains(pidHex, StringComparer.OrdinalIgnoreCase)) 
                ?? new BrandProfile { BrandName = "Generic" };
        }

        public BrandProfile GetProfileByName(string name)
        {
            return _db.Profiles.FirstOrDefault(p => p.BrandName.Equals(name, StringComparison.OrdinalIgnoreCase)) 
                ?? new BrandProfile { BrandName = name };
        }

        private void InitializeDefaults()
        {
            _db.Profiles = new List<BrandProfile>
            {
                new BrandProfile 
                { 
                    BrandName = "Xiaomi", 
                    CommonVids = new List<string> { "05C6", "2717" },
                    CommonPids = new List<string> { "9008", "D001" },
                    SupportsBypassAuth = true,
                    Configs = new Dictionary<string, string> { { "AccountType", "MiAccount" } }
                },
                new BrandProfile 
                { 
                    BrandName = "Oppo", 
                    CommonVids = new List<string> { "22D1" },
                    CommonPids = new List<string> { "9008" },
                    SupportsBypassAuth = false,
                    RequiresCredit = true,
                    AuthServerUrl = "https://auth.deep-eye.io/oppo"
                },
                new BrandProfile 
                { 
                    BrandName = "Samsung", 
                    CommonVids = new List<string> { "04E8" },
                    CommonPids = new List<string> { "685D", "6860" },
                    Configs = new Dictionary<string, string> { { "DefaultProtocol", "Odin" } }
                }
            };
        }

        public void SaveProfiles()
        {
            try
            {
                string dir = Path.GetDirectoryName(ProfilePath)!;
                if (!Directory.Exists(dir)) Directory.CreateDirectory(dir);
                
                string json = JsonConvert.SerializeObject(_db, Formatting.Indented);
                File.WriteAllText(ProfilePath, json);
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to save profiles.");
            }
        }
    }
}
