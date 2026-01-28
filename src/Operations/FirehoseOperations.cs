using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols.Qualcomm;
using LibUsbDotNet;

namespace DeepEyeUnlocker.Operations
{
    /// <summary>
    /// Operation to initialize a Firehose session after EDL entry
    /// </summary>
    public class FirehoseInitOperation : Operation
    {
        private readonly UsbDevice? _usbDevice;
        private readonly string? _programmerPath;
        private readonly DeviceContext _deviceContext;
        private FirehoseManager? _manager;

        public FirehoseManager? Manager => _manager;

        public FirehoseInitOperation(DeviceContext context, UsbDevice? usbDevice = null, string? programmerPath = null)
        {
            _deviceContext = context;
            _usbDevice = usbDevice;
            _programmerPath = programmerPath;
            Name = "Initialize Firehose Session";
        }

        public override async Task<bool> ExecuteAsync(Device device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            if (_usbDevice == null)
            {
                Report(progress, 0, "No USB device connected", LogLevel.Error);
                return false;
            }

            if (_deviceContext.Mode != ConnectionMode.EDL)
            {
                Report(progress, 0, "Device must be in EDL mode to initialize Firehose", LogLevel.Error);
                return false;
            }

            Report(progress, 5, "Creating Firehose session...");
            _manager = new FirehoseManager();

            // Show available programmers
            var programmers = _manager.GetAvailableProgrammers(_deviceContext);
            var count = 0;
            foreach (var p in programmers)
            {
                count++;
                Logger.Debug($"Available programmer: {p.FileName} ({p.FileSize} bytes)", "FIREHOSE");
            }
            Report(progress, 10, $"Found {count} programmer files");

            // Initialize session
            var result = await _manager.InitializeSessionAsync(
                _usbDevice,
                _programmerPath,
                _deviceContext,
                progress,
                ct);

            if (result.Success)
            {
                Report(progress, 100, $"✅ Firehose ready! Programmer: {result.LoadedProgrammer?.FileName}");
                Logger.Success($"Firehose session initialized in {result.Duration.TotalMilliseconds:F0}ms", "FIREHOSE");
                return true;
            }
            else
            {
                Report(progress, 0, result.Message, LogLevel.Error);
                Logger.Error($"Firehose init failed: {result.Message}", "FIREHOSE");
                return false;
            }
        }
    }

    /// <summary>
    /// Operation to read a partition via Firehose
    /// </summary>
    public class FirehoseReadOperation : Operation
    {
        private readonly FirehoseManager _manager;
        private readonly string _partitionName;
        private readonly string _outputPath;

        public FirehoseReadOperation(FirehoseManager manager, string partitionName, string outputPath)
        {
            _manager = manager;
            _partitionName = partitionName;
            _outputPath = outputPath;
            Name = $"Read Partition: {partitionName}";
        }

        public override async Task<bool> ExecuteAsync(Device device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            if (!_manager.IsReady)
            {
                Report(progress, 0, "Firehose session is not ready", LogLevel.Error);
                return false;
            }

            try
            {
                Report(progress, 10, $"Reading partition: {_partitionName}...");
                
                var data = await _manager.ReadPartitionAsync(_partitionName, progress, ct);
                
                if (data.Length == 0)
                {
                    Report(progress, 0, $"No data read from {_partitionName}", LogLevel.Error);
                    return false;
                }

                Report(progress, 80, $"Writing {data.Length} bytes to {Path.GetFileName(_outputPath)}...");
                
                await File.WriteAllBytesAsync(_outputPath, data, ct);
                
                Report(progress, 100, $"✅ Saved {_partitionName} to {Path.GetFileName(_outputPath)}");
                Logger.Success($"Read {data.Length} bytes from {_partitionName}", "FIREHOSE");
                return true;
            }
            catch (Exception ex)
            {
                Report(progress, 0, $"Read failed: {ex.Message}", LogLevel.Error);
                Logger.Error(ex, $"Failed to read partition {_partitionName}");
                return false;
            }
        }
    }

    /// <summary>
    /// Operation to write a partition via Firehose
    /// </summary>
    public class FirehoseWriteOperation : Operation
    {
        private readonly FirehoseManager _manager;
        private readonly string _partitionName;
        private readonly string _inputPath;

        public FirehoseWriteOperation(FirehoseManager manager, string partitionName, string inputPath)
        {
            _manager = manager;
            _partitionName = partitionName;
            _inputPath = inputPath;
            Name = $"Write Partition: {partitionName}";
        }

