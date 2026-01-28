using System;
using System.Threading.Tasks;
using LibUsbDotNet;
using DeepEyeUnlocker.Protocols;

namespace DeepEyeUnlocker.Core
{
    public class OperationFactory
    {
        public static IProtocol? CreateEngine(string chipset, UsbDevice usbDevice)
        {
            switch (chipset.ToLower())
            {
                case "qualcomm":
                case "qualcomm edl":
                    return new Protocols.Qualcomm.QualcommEngine(usbDevice);
                case "mediatek":
                case "mtk":
                    return new Protocols.MTK.MTKEngine(usbDevice);
                case "samsung":
                    return new Protocols.Samsung.SamsungEngine(usbDevice);
                default:
                    Logger.Error($"Unsupported chipset: {chipset}");
                    return null;
            }
        }
    }
}
