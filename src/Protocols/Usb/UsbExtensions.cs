using System.Threading.Tasks;
using LibUsbDotNet.Main;

namespace DeepEyeUnlocker.Protocols.Usb
{
    public static class UsbExtensions
    {
        public static Task<int> WriteAsync(this IUsbDevice device, byte[] buffer, int timeout = 1000)
        {
            return Task.Run(() =>
            {
                var writer = device.OpenEndpointWriter(WriteEndpointID.Ep01);
                writer.Write(buffer, timeout, out int written);
                return written;
            });
        }

        public static Task<byte[]> ReadAsync(this IUsbDevice device, int length, int timeout = 1000)
        {
            return Task.Run(() =>
            {
                var reader = device.OpenEndpointReader(ReadEndpointID.Ep01);
                byte[] buffer = new byte[length];
                reader.Read(buffer, timeout, out int read);
                if (read < length)
                {
                    byte[] actual = new byte[read];
                    System.Array.Copy(buffer, 0, actual, 0, read);
                    return actual;
                }
                return buffer;
            });
        }
    }
}
