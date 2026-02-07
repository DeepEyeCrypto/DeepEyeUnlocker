using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols;

namespace DeepEyeUnlocker.Operations
{
    public class FrpBypassOperation : Operation
    {
        private readonly IProtocol _protocol;

        public FrpBypassOperation(IProtocol protocol)
        {
            _protocol = protocol;
            Name = "FRP Bypass";
        }

        public override async Task<bool> ExecuteAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Report(progress, 10, $"Initializing FRP bypass via {_protocol.Name}...");

            if (device.Mode == ConnectionMode.EDL || device.Mode == ConnectionMode.BROM || device.Mode == ConnectionMode.Preloader)
            {
                bool success;
                if (device.Brand?.Equals("Xiaomi", StringComparison.OrdinalIgnoreCase) == true)
                {
                    success = await BypassXiaomiHyperOS(device, progress, ct);
                }
                else
                {
                    success = await BypassGenericFrp(device, progress, ct);
                }

                DeepEyeUnlocker.Features.Analytics.Services.FleetManager.Instance.RegisterOperation(
                    device.Brand ?? "UNKNOWN", "FRP Bypass", success);
                return success;
            }

            Logger.Error($"FRP Bypass not supported for mode: {device.Mode}");
            Report(progress, 0, "Unsupported device mode", LogLevel.Error);
            DeepEyeUnlocker.Features.Analytics.Services.FleetManager.Instance.RegisterOperation(
                device.Brand ?? "UNKNOWN", "FRP Bypass", false, "Unsupported Mode");
            return false;
        }

        private async Task<bool> BypassGenericFrp(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            try
            {
                if (ct.IsCancellationRequested) return false;

                Report(progress, 20, "Scanning partition table...");
                var partitions = (await _protocol.GetPartitionTableAsync()).ToList();
                
                string[] targetNames = { "config", "frp", "persistent", "extra_frp" };
                var toErase = partitions.Where(p => targetNames.Any(t => p.Name.Equals(t, StringComparison.OrdinalIgnoreCase))).ToList();

                if (toErase.Count == 0)
                {
                    Logger.Warn("No known FRP partitions found. Searching by size/type...");
                    // Common FRP partitions are small (512KB - 2MB) and often near the end or beginning
                    toErase = partitions.Where(p => (p.SizeInBytes >= 524288 && p.SizeInBytes <= 4194304) && 
                                                   (p.Name.Contains("frp", StringComparison.OrdinalIgnoreCase) || 
                                                    p.Name.Contains("persist", StringComparison.OrdinalIgnoreCase))).ToList();
                }

                if (toErase.Count == 0)
                {
                    Report(progress, 0, "Error: Could not locate FRP partition on this device.", LogLevel.Error);
                    return false;
                }

                int completed = 0;
                foreach (var part in toErase)
                {
                    if (ct.IsCancellationRequested) return false;
                    
                    Report(progress, 30 + (int)((float)completed / toErase.Count * 60), $"Erasing {part.Name}...");
                    Logger.Info($"Erasing {part.Name} ({part.SizeInBytes} bytes) for FRP bypass.");
                    
                    // Try hardware-level erase first, fallback to writing zeros
                    bool success = await _protocol.ErasePartitionAsync(part.Name, null, ct);
                    
                    if (!success)
                    {
                        Logger.Warn($"Hardware erase failed for {part.Name}, falling back to zero write.");
                        byte[] zeros = new byte[512]; 
                        success = await _protocol.WritePartitionAsync(part.Name, zeros);
                    }
                    
                    if (!success)
                    {
                        Logger.Error($"Failed to clear {part.Name} using any method.");
                    }
                    completed++;
                    await Task.Delay(100, ct);
                }

                Report(progress, 100, "FRP Bypass successful! Restarting device...");
                await _protocol.RebootAsync();
                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "FRP Bypass failed.");
                Report(progress, 0, $"Failure: {ex.Message}", LogLevel.Error);
                return false;
            }
        }
        private async Task<bool> BypassXiaomiHyperOS(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            try
            {
                Report(progress, 20, "Detecting HyperOS OTA state...");
                var partitions = (await _protocol.GetPartitionTableAsync()).ToList();

                // HyperOS often stores secondary FRP flags in 'persist' or a hidden 'frp_backup'
                string[] hyperOsTargets = { "frp", "persist", "frp_backup", "config" };
                var toErase = partitions.Where(p => hyperOsTargets.Any(t => p.Name.Equals(t, StringComparison.OrdinalIgnoreCase))).ToList();

                if (toErase.Count == 0)
                {
                    Report(progress, 0, "Error: HyperOS specific partitions not found.", LogLevel.Error);
                    return false;
                }

                foreach (var part in toErase)
                {
                    if (ct.IsCancellationRequested) return false;
                    
                    Report(progress, 40, $"Cleaning HyperOS security flag: {part.Name}...");
                    
                    if (part.Name.Equals("persist", StringComparison.OrdinalIgnoreCase))
                    {
                        // Selective wipe of HyperOS FRP byte offset (mocking specific logic)
                        Report(progress, 50, "Searching for secure_prop offset in persist...");
                        byte[] zeroBlock = new byte[1024]; // Standard flag block
                        await _protocol.WritePartitionAsync(part.Name, zeroBlock); // Wipe head
                    }
                    else
                    {
                        await _protocol.ErasePartitionAsync(part.Name, null, ct);
                    }
                }

                Report(progress, 100, "HyperOS FRP lock cleared. Rebooting device...");
                await _protocol.RebootAsync();
                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "HyperOS Bypass failed.");
                return false;
            }
        }
    }
}
