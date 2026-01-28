using System;
using System.Net.Http;
using System.IO;
using System.Threading.Tasks;
namespace DeepEyeUnlocker.Core
{
    public class ResourceManager
    {
        private static readonly HttpClient Client = new HttpClient();
        private const string BaseUrl = "https://repo.deepeyeunlocker.io/resource/"; // Placeholder

        public static async Task<string?> DownloadProgrammerAsync(string brand, string model, string chipset, Action<int>? progressCallback = null)
        {
            string fileName = $"{brand}_{model}_{chipset}.mbn".ToLower();
            string resourceDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Resources", "Programmers");
            string localPath = Path.Combine(resourceDir, fileName);

            if (File.Exists(localPath))
            {
                Logger.Info($"Programmer already exists locally: {fileName}");
                return localPath;
            }

            try
            {
                if (!Directory.Exists(resourceDir))
                    Directory.CreateDirectory(resourceDir);

                Logger.Info($"Connecting to cloud storage for: {fileName}...");
                
                using var response = await Client.GetAsync($"{BaseUrl}{fileName}", HttpCompletionOption.ResponseHeadersRead);
                response.EnsureSuccessStatusCode();

                var totalBytes = response.Content.Headers.ContentLength ?? -1L;
                using var contentStream = await response.Content.ReadAsStreamAsync();
                using var fileStream = new FileStream(localPath, FileMode.Create, FileAccess.Write, FileShare.None, 8192, true);

                var buffer = new byte[8192];
                var bytesReadTotal = 0L;
                int bytesRead;

                while ((bytesRead = await contentStream.ReadAsync(buffer, 0, buffer.Length)) > 0)
                {
                    await fileStream.WriteAsync(buffer, 0, bytesRead);
                    bytesReadTotal += bytesRead;

                    if (totalBytes != -1)
                    {
                        var percentage = (int)((bytesReadTotal * 100) / totalBytes);
                        progressCallback?.Invoke(percentage);
                    }
                }

                Logger.Info($"Resource downloaded successfully: {localPath} ({bytesReadTotal} bytes)");
                return localPath;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, $"Critical error downloading resource: {fileName}. Check internet connection or base URL.");
                if (File.Exists(localPath)) File.Delete(localPath); // Clean up partial download
                return null;
            }
        }
    }
}
