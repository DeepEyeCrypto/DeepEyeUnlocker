using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.FrpBypass.Interfaces;
using DeepEyeUnlocker.Features.FrpBypass.Models;
using DeepEyeUnlocker.Features.FrpBypass.Xiaomi;
using DeepEyeUnlocker.Features.FrpBypass.Samsung;
using DeepEyeUnlocker.Features.FrpBypass.OppoVivo;
using DeepEyeUnlocker.Features.FrpBypass.Motorola;

namespace DeepEyeUnlocker.Features.FrpBypass
{
    public class FrpEngineCore
    {
        private readonly IFrpProtocol _protocol;
        private readonly FrpBrandProfile _profile;

        public FrpEngineCore(IFrpProtocol protocol, FrpBrandProfile profile)
        {
            _protocol = protocol ?? throw new ArgumentNullException(nameof(protocol));
            _profile = profile ?? throw new ArgumentNullException(nameof(profile));
        }

        public async Task<bool> ExecuteBypassAsync(DeviceContext device)
        {
            Logger.Info($"[FRP] Starting automated bypass for {device.Brand} ({_profile.Os})...");

            // 1. Safety Check
            if (!await SafetyGate.ValidateEnvironment(device, _profile.TargetPartition))
            {
                Logger.Error("[FRP] Safety gate rejected the operation. Aborting.");
                return false;
            }

            // 2. Protocol Execution based on Method
            try
            {
                // Brand-specific overrides
                if (device.Brand.Equals("Xiaomi", StringComparison.OrdinalIgnoreCase))
                {
                    var xiaomiEngine = new XiaomiFrpEngine(_protocol, _profile);
                    return await xiaomiEngine.ExecuteBypassAsync(device, new Progress<ProgressUpdate>());
                }

                if (device.Brand.Equals("Samsung", StringComparison.OrdinalIgnoreCase))
                {
                    var samsungEngine = new SamsungFrpEngine(_protocol, _profile);
                    return await samsungEngine.ExecuteBypassAsync(device, new Progress<ProgressUpdate>());
                }

                if (device.Brand.Equals("Oppo", StringComparison.OrdinalIgnoreCase) || 
                    device.Brand.Equals("Vivo", StringComparison.OrdinalIgnoreCase) ||
                    device.Brand.Equals("Realme", StringComparison.OrdinalIgnoreCase))
                {
                    var oplusEngine = new OppoVivoFrpEngine(_protocol, _profile);
                    return await oplusEngine.ExecuteBypassAsync(device, new Progress<ProgressUpdate>());
                }

                if (device.Brand.Equals("Motorola", StringComparison.OrdinalIgnoreCase))
                {
                    var motoEngine = new MotorolaFrpEngine(_protocol, _profile);
                    return await motoEngine.ExecuteBypassAsync(device, new Progress<ProgressUpdate>());
                }

                switch (_profile.Method)
                {
                    case "EDL_WRITE_FLAG":
                    case "QUALCOMM_EDL_ERASE":
                        return await HandleEdlBypass();
                    case "BROM_PATCH":
                    case "MTK_BROM_FORMAT":
                        return await HandleMtkBypass();
                    case "ODIN_RESET_COMMAND":
                    case "ODIN_KERNEL_PATCH":
                        return await HandleSamsungBypass();
                    case "FASTBOOT_OEM_FORMAT":
                        return await HandleFastbootBypass();
                    default:
                        Logger.Error($"[FRP] Unsupported bypass method: {_profile.Method}");
                        return false;
                }
            }
            catch (Exception ex)
            {
                Logger.Error($"[FRP] Critical failure during execution: {ex.Message}");
                return false;
            }
        }

        private async Task<bool> HandleEdlBypass()
        {
            if (_profile.Method == "QUALCOMM_EDL_ERASE")
            {
                Logger.Info($"[EDL] Erasing partition '{_profile.TargetPartition}'...");
                return await _protocol.EDL_Erase_Partition(_profile.TargetPartition);
            }

            Logger.Info($"[EDL] Writing reset flag to '{_profile.TargetPartition}' at offset {_profile.Offset}...");
            byte[] payload = ConvertHexToBytes(_profile.HexPayload);
            return await _protocol.EDL_Flash_Partition(_profile.TargetPartition, payload);
        }

        private async Task<bool> HandleMtkBypass()
        {
            Logger.Info("[MTK] Executing Brom-level security patch...");
            byte[] payload = ConvertHexToBytes(_profile.HexPayload);
            return await _protocol.MTK_Brom_Execute((uint)_profile.Offset, payload);
        }

        private async Task<bool> HandleSamsungBypass()
        {
            Logger.Info("[ODIN] Delivering specialized FRP reset kernel...");
            return await _protocol.Samsung_Odin_Send_Command("RESET_FRP_BIT");
        }

        private async Task<bool> HandleFastbootBypass()
        {
            Logger.Info("[FASTBOOT] Formatting config partition via OEM command...");
            return await _protocol.Fastboot_Oem_Unlock();
        }

        private byte[] ConvertHexToBytes(string hex)
        {
            if (string.IsNullOrEmpty(hex) || hex == "none") return Array.Empty<byte>();

            // Remove common separators
            hex = hex.Replace("-", "").Replace(" ", "").Replace("0x", "");

            if (hex.Length % 2 != 0)
            {
                Logger.Warn($"[FRP] Invalid hex length for '{hex}'. Padding with leading zero.");
                hex = "0" + hex;
            }

            try
            {
                byte[] bytes = new byte[hex.Length / 2];
                for (int i = 0; i < bytes.Length; i++)
                {
                    bytes[i] = Convert.ToByte(hex.Substring(i * 2, 2), 16);
                }
                return bytes;
            }
            catch (Exception ex)
            {
                Logger.Error($"[FRP] Hex conversion failed for '{hex}': {ex.Message}");
                return Array.Empty<byte>();
            }
        }
    }
}
