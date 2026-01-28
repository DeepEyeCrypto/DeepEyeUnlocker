using System;
using System.IO;
using System.Runtime.InteropServices;
using System.Threading.Tasks;
using LibUsbDotNet;
using LibUsbDotNet.Main;
using DeepEyeUnlocker.Core;
namespace DeepEyeUnlocker.Protocols.Qualcomm
{
    public class SaharaProtocol
    {
        private readonly UsbDevice _usbDevice;
        private readonly UsbEndpointReader _reader;
        private readonly UsbEndpointWriter _writer;

        public SaharaProtocol(UsbDevice usbDevice)
        {
            _usbDevice = usbDevice;
            _reader = _usbDevice.OpenEndpointReader(ReadEndpointID.Ep01);
            _writer = _usbDevice.OpenEndpointWriter(WriteEndpointID.Ep01);
        }

        public async Task<bool> ProcessHelloAsync()
        {
            byte[] buffer = new byte[1024];
            int bytesRead;
            
            _reader.Read(buffer, 1000, out bytesRead);
            await Task.Yield();
            if (bytesRead == 0) return false;

            var header = MemoryMarshal.Cast<byte, SaharaPacketHeader>(buffer)[0];
            if (header.Command != SaharaCommand.Hello)
            {
                Logger.Error($"Expected Hello packet, got {header.Command}");
                return false;
            }

            var hello = MemoryMarshal.Cast<byte, SaharaHelloPacket>(buffer)[0];
            Logger.Info($"Received Sahara Hello. Version: {hello.Version}, Mode: {hello.Mode}");

            // Send Hello Response
            SaharaHelloPacket response = new SaharaHelloPacket
            {
                Header = new SaharaPacketHeader { Command = SaharaCommand.HelloResponse, Length = (uint)Marshal.SizeOf<SaharaHelloPacket>() },
                Version = 2,
                MinVersion = 1,
                Mode = hello.Mode,
                MaxRawDataLength = 4096
            };

            return SendPacket(response);
        }

        public async Task<bool> UploadProgrammerAsync(string filePath)
        {
            if (!File.Exists(filePath))
            {
                Logger.Error($"Programmer file not found: {filePath}");
                return false;
            }

            byte[] programmerData = await File.ReadAllBytesAsync(filePath);
            Logger.Info($"Uploading programmer: {Path.GetFileName(filePath)} ({programmerData.Length} bytes)");

            while (true)
            {
                byte[] buffer = new byte[1024];
                int bytesRead;
                _reader.Read(buffer, 5000, out bytesRead);

                if (bytesRead == 0) break;

                var header = MemoryMarshal.Cast<byte, SaharaPacketHeader>(buffer)[0];
                
                if (header.Command == SaharaCommand.ReadData)
                {
                    var readReq = MemoryMarshal.Cast<byte, SaharaReadDataPacket>(buffer)[0];
                    byte[] chunk = new byte[readReq.DataLength];
                    Array.Copy(programmerData, readReq.DataOffset, chunk, 0, readReq.DataLength);
                    
                    int written;
                    _writer.Write(chunk, 1000, out written);
                }
                else if (header.Command == SaharaCommand.EndTransfer)
                {
                    var endTransfer = MemoryMarshal.Cast<byte, SaharaEndTransferPacket>(buffer)[0];
                    if (endTransfer.Status == SaharaStatus.Success)
                    {
                        Logger.Info("Programmer upload successful.");
                        return await SendDoneAsync();
                    }
                    else
                    {
                        Logger.Error($"Programmer upload failed with status: {endTransfer.Status}");
                        return false;
                    }
                }
            }

            return false;
        }

        private async Task<bool> SendDoneAsync()
        {
            SaharaPacketHeader done = new SaharaPacketHeader { Command = SaharaCommand.Done, Length = (uint)Marshal.SizeOf<SaharaPacketHeader>() };
            SendPacket(done);

            byte[] buffer = new byte[1024];
            int bytesRead;
            _reader.Read(buffer, 1000, out bytesRead);
            await Task.Yield();
            
            if (bytesRead > 0)
            {
                var header = MemoryMarshal.Cast<byte, SaharaPacketHeader>(buffer)[0];
                return header.Command == SaharaCommand.DoneResponse;
            }
            return false;
        }

        private bool SendPacket<T>(T packet) where T : struct
        {
            int size = Marshal.SizeOf(packet);
            byte[] buffer = new byte[size];
            IntPtr ptr = Marshal.AllocHGlobal(size);
            try
            {
                Marshal.StructureToPtr(packet, ptr, false);
                Marshal.Copy(ptr, buffer, 0, size);
                int bytesWritten;
                _writer.Write(buffer, 1000, out bytesWritten);
                return bytesWritten == size;
            }
            finally
            {
                Marshal.FreeHGlobal(ptr);
            }
        }
    }
}
