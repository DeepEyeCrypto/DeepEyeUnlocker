using System;
using System.IO;
using System.Net.Http;
using System.Security.Cryptography;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Core.Cloud
{
    public class DownloadManager
    {
        private static readonly HttpClient Client = new HttpClient();

        public event Action<int>? OnProgress;

        public async Task<bool> DownloadFileAsync(string url, string destinationPath, string expectedChecksum = "")
        {
            try
            {
                Logger.Info($"Downloading resource: {Path.GetFileName(destinationPath)}");
                
                using var response = await Client.GetAsync(url, HttpCompletionOption.ResponseHeadersRead);
                response.EnsureSuccessStatusCode();

                var totalBytes = response.Content.Headers.ContentLength ?? -1L;
                using var contentStream = await response.Content.ReadAsStreamAsync();
                using var fileStream = new FileStream(destinationPath, FileMode.Create, FileAccess.Write, FileShare.None, 8192, true);

                var buffer = new byte[8192];
                var totalRead = 0L;
                int bytesRead;

                while ((bytesRead = await contentStream.ReadAsync(buffer, 0, buffer.Length)) != 0)
                {
                    await fileStream.WriteAsync(buffer, 0, bytesRead);
                    totalRead += bytesRead;

                    if (totalBytes != -1)
                    {
                        int progress = (int)((double)totalRead / totalBytes * 100);
                        OnProgress?.Invoke(progress);
                    }
                }

                if (!string.IsNullOrEmpty(expectedChecksum))
                {
                    return await VerifyChecksumAsync(destinationPath, expectedChecksum);
                }

                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, $"Download failed for {url}");
                return false;
            }
        }

        private async Task<bool> VerifyChecksumAsync(string filePath, string expected)
        {
            using var sha256 = SHA256.Create();
            using var stream = File.OpenRead(filePath);
            var hashBytes = await sha256.ComputeHashAsync(stream);
            var hashString = BitConverter.ToString(hashBytes).Replace("-", "").ToLower();
            
            bool match = hashString == expected.ToLower();
            if (!match) Logger.Warning($"Checksum mismatch for {filePath}. Expected {expected}, got {hashString}");
            return match;
        }
    }
}
