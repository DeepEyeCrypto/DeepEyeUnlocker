using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols.Qualcomm;

namespace DeepEyeUnlocker.Operations
{

    /// <summary>
    /// Factory Reset Protection (FRP) bypass manager
    /// </summary>
    public class FrpBypassManager
    {
        private readonly FirehoseManager? _firehose;
        private readonly PartitionTableParser _partitionParser;

        // FRP partition names by brand
        private static readonly Dictionary<string, string[]> FrpPartitionNames = new()
        {
            { "XIAOMI", new[] { "frp", "config" } },
            { "SAMSUNG", new[] { "persistent", "frp", "sec_efs" } },
            { "ONEPLUS", new[] { "frp", "config" } },
            { "OPPO", new[] { "frp", "opporeserve2" } },
            { "REALME", new[] { "frp", "oplusreserve2" } },
            { "VIVO", new[] { "frp", "reserve1" } },
            { "GOOGLE", new[] { "frp" } },
            { "MOTOROLA", new[] { "frp", "utags" } },
            { "LG", new[] { "frp", "OP" } },
            { "HUAWEI", new[] { "frp", "oeminfo" } },
            { "DEFAULT", new[] { "frp", "config", "persistent" } }
        };

        // Empty/clean FRP partition pattern (all zeros with header)
        private static readonly byte[] CleanFrpHeader = new byte[]
        {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };

        public FrpBypassManager() : this(null) { }

        public FrpBypassManager(FirehoseManager? firehose)
        {
            _firehose = firehose;
            _partitionParser = new PartitionTableParser();
        }

        #region Detection

        /// <summary>
        /// Detect FRP status from device
        /// </summary>
        public async Task<FrpStatus> DetectFrpStatusAsync(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress = null,
            CancellationToken ct = default)
        {
            var info = new FrpStatus { Status = FrpLockStatus.Unknown };

            try
            {
                Report(progress, 10, "Detecting FRP status...");

                // Get FRP partition name for this brand
                var frpPartitions = GetFrpPartitionNames(device.Brand);
                info.FrpPartitionName = frpPartitions.FirstOrDefault();

                if (_firehose != null && _firehose.IsReady)
                {
                    // Read FRP partition data
                    foreach (var partName in frpPartitions)
                    {
                        Report(progress, 30, $"Reading {partName} partition...");
                        
                        try
                        {
                            var data = await _firehose.ReadPartitionAsync(partName, null, ct);
                            if (data.Length > 0)
                            {
                                info.FrpPartitionName = partName;
                                info.FrpPartitionSize = (ulong)data.Length;
                                info.Status = AnalyzeFrpData(data);
                                info.DetectionMethod = FrpDetectionMethod.EdlPartitionRead;
                                
                                if (info.Status == FrpLockStatus.Locked)
                                {
                                    info.IsGoogleAccountBound = true;
                                    info.AccountHint = ExtractAccountHint(data);
                                }
                                
                                break;
                            }
                        }
                        catch (Exception ex)
                        {
                            Logger.Debug($"Could not read {partName}: {ex.Message}", "FRP");
                        }
                    }
                }
                else
                {
                    // ADB-based detection (if available)
                    info = await DetectViaAdbAsync(device, progress, ct);
                }

                Report(progress, 100, $"FRP Status: {info.Status}");
                Logger.Info($"FRP detection complete: {info.Status}", "FRP");
            }
            catch (Exception ex)
            {
                info.Status = FrpLockStatus.Error;
                Logger.Error(ex, "FRP detection failed");
            }

            return info;
        }

