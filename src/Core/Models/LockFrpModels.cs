using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.Models
{
    #region FRP Models

    public enum FrpLockStatus
    {
        Unknown,
        Locked,
        Unlocked,
        PartiallyCleared,
        Error
    }

    public enum FrpDetectionMethod
    {
        Unknown,
        FastbootGetvar,
        AdbSettings,
        RecoveryLog,
        PartitionAnalysis,
        EdlPartitionRead,
        Heuristic
    }

    public class FrpStatus
    {
        public FrpLockStatus Status { get; set; } = FrpLockStatus.Unknown;
        public bool IsGoogleAccountBound { get; set; }
        public string? AccountHint { get; set; }
        public OemFrpInfo? OemInfo { get; set; }
        public FrpDetectionMethod DetectionMethod { get; set; }
        public bool AllowOemUnlock { get; set; }
        public string? FrpPartitionName { get; set; }
        public ulong FrpPartitionSize { get; set; }
        public bool PartitionHasData { get; set; }
        public string? Notes { get; set; }
        public List<string> RecommendedActions { get; set; } = new();
        public DateTime DetectedAt { get; set; } = DateTime.UtcNow;
    }

    public class OemFrpInfo
    {
        public string OemName { get; set; } = "";
        public bool HasOemAccountLock { get; set; }
        public string? OemAccountType { get; set; }
        public bool RequiresServerVerification { get; set; }
        public string? OfficialUnlockUrl { get; set; }
    }

    public enum FrpBypassMethod
    {
        PartitionErase,
        PartitionOverwrite,
        PersistClear,
        UserdataFormat,
        AdbBypass,
        FastbootUnlock,
        ServiceMode
    }

    public class FrpBypassResult
    {
        public bool Success { get; set; }
        public FrpBypassMethod MethodUsed { get; set; }
        public string Message { get; set; } = "";
        public bool RequiresReboot { get; set; }
        public string? AdditionalSteps { get; set; }
    }

    #endregion

    #region Screen Lock Models

    public enum LockType
    {
        Unknown,
        None,
        Pattern,
        PIN,
        Password,
        BiometricOnly,
        SmartLock
    }

    public enum LockSecurityLevel
    {
        Unknown,
        Legacy,           // Android 4-6
        Gatekeeper,       // Android 7-9
        GatekeeperWeaver  // Android 10+
    }

    public class ScreenLockStatus
    {
        public bool? IsLockEnabled { get; set; }
        public LockType LockType { get; set; } = LockType.Unknown;
        public string? AndroidVersion { get; set; }
        public LockSecurityLevel SecurityLevel { get; set; } = LockSecurityLevel.Unknown;
        public bool CanRecoverDataWithoutCredential { get; set; }
        public List<string> Warnings { get; set; } = new();
        public List<RecoveryOption> AvailableOptions { get; set; } = new();
    }

    public class RecoveryOption
    {
        public string Name { get; set; } = "";
        public string Description { get; set; } = "";
        public bool ResultsInDataLoss { get; set; }
        public bool RequiresExpertMode { get; set; }
        public RecoveryOptionType Type { get; set; }
    }

    public enum RecoveryOptionType
    {
        GoogleFindMyDevice,
        OemRemoteUnlock,
        FactoryResetFastboot,
        FactoryResetRecovery,
        AdbBackupFirst,
        ServiceCenterReferral
    }

    #endregion

    #region Combined Diagnostics

    public class LockFrpDiagnostics
    {
        public DeviceContext Device { get; set; } = null!;
        public FrpStatus FrpStatus { get; set; } = new();
        public ScreenLockStatus LockStatus { get; set; } = new();
        public DateTime ScanTime { get; set; } = DateTime.UtcNow;
        public string Summary => GenerateSummary();

        private string GenerateSummary()
        {
            var frp = FrpStatus.Status == FrpLockStatus.Locked ? "🔴 FRP Active" : "🟢 FRP Clear";
            var lockStr = LockStatus.IsLockEnabled == true ? "🔒 Locked" : "🔓 Unknown";
            return $"{frp} | {lockStr} | {Device.Brand} {Device.Model}";
        }
    }

    #endregion
}
