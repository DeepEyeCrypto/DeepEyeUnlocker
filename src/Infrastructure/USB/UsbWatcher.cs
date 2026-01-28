using System;
using System.Management;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Infrastructure.USB
{
    public class UsbWatcher : IDisposable
    {
        private ManagementEventWatcher? _insertWatcher;
        private ManagementEventWatcher? _removeWatcher;

        public event Action? OnDeviceChanged;

        public void Start()
        {
            try
            {
                // Watch for PnP Device Arrivals
                _insertWatcher = new ManagementEventWatcher(new WqlEventQuery("SELECT * FROM __InstanceCreationEvent WITHIN 2 WHERE TargetInstance ISA 'Win32_PnPEntity'"));
                _insertWatcher.EventArrived += (s, e) => OnDeviceChanged?.Invoke();
                _insertWatcher.Start();

                // Watch for PnP Device Removals
                _removeWatcher = new ManagementEventWatcher(new WqlEventQuery("SELECT * FROM __InstanceDeletionEvent WITHIN 2 WHERE TargetInstance ISA 'Win32_PnPEntity'"));
                _removeWatcher.EventArrived += (s, e) => OnDeviceChanged?.Invoke();
                _removeWatcher.Start();

                Logger.Info("USB WMI Watcher started successfully.");
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to initialize WMI USB Watcher. Falling back to polling.");
            }
        }

        public void Dispose()
        {
            _insertWatcher?.Stop();
            _insertWatcher?.Dispose();
            _removeWatcher?.Stop();
            _removeWatcher?.Dispose();
        }
    }
}
