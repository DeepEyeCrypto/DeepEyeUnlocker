using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure.Logging;

namespace DeepEyeUnlocker.Operations
{
    public class FlashOperation : Operation
    {
        private readonly string? _firmwarePath;
        private readonly ResourceManager _resourceManager;
        private readonly IProtocol _protocol;

        public FlashOperation(string? firmwarePath, IProtocol protocol)
        {
            _protocol = protocol;
            Name = "Flash Firmware";
            _firmwarePath = firmwarePath;
            _resourceManager = new ResourceManager();
        }

        public override async Task<bool> ExecuteAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            if (_firmwarePath == null)
            {
                Report(progress, 0, "No firmware path provided.", LogLevel.Error);
                return false;
            }

            Report(progress, 5, "Initializing Flash Pipeline...");
            
            try
            {
                Report(progress, 10, "Scanning Firmware Package...");
                var flashMgr = new FlashManager();
                var manifest = await flashMgr.ParseFirmwareAsync(_firmwarePath);

                if (manifest.Partitions.Count == 0)
                {
                    Report(progress, 0, "No flashable partitions found in package.", LogLevel.Error);
                    return false;
                }

                var toFlash = manifest.Partitions.Where(p => p.IsSelected).OrderBy(p => p.Order).ToList();
                int count = 0;

                foreach (var p in toFlash)
                {
                    if (ct.IsCancellationRequested) return false;

                    Report(progress, 10 + (int)((float)count / toFlash.Count * 85), $"Flashing {p.PartitionName}...");
                    Logger.Info($"Flashing {p.PartitionName} from {p.FileName} ({p.Size / 1024 / 1024} MB)...");

                    if (!File.Exists(p.FilePath))
                    {
                        Logger.Error($"Missing file for partition {p.PartitionName}: {p.FilePath}");
                        continue;
                    }

                    using (var fs = new FileStream(p.FilePath, FileMode.Open, FileAccess.Read))
                    {
                        string target = p.PartitionName;
                        if (manifest.Type == FirmwareType.QualcommFirehose && p.StartSector > 0)
                        {
                            int sectorCount = (int)((p.Size + 511) / 512);
                            target = $"RAW:{p.StartSector}:{sectorCount}";
                        }

                        // Use the shared protocol's streaming write
                        bool success = await _protocol.WritePartitionFromStreamAsync(target, fs, progress, ct);
                        
                        if (!success)
                        {
                            Logger.Error($"Failed to flash partition: {p.PartitionName}");
                            Report(progress, 0, $"Failure writing {p.PartitionName}", LogLevel.Error);
                            return false;
                        }
                    }
                    count++;
                }

                Report(progress, 100, "Flash Successful! Device will now reboot.");
                await _protocol.RebootAsync();
                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Flash operation failed");
                Report(progress, 0, ex.Message, LogLevel.Error);
                return false;
            }
        }
    }
}
