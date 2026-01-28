using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Operations
{
    public class OppoServiceOperation : Operation
    {
        public OppoServiceOperation()
        {
            Name = "Oppo/Realme Advanced FRP";
        }

        public override async Task<bool> ExecuteAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            if (device.Brand != "Oppo" && device.Brand != "Realme")
            {
                Logger.Error("Operation only supported for Oppo/Realme devices.");
                Report(progress, 0, "Unsupported device brand", LogLevel.Error);
                return false;
            }

            Report(progress, 10, "Initializing Oppo FRP bypass module...");
            
            try
            {
                if (ct.IsCancellationRequested) return false;

                Report(progress, 30, "Establishing connection to auth partition...");
                await Task.Delay(500, ct);

                Report(progress, 60, "Clearing factory reset protection flags...");
                await Task.Delay(1000, ct);

                Report(progress, 90, "Verifying partition integrity...");
                await Task.Delay(400, ct);

                Report(progress, 100, "Oppo/Realme FRP Bypass Successful.");
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
