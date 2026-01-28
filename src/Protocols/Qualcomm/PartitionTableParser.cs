using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Protocols.Qualcomm
{
    #region Enums and Constants

    /// <summary>
    /// Partition table type
    /// </summary>
    public enum PartitionTableType
    {
        Unknown,
        MBR,
        GPT
    }

    /// <summary>
    /// eMMC/UFS partition type identifiers
    /// </summary>
    public static class PartitionTypeGuids
    {
        public static readonly Guid Unused = Guid.Empty;
        public static readonly Guid EFISystem = new("C12A7328-F81F-11D2-BA4B-00A0C93EC93B");
        public static readonly Guid MicrosoftBasicData = new("EBD0A0A2-B9E5-4433-87C0-68B6B72699C7");
        public static readonly Guid AndroidBoot = new("20117F86-E985-4357-B9EE-374BC1D8487D");
        public static readonly Guid AndroidRecovery = new("495FB8F7-A330-4F64-8A73-E5F51E3F2B87");
        public static readonly Guid AndroidSystem = new("38F428E6-D326-425D-9140-6E0EA133647C");
        public static readonly Guid AndroidVendor = new("4A7D5BEF-D4E7-4F1B-8F76-6C8E324F39C7");
        public static readonly Guid AndroidUserdata = new("1B81E7E6-F50D-419B-A739-2AEEF8DA3335");
        public static readonly Guid AndroidCache = new("6C95E238-E343-4BA8-B489-8681ED22AD0B");
        public static readonly Guid AndroidMisc = new("EF32A33B-A409-486C-9141-9FFB711F6266");
        public static readonly Guid QualcommSBL1 = new("DEA0BA2C-CBDD-4805-B4F9-F428251C3E98");
        public static readonly Guid QualcommRPM = new("098DF793-D712-413D-9D4E-89D711772228");
        public static readonly Guid QualcommTZ = new("A053AA7F-40B8-4B1C-BA08-2F68AC71A4F4");
        public static readonly Guid QualcommModem = new("638FF8E2-22C9-E33B-8F5D-0E81686A68CB");
    }

    #endregion

    #region Data Structures

    /// <summary>
    /// GPT Header structure (92 bytes at LBA 1)
    /// </summary>
    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    public struct GptHeader
    {
        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 8)]
        public byte[] Signature;          // "EFI PART"
        public uint Revision;
        public uint HeaderSize;
        public uint HeaderCrc32;
        public uint Reserved;
        public ulong CurrentLba;
        public ulong BackupLba;
        public ulong FirstUsableLba;
        public ulong LastUsableLba;
        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 16)]
        public byte[] DiskGuid;
        public ulong PartitionEntryLba;
        public uint NumberOfPartitionEntries;
        public uint SizeOfPartitionEntry;
        public uint PartitionEntryArrayCrc32;
    }

    /// <summary>
    /// GPT Partition Entry (128 bytes each)
    /// </summary>
    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    public struct GptPartitionEntry
    {
        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 16)]
        public byte[] TypeGuid;
        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 16)]
        public byte[] UniqueGuid;
        public ulong FirstLba;
        public ulong LastLba;
        public ulong Attributes;
        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 72)]
        public byte[] Name;  // UTF-16LE
    }

    /// <summary>
    /// MBR Partition Entry (16 bytes each)
    /// </summary>
    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    public struct MbrPartitionEntry
    {
        public byte BootIndicator;
        public byte StartHead;
        public byte StartSectorCylinder;
        public byte StartCylinderHigh;
        public byte SystemId;
        public byte EndHead;
        public byte EndSectorCylinder;
        public byte EndCylinderHigh;
        public uint RelativeSectors;
        public uint TotalSectors;
    }

    #endregion


    /// <summary>
    /// Complete partition table information
    /// </summary>
    public class PartitionTable
    {
        public PartitionTableType Type { get; set; }
        public int SectorSize { get; set; } = 512;
        public Guid DiskGuid { get; set; }
        public List<PartitionInfo> Partitions { get; set; } = new();
        public ulong FirstUsableLba { get; set; }
        public ulong LastUsableLba { get; set; }
        public ulong TotalSectors { get; set; }
        public string TotalSizeFormatted => FormatSize(TotalSectors * (ulong)SectorSize);
        public bool IsValid { get; set; }
        public string? ParseError { get; set; }

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

    /// <summary>
    /// GPT/MBR Partition Table Parser
    /// </summary>
    public class PartitionTableParser
    {
        private const int SECTOR_SIZE = 512;
        private const string GPT_SIGNATURE = "EFI PART";
        private const ushort MBR_SIGNATURE = 0xAA55;
        private const byte GPT_PROTECTIVE_MBR = 0xEE;

        // Known partition type mappings
        private static readonly Dictionary<Guid, string> KnownPartitionTypes = new()
        {
            { PartitionTypeGuids.EFISystem, "EFI System" },
            { PartitionTypeGuids.MicrosoftBasicData, "Basic Data" },
            { PartitionTypeGuids.AndroidBoot, "Android Boot" },
            { PartitionTypeGuids.AndroidRecovery, "Android Recovery" },
            { PartitionTypeGuids.AndroidSystem, "Android System" },
            { PartitionTypeGuids.AndroidVendor, "Android Vendor" },
            { PartitionTypeGuids.AndroidUserdata, "Android Userdata" },
            { PartitionTypeGuids.AndroidCache, "Android Cache" },
            { PartitionTypeGuids.AndroidMisc, "Android Misc" },
            { PartitionTypeGuids.QualcommSBL1, "Qualcomm SBL1" },
            { PartitionTypeGuids.QualcommRPM, "Qualcomm RPM" },
            { PartitionTypeGuids.QualcommTZ, "Qualcomm TrustZone" },
            { PartitionTypeGuids.QualcommModem, "Qualcomm Modem" },
        };

        /// <summary>
        /// Parse partition table from raw sector data
        /// </summary>
        public PartitionTable Parse(byte[] data, int sectorSize = SECTOR_SIZE)
        {
            var table = new PartitionTable { SectorSize = sectorSize };

            if (data == null || data.Length < sectorSize * 2)
            {
                table.IsValid = false;
                table.ParseError = "Insufficient data for partition table parsing";
                return table;
            }

            try
            {
                // Check for GPT (LBA 1)
                if (data.Length >= sectorSize * 2)
                {
                    var gptSignature = Encoding.ASCII.GetString(data, sectorSize, 8);
                    if (gptSignature == GPT_SIGNATURE)
                    {
                        return ParseGpt(data, sectorSize);
                    }
                }

                // Check for MBR (LBA 0)
                var mbrSignature = BitConverter.ToUInt16(data, 510);
                if (mbrSignature == MBR_SIGNATURE)
                {
                    // Check if it's a protective MBR for GPT
                    if (data[0x1C2] == GPT_PROTECTIVE_MBR)
                    {
                        table.IsValid = false;
                        table.ParseError = "Protective MBR found but GPT header missing or corrupt";
                        return table;
                    }

                    return ParseMbr(data, sectorSize);
                }

                table.IsValid = false;
                table.ParseError = "No valid partition table signature found";
            }
            catch (Exception ex)
            {
                table.IsValid = false;
                table.ParseError = $"Parse error: {ex.Message}";
                Logger.Error(ex, "Partition table parsing failed");
            }

            return table;
        }

        /// <summary>
        /// Parse partition table from a file
        /// </summary>
        public PartitionTable ParseFromFile(string filePath, int sectorSize = SECTOR_SIZE)
        {
            if (!File.Exists(filePath))
            {
                return new PartitionTable
                {
                    IsValid = false,
                    ParseError = "File not found"
                };
            }

            // Read enough sectors for GPT (header + entries)
            var fileInfo = new FileInfo(filePath);
            var bytesToRead = Math.Min((int)fileInfo.Length, sectorSize * 34); // MBR + GPT header + 32 entry sectors

            var data = new byte[bytesToRead];
            using (var fs = File.OpenRead(filePath))
            {
                fs.Read(data, 0, bytesToRead);
            }

            return Parse(data, sectorSize);
        }

        /// <summary>
        /// Parse GPT partition table
        /// </summary>
        private PartitionTable ParseGpt(byte[] data, int sectorSize)
        {
            var table = new PartitionTable
            {
                Type = PartitionTableType.GPT,
                SectorSize = sectorSize
            };

            // Parse GPT header at LBA 1
            var headerOffset = sectorSize;
            var header = ParseGptHeader(data, headerOffset);

            if (header == null)
            {
                table.IsValid = false;
                table.ParseError = "Invalid GPT header";
                return table;
            }

            table.DiskGuid = new Guid(header.Value.DiskGuid);
            table.FirstUsableLba = header.Value.FirstUsableLba;
            table.LastUsableLba = header.Value.LastUsableLba;
            table.TotalSectors = header.Value.LastUsableLba - header.Value.FirstUsableLba + 1;

            // Parse partition entries
            var entryOffset = (int)(header.Value.PartitionEntryLba * (ulong)sectorSize);
            var entrySize = (int)header.Value.SizeOfPartitionEntry;
            var entryCount = (int)header.Value.NumberOfPartitionEntries;

            Logger.Info($"GPT: {entryCount} partition entries at LBA {header.Value.PartitionEntryLba}", "PARTITION");

            for (int i = 0; i < entryCount; i++)
            {
                var offset = entryOffset + (i * entrySize);
                if (offset + entrySize > data.Length) break;

                var entry = ParseGptEntry(data, offset);
                if (entry == null) continue;

                var typeGuid = new Guid(entry.Value.TypeGuid);
                if (typeGuid == Guid.Empty) continue; // Unused entry

                var partition = new PartitionInfo
                {
                    Index = i,
                    Name = GetPartitionName(entry.Value.Name),
                    StartLba = entry.Value.FirstLba,
                    EndLba = entry.Value.LastLba,
                    SizeInBytes = (entry.Value.LastLba - entry.Value.FirstLba + 1) * (ulong)sectorSize,
                    TypeGuid = typeGuid,
                    UniqueGuid = new Guid(entry.Value.UniqueGuid),
                    Attributes = entry.Value.Attributes,
                    TypeName = GetPartitionTypeName(typeGuid)
                };

                table.Partitions.Add(partition);
            }

            table.IsValid = true;
            Logger.Info($"Parsed GPT: {table.Partitions.Count} partitions, Total: {table.TotalSizeFormatted}", "PARTITION");

            return table;
        }

        /// <summary>
        /// Parse MBR partition table
        /// </summary>
        private PartitionTable ParseMbr(byte[] data, int sectorSize)
        {
            var table = new PartitionTable
            {
                Type = PartitionTableType.MBR,
                SectorSize = sectorSize
            };

            // MBR partition entries start at offset 0x1BE
            const int PARTITION_TABLE_OFFSET = 0x1BE;
            const int PARTITION_ENTRY_SIZE = 16;

            for (int i = 0; i < 4; i++)
            {
                var offset = PARTITION_TABLE_OFFSET + (i * PARTITION_ENTRY_SIZE);
                var entry = ParseMbrEntry(data, offset);

                if (entry.SystemId == 0 || entry.TotalSectors == 0)
                    continue;

                var partition = new PartitionInfo
                {
                    Index = i,
                    Name = $"Partition{i + 1}",
                    StartLba = entry.RelativeSectors,
                    EndLba = entry.RelativeSectors + entry.TotalSectors - 1,
                    SizeInBytes = (ulong)entry.TotalSectors * (ulong)sectorSize,
                    TypeName = GetMbrTypeName(entry.SystemId)
                };

                table.Partitions.Add(partition);
                table.TotalSectors = Math.Max(table.TotalSectors, partition.EndLba);
            }

            table.IsValid = true;
            Logger.Info($"Parsed MBR: {table.Partitions.Count} partitions", "PARTITION");

            return table;
        }

        #region Parsing Helpers

        private GptHeader? ParseGptHeader(byte[] data, int offset)
        {
            if (offset + Marshal.SizeOf<GptHeader>() > data.Length)
                return null;

            var signature = Encoding.ASCII.GetString(data, offset, 8);
            if (signature != GPT_SIGNATURE)
                return null;

            var header = new GptHeader
            {
                Signature = new byte[8],
                DiskGuid = new byte[16]
            };

            Array.Copy(data, offset, header.Signature, 0, 8);
            header.Revision = BitConverter.ToUInt32(data, offset + 8);
            header.HeaderSize = BitConverter.ToUInt32(data, offset + 12);
            header.HeaderCrc32 = BitConverter.ToUInt32(data, offset + 16);
            header.Reserved = BitConverter.ToUInt32(data, offset + 20);
            header.CurrentLba = BitConverter.ToUInt64(data, offset + 24);
            header.BackupLba = BitConverter.ToUInt64(data, offset + 32);
            header.FirstUsableLba = BitConverter.ToUInt64(data, offset + 40);
            header.LastUsableLba = BitConverter.ToUInt64(data, offset + 48);
            Array.Copy(data, offset + 56, header.DiskGuid, 0, 16);
            header.PartitionEntryLba = BitConverter.ToUInt64(data, offset + 72);
            header.NumberOfPartitionEntries = BitConverter.ToUInt32(data, offset + 80);
            header.SizeOfPartitionEntry = BitConverter.ToUInt32(data, offset + 84);
            header.PartitionEntryArrayCrc32 = BitConverter.ToUInt32(data, offset + 88);

            return header;
        }

        private GptPartitionEntry? ParseGptEntry(byte[] data, int offset)
        {
            if (offset + 128 > data.Length)
                return null;

            var entry = new GptPartitionEntry
            {
                TypeGuid = new byte[16],
                UniqueGuid = new byte[16],
                Name = new byte[72]
            };

            Array.Copy(data, offset, entry.TypeGuid, 0, 16);
            Array.Copy(data, offset + 16, entry.UniqueGuid, 0, 16);
            entry.FirstLba = BitConverter.ToUInt64(data, offset + 32);
            entry.LastLba = BitConverter.ToUInt64(data, offset + 40);
            entry.Attributes = BitConverter.ToUInt64(data, offset + 48);
            Array.Copy(data, offset + 56, entry.Name, 0, 72);

            return entry;
        }

        private MbrPartitionEntry ParseMbrEntry(byte[] data, int offset)
        {
            return new MbrPartitionEntry
            {
                BootIndicator = data[offset],
                StartHead = data[offset + 1],
                StartSectorCylinder = data[offset + 2],
                StartCylinderHigh = data[offset + 3],
                SystemId = data[offset + 4],
                EndHead = data[offset + 5],
                EndSectorCylinder = data[offset + 6],
                EndCylinderHigh = data[offset + 7],
                RelativeSectors = BitConverter.ToUInt32(data, offset + 8),
                TotalSectors = BitConverter.ToUInt32(data, offset + 12)
            };
        }

        private static string GetPartitionName(byte[] nameBytes)
        {
            // UTF-16LE encoded name
            var name = Encoding.Unicode.GetString(nameBytes).TrimEnd('\0');
            return string.IsNullOrEmpty(name) ? "unnamed" : name;
        }

        private static string GetPartitionTypeName(Guid typeGuid)
        {
            if (KnownPartitionTypes.TryGetValue(typeGuid, out var name))
                return name;
            return "Unknown";
        }

        private static string GetMbrTypeName(byte systemId) => systemId switch
        {
            0x00 => "Empty",
            0x01 => "FAT12",
            0x04 => "FAT16 <32MB",
            0x05 => "Extended",
            0x06 => "FAT16",
            0x07 => "NTFS/HPFS",
            0x0B => "FAT32",
            0x0C => "FAT32 LBA",
            0x0E => "FAT16 LBA",
            0x0F => "Extended LBA",
            0x82 => "Linux Swap",
            0x83 => "Linux",
            0x8E => "Linux LVM",
            0xEE => "GPT Protective",
            0xEF => "EFI System",
            _ => $"Type 0x{systemId:X2}"
        };

        #endregion

        #region Utility Methods

        /// <summary>
        /// Find partition by name
        /// </summary>
        public PartitionInfo? FindPartition(PartitionTable table, string name)
        {
            return table.Partitions.FirstOrDefault(p => 
                p.Name.Equals(name, StringComparison.OrdinalIgnoreCase));
        }

        /// <summary>
        /// Get partitions in a specific size range
        /// </summary>
        public IEnumerable<PartitionInfo> GetPartitionsBySize(PartitionTable table, ulong minBytes, ulong maxBytes)
        {
            return table.Partitions.Where(p => p.SizeInBytes >= minBytes && p.SizeInBytes <= maxBytes);
        }

        /// <summary>
        /// Generate partition table summary
        /// </summary>
        public string GenerateSummary(PartitionTable table)
        {
            var sb = new StringBuilder();
            sb.AppendLine($"Partition Table Type: {table.Type}");
            sb.AppendLine($"Sector Size: {table.SectorSize}");
            sb.AppendLine($"Total Size: {table.TotalSizeFormatted}");
            sb.AppendLine($"Partitions: {table.Partitions.Count}");
            sb.AppendLine();
            sb.AppendLine("Name                  Start LBA    End LBA       Size         Type");
            sb.AppendLine(new string('-', 80));

            foreach (var p in table.Partitions.OrderBy(x => x.StartLba))
            {
                sb.AppendLine($"{p.Name,-20} {p.StartLba,12} {p.EndLba,12} {p.SizeFormatted,12} {p.TypeName}");
            }

            return sb.ToString();
        }

        #endregion
    }
}
