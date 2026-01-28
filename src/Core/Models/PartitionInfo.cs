using System;

namespace DeepEyeUnlocker.Core.Models
{
    /// <summary>
    /// Canonical Partition Information for all protocols and layers.
    /// </summary>
    public class PartitionInfo
    {
        public string Name { get; set; } = "";
        public int Index { get; set; }
        public ulong StartLba { get; set; }
        public ulong EndLba { get; set; }
        public ulong SizeInBytes { get; set; }
        public string? FileSystem { get; set; }
        public bool IsCritical { get; set; } // true for bootloader, efs, modem, persistent
        public ulong Attributes { get; set; }
        public Guid TypeGuid { get; set; }
        public Guid UniqueGuid { get; set; }
        public string TypeName { get; set; } = "Unknown";

        public string SizeFormatted => FormatSize(SizeInBytes);

        private static string FormatSize(ulong bytes)
        {
            string[] sizes = { "B", "KB", "MB", "GB", "TB" };
            double len = bytes;
            int order = 0;
            while (len >= 1024 && order < sizes.Length - 1)
            {
                order++;
                len = len / 1024;
            }
            return $"{len:0.##} {sizes[order]}";
        }
    }
}
