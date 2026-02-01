using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Simulation;

namespace DeepEyeUnlocker.Core.HIL
{
    public enum FaultType
    {
        None,
        PacketDrop,
        PacketDelay,
        PacketCorruption
    }

    public interface IHardwareFaultInjector
    {
        void SetFault(FaultType fault, int stepIndex, object? parameter = null);
        void ClearFaults();
    }

    public class HardwareFaultInjector : IHardwareFaultInjector
    {
        private readonly ScenarioUsbDevice _usb;
        private readonly string _deviceId;
        private FaultType _activeFault = FaultType.None;
        private int _targetStepIndex = -1;
        private object? _faultParam;

        public HardwareFaultInjector(ScenarioUsbDevice usb, string deviceId)
        {
            _usb = usb;
            _deviceId = deviceId;
            
            // Inject into the hook
            _usb.MutationHook = ProcessMutation;
        }

        public void SetFault(FaultType fault, int stepIndex, object? parameter = null)
        {
            if (!FaultInjectionSafety.IsApproved(_deviceId))
            {
                throw new InvalidOperationException($"Device {_deviceId} is not approved for fault injection.");
            }

            _activeFault = fault;
            _targetStepIndex = stepIndex;
            _faultParam = parameter;
        }

        public void ClearFaults()
        {
            _activeFault = FaultType.None;
            _targetStepIndex = -1;
        }

        private byte[] ProcessMutation(byte[] data)
        {
            // This is called by ScenarioUsbDevice before returning data to the host.
            // We need to know which step we are on. 
            // ScenarioUsbDevice.Result.LastStepIndex might be useful.
            
            int currentStep = _usb.Result.LastStepIndex;
            
            if (currentStep == _targetStepIndex)
            {
                switch (_activeFault)
                {
                    case FaultType.PacketDrop:
                        return Array.Empty<byte>(); // Drop the packet
                    
                    case FaultType.PacketDelay:
                        int delay = _faultParam is int d ? d : 1000;
                        Task.Delay(delay).Wait();
                        return data;

                    case FaultType.PacketCorruption:
                        byte[] corrupted = (byte[])data.Clone();
                        if (corrupted.Length > 0) corrupted[0] ^= 0xFF; // Simple flip
                        return corrupted;
                }
            }

            return data;
        }
    }
}
