using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.Features.PartitionBackup.Engine
{
    /// <summary>
    /// Unifies partition enumeration from various sources (ADB, GPT/EDL).
    /// </summary>
    public class PartitionMetadataCollector
    {
        private readonly IAdbClient _adb;

        public PartitionMetadataCollector(IAdbClient adb)
        {
            _adb = adb;
        }

        /// <summary>
        /// Attempts to gather the partition list via ADB (requires root for full accuracy).
        /// </summary>
        public async Task<List<PartitionInfo>> GetPartitionsViaAdbAsync(CancellationToken ct = default)
        {
            var partitions = new List<PartitionInfo>();
            
            try
            {
                // Method 1: /proc/partitions (kernel view)
                var procOutput = await _adb.ExecuteShellAsync("cat /proc/partitions", ct);
                if (!string.IsNullOrEmpty(procOutput))
                {
                    partitions.AddRange(ParseProcPartitions(procOutput));
                }

                // Method 2: Enhance with names from /dev/block/by-name
                var byName = await _adb.ExecuteShellAsync("ls -l /dev/block/by-name", ct);
                if (!string.IsNullOrEmpty(byName))
                {
                    MapNamesToPartitions(partitions, byName);
                }

                // Method 3: Fallback/Supplement with 'sgdisk' if available (requires root)
                if (partitions.Count == 0 || partitions.All(p => string.IsNullOrEmpty(p.Name)))
                {
                    var sgdisk = await _adb.ExecuteShellAsync("su -c 'sgdisk -p /dev/block/mmcblk0' 2>/dev/null", ct);
                    if (!string.IsNullOrEmpty(sgdisk))
                    {
                        // Logic to parse sgdisk output if needed
                    }
                }
            }
            catch (Exception ex)
            {
                Core.Logger.Error(ex, "Failed to collect partition metadata via ADB");
            }

            return partitions.OrderBy(p => p.Index).ToList();
        }

        private List<PartitionInfo> ParseProcPartitions(string output)
        {
            var list = new List<PartitionInfo>();
            var lines = output.Split('\n', StringSplitOptions.RemoveEmptyEntries);
            
            // Skip header
            foreach (var line in lines.Skip(1))
            {
                var match = Regex.Match(line.Trim(), @"(\d+)\s+(\d+)\s+(\d+)\s+(\w+)");
                if (match.Success)
                {
                    var name = match.Groups[4].Value;
                    if (ulong.TryParse(match.Groups[3].Value, out ulong blocks))
                    {
                        list.Add(new PartitionInfo
                        {
                            Name = name,
                            SizeInBytes = blocks * 1024, // /proc/partitions is usually 1KB blocks
                            FileSystem = name.StartsWith("loop") ? "Loopback" : "Block Device"
                        });
                    }
                }
            }
            return list;
        }

        private void MapNamesToPartitions(List<PartitionInfo> list, string lsOutput)
        {
            var lines = lsOutput.Split('\n', StringSplitOptions.RemoveEmptyEntries);
            foreach (var line in lines)
            {
                // Example: boot -> /dev/block/mmcblk0p21
                var parts = line.Split("->");
                if (parts.Length == 2)
                {
                    var alias = parts[0].Trim().Split(' ').Last();
                    var target = parts[1].Trim().Split('/').Last();

                    var partition = list.FirstOrDefault(p => p.Name == target);
                    if (partition != null)
                    {
                        partition.Name = alias; // Map the friendly name
                    }
                }
            }
        }
    }
}
