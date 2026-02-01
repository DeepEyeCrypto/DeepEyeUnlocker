using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using Microsoft.EntityFrameworkCore;

namespace DeepEyeUnlocker.Infrastructure.Data.Seed
{
    public static class SeedDevices
    {
        public static async Task InitializeAsync(DeviceDbContext context, string seedDataPath)
        {
            await context.Database.EnsureCreatedAsync();

            if (!Directory.Exists(seedDataPath))
            {
                return;
            }

            var files = Directory.GetFiles(seedDataPath, "seed_*.json");
            foreach (var file in files)
            {
                await LoadFromFileAsync(context, file);
            }
        }

        private static async Task LoadFromFileAsync(DeviceDbContext context, string filePath)
        {
            try
            {
                var json = await File.ReadAllTextAsync(filePath);
                var devices = JsonSerializer.Deserialize<List<DeviceProfile>>(json, new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true,
                    Converters = { new System.Text.Json.Serialization.JsonStringEnumConverter() }
                });

                if (devices == null) return;

                foreach (var device in devices)
                {
                    // Check if exists
                    var exists = await context.Devices.AnyAsync(d => d.ModelNumber == device.ModelNumber);
                    if (!exists)
                    {
                        context.Devices.Add(device);
                    }
                }

                await context.SaveChangesAsync();
            }
            catch (Exception ex)
            {
                // In production, log this
                Console.WriteLine($"Failed to seed from {filePath}: {ex.Message}");
            }
        }
    }
}
