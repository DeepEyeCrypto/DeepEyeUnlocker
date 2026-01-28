using System;
using System.Threading.Tasks;
using LibUsbDotNet;
using NLog;
using DeepEyeUnlocker.Protocols;

namespace DeepEyeUnlocker.Core
{
    public class OperationFactory
    {
        private static readonly Logger Logger = LogManager.GetCurrentClassLogger();

        public static IProtocol? CreateProtocol(string chipset, UsbDevice usbDevice)
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
