using System;
using System.Collections.Concurrent;
using System.Runtime.InteropServices;
using DeepEyeUnlocker.Protocols.Usb;
using LibUsbDotNet.Main;

namespace DeepEyeUnlocker.Tests.Mocks
{
    public class MockUsbDevice : DeepEyeUnlocker.Protocols.Usb.IUsbDevice
    {
        public ConcurrentQueue<byte[]> InboundPackets { get; } = new ConcurrentQueue<byte[]>();
        public ConcurrentQueue<byte[]> OutboundPackets { get; } = new ConcurrentQueue<byte[]>();

        public bool IsOpen => true;

        public IUsbEndpointReader OpenEndpointReader(ReadEndpointID readEndpointID)
        {
            return new MockEndpointReader(this);
        }

        public IUsbEndpointWriter OpenEndpointWriter(WriteEndpointID writeEndpointID)
        {
            return new MockEndpointWriter(this);
        }

        public void EnqueuePacket<T>(T packet) where T : struct
        {
            int size = Marshal.SizeOf(packet);
            byte[] bytes = new byte[size];
            IntPtr ptr = Marshal.AllocHGlobal(size);
            try
            {
                Marshal.StructureToPtr(packet, ptr, false);
                Marshal.Copy(ptr, bytes, 0, size);
                InboundPackets.Enqueue(bytes);
            }
            finally
            {
                Marshal.FreeHGlobal(ptr);
            }
        }

        public void Dispose() { }

        private class MockEndpointReader : IUsbEndpointReader
        {
            private readonly MockUsbDevice _parent;
            public MockEndpointReader(MockUsbDevice parent) => _parent = parent;

            public ErrorCode Read(byte[] buffer, int timeout, out int bytesRead)
            {
                if (_parent.InboundPackets.TryDequeue(out var packet))
                {
                    Array.Copy(packet, buffer, Math.Min(packet.Length, buffer.Length));
                    bytesRead = packet.Length;
                    return ErrorCode.None;
                }
                bytesRead = 0;
                return ErrorCode.None;
            }
        }

        private class MockEndpointWriter : IUsbEndpointWriter
        {
            private readonly MockUsbDevice _parent;
            public MockEndpointWriter(MockUsbDevice parent) => _parent = parent;

            public ErrorCode Write(byte[] buffer, int timeout, out int bytesWritten)
            {
                return Write(buffer, 0, buffer.Length, timeout, out bytesWritten);
            }

            public ErrorCode Write(byte[] buffer, int offset, int count, int timeout, out int bytesWritten)
            {
                byte[] copy = new byte[count];
                Array.Copy(buffer, offset, copy, 0, count);
                _parent.OutboundPackets.Enqueue(copy);
                bytesWritten = count;
                return ErrorCode.None;
            }
        }
    }
}
