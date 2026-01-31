using Xunit;
using DeepEyeUnlocker.Features.FrpBypass;
using DeepEyeUnlocker.Core.Models;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Tests.Features.FrpBypass
{
    public class SafetyGateTests
    {
        [Fact]
        public async Task ValidateEnvironment_WithValidDevice_ShouldReturnTrue()
        {
            // Arrange
            var device = new DeviceContext { Serial = "VALID_SN" };

            // Act
            bool result = await SafetyGate.ValidateEnvironment(device);

            // Assert
            Assert.True(result);
        }

        // Note: Future tests should include mocking the battery reader 
        // to test the < 30% failure case once injected.
    }
}
