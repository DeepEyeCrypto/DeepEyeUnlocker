using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Security.Cryptography;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols.Qualcomm;

namespace DeepEyeUnlocker.Operations
{
    #region Backup Models

    /// <summary>
    /// Backup operation type
    /// </summary>
    public enum BackupType
    {
        Full,           // All partitions
        Critical,       // Only boot, recovery, modem, etc.
        Userdata,       // Only userdata partition
        Custom          // User-selected partitions
    }

    /// <summary>
    /// Backup file format
    /// </summary>
    public enum BackupFormat
    {
        Raw,            // Individual .img files
        Compressed,     // Compressed ZIP archive
        DeepEyeBackup   // Proprietary .deb format with metadata
    }

    /// <summary>
    /// Partition backup entry
    /// </summary>
    public class PartitionBackupEntry
    {
        public string Name { get; set; } = "";
        public ulong StartLba { get; set; }
        public ulong SizeBytes { get; set; }
        public string FileName { get; set; } = "";
        public string Sha256 { get; set; } = "";
        public DateTime BackedUpAt { get; set; }
        public bool Verified { get; set; }
    }

    /// <summary>
    /// Full backup manifest
    /// </summary>
    public class BackupManifest
    {
        public string Version { get; set; } = "1.0";
        public string DeviceBrand { get; set; } = "";
        public string DeviceModel { get; set; } = "";
        public string DeviceSerial { get; set; } = "";
        public string Chipset { get; set; } = "";
        public DateTime CreatedAt { get; set; }
        public BackupType Type { get; set; }
        public BackupFormat Format { get; set; }
        public int SectorSize { get; set; } = 512;
        public List<PartitionBackupEntry> Partitions { get; set; } = new();
        public string? Notes { get; set; }
    }

    /// <summary>
    /// Backup result
    /// </summary>
    public class BackupResult
    {
        public bool Success { get; set; }
        public string OutputPath { get; set; } = "";
        public BackupManifest? Manifest { get; set; }
        public int PartitionsBackedUp { get; set; }
        public int PartitionsFailed { get; set; }
        public long TotalBytes { get; set; }
        public TimeSpan Duration { get; set; }
        public string? Error { get; set; }
    }

    /// <summary>
    /// Restore result
    /// </summary>
    public class RestoreResult
    {
        public bool Success { get; set; }
        public int PartitionsRestored { get; set; }
        public int PartitionsFailed { get; set; }
        public TimeSpan Duration { get; set; }
        public string? Error { get; set; }
    }

    #endregion

    /// <summary>
    /// Full device backup and restore manager
    /// </summary>
    public class BackupRestoreManager
    {
        private readonly FirehoseManager _firehose;
        private readonly PartitionTableParser _partitionParser;

        // Critical partitions that should always be backed up
        private static readonly string[] CriticalPartitions = new[]
        {
            "boot", "recovery", "sbl1", "sbl2", "aboot", "rpm",
            "tz", "hyp", "keymaster", "cmnlib", "cmnlib64",
            "devcfg", "modem", "dsp", "bluetooth", "persist",
            "frp", "misc", "fsc", "fsg", "modemst1", "modemst2"
        };

        // Large partitions to skip in critical backup
        private static readonly string[] LargePartitions = new[]
        {
            "system", "system_a", "system_b",
            "vendor", "vendor_a", "vendor_b",
            "product", "product_a", "product_b",
            "odm", "cache", "userdata"
        };

        public BackupRestoreManager(FirehoseManager firehose)
        {
            _firehose = firehose;
            _partitionParser = new PartitionTableParser();
        }

        #region Backup Operations

