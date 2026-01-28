using System;
using System.Collections.Generic;
using LibUsbDotNet;
using DeepEyeUnlocker.Protocols;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core
{
    public class OperationFactory
    {
        public static IProtocol? CreateEngine(DeviceContext context, UsbDevice usbDevice)
        {
            // Priority 1: Brand Overrides
            // e.g. If it's a Samsung in Download mode, use SamsungEngine regardless of it being an Exynos/Snapdragon
            if (context.Brand.Equals("Samsung", StringComparison.OrdinalIgnoreCase))
            {
                return new Protocols.Samsung.SamsungEngine(usbDevice);
            }

            // Priority 2: Chipset Detection
            switch (context.Chipset.ToLower())
            {
                case "qualcomm":
                case "qualcomm edl":
                    return new Protocols.Qualcomm.QualcommEngine(usbDevice);
                case "mediatek":
                case "mtk":
                    return new Protocols.MTK.MTKEngine(usbDevice);
                default:
                    Logger.Error($"Unsupported chipset or brand: {context.Chipset} / {context.Brand}", "FACTORY");
                    return null;
            }
        }

        public static IEnumerable<Operation> GetAvailableOperations(DeviceContext context)
        {
            var ops = new List<Operation>
            {
                new Operations.DeviceInfoOperation()
            };

            if (context.Mode == ConnectionMode.EDL || context.Mode == ConnectionMode.BROM)
            {
                ops.Add(new Operations.BackupOperation(null!)); // Needs protocol instance
                ops.Add(new Operations.FormatOperation());
            }

            if (context.Brand.Equals("Xiaomi", StringComparison.OrdinalIgnoreCase))
            {
                ops.Add(new Operations.XiaomiServiceOperation());
            }
            else if (context.Brand.Equals("Oppo", StringComparison.OrdinalIgnoreCase))
            {
                ops.Add(new Operations.OppoServiceOperation());
            }

            return ops;
        }
    }
}
