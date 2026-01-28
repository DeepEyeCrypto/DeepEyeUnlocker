using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
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

        public QualcommEngine(UsbDevice usbDevice)
        {
            _usbDevice = usbDevice;
        }

        public async Task<bool> ConnectAsync()
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

                // In a real scenario, we'd select the correct programmer based on device ID
                // For now, we assume Path to programmer is provided or handled by the engine
                Logger.Warn("Waiting for programmer upload command...");
                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Error connecting to Qualcomm device.");
                return false;
            }
        }

        public async Task DisconnectAsync()
        {
            Logger.Info("Disconnecting from Qualcomm device.");
            _usbDevice.Close();
            await Task.CompletedTask;
        }

        public Task<byte[]> ReadPartitionAsync(string partitionName)
        {
            if (_firehose == null) throw new InvalidOperationException("Firehose protocol not initialized. Upload programmer first.");
            return _firehose.ReadPartitionAsync(partitionName);
        }

        public Task<bool> WritePartitionAsync(string partitionName, byte[] data)
        {
            if (_firehose == null) throw new InvalidOperationException("Firehose protocol not initialized. Upload programmer first.");
            return _firehose.WritePartitionAsync(partitionName, data);
        }

        public async Task<bool> ErasePartitionAsync(string partitionName)
        {
            if (_firehose == null) return false;
            return await _firehose.SendEraseCommandAsync(partitionName);
        }

        public async Task<System.Collections.Generic.List<Core.PartitionInfo>> GetPartitionTableAsync()
        {
            // In real world, parse GPT or Firehose response
            return new System.Collections.Generic.List<Core.PartitionInfo>
            {
                new Core.PartitionInfo { Name = "sbl1", Size = 524288, StartAddress = 0x0 },
                new Core.PartitionInfo { Name = "aboot", Size = 2097152, StartAddress = 0x80000 },
                new Core.PartitionInfo { Name = "boot", Size = 67108864, StartAddress = 0x280000 }
            };
        }

        public async Task<bool> InitializeFirehoseAsync(string programmerPath)
        {
            if (_sahara == null) return false;

            if (await _sahara.UploadProgrammerAsync(programmerPath))
            {
                Logger.Info("Programmer running. Switching to Firehose protocol.");
                _firehose = new FirehoseProtocol(_usbDevice);
                return true;
            }
            return false;
        }
    }
}
