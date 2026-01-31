using System;

namespace DeepEyeUnlocker.Features.CloudSync.Models
{
    public class CloudBackupStatus
    {
        public string BackupId { get; set; } = string.Empty;
        public string DeviceSerial { get; set; } = string.Empty;
        public string PartitionName { get; set; } = string.Empty;
        public long SizeBytes { get; set; }
        public DateTime UploadDate { get; set; }
        public string Status { get; set; } = "Pending"; // Pending, Uploading, Completed, Failed
        public double Progress { get; set; }
        public string StorageUrl { get; set; } = string.Empty;
    }

    public class CloudSyncSettings
    {
        public bool EnableAutoSync { get; set; } = false;
        public string ApiEndpoint { get; set; } = "https://cloud.deepeye-unlocker.com/api/v1";
        public string ApiKey { get; set; } = string.Empty;
        public bool EncryptBeforeUpload { get; set; } = true;
    }
}
