using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.Models
{
    /// <summary>
    /// EDL capability classification for a device
    /// </summary>
    public enum EdlCapability
    {
        /// <summary>Device supports adb reboot edl or fastboot oem edl</summary>
        SOFTWARE_DIRECT_SUPPORTED,
        
        /// <summary>Older firmware supports, newer blocks – check version</summary>
        SOFTWARE_RESTRICTED,
        
        /// <summary>All software paths blocked, only test-point works</summary>
        HARDWARE_ONLY,
        
        /// <summary>No data available for this model</summary>
        UNKNOWN
    }

    /// <summary>
    /// Method used to attempt EDL reboot
    /// </summary>
    public enum EdlAttemptMethod
    {
        None,
        AdbRebootEdl,
        FastbootOemEdl,
        FastbootRebootEdl
    }

    /// <summary>
    /// Result of an EDL reboot attempt
    /// </summary>
    public class EdlResult
    {
        public bool Success { get; set; }
        public string? FailureReason { get; set; }
        public EdlAttemptMethod MethodUsed { get; set; }
        public string Log { get; set; } = "";
        public TimeSpan ElapsedTime { get; set; }
        
        public static EdlResult Ok(EdlAttemptMethod method, string log = "") => 
            new() { Success = true, MethodUsed = method, Log = log };
            
        public static EdlResult Fail(string reason, EdlAttemptMethod method, string log = "") => 
            new() { Success = false, FailureReason = reason, MethodUsed = method, Log = log };
    }

    /// <summary>
    /// EDL profile for a specific device model
    /// </summary>
    public class EdlProfile
    {
        public string Brand { get; set; } = "";
        public string Model { get; set; } = "";
        public string Codename { get; set; } = "";
        public string SoC { get; set; } = "";
        
        public bool SupportsAdbRebootEdl { get; set; }
        public bool SupportsFastbootOemEdl { get; set; }
        public bool RequiresAuthTool { get; set; }
        public bool RequiresTestPoint { get; set; }
        
        public string? AuthToolName { get; set; }
        public string? TestPointDiagramUrl { get; set; }
        public string? Notes { get; set; }
        
        public EdlCapability Capability => 
            SupportsAdbRebootEdl || SupportsFastbootOemEdl 
                ? EdlCapability.SOFTWARE_DIRECT_SUPPORTED
                : RequiresTestPoint 
                    ? EdlCapability.HARDWARE_ONLY 
                    : EdlCapability.UNKNOWN;
    }

    /// <summary>
    /// Test point information for hardware EDL entry
    /// </summary>
    public class TestPointInfo
    {
        public string DeviceModel { get; set; } = "";
        public string Description { get; set; } = "";
        public string DiagramUrl { get; set; } = "";
        public string ToolsNeeded { get; set; } = "";
        public string Difficulty { get; set; } = "Moderate";
        public bool RequiresBatteryDisconnect { get; set; } = true;
    }

    /// <summary>
    /// EDL security policy configuration
    /// </summary>
    public class EdlSecurityPolicy
    {
        public bool AllowExploitMethods { get; set; } = false;
        public bool RequireExpertMode { get; set; } = true;
        public bool LogAllAttempts { get; set; } = true;
        public bool RequireKnownProfile { get; set; } = false;
        public bool RequireConfirmation { get; set; } = true;
        public int MaxAttempts { get; set; } = 3;
    }

    /// <summary>
    /// Retry policy for EDL operations
    /// </summary>
    public class EdlRetryPolicy
    {
        public int InitialDelayMs { get; set; } = 3000;
        public int ScanIntervalMs { get; set; } = 1000;
        public int MaxWaitSeconds { get; set; } = 15;
        public int MaxRetries { get; set; } = 2;
    }
}
