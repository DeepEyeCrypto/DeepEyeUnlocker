using System;
using System.Runtime.InteropServices;

namespace DeepEyeUnlocker.Protocols.MTK
{
    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    public struct DAConfig
    {
        public uint Magic; // 0x55AA55AA
        public uint Offset;
        public uint Size;
        public uint Flags;
    }

    public enum DACommand : byte
    {
        GetVersion = 0xA0,
        SendDA = 0xD7,
        JumpDA = 0xD5,
        Format = 0xD4,
        WriteData = 0xD1,
        ReadData = 0xD2,
        GetDeviceInfo = 0xFE
    }

    public static class MTKDAVersion
    {
        public const byte DA_SIG_SIZE = 16;
        public const uint DA_MAGIC = 0x55AA55AA;
    }
}