        /// <summary>
        /// Analyze FRP partition data to determine lock status
        /// </summary>
        private FrpLockStatus AnalyzeFrpData(byte[] data)
        {
            if (data.All(b => b == 0x00))
            {
                return FrpLockStatus.Unlocked;
            }

            if (data.All(b => b == 0xFF))
            {
                return FrpLockStatus.Unlocked;
            }

            // Check for Google account signature patterns
            // FRP data typically contains:
            // - Account type marker
            // - Encrypted account hash
            // - Challenge response data

            // Look for known patterns indicating active FRP
            var dataStr = Encoding.ASCII.GetString(data.Take(256).ToArray());
            
            if (dataStr.Contains("google") || 
                dataStr.Contains("account") ||
                ContainsFrpSignature(data))
            {
                return FrpLockStatus.Locked;
            }

            // Check if partially cleared (some data but no account)
            var nonZeroCount = data.Count(b => b != 0x00 && b != 0xFF);
            if (nonZeroCount > 0 && nonZeroCount < data.Length / 10)
            {
                return FrpLockStatus.PartiallyCleared;
            }

            return FrpLockStatus.Unknown;
        }

        /// <summary>
        /// Check for known FRP signature patterns
        /// </summary>
        private bool ContainsFrpSignature(byte[] data)
        {
            // Common FRP signature patterns
            byte[][] signatures = new[]
            {
                new byte[] { 0x46, 0x52, 0x50, 0x00 },  // "FRP\0"
                new byte[] { 0x00, 0x00, 0x00, 0x01 },  // Version marker
                new byte[] { 0x67, 0x6F, 0x6F, 0x67 },  // "goog"
            };

            foreach (var sig in signatures)
            {
                for (int i = 0; i <= data.Length - sig.Length; i++)
                {
                    if (data.Skip(i).Take(sig.Length).SequenceEqual(sig))
                    {
                        return true;
                    }
                }
            }

            return false;
        }

        /// <summary>
        /// Try to extract account hint from FRP data
        /// </summary>
        private string? ExtractAccountHint(byte[] data)
        {
            try
            {
                // Look for email patterns in data
                var str = Encoding.UTF8.GetString(data);
                var atIndex = str.IndexOf('@');
                if (atIndex > 0)
                {
                    // Extract potential email
                    int start = atIndex - 1;
                    while (start > 0 && (char.IsLetterOrDigit(str[start - 1]) || str[start - 1] == '.'))
                        start--;

                    int end = atIndex + 1;
                    while (end < str.Length && (char.IsLetterOrDigit(str[end]) || str[end] == '.'))
                        end++;

                    var email = str.Substring(start, end - start);
                    if (email.Contains('@') && email.Contains('.'))
                    {
                        // Mask the email for privacy
                        var parts = email.Split('@');
                        if (parts[0].Length > 3)
                        {
                            return parts[0].Substring(0, 3) + "***@" + parts[1];
                        }
                    }
                }
            }
            catch { }

            return null;
        }

        /// <summary>
        /// Detect FRP status via ADB
        /// </summary>
        private async Task<FrpStatus> DetectViaAdbAsync(DeviceContext device, IProgress<ProgressUpdate>? progress, CancellationToken ct)
        {
            var info = new FrpStatus { Status = FrpLockStatus.Unknown };

            try
            {
                // Check persist.sys.oem_unlock_allowed
                var unlockAllowed = await ExecuteAdbCommand("shell settings get global oem_unlock_allowed", ct);
                info.AllowOemUnlock = unlockAllowed.Trim() == "1";

                // Check if device is setup complete
                var setupComplete = await ExecuteAdbCommand("shell settings get secure user_setup_complete", ct);
                
                // Check for Google account
                var accounts = await ExecuteAdbCommand("shell pm list packages com.google.android.gms", ct);
                info.IsGoogleAccountBound = !string.IsNullOrEmpty(accounts);

                info.Status = info.IsGoogleAccountBound ? FrpLockStatus.Locked : FrpLockStatus.Unlocked;
                info.DetectionMethod = FrpDetectionMethod.AdbSettings;
            }
            catch (Exception ex)
            {
                Logger.Debug($"ADB FRP detection failed: {ex.Message}", "FRP");
            }

            return info;
        }

