using System;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Protocols.MTK
{
    public enum MTKCommand : byte
    {
        GetHWCode = 0xFD,
        GetHWVer = 0xFC,
        GetSWVer = 0xFB,
        Read16 = 0xA2,
        Read32 = 0xD1,
        Write16 = 0xA1,
        Write32 = 0xD4,
        JumpDA = 0xD5,
        SendDA = 0xD7,
        Identify = 0xB1
    }

    public enum MTKResponse : byte
    {
        Ack = 0x5A,
        Nack = 0xA5
    }

    public class MTKConstants
    {
        public const ushort VID_MEDIATEK = 0x0E8D;
        public const ushort PID_BROM = 0x0003;
        public const ushort PID_PRELOADER = 0x2000;
        public const int HandshakeRetryCount = 10;
        public const int DefaultTimeout = 5000;
    }
}
