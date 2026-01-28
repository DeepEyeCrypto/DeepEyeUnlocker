using System;
using System.Collections.Generic;
using System.Linq;
using LibUsbDotNet;
using LibUsbDotNet.Main;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure.USB;

namespace DeepEyeUnlocker.Core
{
    public class DeviceManager : IDisposable
    {
        private readonly UsbWatcher _watcher;
        public event Action<IEnumerable<DeviceContext>>? OnDevicesChanged;

        public DeviceManager()
        {
            _watcher = new UsbWatcher();
            _watcher.OnDeviceChanged += HandleUsbChange;
            _watcher.Start();
        }

        private void HandleUsbChange()
        {
            var devices = EnumerateDevices();
            OnDevicesChanged?.Invoke(devices);
        }

        public List<DeviceContext> EnumerateDevices()
        {
            var activeDevices = new List<DeviceContext>();
            try
            {
                foreach (UsbRegistry usb in UsbDevice.AllDevices)
                {
                    var discovery = ProtocolDiscoveryService.Discover(usb);
                    activeDevices.Add(new DeviceContext
                    {
                        Vid = usb.Vid,
                        Pid = usb.Pid,
                        Serial = usb.SymbolicName, // Use symbolic name as unique key if serial missing
                        Mode = MapMode(discovery.Mode),
                        Chipset = discovery.Chipset,
                        Brand = discovery.Chipset // Initial heuristic
                    });
                }
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to enumerate USB devices.");
            }
            return activeDevices;
        }

        private ConnectionMode MapMode(string mode) => mode.ToLower() switch
        {
            "edl" => ConnectionMode.EDL,
            "brom" => ConnectionMode.BROM,
            "preloader" => ConnectionMode.Preloader,
            "fastboot" => ConnectionMode.Fastboot,
            "download" => ConnectionMode.DownloadMode,
            _ => ConnectionMode.MTP
        };

        public void Dispose()
        {
            _watcher.Dispose();
        }
    }
}