        #endregion

        #region Bypass Operations

        /// <summary>
        /// Attempt FRP bypass with the best available method
        /// </summary>
        public async Task<FrpBypassResult> BypassFrpAsync(
            DeviceContext device,
            FrpBypassMethod? preferredMethod = null,
            IProgress<ProgressUpdate>? progress = null,
            CancellationToken ct = default)
        {
            var result = new FrpBypassResult();

            try
            {
                // Determine best method based on device state
                var method = preferredMethod ?? DetermineBestMethod(device);
                result.MethodUsed = method;

                Report(progress, 10, $"Attempting FRP bypass via {method}...");
                Logger.Info($"Starting FRP bypass: {method}", "FRP");

                switch (method)
                {
                    case FrpBypassMethod.PartitionErase:
                        result = await BypassViaPartitionErase(device, progress, ct);
                        break;

                    case FrpBypassMethod.PartitionOverwrite:
                        result = await BypassViaPartitionOverwrite(device, progress, ct);
                        break;

                    case FrpBypassMethod.PersistClear:
                        result = await BypassViaPersistClear(device, progress, ct);
                        break;

                    case FrpBypassMethod.FastbootUnlock:
                        result = await BypassViaFastbootUnlock(device, progress, ct);
                        break;

                    case FrpBypassMethod.AdbBypass:
                        result = await BypassViaAdb(device, progress, ct);
                        break;

                    default:
                        result.Success = false;
                        result.Message = $"Method {method} not implemented";
                        break;
                }

                if (result.Success)
                {
                    Logger.Success($"FRP bypass successful via {method}", "FRP");
                }
                else
                {
                    Logger.Error($"FRP bypass failed: {result.Message}", "FRP");
                }
            }
            catch (Exception ex)
            {
                result.Success = false;
                result.Message = ex.Message;
                Logger.Error(ex, "FRP bypass failed");
            }

            return result;
        }

        /// <summary>
        /// Erase FRP partition directly
        /// </summary>
        private async Task<FrpBypassResult> BypassViaPartitionErase(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress,
            CancellationToken ct)
        {
            if (_firehose == null || !_firehose.IsReady)
            {
                return new FrpBypassResult
                {
                    Success = false,
                    Message = "Firehose session required for partition erase",
                    MethodUsed = FrpBypassMethod.PartitionErase
                };
            }

            var frpPartitions = GetFrpPartitionNames(device.Brand);

            foreach (var partName in frpPartitions)
            {
                Report(progress, 50, $"Erasing {partName}...");
                
                try
                {
                    if (await _firehose.ErasePartitionAsync(partName, null, ct))
                    {
                        return new FrpBypassResult
                        {
                            Success = true,
                            MethodUsed = FrpBypassMethod.PartitionErase,
                            Message = $"Successfully erased {partName} partition",
                            RequiresReboot = true,
                            AdditionalSteps = "Reboot device. FRP should be cleared after first boot."
                        };
                    }
                }
                catch (Exception ex)
                {
                    Logger.Debug($"Could not erase {partName}: {ex.Message}", "FRP");
                }
            }

            return new FrpBypassResult
            {
                Success = false,
                MethodUsed = FrpBypassMethod.PartitionErase,
                Message = "Failed to erase FRP partition"
            };
        }

