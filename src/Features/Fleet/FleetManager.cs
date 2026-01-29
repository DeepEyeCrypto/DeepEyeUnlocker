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
            Logger.Info("Refreshing fleet device list...");
            
            // Execute 'adb devices' via the shell bridge
            var rawDevices = await _adb.ExecuteShellAsync("devices"); 
            
            var lines = rawDevices.Split('\n', StringSplitOptions.RemoveEmptyEntries);
            var activeSerials = new List<string>();

            foreach (var line in lines)
            {
                if (line.Contains("\tdevice"))
                {
                    var serial = line.Split('\t')[0].Trim();
                    activeSerials.Add(serial);

                    if (!_devices.ContainsKey(serial))
                    {
                        var device = new FleetDevice 
                        { 
                            Alias = $"Bench-{(char)('A' + _devices.Count)}",
                            Context = new DeviceContext 
                            { 
                                Serial = serial, 
                                Mode = ConnectionMode.ADB,
                                Model = "Android Device"
                            },
                            ConnectedAt = DateTime.UtcNow
                        };
                        _devices.TryAdd(serial, device);
                    }
                }
            }

            // Cleanup disconnected devices
            var currentKeys = _devices.Keys.ToList();
            var toRemove = currentKeys.Except(activeSerials).ToList();
            foreach (var s in toRemove) _devices.TryRemove(s, out _);
        }

        public async Task<BatchResult> BatchExecuteShellAsync(IEnumerable<string> serials, string command)
        {
            Logger.Info($"Fleet: Batch executing command on {serials.Count()} devices: {command}");
            var result = new BatchResult { TotalDevices = serials.Count() };

            foreach (var serial in serials)
            {
                try 
                {
                    // Set context to specific device
                    _adb.TargetSerial = serial;
                    await _adb.ExecuteShellAsync(command);
                    
                    result.SuccessCount++;
                    Logger.Debug($"Fleet: Command success on {serial}");
                }
                catch (Exception ex)
                {
                    result.FailCount++;
                    result.Errors.Add($"{serial}: {ex.Message}");
                }
                finally
                {
                    _adb.TargetSerial = null; // Reset context
                }
            }

            return result;
        }

        public async Task BatchInstallApkAsync(IEnumerable<string> serials, string apkPath)
        {
            if (!File.Exists(apkPath))
            {
                Logger.Error($"Fleet: APK not found at {apkPath}");
                return;
            }

            foreach (var serial in serials)
            {
                try 
                {
                    _adb.TargetSerial = serial;
                    Logger.Info($"Fleet: Installing APK on {serial}...");
                    bool success = await _adb.InstallPackageAsync(apkPath);
                    if (success) Logger.Info($"Fleet: Install success on {serial}");
                    else Logger.Error($"Fleet: Install failed on {serial}");
                }
                catch (Exception ex)
                {
                    Logger.Error($"Fleet: Batch install error on {serial}: {ex.Message}");
                }
                finally
                {
                    _adb.TargetSerial = null;
                }
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
