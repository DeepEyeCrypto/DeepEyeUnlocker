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
        private readonly ProfileManager _profiles;
        public event Action<IEnumerable<DeviceContext>>? OnDevicesChanged;

        public DeviceManager()
        {
            _profiles = new ProfileManager();
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
                    var profile = _profiles.GetProfileForDevice(usb.Vid, usb.Pid);

                    var context = new DeviceContext
                    {
                        Vid = usb.Vid,
                        Pid = usb.Pid,
                        Serial = usb.DeviceProperties.ContainsKey("SymbolicName") ? usb.DeviceProperties["SymbolicName"]?.ToString() ?? "" : usb.SymbolicName,
                        Mode = MapMode(discovery.Mode),
                        Chipset = discovery.Chipset,
                        Brand = profile.BrandName
                    };

                    // Store symbolic name in properties for reopening
                    context.Properties["SymbolicName"] = context.Serial ?? "";

                    // Add brand-specific configs to properties
                    foreach (var cfg in profile.Configs)
                    {
                        context.Properties[cfg.Key] = cfg.Value;
                    }

                    activeDevices.Add(context);
                }
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to enumerate USB devices.");
            }
            return activeDevices;
        }

        public UsbDevice? OpenDevice(DeviceContext context)
        {
            if (context.Properties.TryGetValue("SymbolicName", out var symName))
            {
                var reg = UsbDevice.AllDevices.FirstOrDefault(r => r.SymbolicName == symName);
                return reg?.Device;
            }
            return null;
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
