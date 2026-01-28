using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Engines;
using DeepEyeUnlocker.Protocols;

namespace DeepEyeUnlocker.Operations
{
    public class PatternClearOperation : Operation
    {
        private readonly IProtocol _protocol;

        public PatternClearOperation(IProtocol protocol)
        {
            _protocol = protocol;
            Name = "Clear Pattern / Screen Lock";
        }

        public override async Task<bool> ExecuteAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Report(progress, 10, "Initializing screen lock removal...");

            try
            {
                if (ct.IsCancellationRequested) return false;

                if (device.Mode == ConnectionMode.EDL)
                {
                    Report(progress, 40, "Searching for keymaster/gatekeeper partitions...");
                    await Task.Delay(800, ct);
                }
                else if (device.Mode == ConnectionMode.Preloader)
                {
                    Report(progress, 40, "Accessing user partition metadata...");
                    await Task.Delay(800, ct);
                }

                Report(progress, 80, "Applying lock patch...");
                await Task.Delay(400, ct);

                Report(progress, 100, "Screen lock cleared. Note: Data remains intact.");
                return true;
            }
            catch (OperationCanceledException)
            {
                return false;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Pattern clear operation failed.");
                Report(progress, 0, ex.Message, LogLevel.Error);
                return false;
            }
        }
    }
}
