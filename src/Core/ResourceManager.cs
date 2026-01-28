using System;
using System.Net.Http;
using System.IO;
using System.Threading.Tasks;
using NLog;

namespace DeepEyeUnlocker.Core
{
    public class ResourceManager
    {
        private static readonly Logger Logger = LogManager.GetCurrentClassLogger();
        private static readonly HttpClient Client = new HttpClient();
        private const string BaseUrl = "https://repo.deepeyeunlocker.io/resource/"; // Placeholder

        public static async Task<string?> DownloadProgrammerAsync(string brand, string model, string chipset)
        {
            string fileName = $"{brand}_{model}_{chipset}.mbn";
            string localPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Resources", "Programmers", fileName);

            if (File.Exists(localPath))
            {
                Logger.Info($"Programmer already exists locally: {fileName}");
                return localPath;
            }

            try
            {
                Logger.Info($"Downloading programmer from cloud: {fileName}...");
                
                if (!Directory.Exists(Path.GetDirectoryName(localPath)))
                    Directory.CreateDirectory(Path.GetDirectoryName(localPath)!);

                // Mocking download for demonstration
                await Task.Delay(1000); 
                // Actual: var response = await Client.GetAsync($"{BaseUrl}{fileName}");
                
                // In this dummy version we just create an empty file if successful
                File.WriteAllText(localPath, "DUMMY_PROGRAMMER_DATA");

                Logger.Info($"Resource downloaded successfully: {localPath}");
                return localPath;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, $"Failed to download resource: {fileName}");
                return null;
            }
        }
    }
}
