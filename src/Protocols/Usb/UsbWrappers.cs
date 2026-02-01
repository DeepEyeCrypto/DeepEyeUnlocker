using LibUsbDotNet;
using LibUsbDotNet.Main;
using System;

namespace DeepEyeUnlocker.Protocols.Usb
{
    public class UsbDeviceWrapper : DeepEyeUnlocker.Protocols.Usb.IUsbDevice
    {
        private readonly UsbDevice _device;

        public UsbDeviceWrapper(UsbDevice device)
        {
            _device = device ?? throw new ArgumentNullException(nameof(device));
        }

        public bool IsOpen => _device.IsOpen;

        public IUsbEndpointReader OpenEndpointReader(ReadEndpointID readEndpointID)
        {
            return new UsbEndpointReaderWrapper(_device.OpenEndpointReader(readEndpointID));
        }

        public IUsbEndpointWriter OpenEndpointWriter(WriteEndpointID writeEndpointID)
        {
            return new UsbEndpointWriterWrapper(_device.OpenEndpointWriter(writeEndpointID));
        }

        public void Dispose()
        {
            _device?.Close();
        }
    }

    public class UsbEndpointReaderWrapper : IUsbEndpointReader
    {
        private readonly UsbEndpointReader _reader;

        public UsbEndpointReaderWrapper(UsbEndpointReader reader)
        {
            _reader = reader ?? throw new ArgumentNullException(nameof(reader));
        }

        public ErrorCode Read(byte[] buffer, int timeout, out int bytesRead)
        {
            return _reader.Read(buffer, timeout, out bytesRead);
        }
    }

    public class UsbEndpointWriterWrapper : IUsbEndpointWriter
    {
        private readonly UsbEndpointWriter _writer;

        public UsbEndpointWriterWrapper(UsbEndpointWriter writer)
        {
            _writer = writer ?? throw new ArgumentNullException(nameof(writer));
        }

        public ErrorCode Write(byte[] buffer, int timeout, out int bytesWritten)
        {
            return _writer.Write(buffer, timeout, out bytesWritten);
        }

        public ErrorCode Write(byte[] buffer, int offset, int count, int timeout, out int bytesWritten)
        {
            return _writer.Write(buffer, offset, count, timeout, out bytesWritten);
        }
    }
}
