using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
namespace DeepEyeUnlocker.Core
{
    public class AnalyticsManager
    {
        private static readonly HttpClient Client = new HttpClient();
        private const string ApiUrl = "https://api.deepeyeunlocker.io/api/logs"; // Placeholder

        public static async Task LogJobAsync(string brand, string model, string operation, string status, string errorCode = "")
        {
            try
            {
                var jobData = new
                {
                    deviceId = "ANON_" + Guid.NewGuid().ToString().Substring(0, 8),
                    brand,
                    model,
                    operation,
                    status,
                    errorCode,
                    appVersion = "1.0.0"
                };

                var content = new StringContent(JsonConvert.SerializeObject(jobData), Encoding.UTF8, "application/json");
                // In production, we would use a try-send to avoid delaying the UI
                _ = Client.PostAsync(ApiUrl, content).ContinueWith(t => {
                    if (t.IsFaulted) Logger.Warn("Failed to send analytics to cloud.");
                });
            }
            catch (Exception ex)
            {
                Logger.Debug($"Analytics logging bypassed: " + ex.Message);
            }
            await Task.CompletedTask;
        }
    }
}
