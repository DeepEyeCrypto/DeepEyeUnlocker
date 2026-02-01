using Xunit;
using DeepEyeUnlocker.Protocols.Qualcomm;
using DeepEyeUnlocker.Tests.Mocks;
using System.Threading.Tasks;
using System.Linq;

namespace DeepEyeUnlocker.Tests.Protocols.Qualcomm
{
    public class SaharaProtocolTests
    {
        [Fact]
        public async Task ProcessHelloAsync_ShouldSendHelloResponse_WhenHelloReceived()
        {
            // Arrange
            var mockDevice = new MockUsbDevice();
            var protocol = new SaharaProtocol(mockDevice);

            // Simulate incoming Sahara Hello packet
            var hello = new SaharaHelloPacket
            {
                Header = new SaharaPacketHeader { Command = SaharaCommand.Hello, Length = 48 },
                Version = 2,
                Mode = 0 // Image Transfer Mode
            };
            mockDevice.EnqueuePacket(hello);

            // Act
            bool result = await protocol.ProcessHelloAsync();

            // Assert
            Assert.True(result);
            Assert.Single(mockDevice.OutboundPackets);
            
            // Verify Outbound Packet is HelloResponse
            byte[] response = mockDevice.OutboundPackets.First();
            Assert.Equal((byte)SaharaCommand.HelloResponse, response[0]);
        }
    }
}
