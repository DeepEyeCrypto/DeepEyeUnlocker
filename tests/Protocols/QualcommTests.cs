using System;
using Xunit;
using DeepEyeUnlocker.Protocols.Qualcomm;

namespace DeepEyeUnlocker.Tests.Protocols
{
    public class QualcommTests
    {
        [Fact]
        public void SaharaPacketHeader_SizeVerify()
        {
            // Verify that our structs match the expected binary size for the protocol
            int size = System.Runtime.InteropServices.Marshal.SizeOf<SaharaPacketHeader>();
            Assert.Equal(8, size); // Command (4) + Length (4)
        }

        [Fact]
        public void SaharaHelloPacket_SizeVerify()
        {
            int size = System.Runtime.InteropServices.Marshal.SizeOf<SaharaHelloPacket>();
            // Header(8) + Version(4) + MinVer(4) + MaxData(4) + Mode(4) + Reserved(6*4=24) = 48
            Assert.Equal(48, size);
        }
    }
}
