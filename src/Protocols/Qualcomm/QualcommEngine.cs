using System;
using System.IO;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Engines;
using LibUsbDotNet;
using LibUsbDotNet.Main;

namespace DeepEyeUnlocker.Protocols.Qualcomm
{
    public class QualcommEngine : IProtocol
    {
        private readonly UsbDevice _usbDevice;
        private SaharaProtocol? _sahara;
        private FirehoseProtocol? _firehose;

        public string Name => "Qualcomm EDL";
        public DeviceContext Context { get; }

        public QualcommEngine(UsbDevice usbDevice)
        {
            _usbDevice = usbDevice;
            // Extract VID/PID from device - LibUsbDotNet 2.x compatible
            int vid = 0, pid = 0;
            if (usbDevice.UsbRegistryInfo != null)
            {
                vid = usbDevice.UsbRegistryInfo.Vid;
                pid = usbDevice.UsbRegistryInfo.Pid;
            }
            Context = new DeviceContext 
            { 
                Vid = vid, 
                Pid = pid,
                Mode = ConnectionMode.EDL,
                Chipset = "Qualcomm"
            };
        }

        public async Task<bool> ConnectAsync(CancellationToken ct = default)
        {
            try
            {
                Logger.Info("Connecting to Qualcomm device in EDL mode...");
                _sahara = new SaharaProtocol(_usbDevice);
                
                if (!await _sahara.ProcessHelloAsync())
                {
                    Logger.Error("Failed to complete Sahara Hello handshake.");
                    return false;
                }

                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Error connecting to Qualcomm device.");
                return false;
            }
        }

        public async Task<bool> DisconnectAsync()
        {
            Logger.Info("Disconnecting from Qualcomm device.");
            _usbDevice.Close();
            return await Task.FromResult(true);
        }

        public Task<byte[]> ReadPartitionAsync(string partitionName)
        {
            if (_firehose == null) throw new InvalidOperationException("Firehose protocol not initialized.");
            return _firehose.ReadPartitionAsync(partitionName);
        }

        public async Task<bool> ReadPartitionToStreamAsync(string partitionName, Stream output, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            if (_firehose == null) throw new InvalidOperationException("Firehose protocol not initialized.");
            
            var partitions = await GetPartitionTableAsync();
            var part = partitions.FirstOrDefault(p => p.Name.Equals(partitionName, StringComparison.OrdinalIgnoreCase));
            if (part == null) throw new Exception($"Partition {partitionName} not found.");

            int sectorCount = (int)((part.SizeInBytes + 511) / 512);
            return await _firehose.ReadToStreamAsync(output, (long)part.StartLba, sectorCount, progress, ct);
        }

        public async Task<bool> WritePartitionFromStreamAsync(string partitionName, Stream input, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            if (_firehose == null) throw new InvalidOperationException("Firehose protocol not initialized.");

            long startSector = -1;
            int sectorCount = -1;

            if (partitionName.StartsWith("RAW:", StringComparison.OrdinalIgnoreCase))
            {
                var parts = partitionName.Split(':');
                if (parts.Length >= 3)
                {
                    startSector = long.Parse(parts[1]);
                    sectorCount = int.Parse(parts[2]);
                }
            }

            if (startSector == -1)
            {
                var partitions = (await GetPartitionTableAsync()).ToList();
                var part = partitions.FirstOrDefault(p => p.Name.Equals(partitionName, StringComparison.OrdinalIgnoreCase));
                if (part == null) throw new Exception($"Partition {partitionName} not found.");

                startSector = (long)part.StartLba;
                sectorCount = (int)((part.SizeInBytes + 511) / 512);
            }

            return await _firehose.WriteFromStreamAsync(input, startSector, sectorCount, progress, ct);
        }

        public async Task<IEnumerable<PartitionInfo>> GetPartitionTableAsync()
        {
            if (_firehose == null) throw new InvalidOperationException("Firehose protocol not initialized.");

            // Read first 34 sectors (MBR + GPT Header + GPT Entries)
            byte[] data = await _firehose.ReadPartitionAsync("GPT", 0, 34);
            if (data == null || data.Length == 0) return Enumerable.Empty<PartitionInfo>();

            var parser = new PartitionTableParser();
            var table = parser.Parse(data);
            
            if (table.IsValid)
            {
                return table.Partitions;
            }

            Logger.Error($"Failed to parse partition table: {table.ParseError}");
            return Enumerable.Empty<PartitionInfo>();
        }

        public async Task<bool> RebootAsync(string mode = "system")
        {
            if (_sahara == null) return false;
            return await Task.FromResult(true);
        }

        public async Task<bool> InitializeFirehoseAsync(string programmerPath)
        {
            if (_sahara == null) return false;
            if (await _sahara.UploadProgrammerAsync(programmerPath))
            {
                _firehose = new FirehoseProtocol(_usbDevice);
                return true;
            }
            return false;
        }

        public async Task<bool> WritePartitionAsync(string partitionName, byte[] data)
        {
            using (var stream = new MemoryStream(data))
            {
                return await WritePartitionFromStreamAsync(partitionName, stream, null!, CancellationToken.None);
            }
        }

        public async Task<bool> ErasePartitionAsync(string partitionName, IProgress<ProgressUpdate>? progress, CancellationToken ct)
        {
            if (_firehose == null) throw new InvalidOperationException("Firehose protocol not initialized.");

            var partitions = await GetPartitionTableAsync();
            var part = partitions.FirstOrDefault(p => p.Name.Equals(partitionName, StringComparison.OrdinalIgnoreCase));
            if (part == null) throw new Exception($"Partition {partitionName} not found.");

            long startSector = (long)part.StartLba;
            int sectorCount = (int)((part.SizeInBytes + 511) / 512);

            return await _firehose.ErasePartitionAsync(partitionName, startSector, sectorCount);
        }
    }
}
