using System;
using System.Text;
using System.Threading.Tasks;
using System.Xml.Linq;
using LibUsbDotNet;
using LibUsbDotNet.Main;
using DeepEyeUnlocker.Core;
namespace DeepEyeUnlocker.Protocols.Qualcomm
{
    public class FirehoseProtocol
    {
        private readonly UsbDevice _usbDevice;
        private readonly UsbEndpointReader _reader;
        private readonly UsbEndpointWriter _writer;

        private const int TimeoutMs = 5000;
        private const int MaxPacketSize = 4096;

        public FirehoseProtocol(UsbDevice usbDevice)
        {
            _usbDevice = usbDevice;
            _reader = _usbDevice.OpenEndpointReader(ReadEndpointID.Ep01);
            _writer = _usbDevice.OpenEndpointWriter(WriteEndpointID.Ep01);
        }

        public async Task<bool> ConfigureAsync()
        {
            // Standard Firehose configuration command
            string configXml = "<?xml version=\"1.0\" ?><data><configure MemoryName=\"emmc\" Verbose=\"0\" AlwaysValidate=\"0\" MaxPayloadSizeToTargetInBytes=\"1048576\" /></data>";
            var response = await SendCommandAsync(configXml);
            return response != null && response.Contains("ACK");
        }

        public async Task<byte[]> ReadPartitionAsync(string partitionName, long sectorOffset = 0, int sectorCount = 1)
        {
            Logger.Info($"Reading partition: {partitionName} at offset {sectorOffset}, count {sectorCount}");
            
            string readXml = $"<?xml version=\"1.0\" ?><data><read SECTOR_SIZE_IN_BYTES=\"512\" num_partition_sectors=\"{sectorCount}\" physical_partition_number=\"0\" start_sector=\"{sectorOffset}\" /></data>";
            
            if (await SendCommandOnlyAsync(readXml))
            {
                byte[] data = new byte[sectorCount * 512];
                int totalRead = 0;
                while (totalRead < data.Length)
                {
                    int bytesRead;
                    byte[] buffer = new byte[Math.Min(MaxPacketSize, data.Length - totalRead)];
                    _reader.Read(buffer, TimeoutMs, out bytesRead);
                    if (bytesRead == 0) break;
                    Array.Copy(buffer, 0, data, totalRead, bytesRead);
                    totalRead += bytesRead;
                }
                
                // Read response footer
                await ReceiveResponseAsync();
                return data;
            }
            return Array.Empty<byte>();
        }

        public async Task<bool> WritePartitionAsync(string partitionName, byte[] data, long sectorOffset = 0)
        {
            int sectorCount = (data.Length + 511) / 512;
            Logger.Info($"Writing to partition: {partitionName} at offset {sectorOffset} ({data.Length} bytes)");

            string writeXml = $"<?xml version=\"1.0\" ?><data><program SECTOR_SIZE_IN_BYTES=\"512\" num_partition_sectors=\"{sectorCount}\" physical_partition_number=\"0\" start_sector=\"{sectorOffset}\" /></data>";

            if (await SendCommandOnlyAsync(writeXml))
            {
                int totalWritten = 0;
                while (totalWritten < data.Length)
                {
                    int toWrite = Math.Min(MaxPacketSize, data.Length - totalWritten);
                    byte[] chunk = new byte[toWrite];
                    Array.Copy(data, totalWritten, chunk, 0, toWrite);
                    
                    int written;
                    _writer.Write(chunk, TimeoutMs, out written);
                    if (written == 0) return false;
                    totalWritten += written;
                }

                var response = await ReceiveResponseAsync();
                return response != null && response.Contains("ACK");
            }
            return false;
        }

        public async Task<bool> ErasePartitionAsync(string partitionName, long sectorOffset = 0, int sectorCount = 1)
        {
            Logger.Info($"Erasing partition: {partitionName}");
            string eraseXml = $"<?xml version=\"1.0\" ?><data><erase SECTOR_SIZE_IN_BYTES=\"512\" num_partition_sectors=\"{sectorCount}\" physical_partition_number=\"0\" start_sector=\"{sectorOffset}\" /></data>";
            var response = await SendCommandAsync(eraseXml);
            return response != null && response.Contains("ACK");
        }

        public async Task<bool> SendEraseCommandAsync(string partitionName) => await ErasePartitionAsync(partitionName);

        private async Task<string?> SendCommandAsync(string xml)
        {
            if (await SendCommandOnlyAsync(xml))
            {
                return await ReceiveResponseAsync();
            }
            return null;
        }

        private async Task<bool> SendCommandOnlyAsync(string xml)
        {
            byte[] data = Encoding.UTF8.GetBytes(xml);
            int written;
            _writer.Write(data, TimeoutMs, out written);
            await Task.Yield();
            return written == data.Length;
        }

        private async Task<string?> ReceiveResponseAsync()
        {
            byte[] buffer = new byte[MaxPacketSize];
            int bytesRead;
            _reader.Read(buffer, TimeoutMs, out bytesRead);
            await Task.Yield();
            if (bytesRead > 0)
            {
                string response = Encoding.UTF8.GetString(buffer, 0, bytesRead);
                Logger.Debug($"Firehose Response: {response}");
                return response;
            }
            return null;
        }
    }
}
