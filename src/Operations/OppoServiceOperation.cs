using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Operations
{
    public class OppoServiceOperation : Operation
    {
        private readonly IProtocol? _protocol;

        public OppoServiceOperation(IProtocol? protocol = null)
        {
            _protocol = protocol;
            Name = "Oppo/Realme Advanced FRP";
        }

        public override async Task<bool> ExecuteAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            if (device.Brand != "Oppo" && device.Brand != "Realme" && device.Brand != "OnePlus")
            {
                Logger.Error("Operation only supported for Oppo/Realme/OnePlus devices.");
                Report(progress, 0, "Unsupported device brand", LogLevel.Error);
                return false;
            }

            if (_protocol == null)
            {
                 Report(progress, 0, "Protocol not initialized.", LogLevel.Error);
                 return false;
            }

            Report(progress, 10, "Initializing Oppo FRP bypass module...");
            
            try
            {
                if (ct.IsCancellationRequested) return false;

                Report(progress, 30, "Scanning partitions (frp, opporeserve2, etc.)...");
                var partitions = (await _protocol.GetPartitionTableAsync()).ToList();

                string[] targets = { "frp", "opporeserve2", "oplusreserve2" };
                var found = partitions.Where(p => targets.Any(t => p.Name.Equals(t, StringComparison.OrdinalIgnoreCase))).ToList();

                if(found.Count == 0)
                {
                    Report(progress, 0, "No target partitions found.", LogLevel.Error);
                    return false;
                }

                foreach(var part in found)
                {
                    Report(progress, 50, $"Erasing {part.Name}...");
                     // Real erase
                    await _protocol.WritePartitionAsync(part.Name, new byte[512]); // Quick Wipe
                    await Task.Delay(200, ct);
                }

                Report(progress, 100, "Oppo/Realme FRP Bypass Successful.");
                await _protocol.RebootAsync();
                return true;
            }
            catch (OperationCanceledException)
            {
                return false;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Oppo service operation failed.");
                Report(progress, 0, ex.Message, LogLevel.Error);
                return false;
            }
        }
    }
}