        /// <summary>
        /// Create a full device backup
        /// </summary>
        public async Task<BackupResult> CreateBackupAsync(
            DeviceContext device,
            string outputDirectory,
            BackupType type = BackupType.Full,
            BackupFormat format = BackupFormat.Compressed,
            IProgress<ProgressUpdate>? progress = null,
            CancellationToken ct = default)
        {
            var stopwatch = System.Diagnostics.Stopwatch.StartNew();
            var result = new BackupResult();

            try
            {
                // Validate firehose session
                if (!_firehose.IsReady)
                {
                    result.Error = "Firehose session is not ready";
                    return result;
                }

                // Create output directory
                var backupDir = Path.Combine(outputDirectory, 
                    $"backup_{device.Brand}_{device.Model}_{DateTime.Now:yyyyMMdd_HHmmss}");
                Directory.CreateDirectory(backupDir);

                // Read partition table
                Report(progress, 5, "Reading partition table...");
                var gptData = await _firehose.ReadPartitionAsync("gpt", null, ct);
                var partitionTable = _partitionParser.Parse(gptData);

                if (!partitionTable.IsValid)
                {
                    result.Error = $"Invalid partition table: {partitionTable.ParseError}";
                    return result;
                }

                Logger.Info($"Found {partitionTable.Partitions.Count} partitions", "BACKUP");

                // Filter partitions based on backup type
                var partitionsToBackup = FilterPartitions(partitionTable.Partitions, type);
                Logger.Info($"Backing up {partitionsToBackup.Count} partitions", "BACKUP");

                // Create manifest
                var manifest = new BackupManifest
                {
                    DeviceBrand = device.Brand ?? "Unknown",
                    DeviceModel = device.Model ?? "Unknown",
                    DeviceSerial = device.Serial ?? "Unknown",
                    Chipset = device.Chipset ?? "Unknown",
                    CreatedAt = DateTime.UtcNow,
                    Type = type,
                    Format = format,
                    SectorSize = partitionTable.SectorSize
                };

                // Backup each partition
                int completed = 0;
                foreach (var partition in partitionsToBackup)
                {
                    ct.ThrowIfCancellationRequested();

                    var percent = (int)((completed / (float)partitionsToBackup.Count) * 90) + 5;
                    Report(progress, percent, $"Backing up {partition.Name}...");

                    try
                    {
                        var data = await ReadPartitionDataAsync(partition, ct);
                        if (data.Length == 0)
                        {
                            Logger.Warn($"No data read from {partition.Name}", "BACKUP");
                            result.PartitionsFailed++;
                            continue;
                        }

                        var fileName = $"{partition.Name}.img";
                        var filePath = Path.Combine(backupDir, fileName);
                        await File.WriteAllBytesAsync(filePath, data, ct);

                        // Calculate hash
                        using var sha = SHA256.Create();
                        var hash = sha.ComputeHash(data);

                        manifest.Partitions.Add(new PartitionBackupEntry
                        {
                            Name = partition.Name,
                            StartLba = partition.StartLba,
                            SizeBytes = (ulong)data.Length,
                            FileName = fileName,
                            Sha256 = BitConverter.ToString(hash).Replace("-", "").ToLower(),
                            BackedUpAt = DateTime.UtcNow,
                            Verified = true
                        });

                        result.TotalBytes += data.Length;
                        result.PartitionsBackedUp++;
                        Logger.Info($"Backed up {partition.Name}: {data.Length:N0} bytes", "BACKUP");
                    }
                    catch (Exception ex)
                    {
                        Logger.Error($"Failed to backup {partition.Name}: {ex.Message}", "BACKUP");
                        result.PartitionsFailed++;
                    }

                    completed++;
                }

                // Save manifest
                Report(progress, 95, "Saving manifest...");
                var manifestPath = Path.Combine(backupDir, "manifest.json");
                var manifestJson = JsonSerializer.Serialize(manifest, new JsonSerializerOptions { WriteIndented = true });
                await File.WriteAllTextAsync(manifestPath, manifestJson, ct);

                // Compress if requested
                if (format == BackupFormat.Compressed || format == BackupFormat.DeepEyeBackup)
                {
                    Report(progress, 97, "Compressing backup...");
                    var extension = format == BackupFormat.DeepEyeBackup ? ".deb" : ".zip";
                    var archivePath = backupDir + extension;
                    
                    ZipFile.CreateFromDirectory(backupDir, archivePath, CompressionLevel.Optimal, false);
                    
                    // Clean up raw files
                    Directory.Delete(backupDir, true);
                    result.OutputPath = archivePath;
                }
                else
                {
                    result.OutputPath = backupDir;
                }

                result.Success = true;
                result.Manifest = manifest;
                stopwatch.Stop();
                result.Duration = stopwatch.Elapsed;

                Report(progress, 100, $"✅ Backup complete: {result.PartitionsBackedUp} partitions");
                Logger.Success($"Backup completed: {result.TotalBytes:N0} bytes in {result.Duration.TotalSeconds:F1}s", "BACKUP");
            }
            catch (OperationCanceledException)
            {
                result.Error = "Backup cancelled";
            }
            catch (Exception ex)
            {
                result.Error = ex.Message;
                Logger.Error(ex, "Backup failed");
            }

            return result;
        }

