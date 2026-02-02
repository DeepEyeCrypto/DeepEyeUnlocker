using System;
using System.Linq;
using Xunit;
using DeepEyeUnlocker.Protocols.Samsung;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Tests.Protocols
{
    public class SamsungPitTests
    {
        [Fact]
        public void TestPitGenerationAndParsing()
        {
            // Arrange
            byte[] mockPit = PitParser.CreateMockPit();

            // Act
            var partitions = PitParser.Parse(mockPit).ToList();

            // Assert
            Assert.NotEmpty(partitions);
            Assert.Contains(partitions, p => p.Name == "SYSTEM");
            Assert.Contains(partitions, p => p.Name == "USERDATA");
            Assert.Contains(partitions, p => p.Name == "PERSISTENT");
            
            var persistent = partitions.First(p => p.Name == "PERSISTENT");
            Assert.Equal(1024UL, persistent.SizeInBytes);
        }

        [Fact]
        public void TestInvalidPitData()
        {
            // Arrange
            byte[] invalidData = new byte[10] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

            // Act
            var partitions = PitParser.Parse(invalidData);

            // Assert
            Assert.Empty(partitions);
        }
    }
}
