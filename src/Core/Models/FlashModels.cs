using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.Models
{
    public enum FirmwareType
    {
        Unknown,
        QualcommFirehose,    // rawprogram0.xml + patch0.xml
        MediaTekScatter,     // MTxxxx_Android_scatter.txt
        SamsungOdin,        // BL/AP/CP/CSC/HOME_CSC (.tar.md5)
        FastbootImages,     // images/ folder with flash-all script
        AdbSideloadZip,     // update.zip
        SingleImage         // boot.img, recovery.img etc
    }

    public class FlashPartitionInfo
    {
        public string PartitionName { get; set; } = "";
        public string FileName { get; set; } = "";
        public string? FilePath { get; set; }
        public long Size { get; set; }
        public long StartSector { get; set; }
        public int PhysicalPartition { get; set; }
        public string? Sha256 { get; set; }
        public bool IsSelected { get; set; } = true;
        public bool IsCritical { get; set; } // true for bootloader, efs, modem
        public int Order { get; set; }
    }

    public class FirmwareManifest
    {
        public string FirmwareName { get; set; } = "";
        public FirmwareType Type { get; set; } = FirmwareType.Unknown;
        public string? BaseDirectory { get; set; }
        public List<FlashPartitionInfo> Partitions { get; set; } = new();
        public Dictionary<string, string> Metadata { get; set; } = new(StringComparer.OrdinalIgnoreCase);
        
        public long TotalSize => Partitions.FindAll(p => p.IsSelected).ConvertAll(p => p.Size).Sum();
    }

    public class FlashResult
    {
        public bool Success { get; set; }
        public string Message { get; set; } = "";
        public List<string> FlashedPartitions { get; set; } = new();
        public List<string> FailedPartitions { get; set; } = new();
        public TimeSpan Duration { get; set; }
    }
}
