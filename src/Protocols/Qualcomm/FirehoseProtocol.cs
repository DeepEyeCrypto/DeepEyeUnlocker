using System;
using System.Text;
using System.Threading.Tasks;
using System.Xml.Linq;
using LibUsbDotNet;
using LibUsbDotNet.Main;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
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

        public async Task<bool> ReadToStreamAsync(Stream output, long sectorOffset, int sectorCount, IProgress<ProgressUpdate>? progress = null, CancellationToken ct = default)
        {
            Logger.Info($"Streaming sectors from EDL: Offset {sectorOffset}, Count {sectorCount}");
            
            string readXml = $"<?xml version=\"1.0\" ?><data><read SECTOR_SIZE_IN_BYTES=\"512\" num_partition_sectors=\"{sectorCount}\" physical_partition_number=\"0\" start_sector=\"{sectorOffset}\" /></data>";
            
            if (await SendCommandOnlyAsync(readXml))
            {
                long totalBytes = (long)sectorCount * 512;
                long totalRead = 0;
                byte[] buffer = new byte[MaxPacketSize];

                while (totalRead < totalBytes)
                {
                    if (ct.IsCancellationRequested) return false;

                    int bytesRead;
                    int toRead = (int)Math.Min(MaxPacketSize, totalBytes - totalRead);
                    
                    _reader.Read(buffer, TimeoutMs, out bytesRead);
                    if (bytesRead == 0) break;

                    await output.WriteAsync(buffer, 0, bytesRead, ct);
                    totalRead += bytesRead;

                    progress?.Report(new ProgressUpdate 
                    { 
                        Percentage = (int)((float)totalRead / totalBytes * 100),
                        Status = $"Streaming: {totalRead / 1024 / 1024}MB / {totalBytes / 1024 / 1024}MB"
                    });
                }
                
                await ReceiveResponseAsync();
                return totalRead == totalBytes;
            }
            return false;
        }

        public async Task<byte[]> ReadPartitionAsync(string partitionName, long sectorOffset = 0, int sectorCount = 1)
        {
            using var ms = new MemoryStream();
            if (await ReadToStreamAsync(ms, sectorOffset, sectorCount))
            {
                return ms.ToArray();
            }
            return Array.Empty<byte>();
        }

        public async Task<bool> WriteFromStreamAsync(Stream input, long sectorOffset, int sectorCount, IProgress<ProgressUpdate>? progress = null, CancellationToken ct = default)
        {
            Logger.Info($"Streaming sectors to EDL: Offset {sectorOffset}, Count {sectorCount}");

            string writeXml = $"<?xml version=\"1.0\" ?><data><program SECTOR_SIZE_IN_BYTES=\"512\" num_partition_sectors=\"{sectorCount}\" physical_partition_number=\"0\" start_sector=\"{sectorOffset}\" /></data>";

            if (await SendCommandOnlyAsync(writeXml))
            {
                long totalBytes = (long)sectorCount * 512;
                long totalWritten = 0;
                byte[] buffer = new byte[MaxPacketSize];

                while (totalWritten < totalBytes)
                {
                    if (ct.IsCancellationRequested) return false;

                    int bytesToRead = (int)Math.Min(MaxPacketSize, totalBytes - totalWritten);
                    int readFromStream = await input.ReadAsync(buffer, 0, bytesToRead, ct);
                    if (readFromStream == 0) break;

                    int written;
                    _writer.Write(buffer, 0, readFromStream, TimeoutMs, out written);
                    if (written == 0) return false;
                    
                    totalWritten += written;
                    int totalSizeMB = (int)(totalBytes / 1024 / 1024);

                    progress?.Report(new ProgressUpdate 
                    { 
                        Percentage = (int)((float)totalWritten / totalBytes * 100),
                        Status = $"Writing: {totalWritten / 1024 / 1024}MB / {totalSizeMB}MB"
                    });
                }

                var response = await ReceiveResponseAsync();
                return response != null && response.Contains("ACK");
            }
            return false;
        }

        public async Task<bool> WritePartitionAsync(string partitionName, byte[] data, long sectorOffset = 0)
        {
            using var ms = new MemoryStream(data);
            int sectorCount = (data.Length + 511) / 512;
            return await WriteFromStreamAsync(ms, sectorOffset, sectorCount);
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
