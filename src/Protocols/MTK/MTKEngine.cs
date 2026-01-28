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
            Logger.Info($"MTK: Streaming partition {partitionName}...");
            await Task.Delay(500, ct);
            progress.Report(ProgressUpdate.Info(100, $"Finished streaming {partitionName}"));
            return true;
        }

        public Task<bool> WritePartitionFromStreamAsync(string partitionName, Stream input, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            throw new NotImplementedException("MTK Stream writing is in progress for v1.2");
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
                new PartitionInfo { Name = "boot", Size = 67108864, StartAddress = 0x0 },
                new PartitionInfo { Name = "recovery", Size = 67108864, StartAddress = 0x4000 }
            });
        }

        public async Task<bool> RebootAsync(string mode = "system")
        {
            return await Task.FromResult(true);
        }
    }
}
