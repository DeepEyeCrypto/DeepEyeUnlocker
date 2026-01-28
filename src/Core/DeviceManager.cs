using System;
using System.Collections.Generic;
using LibUsbDotNet;
using LibUsbDotNet.Main;
using NLog;

namespace DeepEyeUnlocker.Core
{
    public class DeviceManager
    {
        private static readonly Logger Logger = LogManager.GetCurrentClassLogger();

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
            // Qualcomm EDL: VID 05C6, PID 9008
            if (device.Vid == 0x05C6 && device.Pid == 0x9008)
                return "Qualcomm EDL";

            // MediaTek Preloader: VID 0E8D, PID 2000
            if (device.Vid == 0x0E8D && device.Pid == 0x2000)
                return "MediaTek Preloader";

            // Samsung Download: VID 04E8, PID 685D
            if (device.Vid == 0x04E8 && device.Pid == 0x685D)
                return "Samsung Download";

            // Fastboot: VID 18D1, PID D00D
            if (device.Vid == 0x18D1 && device.Pid == 0xD00D)
                return "Fastboot";

            return "Unknown / MTP";
        }
    }
}
