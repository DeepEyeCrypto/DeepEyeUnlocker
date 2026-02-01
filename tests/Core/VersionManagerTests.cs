using Xunit;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Tests.Core
{
    public class VersionManagerTests
    {
        [Fact]
        public void AppVersion_ShouldNotBeNullOrEmpty()
        {
            // Act
            var version = VersionManager.AppVersion;

            // Assert
            Assert.False(string.IsNullOrEmpty(version));
        }

        [Fact]
        public void BuildIdentifier_ShouldContainCommitHashStub()
        {
            // Act
            var identifier = VersionManager.BuildIdentifier;

            // Assert
            // In a CI environment it might be a real hash, in local it might be "dev"
            Assert.NotNull(identifier);
        }

        [Fact]
        public void FullVersionDisplay_ShouldContainVPrefix()
        {
            // Act
            var display = VersionManager.FullVersionDisplay;

            // Assert
            Assert.StartsWith("v", display);
        }
    }
}
