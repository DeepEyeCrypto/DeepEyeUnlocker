using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Architecture;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Services;
using DeepEyeUnlocker.Protocols.Usb;

namespace DeepEyeUnlocker.Protocols.ModelSpecific
{
    public class ModelSpecificPlugin : IProtocolPlugin
    {
        private readonly CloudProfileService _cloudService;
        private readonly List<IModelSpecificHandler> _handlers = new();
        private IUsbDevice? _usb;

        public string ProtocolName => "Model_Specific_Precision";

        // Supports any chip capable of running high-level handlers
        public string[] SupportedChips => new[] { "Universal_Logic" };

        public ModelSpecificPlugin(CloudProfileService cloudService)
        {
            _cloudService = cloudService;
        }

        public void RegisterHandler(IModelSpecificHandler handler)
        {
            _handlers.Add(handler);
        }

        public async Task<bool> DetectDeviceAsync(IUsbDevice device)
        {
            // Logic-only plugin, usually selected by Router
            return await Task.FromResult(false);
        }

        public async Task<ConnectionResult> ConnectAsync(ConnectionOptions options)
        {
            _usb = options.Device;
            return await Task.FromResult(new ConnectionResult 
            { 
                Success = true, 
                Message = "Model Specific Logic Layer Active" 
            });
        }

        public Task<DeviceInfo> GetDeviceInfoAsync()
        {
            return Task.FromResult(new DeviceInfo 
            { 
                Chipset = "Logic", 
                SecureBoot = "Managed", 
                SerialNumber = "LOGIC-00" 
            });
        }

        public async Task<OperationResult> ExecuteOperationAsync(
             string operation,
             DeviceProfile device,
             Dictionary<string, object> parameters)
        {
             // 1. Find Handler
             var handler = _handlers.FirstOrDefault(h => h.TargetModel == device.ModelNumber);
             
             // If not found, try to sync from cloud
             if (handler == null)
             {
                 var profile = await _cloudService.GetProfileAsync(device.ModelNumber);
                 if (profile != null) 
                 {
                     // In a real app we'd download the handler assembly/script here
                     return new OperationResult { Success = false, Message = "Profile found in cloud but handler code missing (Requires Dynamic Loading)" };
                 }
                 return new OperationResult { Success = false, Message = $"No precision handler for {device.ModelNumber}" };
             }

             // 2. Execute
             return await handler.ExecuteAsync(operation, device, parameters, this);
        }
    }
}
