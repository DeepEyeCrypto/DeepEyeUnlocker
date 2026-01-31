using System.Collections.Generic;
using System;

namespace DeepEyeUnlocker.Features.PartitionBackup.Models
{
    public class BackupMetadata
    {
        public string Version { get; set; } = "1.0.0";
        public string DeviceSerialNumber { get; set; } = string.Empty;
        public string DeviceModel { get; set; } = string.Empty;
        public DateTime Timestamp { get; set; } = DateTime.UtcNow;
        public List<PartitionBackupInfo> Partitions { get; set; } = new();
        public string Salt { get; set; } = string.Empty;
        public string Iv { get; set; } = string.Empty;
    }

    public class PartitionBackupInfo
    {
        public string Name { get; set; } = string.Empty;
        public long OriginalSize { get; set; }
        public long CompressedSize { get; set; }
        public string Sha256Hash { get; set; } = string.Empty;
        public bool IsCompressed { get; set; } = true;
        public bool IsEncrypted { get; set; } = true;
    }
}
