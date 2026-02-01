using System;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure.Data;
using DeepEyeUnlocker.Infrastructure.Data.Seed;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Xunit;

namespace DeepEyeUnlocker.Tests.Integration
{
    public class DeviceDatabaseTests : IDisposable
    {
        private readonly SqliteConnection _connection;
        private readonly DeviceDbContext _context;
        private readonly string _tempSeedPath;

        public DeviceDatabaseTests()
        {
            // Setup File-based SQLite for robust testing
            var uniqueId = Guid.NewGuid().ToString();
            _tempSeedPath = Path.Combine("/tmp/deepeye_debug", uniqueId);
            Directory.CreateDirectory(_tempSeedPath);
            
            var dbPath = Path.Combine(_tempSeedPath, "test.db");

            _connection = new SqliteConnection($"DataSource={dbPath}");
            _connection.Open();

            var options = new DbContextOptionsBuilder<DeviceDbContext>()
                .UseSqlite(_connection)
                .Options;

            _context = new DeviceDbContext(options);
            _context.Database.EnsureCreated();

            // Setup Seed Data
            // Path already set
            
            var seedJson = @"[
              {
                ""ModelNumber"": ""TEST-001"",
                ""MarketingName"": ""Test Device"",
                ""Brand"": ""Samsung"",
                ""Chipset"": { ""Model"": ""Exynos Test"" },
                ""SupportedBootModes"": [ ""Download"" ],
                ""SupportedOperations"": [
                  { ""OperationName"": ""FrpRemove"", ""RiskLevel"": ""Low"" }
                ]
              }
            ]";
            File.WriteAllText(Path.Combine(_tempSeedPath, "seed_test.json"), seedJson);
        }

        [Fact]
        public async Task Seeding_ShouldPopulateDatabase()
        {
            var script = _context.Database.GenerateCreateScript();
            File.WriteAllText(Path.Combine(_tempSeedPath, "script.sql"), script);
            
            // Check tables
            using var cmd = _connection.CreateCommand();
            cmd.CommandText = "SELECT name FROM sqlite_master WHERE type='table'";
            using var reader = cmd.ExecuteReader();
            var tables = "";
            while(reader.Read()) tables += reader.GetString(0) + ", ";
            File.WriteAllText(Path.Combine(_tempSeedPath, "tables.txt"), tables);

            // Act
            await SeedDevices.InitializeAsync(_context, _tempSeedPath);

            // Assert
            var count = await _context.Devices.CountAsync();
            Assert.Equal(1, count);

            var device = await _context.Devices.FirstAsync();
            Assert.Equal("TEST-001", device.ModelNumber);
            Assert.Equal("Samsung", device.Brand);
            Assert.Equal("Exynos Test", device.Chipset.Model);
            Assert.Contains("Download", device.SupportedBootModes);
            Assert.Single(device.SupportedOperations);
            Assert.Equal("FrpRemove", device.SupportedOperations.First().OperationName);
        }

        [Fact]
        public async Task SimpleInsert_ShouldWork()
        {
            var device = new DeviceProfile
            {
                ModelNumber = "SIMPLE-001",
                MarketingName = "Simple Device",
                Chipset = new ChipsetInfo { Model = "Snapdragon 8" },
                SupportedOperations = new System.Collections.Generic.List<OperationSupport>
                {
                    new OperationSupport { OperationName = "TestOp", RiskLevel = RiskLevel.Low }
                }
            };

            _context.Devices.Add(device);
            await _context.SaveChangesAsync();

            Assert.Equal(1, await _context.Devices.CountAsync());
        }

        public void Dispose()
        {
            _context.Dispose();
            _connection.Dispose();
            // if (Directory.Exists(_tempSeedPath))
            //    Directory.Delete(_tempSeedPath, true);
        }
    }
}
