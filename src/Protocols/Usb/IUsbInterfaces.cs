using System;
using LibUsbDotNet.Main;

namespace DeepEyeUnlocker.Protocols.Usb
{
    public interface IUsbDevice : IDisposable
    {
        IUsbEndpointReader OpenEndpointReader(ReadEndpointID readEndpointID);
        IUsbEndpointWriter OpenEndpointWriter(WriteEndpointID writeEndpointID);
        bool IsOpen { get; }
    }

    public interface IUsbEndpointReader
    {
        ErrorCode Read(byte[] buffer, int timeout, out int bytesRead);
    }

    public interface IUsbEndpointWriter
    {
        ErrorCode Write(byte[] buffer, int timeout, out int bytesWritten);
        ErrorCode Write(byte[] buffer, int offset, int count, int timeout, out int bytesWritten);
    }
}
