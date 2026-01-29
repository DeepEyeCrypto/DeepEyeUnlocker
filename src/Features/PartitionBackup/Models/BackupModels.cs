using System;
using System.Collections.Generic;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Features.PartitionBackup.Models
{
    public class BackupJob
    {
        public string Id { get; set; } = Guid.NewGuid().ToString("N");
        public string DeviceSerial { get; set; } = "";
        public List<PartitionInfo> TargetPartitions { get; set; } = new();
        public string DestinationPath { get; set; } = "";
        public bool Encrypt { get; set; } = true;
        public bool Compress { get; set; } = true;
        
        public BackupStatus Status { get; set; } = BackupStatus.Pending;
        public int ProgressPercentage { get; set; }
        public string CurrentPartition { get; set; } = "";
        public DateTime StartTime { get; set; }
        public DateTime? EndTime { get; set; }
    }

    public enum BackupStatus
    {
        Pending,
        Running,
        Completed,
        Failed,
        Cancelled
    }

    public class BackupManifest
    {
        public string Version { get; set; } = "1.0";
        public string DeviceSerial { get; set; } = "";
        public DateTime Timestamp { get; set; }
        public List<PartitionBackupEntry> Entries { get; set; } = new();
    }

    public class PartitionBackupEntry
    {
        public string PartitionName { get; set; } = "";
        public string FileName { get; set; } = "";
        public ulong OriginalSize { get; set; }
        public string Sha256Hash { get; set; } = "";
        public bool IsEncrypted { get; set; }
        public bool IsCompressed { get; set; }
    }
}
