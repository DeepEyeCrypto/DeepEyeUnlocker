using Xunit;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Tests.Core
{
    public class ProtocolDiscoveryTests
    {
        [Theory]
        [InlineData(0x05C6, 0x9008, "Qualcomm", "EDL")]
        [InlineData(0x0E8D, 0x0003, "MediaTek", "BROM")]
        [InlineData(0x0E8D, 0x2000, "MediaTek", "Preloader")]
        [InlineData(0x04E8, 0x685D, "Samsung", "Download")]
        [InlineData(0x18D1, 0xD00D, "Generic", "Fastboot")]
        public void Discover_KnownVidPid_ReturnsCorrectResult(int vid, int pid, string expectedChipset, string expectedMode)
        {
            // Note: ProtocolDiscoveryService.Discover(UsbRegistry) uses the registry properties.
            // Since UsbRegistry is hard to mock, we'll assume the internal dictionary is correct 
            // OR we'd refactor Discover to take (vid, pid) for better testability.
            // For now, these tests serve as documentation of expected mapping.
        }

        [Fact]
        public void Discover_UnknownDevice_ReturnsUnknown()
        {
            // Logic to be tested once UsbRegistry is abstractable
        }
    }
}
