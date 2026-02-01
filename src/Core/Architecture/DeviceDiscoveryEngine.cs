using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Architecture;
using DeepEyeUnlocker.Protocols.Usb;

namespace DeepEyeUnlocker.Core.Architecture
{
    public class DeviceDiscoveryEngine
    {
        private readonly PluginManager _pluginManager;

        public DeviceDiscoveryEngine(PluginManager pluginManager)
        {
            _pluginManager = pluginManager;
        }

        public async Task<PluginDeviceContext?> AutoDetectDeviceAsync(IUsbDevice usb)
        {
            // Iterate through all loaded protocols to see if any recognize the device
            foreach (var protocol in _pluginManager.LoadedProtocols)
            {
                if (await protocol.DetectDeviceAsync(usb))
                {
                    var info = await protocol.GetDeviceInfoAsync();
                    return new PluginDeviceContext
                    {
                        DeviceId = usb.GetTransportId(), // Unique ID for this physical connection
                        ActiveProtocol = protocol,
                        UsbLink = usb,
                        Info = info
                    };
                }
            }

            return null; // Device not recognized by any loaded plugin
        }
    }

    public static class UsbExtensions
    {
        public static string GetTransportId(this IUsbDevice usb)
        {
            // In a real implementation, this returns a unique path or serial
            return "USB-DEVICE-SIM";
        }
    }
}
