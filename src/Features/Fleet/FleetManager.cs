using System;
using System.Collections.Generic;
using System.Collections.Concurrent;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.Features.Fleet
{
    public class FleetDevice
    {
        public DeviceContext Context { get; set; } = null!;
        public string Alias { get; set; } = string.Empty;
        public string LastStatus { get; set; } = "Connected";
        public DateTime ConnectedAt { get; set; } = DateTime.UtcNow;
    }

    /// <summary>
    /// Manages multiple connected devices and orchestrates batch operations (Epic E).
    /// </summary>
    public class FleetManager
    {
        private readonly IAdbClient _adb;
        private readonly ConcurrentDictionary<string, FleetDevice> _devices = new();
        private DeviceContext? _selectedDevice;

        public event EventHandler<DeviceContext?>? SelectedDeviceChanged;

        public DeviceContext? SelectedDevice
        {
            get => _selectedDevice;
            set
            {
                if (_selectedDevice != value)
                {
                    _selectedDevice = value;
                    SelectedDeviceChanged?.Invoke(this, _selectedDevice);
                }
            }
        }

        public FleetManager(IAdbClient adb)
        {
            _adb = adb ?? throw new ArgumentNullException(nameof(adb));
        }

        public IEnumerable<FleetDevice> GetDevices() => _devices.Values;

        public async Task RefreshDevicesAsync()
        {
            // Note: Real implementation would poll AdbClient for device list
            Logger.Info("Refreshing fleet device list...");
            
            // Simulation logic for demonstration
            var serials = new[] { "SERIAL_A_123", "SERIAL_B_456" };
            
            foreach (var s in serials)
            {
                if (!_devices.ContainsKey(s))
                {
                    _devices.TryAdd(s, new FleetDevice 
                    { 
                        Alias = $"Bench-{(char)('A' + _devices.Count)}",
                        Context = new DeviceContext { Serial = s, Model = "Generic Android" } 
                    });
                }
            }
        }

        public async Task<BatchResult> BatchExecuteShellAsync(IEnumerable<string> serials, string command)
        {
            Logger.Info($"Fleet: Batch executing command on {serials.Count()} devices: {command}");
            var result = new BatchResult { TotalDevices = serials.Count() };

            var tasks = serials.Select(async s => 
            {
                try 
                {
                    // In a real scenario, we'd need an AdbClient that supports -s parameter
                    // For now, we simulate success
                    await Task.Delay(100); 
                    result.SuccessCount++;
                    Logger.Debug($"Fleet: Command success on {s}");
                }
                catch (Exception ex)
                {
                    result.FailCount++;
                    result.Errors.Add($"{s}: {ex.Message}");
                }
            });

            await Task.WhenAll(tasks);
            return result;
        }

        public async Task BatchInstallApkAsync(IEnumerable<string> serials, string apkPath)
        {
            // Safety Check: Avoid destructive batch actions without explicit single-selection in UI
            // This manager provides the logic, UI forces the selection.
            foreach (var serial in serials)
            {
                Logger.Info($"Fleet: Pushing APK to {serial}...");
                // Simulation
                await Task.Delay(200);
            }
        }
    }

    public class BatchResult
    {
        public int TotalDevices { get; set; }
        public int SuccessCount { get; set; }
        public int FailCount { get; set; }
        public List<string> Errors { get; set; } = new();
    }
}
