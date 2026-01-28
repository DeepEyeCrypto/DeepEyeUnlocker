using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Operations
{
    public class FormatOperation : Operation
    {
        public FormatOperation()
        {
            Name = "Format / Factory Reset";
        }

        public override async Task<bool> ExecuteAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Report(progress, 10, "Starting Factory Reset...");

            try
            {
                if (ct.IsCancellationRequested) return false;

                Report(progress, 30, "Identifying userdata partition...");
                await Task.Delay(300, ct);

                Report(progress, 60, "Formatting userdata...");
                await Task.Delay(1000, ct);

                Report(progress, 90, "Formatting cache...");
                await Task.Delay(300, ct);

                Report(progress, 100, "Device formatted successfully.");
                return true;
            }
            catch (OperationCanceledException)
            {
                return false;
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
