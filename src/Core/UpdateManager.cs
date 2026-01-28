using System;
using System.Net.Http;
using System.Threading.Tasks;
using Newtonsoft.Json;
using NLog;

namespace DeepEyeUnlocker.Core
{
    public class UpdateManager
    {
        private static readonly Logger Logger = LogManager.GetCurrentClassLogger();
        private static readonly HttpClient Client = new HttpClient();
        private const string VersionApi = "https://api.deepeyeunlocker.io/api/version/latest"; // Placeholder

        public class VersionInfo
        {
            public string Version { get; set; } = "1.0.0";
            public string DownloadUrl { get; set; } = "";
            public string Changelog { get; set; } = "";
        }

        public static async Task<VersionInfo?> CheckForUpdatesAsync(string currentVersion)
        {
            try
            {
                // In a real scenario, this would call the backend
                // string json = await Client.GetStringAsync(VersionApi);
                // var latest = JsonConvert.DeserializeObject<VersionInfo>(json);
                
                // Mocking a newer version for demonstration
                await Task.Delay(500); 
                var latest = new VersionInfo 
                { 
                    Version = "1.1.0", 
                    DownloadUrl = "https://github.com/yourusername/deepeyeunlocker/releases/v1.1.0",
                    Changelog = "Added Hindi localization and MSI installer support."
                };

                if (IsNewerVersion(currentVersion, latest.Version))
                {
                    Logger.Info($"Update Available: {latest.Version} (Current: {currentVersion})");
                    return latest;
                }
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to check for updates.");
            }
            return null;
        }

        private static bool IsNewerVersion(string current, string latest)
        {
            Version vCurrent = new Version(current);
            Version vLatest = new Version(latest);
            return vLatest > vCurrent;
        }
    }
}
