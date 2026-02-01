using System;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Core.Architecture
{
    public enum SafetyLevel
    {
        ReadOnly,
        Safe,      // Information gathering only
        Normal,    // Standard operations (Flash, Backup)
        Destructive // Partition formatting, FRP reset
    }

    public class SafetyInterlock
    {
        public static async Task<bool> PreFlightCheckAsync(PluginDeviceContext ctx, IOperationHandler handler)
        {
            // 1. Detect FRP State first
            bool frpEnabled = await DetectFrpStateAsync(ctx);
            
            // 2. Identify Safety Level of the operation
            var level = IdentifySafetyLevel(handler);

            if (level == SafetyLevel.Destructive && frpEnabled)
            {
                // Warn about FRP lock
                // In production, this would trigger a UI prompt
                return false; 
            }

            return true; // Proceed with caution
        }

        private static Task<bool> DetectFrpStateAsync(PluginDeviceContext ctx)
        {
            // Implementation varies by protocol
            // Usually involves reading specific partition flags or persistent properties
            return Task.FromResult(true); // Placeholder
        }

        private static SafetyLevel IdentifySafetyLevel(IOperationHandler handler)
        {
            if (handler.OperationName.Contains("Format") || handler.OperationName.Contains("Reset"))
                return SafetyLevel.Destructive;
                
            if (handler.OperationName.Contains("Read") || handler.OperationName.Contains("Info"))
                return SafetyLevel.Safe;

            return SafetyLevel.Normal;
        }
    }
}
