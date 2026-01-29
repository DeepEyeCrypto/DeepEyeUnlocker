using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.Models
{
    public enum BackupStatus
    {
        Pending,
        InProgress,
        Completed,
        Failed,
        Cancelled
    }

    public class PartitionBackupJob
    {
        public string JobId { get; set; } = Guid.NewGuid().ToString("N");
        public string DeviceSerial { get; set; } = "";
        public List<string> Partitions { get; set; } = new();
        public string OutputDirectory { get; set; } = "";
        public bool Encrypt { get; set; }
        public string? EncryptionKey { get; set; }
        public bool Compres { get; set; }
        public DateTime StartTime { get; set; } = DateTime.UtcNow;
        public DateTime? EndTime { get; set; }
        public BackupStatus Status { get; set; } = BackupStatus.Pending;
        public string? ErrorMessage { get; set; }
        
        public List<PartitionBackupEntry> Entries { get; set; } = new();
    }

    public class PartitionBackupEntry
    {
        public string PartitionName { get; set; } = "";
        public long SizeBytes { get; set; }
        public long TransferredBytes { get; set; }
        public string? Sha256 { get; set; }
        public bool Success { get; set; }
        public string? FilePath { get; set; }
        public double Progress => SizeBytes > 0 ? (double)TransferredBytes / SizeBytes : 0;
    }

    public class BackupManifest
    {
        public string ToolVersion { get; set; } = "1.3.0";
        public string DeviceModel { get; set; } = "";
        public string DeviceSerial { get; set; } = "";
        public DateTime Timestamp { get; set; } = DateTime.UtcNow;
        public List<PartitionBackupEntry> Backups { get; set; } = new();
        public bool IsEncrypted { get; set; }
    }
}
