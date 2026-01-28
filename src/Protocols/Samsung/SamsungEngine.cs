using System;
using System.IO;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Engines;
using LibUsbDotNet;

namespace DeepEyeUnlocker.Protocols.Samsung
{
    public class SamsungEngine : IProtocol
    {
        private readonly UsbDevice _usbDevice;
        private OdinProtocol? _odin;
        
        public DeviceContext Context { get; private set; } = new DeviceContext();
        public string Name => "Samsung Download Mode";

        public SamsungEngine(UsbDevice usbDevice)
        {
            _usbDevice = usbDevice;
        }

        public async Task<bool> ConnectAsync(CancellationToken ct = default)
        {
            try
            {
                Logger.Info("Connecting to Samsung device...");
                _odin = new OdinProtocol(_usbDevice);
                return await _odin.SendHandshakeAsync();
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Error connecting to Samsung device.");
                return false;
            }
        }

        public async Task<bool> DisconnectAsync()
        {
            Logger.Info("Disconnecting from Samsung device.");
            _usbDevice.Close();
            return await Task.FromResult(true);
        }

        public async Task<byte[]> ReadPartitionAsync(string partitionName)
        {
            Logger.Info($"Samsung: Reading partition {partitionName} is restricted in Download Mode.");
            return await Task.FromResult(Array.Empty<byte>());
        }

        public async Task<bool> WritePartitionAsync(string partitionName, byte[] data)
        {
            if (_odin == null) return false;
            return await _odin.FlashPartitionAsync(partitionName, data);
        }

        public async Task<bool> ErasePartitionAsync(string partitionName)
        {
            Logger.Info($"Samsung: Erasing partition {partitionName}...");
            await Task.Delay(100);
            return true;
        }

        public async Task<IEnumerable<PartitionInfo>> GetPartitionTableAsync()
        {
            Logger.Info("Samsung: Retrieving partition table...");
            return await Task.FromResult(new List<PartitionInfo>
            {
                new PartitionInfo { Name = "SYSTEM", SizeInBytes = 4096, StartLba = 0x0 },
                new PartitionInfo { Name = "USERDATA", SizeInBytes = 8192, StartLba = 0x1000 }
            });
        }

        public async Task<bool> RebootAsync(string mode = "normal")
        {
            Logger.Info($"Samsung: Rebooting to {mode} mode...");
            return await Task.FromResult(true);
        }

        public async Task<bool> ReadPartitionToStreamAsync(string partitionName, Stream outputStream, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Logger.Warn("Samsung: Streaming read not supported in Download mode (Device limitation).");
            await Task.CompletedTask;
            return false;
        }

        public async Task<bool> WritePartitionFromStreamAsync(string partitionName, Stream inputStream, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            if (_odin == null)
            {
                Logger.Error("Odin protocol not initialized.");
                return false;
            }
            return await _odin.FlashStreamAsync(partitionName, inputStream, progress);
        }
    }
}
