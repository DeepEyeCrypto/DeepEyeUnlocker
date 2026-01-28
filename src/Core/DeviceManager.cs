using System;
using System.Collections.Generic;
using LibUsbDotNet;
using LibUsbDotNet.Main;
namespace DeepEyeUnlocker.Core
{
    public class DeviceManager
    {
        public List<UsbRegistry> EnumerateDevices()
        {
            List<UsbRegistry> devices = new List<UsbRegistry>();
            try
            {
                UsbRegDeviceList allDevices = UsbDevice.AllDevices;
                foreach (UsbRegistry usbRegistry in allDevices)
                {
                    devices.Add(usbRegistry);
                    Logger.Info($"Found Device: {usbRegistry.FullName} (VID: {usbRegistry.Vid:X4} PID: {usbRegistry.Pid:X4})");
                }
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to enumerate USB devices.");
            }
            return devices;
        }

        public string IdentifyMode(UsbRegistry device)
        {
            var discovery = ProtocolDiscoveryService.Discover(device);
            if (discovery.Chipset != "Unknown")
            {
                return $"{discovery.Chipset} {discovery.Mode}";
            }
            return "Unknown / MTP";
        }
    }
}