        /// <summary>
        /// Restore from a backup
        /// </summary>
        public async Task<RestoreResult> RestoreBackupAsync(
            string backupPath,
            IEnumerable<string>? selectedPartitions = null,
            IProgress<ProgressUpdate>? progress = null,
            CancellationToken ct = default)
        {
            var stopwatch = System.Diagnostics.Stopwatch.StartNew();
            var result = new RestoreResult();

            try
            {
                if (!_firehose.IsReady)
                {
                    result.Error = "Firehose session is not ready";
                    return result;
                }

                // Extract if archive
                string backupDir;
                bool needsCleanup = false;

                if (File.Exists(backupPath) && (backupPath.EndsWith(".zip") || backupPath.EndsWith(".deb")))
                {
                    Report(progress, 5, "Extracting backup archive...");
                    backupDir = Path.Combine(Path.GetTempPath(), $"restore_{Guid.NewGuid():N}");
                    ZipFile.ExtractToDirectory(backupPath, backupDir);
                    needsCleanup = true;
                }
                else if (Directory.Exists(backupPath))
                {
                    backupDir = backupPath;
                }
                else
                {
                    result.Error = "Backup path not found";
                    return result;
                }

                // Load manifest
                var manifestPath = Path.Combine(backupDir, "manifest.json");
                if (!File.Exists(manifestPath))
                {
                    result.Error = "Manifest not found in backup";
                    return result;
                }

                var manifestJson = await File.ReadAllTextAsync(manifestPath, ct);
                var manifest = JsonSerializer.Deserialize<BackupManifest>(manifestJson);
                if (manifest == null)
                {
                    result.Error = "Invalid manifest";
                    return result;
                }

                Logger.Info($"Restoring backup from {manifest.CreatedAt:yyyy-MM-dd HH:mm}", "RESTORE");

                // Filter partitions to restore
                var partitionsToRestore = manifest.Partitions.AsEnumerable();
                if (selectedPartitions != null && selectedPartitions.Any())
                {
                    var selected = selectedPartitions.ToHashSet(StringComparer.OrdinalIgnoreCase);
                    partitionsToRestore = partitionsToRestore.Where(p => selected.Contains(p.Name));
                }

                var toRestore = partitionsToRestore.ToList();
                int completed = 0;

                foreach (var partition in toRestore)
                {
                    ct.ThrowIfCancellationRequested();

                    var percent = (int)((completed / (float)toRestore.Count) * 90) + 10;
                    Report(progress, percent, $"Restoring {partition.Name}...");

                    try
                    {
                        var filePath = Path.Combine(backupDir, partition.FileName);
                        if (!File.Exists(filePath))
                        {
                            Logger.Warn($"Partition file not found: {partition.FileName}", "RESTORE");
                            result.PartitionsFailed++;
                            continue;
                        }

                        // Verify hash
                        var data = await File.ReadAllBytesAsync(filePath, ct);
                        using var sha = SHA256.Create();
                        var actualHash = BitConverter.ToString(sha.ComputeHash(data)).Replace("-", "").ToLower();
                        
                        if (actualHash != partition.Sha256)
                        {
                            Logger.Error($"Hash mismatch for {partition.Name}", "RESTORE");
                            result.PartitionsFailed++;
                            continue;
                        }

                        // Write partition
                        var success = await _firehose.WritePartitionAsync(partition.Name, data, null, ct);
                        if (success)
                        {
                            result.PartitionsRestored++;
                            Logger.Info($"Restored {partition.Name}: {data.Length:N0} bytes", "RESTORE");
                        }
                        else
                        {
                            result.PartitionsFailed++;
                            Logger.Error($"Failed to write {partition.Name}", "RESTORE");
                        }
                    }
                    catch (Exception ex)
                    {
                        Logger.Error($"Restore error for {partition.Name}: {ex.Message}", "RESTORE");
                        result.PartitionsFailed++;
                    }

                    completed++;
                }

                // Cleanup
                if (needsCleanup)
                {
                    try { Directory.Delete(backupDir, true); } catch { }
                }

                result.Success = result.PartitionsFailed == 0;
                stopwatch.Stop();
                result.Duration = stopwatch.Elapsed;

                Report(progress, 100, $"✅ Restore complete: {result.PartitionsRestored} partitions");
                Logger.Success($"Restore completed in {result.Duration.TotalSeconds:F1}s", "RESTORE");
            }
            catch (OperationCanceledException)
            {
                result.Error = "Restore cancelled";
            }
            catch (Exception ex)
            {
                result.Error = ex.Message;
                Logger.Error(ex, "Restore failed");
            }

            return result;
        }

