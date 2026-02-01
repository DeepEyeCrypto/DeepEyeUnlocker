using Xunit;
using DeepEyeUnlocker.Core.Services;
using System.IO;

namespace DeepEyeUnlocker.Tests.Core
{
    public class DiagnosticServiceTests
    {
        [Fact]
        public void GenerateSystemReport_ShouldContainAppVersion()
        {
            // Arrange
            var service = new DiagnosticService();

            // Act
            var report = service.GenerateSystemReport();

            // Assert
            Assert.Contains("App Version", report);
            Assert.Contains("DEEPEYEUNLOCKER DIAGNOSTIC REPORT", report);
        }

        [Fact]
        public void ExportReport_ShouldCreateFile()
        {
            // Arrange
            var service = new DiagnosticService();
            string tempPath = Path.Combine(Path.GetTempPath(), "test_diag.txt");

            try
            {
                // Act
                service.ExportReport(tempPath);

                // Assert
                Assert.True(File.Exists(tempPath));
                string content = File.ReadAllText(tempPath);
                Assert.Contains("END REPORT", content);
            }
            finally
            {
                if (File.Exists(tempPath)) File.Delete(tempPath);
            }
        }
    }
}
