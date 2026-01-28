using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols;
using DeepEyeUnlocker.Infrastructure.Logging;
using LogLevel = DeepEyeUnlocker.Core.Models.LogLevel;

namespace DeepEyeUnlocker.Operations
{
    public class BootloaderOperation : Operation
    {
        private readonly IProtocol _protocol;

        public BootloaderOperation(IProtocol protocol)
        {
            _protocol = protocol;
            Name = "Advanced Bootloader Unlock";
        }

        public override async Task<bool> ExecuteAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Report(progress, 5, "Initializing Security Check...");

            Report(progress, 10, "⚠️ WARNING: Data will be wiped!", LogLevel.Warn);
            await Task.Delay(1000, ct);

            try
            {
                if (ct.IsCancellationRequested) return false;

                BootloaderUnlockMethod method = device.Chipset?.ToLower() switch
                {
                    "qualcomm" => new QualcommBootloaderUnlock(),
                    "mtk" => new MTKBootloaderUnlock(),
                    "samsung" => new SamsungBootloaderUnlock(),
                    _ => new GenericOemUnlock()
                };

                Report(progress, 30, $"Executing {method.GetType().Name}...");
                bool result = await method.ExecuteAsync(device);

                if (result)
                {
                    Report(progress, 100, "✅ Bootloader Unlocked Successfully.");
                    return true;
                }
                
                Report(progress, 0, "Unlock method failed.", LogLevel.Error);
                return false;
            }
            catch (OperationCanceledException)
            {
                return false;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Bootloader unlock orchestration failed.");
                Report(progress, 0, ex.Message, LogLevel.Error);
                return false;
            }
        }
    }
}
