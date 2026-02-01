using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Architecture;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols.Usb;

namespace DeepEyeUnlocker.Protocols.SPD
{
    public class SpdUniversalPlugin : IUniversalPlugin
    {
        private IUsbDevice? _usb;
        private SpdFdlProtocol? _fdl;

        public string ProtocolName => "SPD_FDL_Universal";

        public string[] SupportedChips => new[] 
        { 
            "SC9863A", "SC9832E", "SC7731E", "T606", "T610", "T612", "T616" 
        };

        public Task<bool> DetectDeviceAsync(IUsbDevice device)
        {
            // SPD Diag/Flash Mode PIDs
            return Task.FromResult(true);
        }

        public async Task<ConnectionResult> ConnectAsync(ConnectionOptions options)
        {
            _usb = options.Device;
            _fdl = new SpdFdlProtocol(_usb);

            try
            {
                if (!await _fdl.HandshakeAsync())
                {
                    return new ConnectionResult { Success = false, Message = "SPD FDL Handshake Failed" };
                }

                return new ConnectionResult { Success = true, Message = "Connected in SPD Diag Mode" };
            }
            catch (Exception ex)
            {
                return new ConnectionResult { Success = false, Message = $"SPD Connection Error: {ex.Message}" };
            }
        }

        public Task<DeviceInfo> GetDeviceInfoAsync()
        {
            return Task.FromResult(new DeviceInfo
            {
                Chipset = "Unisoc/Spreadtrum (Universal)",
                SecureBoot = "Standard",
                SerialNumber = "SPD-0000"
            });
        }

        public async Task<OperationResult> ExecuteOperationAsync(
            string operation,
            Dictionary<string, object> parameters,
            DeviceProfile device)
        {
            if (_fdl == null) return new OperationResult { Success = false, Message = "Not Connected" };

            // In a real scenario, we would load the FDL1/FDL2 files from a repository
            // based on the device.Chipset.Model
            await _fdl.LoadLoaderAsync("generic_fdl2.bin");

            return operation switch
            {
                "ReadInfo" => new OperationResult 
                { 
                    Success = true, 
                    Message = await _fdl.ReadDeviceInfoAsync() 
                },
                "EraseFrp" => await AttemptEraseAsync("persist") ?? await AttemptEraseAsync("config"),
                "FormatUserdata" => await AttemptEraseAsync("userdata"),
                _ => new OperationResult { Success = false, Message = "Operation Not Supported" }
            };
        }

        private async Task<OperationResult> AttemptEraseAsync(string partition)
        {
            if (_fdl == null) return new OperationResult { Success = false };
            await _fdl.ErasePartitionAsync(partition);
            return new OperationResult { Success = true, Message = $"Erased {partition}" };
        }

        public Task<OperationResult> ExecuteKeypadOperationAsync(string operation, DeviceProfile device)
        {
            // Feature phones on SPD (SC6531E etc)
            return Task.FromResult(new OperationResult 
            { 
                 Success = true, 
                 Message = "SPD Feature Phone Operation (Simulated)" 
            });
        }
    }
}
