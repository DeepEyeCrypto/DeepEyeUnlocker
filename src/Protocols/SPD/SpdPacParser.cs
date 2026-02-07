using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

namespace DeepEyeUnlocker.Protocols.SPD
{
    public class SpdPacFileEntry
    {
        public string Id { get; set; }
        public string FileName { get; set; }
        public long Offset { get; set; }
        public long Size { get; set; }
        public bool IsActive { get; set; }
    }

    public class SpdPacParser
    {
        private const string PAC_MAGIC_V1 = "SPRD";
        private const string PAC_MAGIC_V2 = "PAC_";

        public static List<SpdPacFileEntry> Parse(string filePath)
        {
            var entries = new List<SpdPacFileEntry>();
            using (var fs = new FileStream(filePath, FileMode.Open, FileAccess.Read))
            using (var reader = new BinaryReader(fs))
            {
                if (fs.Length < 0x200) return entries;

                // 1. Read Header
                byte[] magicBuf = reader.ReadBytes(4);
                string magic = Encoding.ASCII.GetString(magicBuf);

                if (!magic.StartsWith(PAC_MAGIC_V1) && !magic.StartsWith(PAC_MAGIC_V2))
                {
                    throw new InvalidDataException("Not a valid Spreadtrum PAC file.");
                }

                // Header versioning/size differences exist, but count is usually at offset 0x1D0 or 0x1E0
                // For modern PACs (V2), count is at 0x1E0.
                fs.Position = 0x1E0;
                int fileCount = reader.ReadInt32();

                if (fileCount < 0 || fileCount > 100) return entries;

                // Entries typically start at 0x800
                fs.Position = 0x800;

                for (int i = 0; i < fileCount; i++)
                {
                    // Each entry is approx 0x124 bytes in V2
                    long entryStart = fs.Position;

                    byte[] idBuf = reader.ReadBytes(0x100);
                    string id = Encoding.Unicode.GetString(idBuf).TrimEnd('\0');

                    byte[] fileBuf = reader.ReadBytes(0x100);
                    string fileName = Encoding.Unicode.GetString(fileBuf).TrimEnd('\0');

                    // Skip metadata
                    fs.Position = entryStart + 0x200 + 0x0C; // Offset of size/offset fields

                    long size = reader.ReadInt64();
                    long offset = reader.ReadInt64();
                    int active = reader.ReadInt32();

                    if (size > 0 && offset > 0)
                    {
                        entries.Add(new SpdPacFileEntry {
                            Id = id,
                            FileName = fileName,
                            Offset = offset,
                            Size = size,
                            IsActive = active == 1
                        });
                    }

                    // Move to next entry boundary (Entries are usually 0x21C or similar)
                    fs.Position = entryStart + 0x21C; 
                }
            }

            return entries;
        }

        public static void ExtractEntry(string pacPath, SpdPacFileEntry entry, string outputPath)
        {
            using (var pacFs = new FileStream(pacPath, FileMode.Open, FileAccess.Read))
            using (var outFs = new FileStream(outputPath, FileMode.Create, FileAccess.Write))
            {
                pacFs.Position = entry.Offset;
                byte[] buffer = new byte[8192];
                long remaining = entry.Size;

                while (remaining > 0)
                {
                    int toRead = (int)Math.Min(buffer.Length, remaining);
                    int read = pacFs.Read(buffer, 0, toRead);
                    if (read <= 0) break;
                    outFs.Write(buffer, 0, read);
                    remaining -= read;
                }
            }
        }
    }
}
