using System;
using System.IO;
using System.Threading.Tasks;
using LibUsbDotNet;
using LibUsbDotNet.Main;
using NLog;

namespace DeepEyeUnlocker.Protocols.MTK
{
    public class MTKPreloader
    {
        private static readonly Logger Logger = LogManager.GetCurrentClassLogger();
        private readonly UsbDevice _usbDevice;
        private readonly UsbEndpointReader _reader;
        private readonly UsbEndpointWriter _writer;

        public MTKPreloader(UsbDevice usbDevice)
        {
            _usbDevice = usbDevice;
            _reader = _usbDevice.OpenEndpointReader(ReadEndpointID.Ep01);
            _writer = _usbDevice.OpenEndpointWriter(WriteEndpointID.Ep01);
        }

        public async Task<bool> HandshakeAsync()
        {
            Logger.Info("Starting MediaTek handshake...");
            
            byte[] handshake = { 0xA0, 0x0A, 0x50, 0x05 };
            foreach (byte b in handshake)
            {
                if (!await SendByteAsync(b)) return false;
                
                byte[] response = new byte[1];
                int read;
                _reader.Read(response, 1000, out read);
                
                // MTK BROM echoes back inverted bits or ACK
                if (read == 0)
                {
                    Logger.Error($"MTK Handshake failed at byte {b:X2}");
                    return false;
                }
            }

            Logger.Info("MediaTek Handshake successful.");
            return true;
        }

        public async Task<ushort> GetHWCodeAsync()
        {
            await SendByteAsync((byte)MTKCommand.GetHWCode);
            byte[] response = new byte[2];
            int read;
            _reader.Read(response, 1000, out read);
            if (read == 2)
            {
                ushort hwCode = BitConverter.ToUInt16(response, 0);
                Logger.Info($"MTK HW Code: 0x{hwCode:X4}");
                return hwCode;
            }
            return 0;
        }

        private async Task<bool> SendByteAsync(byte b)
        {
            int written;
            _writer.Write(new byte[] { b }, 1000, out written);
            return written == 1;
        }
    }
}
