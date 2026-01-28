using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace DeepEyeUnlocker.Core.Cloud
{
    public class FirmwareEntry
    {
        public string Id { get; set; } = string.Empty;
        public string Model { get; set; } = string.Empty;
        public string Chipset { get; set; } = string.Empty;
        public string FileName { get; set; } = string.Empty;
        public string FileUrl { get; set; } = string.Empty;
        public long Size { get; set; }
        public string Checksum { get; set; } = string.Empty;
        public string Type { get; set; } = string.Empty; // "Firehose", "DA", "AuthToken"
    }

    public class FirmwareRepository
    {
        private static readonly HttpClient Client = new HttpClient();
        private const string BaseApiUrl = "https://api.deepeyeunlocker.io/api/firmware";

        public async Task<List<FirmwareEntry>> SearchAsync(string query)
        {
            try
            {
                // In real use: string response = await Client.GetStringAsync($"{BaseApiUrl}/search?q={Uri.EscapeDataString(query)}");
                // Mock response for builder demonstration
                await Task.Delay(500);
                return new List<FirmwareEntry>
                {
                    new FirmwareEntry { Id = "1", Model = "Xiaomi Redmi Note 10", Chipset = "SDM678", FileName = "prog_firehose_ddr.elf", Type = "Firehose", Size = 450123 },
                    new FirmwareEntry { Id = "2", Model = "Samsung A50", Chipset = "Exynos 9610", FileName = "DA_A50_v2.bin", Type = "DA", Size = 120500 },
                    new FirmwareEntry { Id = "3", Model = "Oppo A5s", Chipset = "MT6765", FileName = "MTK_AllInOne_DA.bin", Type = "DA", Size = 890400 }
                };
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to fetch firmware data from cloud.");
                return new List<FirmwareEntry>();
            }
        }
    }
}
