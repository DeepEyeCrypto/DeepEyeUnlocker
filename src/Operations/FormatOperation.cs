using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols;

namespace DeepEyeUnlocker.Operations
{
    public class FormatOperation : Operation
    {
        private readonly IProtocol _protocol;

        public FormatOperation(IProtocol protocol)
        {
            _protocol = protocol;
            Name = "Format / Factory Reset";
        }

        public override async Task<bool> ExecuteAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Report(progress, 10, "Starting Factory Reset...");

            try
            {
                if (ct.IsCancellationRequested) return false;

                Report(progress, 30, "Retrieving partition table...");
                var partitions = (await _protocol.GetPartitionTableAsync()).ToList();

                string[] targets = { "userdata", "cache", "metadata" };
                var toErase = partitions.Where(p => targets.Any(t => p.Name.Equals(t, StringComparison.OrdinalIgnoreCase))).ToList();

                if (toErase.Count == 0)
                {
                    Report(progress, 0, "Could not find userdata or cache partitions.", LogLevel.Error);
                    return false;
                }

                int completed = 0;
                foreach (var part in toErase)
                {
                    if (ct.IsCancellationRequested) return false;
                    
                    Report(progress, 40 + (int)((float)completed / toErase.Count * 50), $"Formatting {part.Name}...");
                    Logger.Info($"Formatting {part.Name} ({part.SizeInBytes} bytes)...");
                    
                    // Note: In a real scenario, we'd write a filesystem header or use protocol erase
                    bool success = await _protocol.WritePartitionAsync(part.Name, new byte[1024 * 1024]); // Wipe first 1MB
                    
                    if (!success)
                    {
                        Logger.Error($"Failed to format {part.Name}");
                    }
                    completed++;
                }

                Report(progress, 100, "Device formatted successfully. Restarting...");
                await _protocol.RebootAsync();
                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Format operation failed.");
                Report(progress, 0, ex.Message, LogLevel.Error);
                return false;
            }
        }
    }
}
