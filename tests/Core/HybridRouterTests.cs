using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xunit;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Services;
using DeepEyeUnlocker.Protocols.ModelSpecific;
using DeepEyeUnlocker.Protocols.MTK;
using DeepEyeUnlocker.Protocols.SPD;
using DeepEyeUnlocker.Protocols.Qualcomm;
using DeepEyeUnlocker.Infrastructure.Data;
using Microsoft.EntityFrameworkCore;
using DeepEyeUnlocker.Protocols.ModelSpecific.Handlers;

using Microsoft.Data.Sqlite;
using DeepEyeUnlocker.Core.Services;
using DeepEyeUnlocker.Core.Architecture;
using DeepEyeUnlocker.Protocols.Usb;

namespace DeepEyeUnlocker.Tests.Core
{
    public class HybridRouterTests
    {
        private readonly HybridOperationRouter _router;
        private readonly DeviceDbContext _context;
        private readonly SqliteConnection _connection;

        public HybridRouterTests()
        {
            // 1. Setup SQLite In-Memory DB for Cloud Service
            _connection = new SqliteConnection("DataSource=:memory:");
            _connection.Open();

            var options = new DbContextOptionsBuilder<DeviceDbContext>()
                .UseSqlite(_connection)
                .Options;
                
            _context = new DeviceDbContext(options);
            _context.Database.EnsureCreated();
            
            var cloudService = new CloudProfileService(_context);
            var modelPlugin = new ModelSpecificPlugin(cloudService);
            
            // Register a handler to simulate "UnlockTool Mode" availability
            modelPlugin.RegisterHandler(new SamsungS24Handler());

            // 2. Setup Universal Plugins (Mocks)
            var plugins = new List<IUniversalPlugin>
            {
                new FakeUniversalPlugin("MTK_BootROM_Universal"),
                new FakeUniversalPlugin("SPD_FDL_Universal"),
                new FakeUniversalPlugin("Qualcomm_EDL_Universal")
            };
            
            var classifier = new DeviceClassifier();
            var mockInteraction = new FakeUserInteraction();

            // 3. Create Router
            _router = new HybridOperationRouter(classifier, modelPlugin, plugins, mockInteraction);
        }

        class FakeUserInteraction : IUserInteraction
        {
             public Task<bool> ConfirmAsync(string title, string message, bool isRisk) => Task.FromResult(true);
        }

        [Fact]
        public async Task Route_Flagship_ToModelSpecific()
        {
            // Arrange
            var s24 = new DeviceProfile
            {
                ModelNumber = "SM-S921B",
                Brand = "Samsung",
                MarketingName = "Galaxy S24",
                Chipset = new ChipsetInfo { Model = "Snapdragon 8 Gen 3" }
            };

            // Act
            var result = await _router.ExecuteSmartAsync("Odin_Flash", s24, new Dictionary<string, object>());

            // Assert
            Assert.True(result.Success);
            Assert.Contains("Odin Protocol v4", result.Message);
        }

        [Fact]
        public async Task Route_LegacyMTK_ToUniversal()
        {
            // Arrange
            var legacyMtk = new DeviceProfile
            {
                ModelNumber = "Generic-MTK",
                Brand = "Generic",
                MarketingName = "Cheap Phone",
                Chipset = new ChipsetInfo { Model = "MT6735" }
            };

            // Act
            var result = await _router.ExecuteSmartAsync("ReadFlash", legacyMtk, new Dictionary<string, object>());

            // Assert
            Assert.True(result.Success);
            Assert.Contains("Universal Operation Success", result.Message); 
        }
        
        [Fact]
        public async Task Route_KeypadPhone_ToUniversalKeypadLogic()
        {
            // Arrange
            var keypad = new DeviceProfile
            {
                ModelNumber = "Jio-F220B",
                Brand = "Jio",
                MarketingName = "JioPhone",
                Chipset = new ChipsetInfo { Model = "SC9820A" } 
            };

            // Act
            var result = await _router.ExecuteSmartAsync("ReadCode", keypad, new Dictionary<string, object>());

            // Assert
            Assert.True(result.Success);
            Assert.Contains("Universal Keypad Success", result.Message);
        }

        class FakeUniversalPlugin : IUniversalPlugin
        {
            public string ProtocolName { get; }
            public string[] SupportedChips => Array.Empty<string>();

            public FakeUniversalPlugin(string name) => ProtocolName = name;

            public Task<bool> DetectDeviceAsync(IUsbDevice device) => Task.FromResult(true);
            public Task<ConnectionResult> ConnectAsync(ConnectionOptions options) => Task.FromResult(new ConnectionResult { Success = true });
            public Task<DeviceInfo> GetDeviceInfoAsync() => Task.FromResult(new DeviceInfo());

            public Task<OperationResult> ExecuteOperationAsync(string operation, Dictionary<string, object> parameters, DeviceProfile device)
            {
                return Task.FromResult(new OperationResult { Success = true, Message = "Universal Operation Success" });
            }

            public Task<OperationResult> ExecuteKeypadOperationAsync(string operation, DeviceProfile device)
            {
                return Task.FromResult(new OperationResult { Success = true, Message = "Universal Keypad Success" });
            }
        }
    }
}
