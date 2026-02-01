using System;
#if WINDOWS
using System.Management;
#endif
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Infrastructure.USB
{
    public class UsbWatcher : IDisposable
    {
#if WINDOWS
        private ManagementEventWatcher? _insertWatcher;
        private ManagementEventWatcher? _removeWatcher;
#endif

#pragma warning disable CS0067
        public event Action? OnDeviceChanged;
#pragma warning restore CS0067

        public void Start()
        {
#if WINDOWS
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
#else
            Logger.Warn("USB Monitoring (WMI) is not available on this platform.");
#endif
        }

        public void Dispose()
        {
#if WINDOWS
            _insertWatcher?.Stop();
            _insertWatcher?.Dispose();
            _removeWatcher?.Stop();
            _removeWatcher?.Dispose();
#endif
        }
    }
}