        /// <summary>
        /// Load and validate a backup manifest
        /// </summary>
        public BackupManifest? LoadManifest(string backupPath)
        {
            try
            {
                string manifestPath;

                if (File.Exists(backupPath) && (backupPath.EndsWith(".zip") || backupPath.EndsWith(".deb")))
                {
                    using var archive = ZipFile.OpenRead(backupPath);
                    var manifestEntry = archive.GetEntry("manifest.json");
                    if (manifestEntry == null) return null;

                    using var stream = manifestEntry.Open();
                    using var reader = new StreamReader(stream);
                    return JsonSerializer.Deserialize<BackupManifest>(reader.ReadToEnd());
                }
                else if (Directory.Exists(backupPath))
                {
                    manifestPath = Path.Combine(backupPath, "manifest.json");
                    if (!File.Exists(manifestPath)) return null;
                    return JsonSerializer.Deserialize<BackupManifest>(File.ReadAllText(manifestPath));
                }
            }
            catch (Exception ex)
            {
                Logger.Error($"Failed to load manifest: {ex.Message}", "BACKUP");
            }

            return null;
        }

        #endregion

        #region Private Helpers

        private List<PartitionInfo> FilterPartitions(List<PartitionInfo> partitions, BackupType type)
        {
            switch (type)
            {
                case BackupType.Full:
                    return partitions;

                case BackupType.Critical:
                    return partitions
                        .Where(p => CriticalPartitions.Contains(p.Name.ToLower()) ||
                                   !LargePartitions.Contains(p.Name.ToLower()))
                        .ToList();

                case BackupType.Userdata:
                    return partitions
                        .Where(p => p.Name.Equals("userdata", StringComparison.OrdinalIgnoreCase))
                        .ToList();

                default:
                    return partitions;
            }
        }

        private async Task<byte[]> ReadPartitionDataAsync(PartitionInfo partition, CancellationToken ct)
        {
            // For now, read via Firehose (in real implementation, would use sector-level access)
            return await _firehose.ReadPartitionAsync(partition.Name, null, ct);
        }

        private static void Report(IProgress<ProgressUpdate>? progress, int percent, string message)
        {
            progress?.Report(ProgressUpdate.Info(percent, message));
        }

        #endregion
    }
}
