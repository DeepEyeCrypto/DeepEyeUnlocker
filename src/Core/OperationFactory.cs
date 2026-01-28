using System;
using System.Collections.Generic;
using LibUsbDotNet;
using DeepEyeUnlocker.Protocols;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Engines;

namespace DeepEyeUnlocker.Core
{
    public class OperationFactory
    {
        private static readonly EdlManager _edlManager = new();

        public static IProtocol? CreateEngine(DeviceContext context, UsbDevice usbDevice)
        {
            // Priority 1: Brand Overrides
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

            // EDL Operations - Add for Qualcomm devices not already in EDL
            if (IsQualcommDevice(context) && context.Mode != ConnectionMode.EDL)
            {
                ops.Add(new Operations.RebootToEdlOperation(context, _edlManager));
            }

            // EDL Check - Always available
            ops.Add(new Operations.CheckEdlModeOperation(_edlManager));

            // Flash operations - Available in low-level modes
            if (context.Mode == ConnectionMode.EDL || context.Mode == ConnectionMode.BROM)
            {
                ops.Add(new Operations.BackupOperation(null!)); // Needs protocol instance
                ops.Add(new Operations.FormatOperation());
                ops.Add(new Operations.FlashOperation());
                ops.Add(new Operations.FrpBypassOperation());
                ops.Add(new Operations.PatternClearOperation());
                ops.Add(new Operations.BootloaderOperation());
            }

            // Brand-specific operations
            if (context.Brand.Equals("Xiaomi", StringComparison.OrdinalIgnoreCase))
            {
                ops.Add(new Operations.XiaomiServiceOperation());
            }
            else if (context.Brand.Equals("Oppo", StringComparison.OrdinalIgnoreCase) ||
                     context.Brand.Equals("Realme", StringComparison.OrdinalIgnoreCase))
            {
                ops.Add(new Operations.OppoServiceOperation());
            }

            return ops;
        }

        /// <summary>
        /// Get EDL capability for a device
        /// </summary>
        public static EdlCapability GetEdlCapability(DeviceContext context)
        {
            return _edlManager.GetCapabilityFor(context);
        }

        /// <summary>
        /// Get EDL profile for a device
        /// </summary>
        public static EdlProfile? GetEdlProfile(DeviceContext context)
        {
            return _edlManager.GetProfileFor(context);
        }

        private static bool IsQualcommDevice(DeviceContext context)
        {
            var chipset = context.Chipset?.ToLower() ?? "";
            var soc = context.SoC?.ToLower() ?? "";
            
            return chipset.Contains("qualcomm") ||
                   chipset.Contains("snapdragon") ||
                   soc.StartsWith("sm") ||
                   soc.StartsWith("sdm") ||
                   soc.StartsWith("msm");
        }
    }
}
