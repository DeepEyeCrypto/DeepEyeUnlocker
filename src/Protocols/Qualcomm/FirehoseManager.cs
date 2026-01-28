using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Protocols.Qualcomm
{
    /// <summary>
    /// Firehose session state
    /// </summary>
    public enum FirehoseSessionState
    {
        Disconnected,
        SaharaHandshaking,
        UploadingProgrammer,
        ConfiguringFirehose,
        Ready,
        TransferInProgress,
        Error
    }

    /// <summary>
    /// Programmer file information
    /// </summary>
    public class ProgrammerInfo
    {
        public string FilePath { get; set; } = "";
        public string FileName { get; set; } = "";
        public long FileSize { get; set; }
        public string Sha256 { get; set; } = "";
        public string? ChipsetFamily { get; set; }
        public bool IsValid { get; set; }
        public string? ValidationError { get; set; }
        public List<string> SupportedChipsets { get; set; } = new();
    }

    /// <summary>
    /// Firehose configuration settings
    /// </summary>
    public class FirehoseConfig
    {
        public string MemoryName { get; set; } = "emmc";  // emmc, ufs, nand
        public int MaxPayloadSize { get; set; } = 1048576; // 1MB default
        public int SectorSize { get; set; } = 512;
        public bool SkipStorageInit { get; set; } = false;
        public bool AlwaysValidate { get; set; } = false;
        public int Verbose { get; set; } = 0;
    }

    /// <summary>
    /// Session result for Firehose operations
    /// </summary>
    public class FirehoseSessionResult
    {
        public bool Success { get; set; }
        public string Message { get; set; } = "";
        public FirehoseSessionState FinalState { get; set; }
        public TimeSpan Duration { get; set; }
        public ProgrammerInfo? LoadedProgrammer { get; set; }
    }

    /// <summary>
    /// Manages Firehose programmer loading and session lifecycle
    /// </summary>
    public class FirehoseManager : IDisposable
    {
        private readonly string _programmersDirectory;
        private readonly Dictionary<string, List<ProgrammerInfo>> _programmerCache = new();
        
        private SaharaProtocol? _sahara;
        private FirehoseProtocol? _firehose;
        private FirehoseSessionState _state = FirehoseSessionState.Disconnected;
        private ProgrammerInfo? _currentProgrammer;
        private FirehoseConfig _config = new();
        
        // Known chipset to programmer mappings
        private static readonly Dictionary<string, string[]> ChipsetProgrammerMap = new()
        {
            // Snapdragon 8 Gen series
            { "SM8550", new[] { "prog_firehose_ddr.elf", "prog_ufs_firehose_8550.elf" } },
            { "SM8450", new[] { "prog_firehose_ddr.elf", "prog_ufs_firehose_8450.elf" } },
            { "SM8350", new[] { "prog_firehose_ddr.elf", "prog_ufs_firehose_8350.elf" } },
            { "SM8250", new[] { "prog_firehose_ddr.elf", "prog_ufs_firehose_8250.elf" } },
            
            // Snapdragon 7xx series
            { "SM7325", new[] { "prog_firehose_ddr.elf", "prog_ufs_firehose_7325.elf" } },
            { "SM7250", new[] { "prog_firehose_ddr.elf", "prog_ufs_firehose_7250.elf" } },
            { "SM7150", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_7150.mbn" } },
            { "SM7125", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_7125.mbn" } },
            
            // Snapdragon 6xx series
            { "SM6350", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_6350.mbn" } },
            { "SM6225", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_6225.mbn" } },
            { "SM6115", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_6115.mbn" } },
            
            // Snapdragon 4xx series
            { "SM4350", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_4350.mbn" } },
            { "SM4250", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_4250.mbn" } },
            
            // Legacy SDM series
            { "SDM845", new[] { "prog_firehose_ddr.elf", "prog_ufs_firehose_845.elf" } },
            { "SDM710", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_710.mbn" } },
            { "SDM670", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_670.mbn" } },
            { "SDM660", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_660.mbn" } },
            { "SDM636", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_636.mbn" } },
            { "SDM450", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_450.mbn" } },
            
            // MSM series
            { "MSM8998", new[] { "prog_firehose_ddr.elf", "prog_ufs_firehose_8998.elf" } },
            { "MSM8996", new[] { "prog_firehose_ddr.elf", "prog_ufs_firehose_8996.elf" } },
            { "MSM8953", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_8953.mbn" } },
            { "MSM8937", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_8937.mbn" } },
            { "MSM8917", new[] { "prog_firehose_ddr.elf", "prog_emmc_firehose_8917.mbn" } },
        };

        public FirehoseSessionState State => _state;
        public ProgrammerInfo? CurrentProgrammer => _currentProgrammer;
        public bool IsReady => _state == FirehoseSessionState.Ready;

        public FirehoseManager() : this(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "programmers")) { }

        public FirehoseManager(string programmersDirectory)
        {
            _programmersDirectory = programmersDirectory;
            EnsureDirectoryExists();
            ScanProgrammers();
        }

        #region Public API

        /// <summary>
        /// Initialize a Firehose session with the connected EDL device
        /// </summary>
        public async Task<FirehoseSessionResult> InitializeSessionAsync(
            LibUsbDotNet.UsbDevice usbDevice,
            string? programmerPath,
            DeviceContext device,
            IProgress<ProgressUpdate>? progress = null,
            CancellationToken ct = default)
        {
            var stopwatch = System.Diagnostics.Stopwatch.StartNew();
            
            try
            {
                // Step 1: Initialize Sahara protocol
                Report(progress, 5, "Initializing Sahara protocol...");
                _state = FirehoseSessionState.SaharaHandshaking;
                
                _sahara = new SaharaProtocol(usbDevice);
                
                if (!await _sahara.ProcessHelloAsync())
                {
                    _state = FirehoseSessionState.Error;
                    return new FirehoseSessionResult
                    {
                        Success = false,
                        Message = "Sahara handshake failed. Device may not be in proper EDL mode.",
                        FinalState = _state,
                        Duration = stopwatch.Elapsed
                    };
                }
                
                // Step 2: Find or validate programmer
                Report(progress, 15, "Locating programmer file...");
                
                ProgrammerInfo? programmer;
                if (!string.IsNullOrEmpty(programmerPath))
                {
                    programmer = ValidateProgrammer(programmerPath);
                }
                else
                {
                    programmer = FindBestProgrammerForDevice(device);
                }

                if (programmer == null || !programmer.IsValid)
                {
                    _state = FirehoseSessionState.Error;
                    return new FirehoseSessionResult
                    {
                        Success = false,
                        Message = programmer?.ValidationError ?? "No suitable programmer file found for this device.",
                        FinalState = _state,
                        Duration = stopwatch.Elapsed
                    };
                }

                // Step 3: Upload programmer
                Report(progress, 25, $"Uploading programmer: {programmer.FileName}...");
                _state = FirehoseSessionState.UploadingProgrammer;
                Logger.Info($"Uploading Firehose programmer: {programmer.FileName} ({programmer.FileSize} bytes)", "FIREHOSE");

                if (!await _sahara.UploadProgrammerAsync(programmer.FilePath))
                {
                    _state = FirehoseSessionState.Error;
                    return new FirehoseSessionResult
                    {
                        Success = false,
                        Message = "Failed to upload programmer via Sahara. The programmer may not be signed for this device.",
                        FinalState = _state,
                        Duration = stopwatch.Elapsed
                    };
                }

                _currentProgrammer = programmer;

                // Step 4: Initialize Firehose protocol
                Report(progress, 70, "Initializing Firehose protocol...");
                _state = FirehoseSessionState.ConfiguringFirehose;
                
                // Small delay for device to switch modes
                await Task.Delay(1000, ct);
                
                _firehose = new FirehoseProtocol(usbDevice);
                
                if (!await _firehose.ConfigureAsync())
                {
                    _state = FirehoseSessionState.Error;
                    return new FirehoseSessionResult
                    {
                        Success = false,
                        Message = "Firehose configuration failed. Check device compatibility.",
                        FinalState = _state,
                        Duration = stopwatch.Elapsed
                    };
                }

                // Step 5: Ready
                Report(progress, 100, "Firehose session ready!");
                _state = FirehoseSessionState.Ready;
                stopwatch.Stop();

                Logger.Success($"Firehose session initialized in {stopwatch.ElapsedMilliseconds}ms", "FIREHOSE");

                return new FirehoseSessionResult
                {
                    Success = true,
                    Message = "Firehose session initialized successfully.",
                    FinalState = _state,
                    Duration = stopwatch.Elapsed,
                    LoadedProgrammer = _currentProgrammer
                };
            }
            catch (OperationCanceledException)
            {
                _state = FirehoseSessionState.Error;
                return new FirehoseSessionResult
                {
                    Success = false,
                    Message = "Session initialization was cancelled.",
                    FinalState = _state,
                    Duration = stopwatch.Elapsed
                };
            }
            catch (Exception ex)
            {
                _state = FirehoseSessionState.Error;
                Logger.Error(ex, "Firehose session initialization failed");
                return new FirehoseSessionResult
                {
                    Success = false,
                    Message = $"Unexpected error: {ex.Message}",
                    FinalState = _state,
                    Duration = stopwatch.Elapsed
                };
            }
        }

        /// <summary>
        /// Read a partition using the active Firehose session
        /// </summary>
        public async Task<byte[]> ReadPartitionAsync(string partitionName, IProgress<ProgressUpdate>? progress = null, CancellationToken ct = default)
        {
            EnsureReady();
            _state = FirehoseSessionState.TransferInProgress;
            
            try
            {
                Report(progress, 0, $"Reading partition: {partitionName}...");
                var data = await _firehose!.ReadPartitionAsync(partitionName);
                Report(progress, 100, $"Read {data.Length} bytes from {partitionName}");
                return data;
            }
            finally
            {
                _state = FirehoseSessionState.Ready;
            }
        }

        /// <summary>
        /// Write data to a partition using the active Firehose session
        /// </summary>
        public async Task<bool> WritePartitionAsync(string partitionName, byte[] data, IProgress<ProgressUpdate>? progress = null, CancellationToken ct = default)
        {
            EnsureReady();
            _state = FirehoseSessionState.TransferInProgress;
            
            try
            {
                Report(progress, 0, $"Writing {data.Length} bytes to {partitionName}...");
                var result = await _firehose!.WritePartitionAsync(partitionName, data);
                Report(progress, 100, result ? $"Successfully wrote to {partitionName}" : $"Write failed for {partitionName}");
                return result;
            }
            finally
            {
                _state = FirehoseSessionState.Ready;
            }
        }

        /// <summary>
        /// Erase a partition using the active Firehose session
        /// </summary>
        public async Task<bool> ErasePartitionAsync(string partitionName, IProgress<ProgressUpdate>? progress = null, CancellationToken ct = default)
        {
            EnsureReady();
            _state = FirehoseSessionState.TransferInProgress;
            
            try
            {
                Report(progress, 0, $"Erasing partition: {partitionName}...");
                var result = await _firehose!.ErasePartitionAsync(partitionName);
                Report(progress, 100, result ? $"Successfully erased {partitionName}" : $"Erase failed for {partitionName}");
                return result;
            }
            finally
            {
                _state = FirehoseSessionState.Ready;
            }
        }

        /// <summary>
        /// Get available programmers for a device
        /// </summary>
        public IEnumerable<ProgrammerInfo> GetAvailableProgrammers(DeviceContext device)
        {
            var soc = device.SoC?.ToUpper() ?? "";
            
            // Get programmers that match the SoC
            if (_programmerCache.TryGetValue(soc, out var programmers))
            {
                return programmers;
            }

            // Return all available programmers if no match
            return _programmerCache.Values.SelectMany(p => p).Distinct();
        }

        /// <summary>
        /// Find the best programmer for a device
        /// </summary>
        public ProgrammerInfo? FindBestProgrammerForDevice(DeviceContext device)
        {
            var soc = device.SoC?.ToUpper() ?? "";
            
            // Try direct SoC match
            if (ChipsetProgrammerMap.TryGetValue(soc, out var programmerNames))
            {
                foreach (var name in programmerNames)
                {
                    var programmer = FindProgrammerByName(name);
                    if (programmer != null && programmer.IsValid)
                    {
                        Logger.Info($"Found matching programmer for {soc}: {programmer.FileName}", "FIREHOSE");
                        return programmer;
                    }
                }
            }

            // Try chipset family matching
            var family = ExtractChipsetFamily(soc);
            if (!string.IsNullOrEmpty(family))
            {
                var genericProgrammer = FindProgrammerByName($"prog_emmc_firehose_{family}.mbn") 
                                     ?? FindProgrammerByName($"prog_ufs_firehose_{family}.elf");
                if (genericProgrammer != null)
                    return genericProgrammer;
            }

            // Return first available programmer
            var fallback = _programmerCache.Values.SelectMany(p => p).FirstOrDefault(p => p.IsValid);
            if (fallback != null)
            {
                Logger.Warn($"Using fallback programmer: {fallback.FileName}", "FIREHOSE");
            }
            
            return fallback;
        }

        /// <summary>
        /// Validate a programmer file
        /// </summary>
        public ProgrammerInfo ValidateProgrammer(string filePath)
        {
            var info = new ProgrammerInfo
            {
                FilePath = filePath,
                FileName = Path.GetFileName(filePath)
            };

            if (!File.Exists(filePath))
            {
                info.IsValid = false;
                info.ValidationError = "File not found";
                return info;
            }

            var fileInfo = new FileInfo(filePath);
            info.FileSize = fileInfo.Length;

            // Check file size (programmers are typically 1-10MB)
            if (info.FileSize < 1024)
            {
                info.IsValid = false;
                info.ValidationError = "File too small to be a valid programmer";
                return info;
            }

            if (info.FileSize > 50 * 1024 * 1024) // 50MB
            {
                info.IsValid = false;
                info.ValidationError = "File too large to be a valid programmer";
                return info;
            }

            // Check ELF header (programmers are ELF binaries)
            try
            {
                using var fs = File.OpenRead(filePath);
                var header = new byte[4];
                fs.Read(header, 0, 4);
                
                // ELF magic: 0x7F 'E' 'L' 'F'
                if (header[0] == 0x7F && header[1] == 'E' && header[2] == 'L' && header[3] == 'F')
                {
                    info.IsValid = true;
                }
                // MBN header check (starts with different magic)
                else if (header[0] == 0x05 || header[0] == 0x07)
                {
                    info.IsValid = true;
                }
                else
                {
                    info.IsValid = false;
                    info.ValidationError = "Invalid file format. Expected ELF or MBN.";
                }
            }
            catch (Exception ex)
            {
                info.IsValid = false;
                info.ValidationError = $"Error reading file: {ex.Message}";
            }

            // Calculate SHA256
            if (info.IsValid)
            {
                try
                {
                    using var sha = SHA256.Create();
                    using var fs = File.OpenRead(filePath);
                    var hash = sha.ComputeHash(fs);
                    info.Sha256 = BitConverter.ToString(hash).Replace("-", "").ToLower();
                }
                catch { }
            }

            return info;
        }

        /// <summary>
        /// Add a programmer file to the collection
        /// </summary>
        public bool AddProgrammer(string sourcePath, string? targetName = null)
        {
            var info = ValidateProgrammer(sourcePath);
            if (!info.IsValid)
            {
                Logger.Error($"Cannot add programmer: {info.ValidationError}", "FIREHOSE");
                return false;
            }

            var targetPath = Path.Combine(_programmersDirectory, targetName ?? info.FileName);
            try
            {
                File.Copy(sourcePath, targetPath, overwrite: true);
                ScanProgrammers(); // Refresh cache
                Logger.Info($"Added programmer: {info.FileName}", "FIREHOSE");
                return true;
            }
            catch (Exception ex)
            {
                Logger.Error($"Failed to add programmer: {ex.Message}", "FIREHOSE");
                return false;
            }
        }

        /// <summary>
        /// Get session configuration
        /// </summary>
        public FirehoseConfig GetConfig() => _config;

        /// <summary>
        /// Update session configuration
        /// </summary>
        public void SetConfig(FirehoseConfig config) => _config = config;

        #endregion

        #region Private Helpers

        private void EnsureDirectoryExists()
        {
            if (!Directory.Exists(_programmersDirectory))
            {
                try
                {
                    Directory.CreateDirectory(_programmersDirectory);
                    Logger.Info($"Created programmers directory: {_programmersDirectory}", "FIREHOSE");
                }
                catch (Exception ex)
                {
                    Logger.Error($"Failed to create programmers directory: {ex.Message}", "FIREHOSE");
                }
            }
        }

        private void ScanProgrammers()
        {
            _programmerCache.Clear();

            if (!Directory.Exists(_programmersDirectory))
                return;

            var files = Directory.GetFiles(_programmersDirectory, "*.*", SearchOption.AllDirectories)
                .Where(f => f.EndsWith(".elf", StringComparison.OrdinalIgnoreCase) ||
                           f.EndsWith(".mbn", StringComparison.OrdinalIgnoreCase));

            foreach (var file in files)
            {
                var info = ValidateProgrammer(file);
                if (info.IsValid)
                {
                    // Try to extract chipset from filename
                    var chipset = ExtractChipsetFromFilename(info.FileName);
                    if (!string.IsNullOrEmpty(chipset))
                    {
                        if (!_programmerCache.ContainsKey(chipset))
                            _programmerCache[chipset] = new List<ProgrammerInfo>();
                        _programmerCache[chipset].Add(info);
                    }
                    
                    // Also add to generic list
                    if (!_programmerCache.ContainsKey("GENERIC"))
                        _programmerCache["GENERIC"] = new List<ProgrammerInfo>();
                    _programmerCache["GENERIC"].Add(info);
                }
            }

            Logger.Info($"Scanned {_programmerCache.Values.SelectMany(p => p).Count()} programmers", "FIREHOSE");
        }

        private ProgrammerInfo? FindProgrammerByName(string name)
        {
            return _programmerCache.Values
                .SelectMany(p => p)
                .FirstOrDefault(p => p.FileName.Equals(name, StringComparison.OrdinalIgnoreCase));
        }

        private static string? ExtractChipsetFromFilename(string filename)
        {
            // Extract chipset codes from filenames like "prog_emmc_firehose_8953.mbn"
            var parts = Path.GetFileNameWithoutExtension(filename).Split('_');
            foreach (var part in parts)
            {
                if (part.Length >= 3 && char.IsDigit(part[0]))
                {
                    // Try to map to SoC naming convention
                    if (part.StartsWith("8") && part.Length == 4)
                        return $"MSM{part}";
                    if (part.StartsWith("6") || part.StartsWith("7") || part.StartsWith("4"))
                        return $"SM{part}";
                }
            }
            return null;
        }

        private static string? ExtractChipsetFamily(string soc)
        {
            // Extract numeric part from SoC name
            var digits = new string(soc.Where(char.IsDigit).ToArray());
            return digits.Length >= 3 ? digits.Substring(0, 4) : null;
        }

        private void EnsureReady()
        {
            if (_state != FirehoseSessionState.Ready || _firehose == null)
            {
                throw new InvalidOperationException("Firehose session is not ready. Call InitializeSessionAsync first.");
            }
        }

        private static void Report(IProgress<ProgressUpdate>? progress, int percent, string message)
        {
            progress?.Report(ProgressUpdate.Info(percent, message));
        }

        public void Dispose()
        {
            _state = FirehoseSessionState.Disconnected;
            _firehose = null;
            _sahara = null;
            _currentProgrammer = null;
        }

        #endregion
    }
}
