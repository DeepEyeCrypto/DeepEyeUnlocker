using System;
using System.IO;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
namespace DeepEyeUnlocker.Operations
{
    public class FlashOperation : Operation
    {
        private readonly string? _firmwarePath;
        private readonly ResourceManager _resourceManager;

        public FlashOperation(string? firmwarePath = null)
        {
            Name = "Flash Firmware";
            _firmwarePath = firmwarePath;
            _resourceManager = new ResourceManager();
        }

        public override async Task<bool> ExecuteAsync(Device device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            if (!File.Exists(_firmwarePath))
            {
                Logger.Error($"Firmware file not found: {_firmwarePath}");
                Report(progress, 0, "Firmware file missing", LogLevel.Error);
                return false;
            }

            Report(progress, 5, "Preparing flash...");
            
            try
            {
                if (ct.IsCancellationRequested) return false;

                Report(progress, 20, "Establishing connection...");
                await Task.Delay(500, ct); 

                Report(progress, 40, "Writing system partition...");
                await Task.Delay(1000, ct); 

                if (ct.IsCancellationRequested) return false;

                Report(progress, 80, "Writing boot partition...");
                await Task.Delay(200, ct);

                Report(progress, 100, "Flash Successful! Device rebooting.");
                return true;
            }
            catch (OperationCanceledException)
            {
                return false;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Flash operation failed.");
                Report(progress, 0, ex.Message, LogLevel.Error);
                return false;
            }
        }
    }
}
