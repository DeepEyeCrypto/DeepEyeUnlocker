using System;
using System.Collections.Generic;
using LibUsbDotNet;
using DeepEyeUnlocker.Protocols;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Engines;
using DeepEyeUnlocker.Protocols.Qualcomm;

namespace DeepEyeUnlocker.Core
{
    public class OperationFactory
    {
        private static readonly EdlManager _edlManager = new();
        private static FirehoseManager? _firehoseManager;
        private static PartitionTableParser _partitionParser = new();

        /// <summary>
        /// Get or create a Firehose manager instance
        /// </summary>
        public static FirehoseManager GetFirehoseManager()
        {
            _firehoseManager ??= new FirehoseManager();
            return _firehoseManager;
        }

        /// <summary>
        /// Get the partition table parser
        /// </summary>
        public static PartitionTableParser GetPartitionParser() => _partitionParser;

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

        public static IEnumerable<Operation> GetAvailableOperations(DeviceContext context, IProtocol? protocol = null)
        {
            var ops = new List<Operation>();

            // Basic Info operation if protocol is available
            if (protocol != null)
            {
                ops.Add(new Operations.DeviceInfoOperation(protocol));
            }

            // EDL Operations - Add for Qualcomm devices not already in EDL
            if (IsQualcommDevice(context) && context.Mode != ConnectionMode.EDL)
            {
                ops.Add(new Operations.RebootToEdlOperation(context, _edlManager));
            }

            // EDL Check - Always available
            ops.Add(new Operations.CheckEdlModeOperation(_edlManager));

            // Low-level Mode operations (EDL/BROM)
            if (context.Mode == ConnectionMode.EDL || context.Mode == ConnectionMode.BROM)
            {
                if (protocol != null)
                {
                    ops.Add(new Operations.BackupOperation(protocol)); 
                    ops.Add(new Operations.FormatOperation(protocol));
                    ops.Add(new Operations.FlashOperation("", protocol)); // Source path empty by default
                    ops.Add(new Operations.FrpBypassOperation(protocol));
                    ops.Add(new Operations.PatternClearOperation(protocol));
                    ops.Add(new Operations.BootloaderOperation(protocol));
                }
                else
                {
                    // If no protocol is active yet, we can't add data-path operations
                    Logger.Warning("Operations requested for low-level mode without active protocol engine.", "FACTORY");
                }
            }

            // Brand-specific operations
            AddBrandSpecificOperations(ops, context);

            return ops;
        }

        /// <summary>
        /// Add brand-specific operations
        /// </summary>
        private static void AddBrandSpecificOperations(List<Operation> ops, DeviceContext context)
        {
            switch (context.Brand?.ToUpperInvariant())
            {
                case "XIAOMI":
                case "REDMI":
                case "POCO":
                    ops.Add(new Operations.XiaomiServiceOperation());
                    break;

                case "OPPO":
                case "REALME":
                case "ONEPLUS":
                    ops.Add(new Operations.OppoServiceOperation());
                    break;

                case "SAMSUNG":
                    // Samsung uses Odin mode, not EDL
                    if (context.Mode == ConnectionMode.DownloadMode)
                    {
                        // FormatOperation requires protocol - skip here
                    }
                    break;

                case "MOTOROLA":
                case "LENOVO":
                    // Lenovo-Motorola devices
                    break;

                case "LG":
                    // LG devices (legacy)
                    break;
            }
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

        /// <summary>
        /// Get EDL manager instance
        /// </summary>
        public static EdlManager GetEdlManager() => _edlManager;

        /// <summary>
        /// Check if device is Qualcomm-based
        /// </summary>
        public static bool IsQualcommDevice(DeviceContext context)
        {
            var chipset = context.Chipset?.ToLower() ?? "";
            var soc = context.SoC?.ToLower() ?? "";
            
            return chipset.Contains("qualcomm") ||
                   chipset.Contains("snapdragon") ||
                   soc.StartsWith("sm") ||
                   soc.StartsWith("sdm") ||
                   soc.StartsWith("msm");
        }

        /// <summary>
        /// Check if device is MediaTek-based
        /// </summary>
        public static bool IsMediaTekDevice(DeviceContext context)
        {
            var chipset = context.Chipset?.ToLower() ?? "";
            var soc = context.SoC?.ToLower() ?? "";
            
            return chipset.Contains("mediatek") ||
                   chipset.Contains("mtk") ||
                   soc.StartsWith("mt");
        }

        /// <summary>
        /// Check if device is Samsung Exynos-based
        /// </summary>
        public static bool IsExynosDevice(DeviceContext context)
        {
            var soc = context.SoC?.ToLower() ?? "";
            return soc.Contains("exynos");
        }

        /// <summary>
        /// Get recommended protocol for device
        /// </summary>
        public static string GetRecommendedProtocol(DeviceContext context)
        {
            if (context.Brand?.ToUpperInvariant() == "SAMSUNG")
                return "Odin (Download Mode)";
            
            if (IsQualcommDevice(context))
                return "Qualcomm EDL (9008)";
            
            if (IsMediaTekDevice(context))
                return "MediaTek BROM";
            
            if (IsExynosDevice(context))
                return "Samsung Download Mode";
            
            return "Unknown";
        }
    }
}