        /// <summary>
        /// Overwrite FRP partition with clean data
        /// </summary>
        private async Task<FrpBypassResult> BypassViaPartitionOverwrite(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress,
            CancellationToken ct)
        {
            if (_firehose == null || !_firehose.IsReady)
            {
                return new FrpBypassResult
                {
                    Success = false,
                    Message = "Firehose session required for partition overwrite",
                    MethodUsed = FrpBypassMethod.PartitionOverwrite
                };
            }

            var frpPartitions = GetFrpPartitionNames(device.Brand);

            foreach (var partName in frpPartitions)
            {
                Report(progress, 30, $"Reading {partName} size...");
                
                try
                {
                    // Read current partition to get size
                    var existingData = await _firehose.ReadPartitionAsync(partName, null, ct);
                    if (existingData.Length == 0) continue;

                    // Create clean data (all zeros)
                    var cleanData = new byte[existingData.Length];
                    
                    Report(progress, 60, $"Writing clean {partName}...");
                    
                    if (await _firehose.WritePartitionAsync(partName, cleanData, null, ct))
                    {
                        return new FrpBypassResult
                        {
                            Success = true,
                            MethodUsed = FrpBypassMethod.PartitionOverwrite,
                            Message = $"Successfully cleared {partName} partition",
                            RequiresReboot = true,
                            AdditionalSteps = "Reboot device to apply changes."
                        };
                    }
                }
                catch (Exception ex)
                {
                    Logger.Debug($"Could not overwrite {partName}: {ex.Message}", "FRP");
                }
            }

            return new FrpBypassResult
            {
                Success = false,
                MethodUsed = FrpBypassMethod.PartitionOverwrite,
                Message = "Failed to overwrite FRP partition"
            };
        }

        /// <summary>
        /// Clear persist partition (clears FRP + other calibration)
        /// </summary>
        private async Task<FrpBypassResult> BypassViaPersistClear(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress,
            CancellationToken ct)
        {
            if (_firehose == null || !_firehose.IsReady)
            {
                return new FrpBypassResult
                {
                    Success = false,
                    Message = "Firehose session required",
                    MethodUsed = FrpBypassMethod.PersistClear
                };
            }

            Report(progress, 50, "Erasing persist partition...");

            // ⚠️ Warning: This erases calibration data too!
            if (await _firehose.ErasePartitionAsync("persist", null, ct))
            {
                return new FrpBypassResult
                {
                    Success = true,
                    MethodUsed = FrpBypassMethod.PersistClear,
                    Message = "Persist partition cleared (includes FRP)",
                    RequiresReboot = true,
                    AdditionalSteps = "⚠️ Device calibration may be affected. Consider restoring persist backup."
                };
            }

            return new FrpBypassResult
            {
                Success = false,
                MethodUsed = FrpBypassMethod.PersistClear,
                Message = "Failed to erase persist partition"
            };
        }

        /// <summary>
        /// Fastboot OEM unlock (if available)
        /// </summary>
        private async Task<FrpBypassResult> BypassViaFastbootUnlock(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress,
            CancellationToken ct)
        {
            Report(progress, 30, "Attempting fastboot OEM unlock...");

            try
            {
                var output = await ExecuteFastbootCommand("oem unlock", ct);
                
                if (output.Contains("OKAY") || output.Contains("success", StringComparison.OrdinalIgnoreCase))
                {
                    return new FrpBypassResult
                    {
                        Success = true,
                        MethodUsed = FrpBypassMethod.FastbootUnlock,
                        Message = "Bootloader unlocked successfully",
                        RequiresReboot = true,
                        AdditionalSteps = "Device will factory reset on next boot. FRP will be cleared."
                    };
                }
                else if (output.Contains("not allowed") || output.Contains("disabled"))
                {
                    return new FrpBypassResult
                    {
                        Success = false,
                        MethodUsed = FrpBypassMethod.FastbootUnlock,
                        Message = "OEM unlock is disabled. Enable in Developer Options first.",
                        AdditionalSteps = "Settings > Developer Options > OEM Unlocking"
                    };
                }
            }
            catch (Exception ex)
            {
                Logger.Debug($"Fastboot unlock failed: {ex.Message}", "FRP");
            }

            return new FrpBypassResult
            {
                Success = false,
                MethodUsed = FrpBypassMethod.FastbootUnlock,
                Message = "Fastboot OEM unlock failed"
            };
        }

