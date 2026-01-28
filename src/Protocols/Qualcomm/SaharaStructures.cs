using System;
using System.Runtime.InteropServices;

namespace DeepEyeUnlocker.Protocols.Qualcomm
{
    public enum SaharaCommand : uint
    {
        Hello = 0x01,
        HelloResponse = 0x02,
        ReadData = 0x03,
        EndTransfer = 0x04,
        Done = 0x05,
        DoneResponse = 0x06,
        Reset = 0x07,
        ResetResponse = 0x08,
        MemoryDebug = 0x09,
        MemoryRead = 0x0A,
        Command = 0x0B,
        CommandResponse = 0x0C,
        SwitchMode = 0x0D,
        Execute = 0x0E,
        ExecuteResponse = 0x0F,
        ExecuteData = 0x10
    }

    public enum SaharaStatus : uint
    {
        Success = 0x00,
        InvalidCmd = 0x01,
        ProtocolVerMismatch = 0x02,
        InvalidTargetVer = 0x03,
        InvalidHelloPacket = 0x04,
        InvalidPacketSize = 0x05,
        UnexpectedPacket = 0x06,
        InvalidFlashId = 0x07,
        InvalidTimeout = 0x08,
        InvalidImageSize = 0x09,
        InvalidDataSize = 0x0A,
        InvalidImageId = 0x0B,
        InvalidImageTxSize = 0x0C,
        InvalidImageTxOffset = 0x0D,
        InvalidImageTxAddress = 0x0E,
        InvalidDataTxSize = 0x10,
        InvalidDataTxOffset = 0x11,
        InvalidDataTxAddress = 0x12,
        MemoryDebugNotSupported = 0x13,
        InvalidMemoryDebugLength = 0x14,
        InvalidMemoryDebugAddress = 0x15,
        InvalidCommand = 0x16,
        CommandNotSupported = 0x17,
        InvalidCommandParameter = 0x18,
        CommandExecuteError = 0x19,
        InvalidClientCode = 0x1A,
        I7InvalidProcedure = 0x1B,
        HashVerificationFailed = 0x1C,
        SecurityError = 0x1D
    }

    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    public struct SaharaPacketHeader
    {
        public SaharaCommand Command;
        public uint Length;
    }

    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    public struct SaharaHelloPacket
    {
        public SaharaPacketHeader Header;
        public uint Version;
        public uint MinVersion;
        public uint MaxRawDataLength;
        public uint Mode;
        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 6)]
        public uint[] Reserved;
    }

    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    public struct SaharaReadDataPacket
    {
        public SaharaPacketHeader Header;
        public uint ImageId;
        public uint DataOffset;
        public uint DataLength;
    }

    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    public struct SaharaEndTransferPacket
    {
        public SaharaPacketHeader Header;
        public uint ImageId;
        public SaharaStatus Status;
    }
}
