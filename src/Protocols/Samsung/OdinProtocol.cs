using System;
using System.IO;
using System.Text;
using System.Threading.Tasks;
using LibUsbDotNet;
using LibUsbDotNet.Main;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Diagnostics;
namespace DeepEyeUnlocker.Protocols.Samsung
{
    public class OdinProtocol
    {
        private readonly UsbDevice _usbDevice;
        private readonly UsbEndpointReader _reader;
        private readonly UsbEndpointWriter _writer;

        private const int TimeoutMs = 5000;
        private const int ChunkSize = 131072; // 128 KB for Odin

        public OdinProtocol(UsbDevice usbDevice)
        {
            _usbDevice = usbDevice;
            _reader = _usbDevice.OpenEndpointReader(ReadEndpointID.Ep01);
            _writer = _usbDevice.OpenEndpointWriter(WriteEndpointID.Ep01);
        }

        public async Task<bool> SendHandshakeAsync()
        {
            Logger.Info("Sending Samsung Handshake (ODIN)...");
            byte[] handshake = Encoding.ASCII.GetBytes("ODIN");
            _writer.Write(handshake, TimeoutMs, out _);
            await Task.Yield();
            
            byte[] response = new byte[4];
            _reader.Read(response, TimeoutMs, out int read);
            
            if (read == 4 && Encoding.ASCII.GetString(response) == "LOKE")
            {
                ProtocolCoverage.Hit("Samsung_Handshake_Success");
                Logger.Info("Samsung Handshake successful (LOKE response).");
                return true;
            }
            return false;
        }

        public async Task<byte[]> DownloadPitAsync()
        {
            Logger.Info("Downloading PIT from device...");
            
            // Magic command to dump PIT: 0x00 0x00 0x00 0x01
            byte[] pitCmd = new byte[] { 0x00, 0x00, 0x00, 0x01 };
            _writer.Write(pitCmd, TimeoutMs, out _);

            byte[] buffer = new byte[4096];
            _reader.Read(buffer, TimeoutMs, out int read);

            if (read > 0)
            {
                byte[] pitData = new byte[read];
                Array.Copy(buffer, pitData, read);
                ProtocolCoverage.Hit("Samsung_Pit_Read_Success");
                return pitData;
            }

            Logger.Error("Failed to download PIT.");
            return Array.Empty<byte>();
        }

        public async Task<bool> FlashPartitionAsync(string partitionName, byte[] data)
        {
            using var ms = new MemoryStream(data);
            return await FlashStreamAsync(partitionName, ms, null);
        }

        public async Task<bool> FlashStreamAsync(string partitionName, Stream input, IProgress<ProgressUpdate>? progress)
        {
            Logger.Info($"Odin: Starting stream flash for {partitionName}...");
            
            // 1. Send Part ID
            byte[] partCmd = Encoding.ASCII.GetBytes(partitionName.PadRight(32, '\0'));
            _writer.Write(partCmd, TimeoutMs, out _);
            
            byte[] ack = new byte[4];
            _reader.Read(ack, TimeoutMs, out int ackLen);
            if (ackLen < 4 || ack[0] != 0x00)
            {
                Logger.Error($"Device rejected partition name: {partitionName}");
                return false;
            }

            // 2. Stream Data
            byte[] chunk = new byte[ChunkSize];
            int bytesRead;
            long totalWritten = 0;
            long totalSize = input.Length;

            while ((bytesRead = await input.ReadAsync(chunk, 0, chunk.Length)) > 0)
            {
                // Send Chunk Header (Size)
                byte[] header = BitConverter.GetBytes(bytesRead);
                _writer.Write(header, TimeoutMs, out _);

                // Send Payload
                _writer.Write(chunk, 0, bytesRead, TimeoutMs, out int written);
                if (written == 0) return false;

                totalWritten += written;
                
                if (totalSize > 0)
                {
                    int percent = (int)((totalWritten * 100) / totalSize);
                    progress?.Report(ProgressUpdate.Info(percent, $"Flashing {partitionName}: {totalWritten/1024/1024}MB / {totalSize/1024/1024}MB"));
                }
            }

            // 3. Send EOF
            byte[] eof = new byte[] { 0x00, 0x00, 0x00, 0x00 };
            _writer.Write(eof, TimeoutMs, out _);

            _reader.Read(ack, TimeoutMs, out ackLen);
            bool success = (ackLen == 4 && ack[0] == 0x00);
            
            if (success) ProtocolCoverage.Hit("Samsung_Flash_Success");
            
            return success;
        }

        public async Task<bool> SendResetFrpCommandAsync()
        {
            Logger.Info("Samsung [2026]: Patching persistent partition for FRP reset...");
            byte[] cmd = Encoding.ASCII.GetBytes("FRP_RESET_BIT_ON");
            _writer.Write(cmd, TimeoutMs, out int written);
            await Task.Yield();
            
            byte[] response = new byte[4];
            _reader.Read(response, TimeoutMs, out int read);
            
            return written > 0 && read > 0;
        }
    }
}
