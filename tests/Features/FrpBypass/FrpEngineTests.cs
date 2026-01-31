using Moq;
using Xunit;
using DeepEyeUnlocker.Features.FrpBypass;
using DeepEyeUnlocker.Features.FrpBypass.Interfaces;
using DeepEyeUnlocker.Features.FrpBypass.Models;
using DeepEyeUnlocker.Core.Models;
using System.Threading.Tasks;
using System.Collections.Generic;
using Newtonsoft.Json;
using System.IO;

namespace DeepEyeUnlocker.Tests.Features.FrpBypass
{
    public class FrpEngineTests
    {
        private readonly Mock<IFrpProtocol> _mockProtocol;
        private readonly DeviceContext _device;

        public FrpEngineTests()
        {
            _mockProtocol = new Mock<IFrpProtocol>();
            _device = new DeviceContext { Serial = "TEST123456", Brand = "Xiaomi" };
        }

        [Fact]
        public async Task ExecuteBypass_WithValidEdlProfile_ShouldCallEdlFlash()
        {
            // Arrange
            var profile = new FrpBrandProfile
            {
                Brand = "Xiaomi",
                Method = "EDL_WRITE_FLAG",
                TargetPartition = "config",
                Offset = 16,
                HexPayload = "00"
            };
            
            _mockProtocol.Setup(p => p.EDL_Flash_Partition(It.IsAny<string>(), It.IsAny<byte[]>()))
                         .ReturnsAsync(true);
            
            var engine = new FrpEngineCore(_mockProtocol.Object, profile);

            // Act
            bool result = await engine.ExecuteBypassAsync(_device);

            // Assert
            Assert.True(result);
            _mockProtocol.Verify(p => p.EDL_Flash_Partition("config", It.IsAny<byte[]>()), Times.Once);
        }
        [Fact]
        public async Task ExecuteBypass_WithXiaomiEdlErase_ShouldCallEdlErase()
        {
            // Arrange
            var profile = new FrpBrandProfile
            {
                Brand = "Xiaomi",
                Method = "QUALCOMM_EDL_ERASE",
                TargetPartition = "frp"
            };
            
            _mockProtocol.Setup(p => p.EDL_Erase_Partition(It.IsAny<string>()))
                         .ReturnsAsync(true);
            
            var engine = new FrpEngineCore(_mockProtocol.Object, profile);

            // Act
            bool result = await engine.ExecuteBypassAsync(_device);

            // Assert
            Assert.True(result);
            _mockProtocol.Verify(p => p.EDL_Erase_Partition("frp"), Times.Once);
        }

        [Fact]
        public async Task ExecuteBypass_WithSamsungOdinProfile_ShouldCallResetCommand()
        {
            // Arrange
            var profile = new FrpBrandProfile
            {
                Brand = "Samsung",
                Method = "ODIN_KERNEL_PATCH"
            };
            
            _mockProtocol.Setup(p => p.Samsung_Odin_Send_Command(It.IsAny<string>()))
                         .ReturnsAsync(true);
            
            var engine = new FrpEngineCore(_mockProtocol.Object, profile);

            // Act
            bool result = await engine.ExecuteBypassAsync(_device);

            // Assert
            Assert.True(result);
            _mockProtocol.Verify(p => p.Samsung_Odin_Send_Command("RESET_FRP_BIT"), Times.Once);
        }

        [Fact]
        public void FrpProfilesJson_ShouldBeValidAndParsable()
        {
            // Arrange
            string jsonPath = Path.Combine("..", "..", "..", "..", "src", "Features", "FrpBypass", "Profiles", "FrpProfiles.json");
            
            // Act
            string json = File.ReadAllText(jsonPath);
            var profiles = JsonConvert.DeserializeObject<List<FrpBrandProfile>>(json);

            // Assert
            Assert.NotNull(profiles);
            Assert.True(profiles.Count > 0);
            Assert.Contains(profiles, p => p.Brand == "Xiaomi");
            Assert.Contains(profiles, p => p.Brand == "Samsung");
        }
        [Fact]
        public async Task ExecuteBypass_WithSamsungOdin_ShouldCallSamsungProtocol()
        {
            // Arrange
            var mockProtocol = new Mock<IFrpProtocol>();
            var profile = new FrpBrandProfile { Brand = "Samsung", Method = "SAMSUNG_ODIN_FACTORY_RESET", TargetPartition = "persistent" };
            var device = new DeviceContext { Brand = "Samsung", Model = "Galaxy S26" };
            var engine = new FrpEngineCore(mockProtocol.Object, profile);

            mockProtocol.Setup(p => p.EDL_Erase_Partition(It.IsAny<string>())).ReturnsAsync(true);

            // Act
            var result = await engine.ExecuteBypassAsync(device);

            // Assert
            Assert.True(result);
            mockProtocol.Verify(p => p.EDL_Erase_Partition("persistent"), Times.AtLeastOnce());
        }
    }
}
