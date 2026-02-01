using System;

namespace DeepEyeUnlocker.Infrastructure.HIL
{
    public enum UsbDirection
    {
        HostToDevice,
        DeviceToHost
    }

    public class UsbPacket
    {
        public long TimestampUs { get; set; }
        public byte[] Data { get; set; } = Array.Empty<byte>();
        public UsbDirection Direction { get; set; }
        public int Endpoint { get; set; }
        public string? Label { get; set; }

        public override string ToString() => $"[{TimestampUs}us] {Direction} EP:{Endpoint} ({Data.Length} bytes)";
    }
}