        /// <summary>
        /// ADB-based bypass (for devices with USB debugging)
        /// </summary>
        private async Task<FrpBypassResult> BypassViaAdb(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress,
            CancellationToken ct)
        {
            Report(progress, 20, "Attempting ADB bypass...");

            try
            {
                // Method 1: Clear Google Play Services data
                Report(progress, 40, "Clearing GMS data...");
                await ExecuteAdbCommand("shell pm clear com.google.android.gms", ct);
                await ExecuteAdbCommand("shell pm clear com.google.android.gsf", ct);
                await ExecuteAdbCommand("shell pm clear com.google.android.gsf.login", ct);

                // Method 2: Disable FRP check
                Report(progress, 60, "Modifying settings...");
                await ExecuteAdbCommand("shell content insert --uri content://settings/secure --bind name:s:user_setup_complete --bind value:s:1", ct);
                await ExecuteAdbCommand("shell settings put global device_provisioned 1", ct);

                // Method 3: Remove account files
                Report(progress, 80, "Removing account data...");
                await ExecuteAdbCommand("shell rm -rf /data/system/users/0/accounts.db*", ct);
                await ExecuteAdbCommand("shell rm -rf /data/system/sync/accounts.xml", ct);

                return new FrpBypassResult
                {
                    Success = true,
                    MethodUsed = FrpBypassMethod.AdbBypass,
                    Message = "ADB FRP bypass commands executed",
                    RequiresReboot = true,
                    AdditionalSteps = "Reboot device. If FRP persists, try factory reset from recovery."
                };
            }
            catch (Exception ex)
            {
                return new FrpBypassResult
                {
                    Success = false,
                    MethodUsed = FrpBypassMethod.AdbBypass,
                    Message = $"ADB bypass failed: {ex.Message}"
                };
            }
        }

        #endregion

        #region Helpers

        private string[] GetFrpPartitionNames(string? brand)
        {
            var brandUpper = brand?.ToUpperInvariant() ?? "DEFAULT";
            
            if (FrpPartitionNames.TryGetValue(brandUpper, out var names))
                return names;
            
            return FrpPartitionNames["DEFAULT"];
        }

        private FrpBypassMethod DetermineBestMethod(DeviceContext device)
        {
            // If in EDL mode with Firehose, use partition erase
            if (device.Mode == ConnectionMode.EDL && _firehose?.IsReady == true)
            {
                return FrpBypassMethod.PartitionErase;
            }

            // If in Fastboot, try OEM unlock
            if (device.Mode == ConnectionMode.Fastboot)
            {
                return FrpBypassMethod.FastbootUnlock;
            }

            // If in ADB mode
            if (device.Mode == ConnectionMode.ADB)
            {
                return FrpBypassMethod.AdbBypass;
            }

            // Default to partition overwrite (safest)
            return FrpBypassMethod.PartitionOverwrite;
        }

        private async Task<string> ExecuteAdbCommand(string args, CancellationToken ct)
        {
            using var process = new System.Diagnostics.Process
            {
                StartInfo = new System.Diagnostics.ProcessStartInfo
                {
                    FileName = "adb",
                    Arguments = args,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                }
            };

            process.Start();
            var output = await process.StandardOutput.ReadToEndAsync(ct);
            await process.WaitForExitAsync(ct);
            return output;
        }

        private async Task<string> ExecuteFastbootCommand(string args, CancellationToken ct)
        {
            using var process = new System.Diagnostics.Process
            {
                StartInfo = new System.Diagnostics.ProcessStartInfo
                {
                    FileName = "fastboot",
                    Arguments = args,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                }
            };

            process.Start();
            var output = await process.StandardOutput.ReadToEndAsync(ct);
            var error = await process.StandardError.ReadToEndAsync(ct);
            await process.WaitForExitAsync(ct);
            return output + error;
        }

        private static void Report(IProgress<ProgressUpdate>? progress, int percent, string message)
        {
            progress?.Report(ProgressUpdate.Info(percent, message));
        }

        #endregion
    }
}
