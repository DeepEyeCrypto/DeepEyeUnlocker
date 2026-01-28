using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Xml.Linq;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure.Logging;

namespace DeepEyeUnlocker.Operations
{
    public class FlashManager
    {
        public async Task<FirmwareManifest> ParseFirmwareAsync(string path)
        {
            var manifest = new FirmwareManifest { BaseDirectory = Directory.Exists(path) ? path : Path.GetDirectoryName(path) };
            
            if (Directory.Exists(path))
            {
                // Check for Qualcomm XMLs
                var rawProgram = Directory.GetFiles(path, "rawprogram*.xml").FirstOrDefault();
                if (rawProgram != null)
                {
                    return await ParseQualcommAsync(path, rawProgram);
                }

                // Check for MTK Scatter
                var scatter = Directory.GetFiles(path, "*_Android_scatter.txt").FirstOrDefault();
                if (scatter != null)
                {
                    return ParseMediaTek(path, scatter);
                }

                // Generic images folder
                return ParseGenericFolder(path);
            }
            
            // Single file parsing (Samsung TAR or single .img)
            var ext = Path.GetExtension(path).ToLower();
            if (ext == ".tar" || ext == ".md5")
            {
                manifest.Type = FirmwareType.SamsungOdin;
                manifest.FirmwareName = Path.GetFileName(path);
                // In a real app we'd extract the TAR headers here
                return manifest;
            }

            return manifest;
        }

        private async Task<FirmwareManifest> ParseQualcommAsync(string dir, string xmlPath)
        {
            var manifest = new FirmwareManifest
            {
                Type = FirmwareType.QualcommFirehose,
                FirmwareName = new DirectoryInfo(dir).Name,
                BaseDirectory = dir
            };

            try
            {
                var doc = XDocument.Load(xmlPath);
                var program = doc.Element("program");
                if (program != null)
                {
                    foreach (var element in program.Elements("program"))
                    {
                        var filename = element.Attribute("filename")?.Value;
                        var label = element.Attribute("label")?.Value;
                        
                        if (!string.IsNullOrEmpty(filename) && !string.IsNullOrEmpty(label))
                        {
                            var fullPath = Path.Combine(dir, filename);
                            var info = new FlashPartitionInfo
                            {
                                PartitionName = label,
                                FileName = filename,
                                FilePath = fullPath,
                                Size = File.Exists(fullPath) ? new FileInfo(fullPath).Length : 0,
                                IsCritical = IsCriticalPartition(label)
                            };
                            manifest.Partitions.Add(info);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to parse Qualcomm rawprogram XML");
            }

            return manifest;
        }

        private FirmwareManifest ParseMediaTek(string dir, string scatterPath)
        {
            var manifest = new FirmwareManifest
            {
                Type = FirmwareType.MediaTekScatter,
                FirmwareName = new DirectoryInfo(dir).Name,
                BaseDirectory = dir
            };
            
            // Simplified scatter parsing
            var lines = File.ReadAllLines(scatterPath);
            foreach (var line in lines)
            {
                if (line.Contains("partition_name:"))
                {
                    var name = line.Split(':')[1].Trim();
                    // Find matching image file in directory
                    var img = Directory.GetFiles(dir, $"{name}.*").FirstOrDefault();
                    if (img != null)
                    {
                        manifest.Partitions.Add(new FlashPartitionInfo
                        {
                            PartitionName = name,
                            FileName = Path.GetFileName(img),
                            FilePath = img,
                            Size = new FileInfo(img).Length,
                            IsCritical = IsCriticalPartition(name)
                        });
                    }
                }
            }
            return manifest;
        }

        private FirmwareManifest ParseGenericFolder(string dir)
        {
            var manifest = new FirmwareManifest
            {
                Type = FirmwareType.FastbootImages,
                FirmwareName = new DirectoryInfo(dir).Name,
                BaseDirectory = dir
            };

            var files = Directory.GetFiles(dir, "*.img");
            foreach (var file in files)
            {
                var name = Path.GetFileNameWithoutExtension(file);
                manifest.Partitions.Add(new FlashPartitionInfo
                {
                    PartitionName = name,
                    FileName = Path.GetFileName(file),
                    FilePath = file,
                    Size = new FileInfo(file).Length,
                    IsCritical = IsCriticalPartition(name)
                });
            }
            return manifest;
        }

        private bool IsCriticalPartition(string name)
        {
            string n = name.ToLower();
            return n.Contains("efs") || n.Contains("modem") || n.Contains("persist") || 
                   n.Contains("nvram") || n.Contains("nvdata") || n.Contains("secxml") ||
                   n.Contains("bootloader") || n.Contains("abl") || n.Contains("xbl");
        }
    }
}
