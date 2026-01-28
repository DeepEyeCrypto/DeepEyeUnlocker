using System;

namespace DeepEyeUnlocker.Protocols.Samsung
{
    public enum OdinCommand : byte
    {
        Handshake = 0x00,
        GetDeviceInfo = 0x01,
        StartSession = 0x02,
        EndSession = 0x03,
        FileTransfer = 0x04,
        FlashFinished = 0x05,
        PitRequest = 0x06
    }

    public class SamsungConstants
    {
        public const ushort VID_SAMSUNG = 0x04E8;
        public const ushort PID_DOWNLOAD_MODE = 0x685D;
        public const int DefaultTimeout = 5000;
        public const int ChunkSize = 131072; // 128 KB
    }
}
