using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using LibUsbDotNet.Main;

namespace DeepEyeUnlocker.Core
{
    public class DiscoveryResult
    {
        public string Chipset { get; set; } = "Unknown";
        public string Mode { get; set; } = "Unknown";
        public string SuggestedProtocol { get; set; } = "Unknown";
        public float Confidence { get; set; } = 0.0f;
    }

    public static class ProtocolDiscoveryService
    {
        private static readonly Dictionary<(int Vid, int Pid), DiscoveryResult> KnownMappings = new Dictionary<(int Vid, int Pid), DiscoveryResult>
        {
            // Qualcomm EDL
            { (0x05C6, 0x9008), new DiscoveryResult { Chipset = "Qualcomm", Mode = "EDL", SuggestedProtocol = "Firehose", Confidence = 1.0f } },
            { (0x05C6, 0x900E), new DiscoveryResult { Chipset = "Qualcomm", Mode = "Diagnostics", SuggestedProtocol = "Diag", Confidence = 0.9f } },
            
            // MediaTek
            { (0x0E8D, 0x0003), new DiscoveryResult { Chipset = "MediaTek", Mode = "BROM", SuggestedProtocol = "MTK_BROM", Confidence = 1.0f } },
            { (0x0E8D, 0x2000), new DiscoveryResult { Chipset = "MediaTek", Mode = "Preloader", SuggestedProtocol = "MTK_DA", Confidence = 1.0f } },
            
            // Samsung
            { (0x04E8, 0x685D), new DiscoveryResult { Chipset = "Samsung", Mode = "Download", SuggestedProtocol = "Odin", Confidence = 1.0f } },
            
            // Spreadtrum / Unisoc
            { (0x1782, 0x4D00), new DiscoveryResult { Chipset = "Unisoc", Mode = "SPD Diag", SuggestedProtocol = "SPD", Confidence = 1.0f } },
            
            // Fastboot Generic
            { (0x18D1, 0xD00D), new DiscoveryResult { Chipset = "Generic", Mode = "Fastboot", SuggestedProtocol = "Fastboot", Confidence = 1.0f } }
        };

        public static DiscoveryResult Discover(UsbRegistry device)
        {
            if (KnownMappings.TryGetValue((device.Vid, device.Pid), out var result))
            {
                Logger.Info($"Matched {device.Vid:X4}:{device.Pid:X4} as {result.Chipset} ({result.Mode})", "DISCOVERY");
                return result;
            }

            // Fallback: Heuristics based on Descriptor strings
            string desc = (device.FullName + device.DeviceProperties.ToString()).ToLower();
            
            if (desc.Contains("qualcomm") || desc.Contains("9008"))
                return new DiscoveryResult { Chipset = "Qualcomm", Mode = "Heuristic EDL", SuggestedProtocol = "Firehose", Confidence = 0.7f };
                
            if (desc.Contains("mediatek") || desc.Contains("mtk") || desc.Contains("preloader"))
                return new DiscoveryResult { Chipset = "MediaTek", Mode = "Heuristic Preloader", SuggestedProtocol = "MTK_DA", Confidence = 0.7f };

            Logger.Warn($"Unknown identity for {device.Vid:X4}:{device.Pid:X4}. FullName: {device.FullName}", "DISCOVERY");
            return new DiscoveryResult();
        }

        public static async System.Threading.Tasks.Task<DiscoveryResult> HandshakeDiscoveryAsync(LibUsbDotNet.UsbDevice usbDevice)
        {
            Logger.Info("Attempting protocol handshake signatures...", "PROBE");

            // 1. Try MediaTek BROM/Preloader Handshake
            try
            {
                var mtk = new Protocols.MTK.MTKPreloader(new Protocols.Usb.UsbDeviceWrapper(usbDevice));
                if (await mtk.HandshakeAsync())
                {
                    ushort hwCode = await mtk.GetHardwareCodeAsync();
                    return new DiscoveryResult { 
                        Chipset = "MediaTek", 
                        Mode = $"BROM/PL (0x{hwCode:X4})", 
                        SuggestedProtocol = "MTK_DA", 
                        Confidence = 0.95f 
                    };
                }
            } catch { /* Silent fail for probe */ }

            // 2. Try Qualcomm Sahara
            try
            {
                var sahara = new Protocols.Qualcomm.SaharaProtocol(new Protocols.Usb.UsbDeviceWrapper(usbDevice));
                if (await sahara.ProcessHelloAsync())
                {
                    return new DiscoveryResult { 
                        Chipset = "Qualcomm", 
                        Mode = "EDL (Sahara)", 
                        SuggestedProtocol = "Firehose", 
                        Confidence = 0.95f 
                    };
                }
            } catch { /* Silent fail for probe */ }

            return new DiscoveryResult();
        }
    }
}
