using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Protocols.Samsung
{
    /// <summary>
    /// Parses Samsung PIT (Partition Information Table) binary data.
    /// </summary>
    public class PitParser
    {
        private const uint PIT_MAGIC = 0x12345678;

        public static IEnumerable<PartitionInfo> Parse(byte[] data)
        {
            var partitions = new List<PartitionInfo>();
            using var ms = new MemoryStream(data);
            using var reader = new BinaryReader(ms);

            if (data.Length < 28) return partitions;

            uint magic = reader.ReadUInt32();
            if (magic != PIT_MAGIC)
            {
                // Some newer loaders use different magic or no magic at offset 0
                ms.Position = 0;
            }

            // Standard PIT Header usually has count at offset 4 or 8
            // For simulation, we assume a standard layout:
            // [Magic 4][Count 4][EntrySize 4][Unknown 16]...
            
            ms.Position = 4;
            int entryCount = reader.ReadInt32();
            
            // Limit count to avoid OOM on corrupt data
            if (entryCount < 0 || entryCount > 512) return partitions;

            ms.Position = 28; // Start of first entry

            for (int i = 0; i < entryCount; i++)
            {
                ms.Position = 28 + (i * 132);
                if (ms.Position + 132 > data.Length) break;

                // Entry structure (approx 132 bytes in modern PITs)
                // 0-4: Binary Type
                // 4-8: Device Type
                // 8-12: ID
                // 12-16: Attr
                // 17: Update
                // 32-64: Partition Name
                // 64-96: Filename
                
                ms.Position += 32; // Skip types/IDs
                
                byte[] nameBuf = reader.ReadBytes(32);
                string name = Encoding.ASCII.GetString(nameBuf).TrimEnd('\0');

                ms.Position += 32; // Skip filename
                
                // Block count / Offset usually follow
                long size = reader.ReadUInt32() * 512; // Blocks to bytes
                long lba = reader.ReadUInt32();

                if (!string.IsNullOrEmpty(name))
                {
                    partitions.Add(new PartitionInfo
                    {
                        Name = name.ToUpperInvariant(),
                        SizeInBytes = (ulong)size,
                        StartLba = (ulong)lba
                    });

                }

                // Advance to next entry boundary (usually 132 or 160 bytes)
                // We'll use 132 for this implementation
            }

            return partitions;
        }

        public static byte[] CreateMockPit()
        {
            using var ms = new MemoryStream();
            using var writer = new BinaryWriter(ms);

            writer.Write(PIT_MAGIC);
            writer.Write(5); // 5 partitions
            writer.Write(132); // Entry size
            writer.Write(new byte[16]); // Padding

            string[] names = { "BOOT", "RECOVERY", "SYSTEM", "USERDATA", "PERSISTENT" };
            long[] sizes = { 65536, 65536, 4194304, 8388608, 1024 };

            for (int i = 0; i < names.Length; i++)
            {
                writer.Write(new byte[32]); // Types
                
                byte[] nameBuf = new byte[32];
                Encoding.ASCII.GetBytes(names[i]).CopyTo(nameBuf, 0);
                writer.Write(nameBuf);

                writer.Write(new byte[32]); // Filename
                
                writer.Write((uint)(sizes[i] / 512)); // Size in blocks
                writer.Write((uint)(i * 0x1000)); // LBA
                
                writer.Write(new byte[132 - 32 - 32 - 32 - 8]); // Remaining padding for 132-byte entry
            }

            return ms.ToArray();
        }
    }
}
