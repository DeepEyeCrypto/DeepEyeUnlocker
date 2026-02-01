using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Protocols.Usb;

namespace DeepEyeUnlocker.Core.Architecture
{
    public class ConnectionOptions
    {
        public IUsbDevice Device { get; set; } = null!;
        public Dictionary<string, object> Parameters { get; set; } = new();
    }

    public interface IProtocolPlugin
    {
        string ProtocolName { get; } // "QualcommEDL", "MTKPreloader", "SamsungOdin"
        string[] SupportedChips { get; }
        Task<bool> DetectDeviceAsync(IUsbDevice device);
        Task<ConnectionResult> ConnectAsync(ConnectionOptions options);
        Task<DeviceInfo> GetDeviceInfoAsync();
    }

    public interface IOperationHandler
    {
        string OperationName { get; } // "Flash", "Format", "FrpBypass"
        string TargetProtocol { get; } // The protocol this handler works with
        Task<bool> ValidatePrerequisitesAsync(DeviceContext ctx);
        Task<OperationResult> ExecuteAsync(DeviceContext ctx, Dictionary<string, object> parameters);
    }

    public class DeviceContext
    {
        public string DeviceId { get; set; } = string.Empty;
        public IProtocolPlugin ActiveProtocol { get; set; } = null!;
        public IUsbDevice UsbLink { get; set; } = null!;
        public DeviceInfo Info { get; set; } = null!;
        public Dictionary<string, object> State { get; } = new();
    }

    public class ConnectionResult
    {
        public bool Success { get; set; }
        public string Message { get; set; } = string.Empty;
        public Dictionary<string, string> Properties { get; set; } = new();
    }

    public class OperationResult
    {
        public bool Success { get; set; }
        public string Message { get; set; } = string.Empty;
        public object? Data { get; set; }
        public List<string> Logs { get; } = new();
    }

    public class DeviceInfo
    {
        public string Chipset { get; set; } = string.Empty;
        public string SecureBoot { get; set; } = "Unknown";
        public string SerialNumber { get; set; } = string.Empty;
        public Dictionary<string, string> ExtendedProperties { get; set; } = new();
    }
}