        public override async Task<bool> ExecuteAsync(Device device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            if (!_manager.IsReady)
            {
                Report(progress, 0, "Firehose session is not ready", LogLevel.Error);
                return false;
            }

            if (!File.Exists(_inputPath))
            {
                Report(progress, 0, $"Input file not found: {_inputPath}", LogLevel.Error);
                return false;
            }

            try
            {
                var data = await File.ReadAllBytesAsync(_inputPath, ct);
                Report(progress, 20, $"Writing {data.Length} bytes to {_partitionName}...");
                
                var success = await _manager.WritePartitionAsync(_partitionName, data, progress, ct);
                
                if (success)
                {
                    Report(progress, 100, $"✅ Successfully wrote {_partitionName}");
                    Logger.Success($"Wrote {data.Length} bytes to {_partitionName}", "FIREHOSE");
                }
                else
                {
                    Report(progress, 0, $"Write failed for {_partitionName}", LogLevel.Error);
                }
                
                return success;
            }
            catch (Exception ex)
            {
                Report(progress, 0, $"Write failed: {ex.Message}", LogLevel.Error);
                Logger.Error(ex, $"Failed to write partition {_partitionName}");
                return false;
            }
        }
    }

    /// <summary>
    /// Operation to erase a partition via Firehose
    /// </summary>
    public class FirehoseEraseOperation : Operation
    {
        private readonly FirehoseManager _manager;
        private readonly string _partitionName;

        public FirehoseEraseOperation(FirehoseManager manager, string partitionName)
        {
            _manager = manager;
            _partitionName = partitionName;
            Name = $"Erase Partition: {partitionName}";
        }

        public override async Task<bool> ExecuteAsync(Device device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            if (!_manager.IsReady)
            {
                Report(progress, 0, "Firehose session is not ready", LogLevel.Error);
                return false;
            }

            try
            {
                Report(progress, 20, $"Erasing partition: {_partitionName}...");
                
                var success = await _manager.ErasePartitionAsync(_partitionName, progress, ct);
                
                if (success)
                {
                    Report(progress, 100, $"✅ Successfully erased {_partitionName}");
                    Logger.Success($"Erased partition {_partitionName}", "FIREHOSE");
                }
                else
                {
                    Report(progress, 0, $"Erase failed for {_partitionName}", LogLevel.Error);
                }
                
                return success;
            }
            catch (Exception ex)
            {
                Report(progress, 0, $"Erase failed: {ex.Message}", LogLevel.Error);
                Logger.Error(ex, $"Failed to erase partition {_partitionName}");
                return false;
            }
        }
    }

    /// <summary>
    /// Batch operation to flash multiple partitions
    /// </summary>
    public class FirehoseBatchFlashOperation : Operation
    {
        private readonly FirehoseManager _manager;
        private readonly (string Partition, string FilePath)[] _files;

        public FirehoseBatchFlashOperation(FirehoseManager manager, params (string Partition, string FilePath)[] files)
        {
            _manager = manager;
            _files = files;
            Name = $"Batch Flash ({files.Length} partitions)";
        }

        public override async Task<bool> ExecuteAsync(Device device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            if (!_manager.IsReady)
            {
                Report(progress, 0, "Firehose session is not ready", LogLevel.Error);
                return false;
            }

            int completed = 0;
            int failed = 0;

            for (int i = 0; i < _files.Length; i++)
            {
                ct.ThrowIfCancellationRequested();
                
                var (partition, filePath) = _files[i];
                var percent = (int)((i / (float)_files.Length) * 100);
                Report(progress, percent, $"Flashing {partition} ({i + 1}/{_files.Length})...");

                if (!File.Exists(filePath))
                {
                    Logger.Warn($"Skipping {partition}: file not found", "FIREHOSE");
                    failed++;
                    continue;
                }

                try
                {
                    var data = await File.ReadAllBytesAsync(filePath, ct);
                    
                    if (await _manager.WritePartitionAsync(partition, data, null, ct))
                    {
                        completed++;
                        Logger.Info($"Flashed {partition} ({data.Length} bytes)", "FIREHOSE");
                    }
                    else
                    {
                        failed++;
                        Logger.Error($"Failed to flash {partition}", "FIREHOSE");
                    }
                }
                catch (Exception ex)
                {
                    failed++;
                    Logger.Error(ex, $"Error flashing {partition}");
                }
            }

            var success = failed == 0;
            var message = success 
                ? $"✅ Successfully flashed all {completed} partitions"
                : $"Completed: {completed}, Failed: {failed}";
            
            Report(progress, 100, message, success ? LogLevel.Info : LogLevel.Warn);
            
            return success;
        }
    }
}
