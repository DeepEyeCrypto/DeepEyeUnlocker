using System;
using System.Collections.Concurrent;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.HIL
{
    public class FaultInjectionSafety
    {
        private static readonly ConcurrentBag<string> ApprovedDeviceIds = new();
        private static bool _emergencyStopActive = false;

        public static void ApproveDevice(string deviceId)
        {
            if (!ApprovedDeviceIds.Contains(deviceId))
            {
                ApprovedDeviceIds.Add(deviceId);
            }
        }

        public static bool IsApproved(string deviceId)
        {
            if (_emergencyStopActive) return false;
            return ApprovedDeviceIds.Contains(deviceId);
        }

        public static void TriggerEmergencyStop()
        {
            _emergencyStopActive = true;
        }

        public static void ResetEmergencyStop()
        {
            _emergencyStopActive = false;
        }

        public static bool IsSafetyCheckPassed(string deviceId, bool isFrpEnabled)
        {
            if (_emergencyStopActive) return false;
            
            // Safety rule: never inject faults if FRP is enabled (potential data loss/protection)
            if (isFrpEnabled) return false;

            return ApprovedDeviceIds.Contains(deviceId);
        }
    }
}
