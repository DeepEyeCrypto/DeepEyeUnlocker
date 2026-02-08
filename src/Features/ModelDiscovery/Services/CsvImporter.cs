using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using DeepEyeUnlocker.Features.ModelDiscovery.Models;
using Microsoft.EntityFrameworkCore;
using DeepEyeUnlocker.Features.ModelDiscovery.Database;

namespace DeepEyeUnlocker.Features.ModelDiscovery.Services
{
    public class CsvImporter
    {
        private readonly DiscoveryDbContext _db;

        public CsvImporter(DiscoveryDbContext db)
        {
            _db = db;
        }

        public async Task ImportFromCsvAsync(string filePath)
        {
            if (!File.Exists(filePath))
            {
                Console.WriteLine($"[Error] File not found: {filePath}");
                return;
            }

            Console.WriteLine($"[Info] Starting CSV import from: {filePath}");

            var lines = await File.ReadAllLinesAsync(filePath);
            var modelsToAdd = new List<SupportedModel>();
            int skipped = 0;

            // Assuming header: Brand,Model,Series,Year,Type
            foreach (var line in lines)
            {
                if (string.IsNullOrWhiteSpace(line) || line.StartsWith("Brand,Model", StringComparison.OrdinalIgnoreCase))
                    continue;

                var parts = line.Split(',');
                if (parts.Length < 2) 
                {
                    skipped++;
                    continue;
                }

                string brand = parts[0].Trim();
                string modelName = parts[1].Trim();
                string series = parts.Length > 2 ? parts[2].Trim() : "";
                string year = parts.Length > 3 ? parts[3].Trim() : "";
                string type = parts.Length > 4 ? parts[4].Trim() : "Unknown";

                // Generate a unique marketing name like "Galaxy S24 Ultra (2024)"
                string finalMarketingName = string.IsNullOrWhiteSpace(year) ? modelName : $"{modelName} ({year})";

                // Default capabilities for all these models
                var operations = new List<string> { "FRP", "Screen", "Factory Reset" };
                
                // Add specific flags based on Type (e.g. Flagship might have tougher security)
                if (type.Contains("Flagship", StringComparison.OrdinalIgnoreCase))
                {
                    operations.Add("Knox Guard Check"); // Assume Samsung/others have high security
                }

                var newModel = new SupportedModel
                {
                    Tool = "DeepEyeUnlocker", // Native support
                    Brand = brand,
                    MarketingName = finalMarketingName,
                    ModelNumber = modelName, // Fallback if no specific code given
                    ChipsetFamily = "Auto-Detect", // Will be detected at runtime
                    OperationsJson = System.Text.Json.JsonSerializer.Serialize(operations),
                    ModesJson = System.Text.Json.JsonSerializer.Serialize(new[] { "MTP", "ADB", "Download Mode", "Fastboot" }),
                    SourceUrl = "Internal CSV Import",
                    FirstSeen = DateTime.UtcNow,
                    LastSeen = DateTime.UtcNow
                };

                // Check for duplicates before adding
                bool exists = await _db.SupportedModels.AnyAsync(m => m.Brand == brand && m.MarketingName == finalMarketingName);
                if (!exists)
                {
                    modelsToAdd.Add(newModel);
                }
                else
                {
                    skipped++;
                }
            }

            if (modelsToAdd.Any())
            {
                await _db.SupportedModels.AddRangeAsync(modelsToAdd);
                await _db.SaveChangesAsync();
                Console.WriteLine($"[Success] Imported {modelsToAdd.Count} new models.");
            }
            else
            {
                Console.WriteLine("[Info] No new models to import.");
            }

            Console.WriteLine($"[Info] Skipped {skipped} entries (duplicates or invalid).");
        }
    }
}
