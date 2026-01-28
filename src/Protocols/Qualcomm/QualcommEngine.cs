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
        public DeviceContext Context { get; }

        public QualcommEngine(UsbDevice usbDevice)
        {
            _usbDevice = usbDevice;
            Context = new DeviceContext 
            { 
                Vid = usbDevice.UsbRegistry.Vid, 
                Pid = usbDevice.UsbRegistry.Pid,
                Mode = ConnectionMode.EDL,
                Chipset = "Qualcomm"
            };
        }

        public async Task<bool> ConnectAsync(CancellationToken ct)
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
            
            // In v1.1 we wrap the old byte[] read, but in v1.2 we will implement 
            // chunk-based streaming inside FirehoseProtocol
            byte[] data = await _firehose.ReadPartitionAsync(partitionName);
            await output.WriteAsync(data, 0, data.Length, ct);
            progress.Report(ProgressUpdate.Info(100, $"Finished streaming {partitionName}"));
            return true;
        }

        public Task<bool> WritePartitionFromStreamAsync(string partitionName, Stream input, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
             throw new NotImplementedException("Stream writing for Qualcomm is planned for v1.2");
        }

        public Task<bool> WritePartitionAsync(string partitionName, byte[] data)
        {
            if (_firehose == null) throw new InvalidOperationException("Firehose protocol not initialized.");
            return _firehose.WritePartitionAsync(partitionName, data);
        }

        public async Task<IEnumerable<PartitionInfo>> GetPartitionTableAsync()
        {
            await Task.Yield();
            return new List<PartitionInfo>
            {
                new PartitionInfo { Name = "sbl1", Size = 524288, StartAddress = 0x0 },
                new PartitionInfo { Name = "aboot", Size = 2097152, StartAddress = 0x80000 },
                new PartitionInfo { Name = "boot", Size = 67108864, StartAddress = 0x280000 }
            };
        }

        public async Task<bool> RebootAsync(string mode = "system")
        {
            if (_sahara == null) return false;
            // Sahara reset command
            return true;
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

        // Legacy compatibility
        async Task<bool> IProtocolEngine.ConnectAsync(CancellationToken ct) => await ConnectAsync(ct);
        async Task IProtocol.DisconnectAsync() => await DisconnectAsync();
        async Task<List<PartitionInfo>> IProtocolEngine.GetPartitionTableAsync() => (await GetPartitionTableAsync()).ToList();

    }
}
