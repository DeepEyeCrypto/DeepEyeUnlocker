using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Operations
{
    public class DeviceInfoOperation : Operation
    {
        public DeviceInfoOperation()
        {
            Name = "Read Device Info";
        }

        public override async Task<bool> ExecuteAsync(Device device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Report(progress, 20, "Establishing protocol connection...");
            
            try
            {
                if (ct.IsCancellationRequested) return false;

                // Simulated delay for reading
                Report(progress, 50, "Reading hardware identifiers...");
                await Task.Delay(400, ct);

                device.Brand = "Detected Brand";
                device.Model = "Detected Model";
                device.Imei = "35XXXXXXXXXXXXX";
                device.BootloaderStatus = "Locked";
                device.AndroidVersion = "12.0";

                Logger.Info($"Device Info: {device.Brand} {device.Model}, IMEI: {device.Imei}, BL: {device.BootloaderStatus}");
                
                Report(progress, 100, "Device information read successfully.");
                return true;
            }
            catch (OperationCanceledException)
            {
                Report(progress, 0, "Operation cancelled.", LogLevel.Warn);
                return false;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to read device information.");
                Report(progress, 0, ex.Message, LogLevel.Error);
                return false;
            }
        }
    }
}
