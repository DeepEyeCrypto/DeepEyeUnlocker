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

namespace DeepEyeUnlocker.Protocols.MTK
{
    public class MTKEngine : IProtocol
    {
        private readonly UsbDevice _usbDevice;
        private MTKDAProtocol? _daProtocol;

        public string Name => "MediaTek Preloader";
        public DeviceContext Context { get; }

        public MTKEngine(UsbDevice usbDevice)
        {
            _usbDevice = usbDevice;
            Context = new DeviceContext
            {
                Vid = usbDevice.UsbRegistry.Vid,
                Pid = usbDevice.UsbRegistry.Pid,
                Mode = ConnectionMode.BROM,
                Chipset = "MediaTek"
            };
        }

        public async Task<bool> ConnectAsync(CancellationToken ct = default)
        {
            try
            {
                Logger.Info("Connecting to MediaTek device...");
                var preloader = new MTKPreloader(_usbDevice);
                
                if (await preloader.HandshakeAsync())
                {
                    uint hwCode = await preloader.GetHardwareCodeAsync();
                    Logger.Info($"Found MTK Hardware Code: 0x{hwCode:X4}");
                    Context.SoC = $"MT{hwCode:X4}";
                    
                    _daProtocol = new MTKDAProtocol(_usbDevice);
                    return true;
                }
                return false;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Error connecting to MediaTek device.");
                return false;
            }
        }

        public async Task<bool> DisconnectAsync()
        {
            Logger.Info("Disconnecting from MediaTek device.");
            _usbDevice.Close();
            return await Task.FromResult(true);
        }

        public async Task<byte[]> ReadPartitionAsync(string partitionName)
        {
            await Task.Delay(100);
            return Array.Empty<byte>();
        }

        public async Task<bool> ReadPartitionToStreamAsync(string partitionName, Stream output, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            if (_daProtocol == null) return false;

            var partitions = await GetPartitionTableAsync();
            var part = partitions.FirstOrDefault(p => p.Name.Equals(partitionName, StringComparison.OrdinalIgnoreCase));
            if (part == null) return false;

            Logger.Info($"MTK: Streaming read {partitionName}...");
            
            long startAddress = (long)part.StartLba * 512;
            long remaining = part.SizeInBytes;
            long totalRead = 0;
            int chunkSize = 1024 * 64; // 64KB

            while (remaining > 0)
            {
                if (ct.IsCancellationRequested) return false;

                int toRead = (int)Math.Min(chunkSize, remaining);
                byte[] data = await _daProtocol.ReadDataAsync((uint)(startAddress + totalRead), toRead);
                
                await output.WriteAsync(data, 0, data.Length, ct);
                
                totalRead += data.Length;
                remaining -= data.Length;

                int percent = (int)((totalRead * 100) / part.SizeInBytes);
                progress?.Report(ProgressUpdate.Info(percent, $"Reading {partitionName}..."));
            }

            return true;
        }

        public async Task<bool> WritePartitionFromStreamAsync(string partitionName, Stream input, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            if (_daProtocol == null)
            {
                 Logger.Error("MTK DA not initialized.");
                 return false;
            }

            var partitions = await GetPartitionTableAsync();
            var part = partitions.FirstOrDefault(p => p.Name.Equals(partitionName, StringComparison.OrdinalIgnoreCase));
            
            // Allow flashing unknown partitions if we just assume they exist or if we want to be strict?
            // For now be strict but maybe log warning.
            long startAddress = 0; 
            if (part != null)
            {
                startAddress = (long)part.StartLba * 512;
            }
            else
            {
                Logger.Warn($"MTK: Partition {partitionName} not found in table. Assuming 0x0 or failing...");
                // Ideally we might want lookup by address if provided (like RAW:address), currently assuming named partitions.
                return false;
            }

            Logger.Info($"MTK: Flashing {partitionName} starting at 0x{startAddress:X}...");

            byte[] buffer = new byte[1024 * 64]; 
            int bytesRead;
            long totalBytes = 0;
            long totalSize = input.Length;

            while ((bytesRead = await input.ReadAsync(buffer, 0, buffer.Length, ct)) > 0)
            {
                if (ct.IsCancellationRequested) return false;

                bool success = await _daProtocol.WriteDataAsync((uint)(startAddress + totalBytes), buffer, bytesRead);
                if (!success) return false;

                totalBytes += bytesRead;
                
                if (totalSize > 0)
                {
                    int percent = (int)((totalBytes * 100) / totalSize);
                    progress?.Report(ProgressUpdate.Info(percent, $"Flashing {partitionName}..."));
                }
            }
            
            return true;
        }

        public async Task<bool> WritePartitionAsync(string partitionName, byte[] data)
        {
            await Task.Delay(100);
            return true;
        }

        public async Task<IEnumerable<PartitionInfo>> GetPartitionTableAsync()
        {
            return await Task.FromResult(new List<PartitionInfo>
            {
                new PartitionInfo { Name = "boot", SizeInBytes = 67108864, StartLba = 0x0 },
                new PartitionInfo { Name = "recovery", SizeInBytes = 67108864, StartLba = 0x4000 }
            });
        }

        public async Task<bool> RebootAsync(string mode = "system")
        {
            return await Task.FromResult(true);
        }
    }
}
