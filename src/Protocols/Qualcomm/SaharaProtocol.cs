using System;
using System.IO;
using System.Runtime.InteropServices;
using System.Threading.Tasks;
using LibUsbDotNet;
using LibUsbDotNet.Main;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Diagnostics;
using DeepEyeUnlocker.Protocols.Usb;

namespace DeepEyeUnlocker.Protocols.Qualcomm
{
    public class SaharaProtocol
    {
        private readonly DeepEyeUnlocker.Protocols.Usb.IUsbDevice _usbDevice;
        private readonly IUsbEndpointReader _reader;
        private readonly IUsbEndpointWriter _writer;

        public SaharaHelloPacket SelectedHello { get; private set; }

        public SaharaProtocol(DeepEyeUnlocker.Protocols.Usb.IUsbDevice usbDevice)
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
            if (bytesRead < Marshal.SizeOf<SaharaPacketHeader>()) 
            {
                ProtocolCoverage.Hit("Sahara_ProcessHello_HeaderTooSmall");
                Logger.Error($"Received insufficient data ({bytesRead} bytes) for Sahara header.");
                return false;
            }

            var header = MemoryMarshal.Cast<byte, SaharaPacketHeader>(buffer)[0];
            if (header.Command != SaharaCommand.Hello)
            {
                ProtocolCoverage.Hit("Sahara_ProcessHello_WrongCommand");
                Logger.Error($"Expected Hello packet, got {header.Command}");
                return false;
            }

            if (bytesRead < Marshal.SizeOf<SaharaHelloPacket>())
            {
                ProtocolCoverage.Hit("Sahara_ProcessHello_BodyTooSmall");
                Logger.Error($"Received insufficient data for Sahara Hello body.");
                return false;
            }

            SelectedHello = MemoryMarshal.Cast<byte, SaharaHelloPacket>(buffer)[0];
            ProtocolCoverage.Hit("Sahara_ProcessHello_Success");
            Logger.Info($"Received Sahara Hello. Version: {SelectedHello.Version}, Mode: {SelectedHello.Mode}");

            // Send Hello Response
            SaharaHelloPacket response = new SaharaHelloPacket
            {
                Header = new SaharaPacketHeader { Command = SaharaCommand.HelloResponse, Length = (uint)Marshal.SizeOf<SaharaHelloPacket>() },
                Version = 2,
                MinVersion = 1,
                Mode = SelectedHello.Mode,
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
                    ProtocolCoverage.Hit("Sahara_Upload_ReadDataReq");
                    if (bytesRead < Marshal.SizeOf<SaharaReadDataPacket>()) continue;
                    var readReq = MemoryMarshal.Cast<byte, SaharaReadDataPacket>(buffer)[0];
                    
                    if (readReq.DataOffset + readReq.DataLength > programmerData.Length)
                    {
                        ProtocolCoverage.Hit("Sahara_Upload_OutOfBounds");
                        Logger.Error($"Device requested out-of-bounds data. Offset: {readReq.DataOffset}, Length: {readReq.DataLength}, Total: {programmerData.Length}");
                        return false;
                    }

                    byte[] chunk = new byte[readReq.DataLength];
                    Array.Copy(programmerData, (int)readReq.DataOffset, chunk, 0, (int)readReq.DataLength);
                    
                    int written;
                    _writer.Write(chunk, 1000, out written);
                }
                else if (header.Command == SaharaCommand.EndTransfer)
                {
                    ProtocolCoverage.Hit("Sahara_Upload_EndTransferReq");
                    var endTransfer = MemoryMarshal.Cast<byte, SaharaEndTransferPacket>(buffer)[0];
                    if (endTransfer.Status == SaharaStatus.Success)
                    {
                        ProtocolCoverage.Hit("Sahara_Upload_StatusSuccess");
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
                if (header.Command == SaharaCommand.DoneResponse) ProtocolCoverage.Hit("Sahara_SendDone_Success");
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
