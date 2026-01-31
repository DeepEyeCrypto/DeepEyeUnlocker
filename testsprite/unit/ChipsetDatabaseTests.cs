using DeepEyeUnlocker.Protocols.MTK;
using Xunit;

namespace DeepEyeUnlocker.Testsprite.Unit
{
    public class ChipsetDatabaseTests
    {
        [Fact]
        public void GetName_ValidHwCode_ReturnsCorrectName()
        {
            // Arrange
            uint hwCode = 0x6765;
            string expected = "MT6765 (Helio P35)";

            // Act
            string result = MTKChipsetDatabase.GetName(hwCode);

            // Assert
            Assert.Equal(expected, result);
        }

        [Fact]
        public void GetName_UnknownHwCode_ReturnsFormattedHex()
        {
            // Arrange
            uint hwCode = 0x9999;
            string expected = "MT9999 (Unknown)";

            // Act
            string result = MTKChipsetDatabase.GetName(hwCode);

            // Assert
            Assert.Equal(expected, result);
        }
    }
}
