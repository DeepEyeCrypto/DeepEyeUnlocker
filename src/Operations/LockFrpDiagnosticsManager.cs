using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols.Qualcomm;

namespace DeepEyeUnlocker.Operations
{
    /// <summary>
    /// Comprehensive Lock and FRP diagnostics manager
    /// </summary>
    public class LockFrpDiagnosticsManager
    {
        private readonly FirehoseManager? _firehose;

        // OEM FRP partition mappings
        private static readonly Dictionary<string, string[]> OemFrpPartitions = new(StringComparer.OrdinalIgnoreCase)
        {
            ["XIAOMI"] = new[] { "frp", "config" },
            ["REDMI"] = new[] { "frp", "config" },
            ["POCO"] = new[] { "frp", "config" },
            ["SAMSUNG"] = new[] { "persistent", "frp", "param", "sec_efs" },
            ["ONEPLUS"] = new[] { "frp", "config" },
            ["OPPO"] = new[] { "frp", "opporeserve2" },
            ["REALME"] = new[] { "frp", "oplusreserve2" },
            ["VIVO"] = new[] { "frp", "reserve1" },
            ["MOTOROLA"] = new[] { "frp", "utags" },
            ["LG"] = new[] { "frp", "OP" },
            ["HUAWEI"] = new[] { "frp", "oeminfo" },
            ["GOOGLE"] = new[] { "frp" },
            ["NOKIA"] = new[] { "frp" },
            ["ASUS"] = new[] { "frp" },
            ["SONY"] = new[] { "frp" }
        };

        // OEM support URLs
        private static readonly Dictionary<string, OemFrpInfo> OemInfoDatabase = new(StringComparer.OrdinalIgnoreCase)
        {
            ["SAMSUNG"] = new OemFrpInfo
            {
                OemName = "Samsung",
                HasOemAccountLock = true,
                OemAccountType = "Samsung Account",
                RequiresServerVerification = true,
                OfficialUnlockUrl = "https://findmymobile.samsung.com/"
            },
            ["XIAOMI"] = new OemFrpInfo
            {
                OemName = "Xiaomi",
                HasOemAccountLock = true,
                OemAccountType = "Mi Account",
                RequiresServerVerification = true,
                OfficialUnlockUrl = "https://i.mi.com/"
            },
            ["HUAWEI"] = new OemFrpInfo
            {
                OemName = "Huawei",
                HasOemAccountLock = true,
                OemAccountType = "Huawei ID",
                RequiresServerVerification = true,
                OfficialUnlockUrl = "https://cloud.huawei.com/"
            }
        };

        public LockFrpDiagnosticsManager(FirehoseManager? firehose = null)
        {
            _firehose = firehose;
        }

        #region FRP Diagnostics

        public async Task<FrpStatus> InspectFrpAsync(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress = null,
            CancellationToken ct = default)
        {
            var status = new FrpStatus();
            progress?.Report(ProgressUpdate.Info(10, "Starting FRP inspection..."));

            // Try detection methods in priority order
            try
            {
                switch (device.Mode)
                {
                    case ConnectionMode.Fastboot:
                        status = await DetectFrpViaFastboot(device, progress, ct);
                        break;

                    case ConnectionMode.ADB:
                        status = await DetectFrpViaAdb(device, progress, ct);
                        break;

                    case ConnectionMode.EDL:
                        if (_firehose?.IsReady == true)
                        {
                            status = await DetectFrpViaEdl(device, progress, ct);
                        }
                        break;
                }
            }
            catch (Exception ex)
            {
                status.Status = FrpLockStatus.Error;
                status.Notes = $"Detection error: {ex.Message}";
            }

            // Add OEM-specific info
            status.OemInfo = GetOemInfo(device.Brand);
            status.RecommendedActions = GenerateFrpRecommendations(status);

            progress?.Report(ProgressUpdate.Info(100, $"FRP Status: {status.Status}"));
            return status;
        }

        private async Task<FrpStatus> DetectFrpViaFastboot(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress,
            CancellationToken ct)
        {
            var status = new FrpStatus { DetectionMethod = FrpDetectionMethod.FastbootGetvar };
            progress?.Report(ProgressUpdate.Info(30, "Querying fastboot variables..."));

            try
            {
                var psi = new ProcessStartInfo("fastboot", "getvar all")
                {
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };

                using var proc = Process.Start(psi);
                if (proc == null) return status;

                var output = await proc.StandardError.ReadToEndAsync(ct);
                await proc.WaitForExitAsync(ct);

                // Look for FRP-related variables
                if (output.Contains("frp: on", StringComparison.OrdinalIgnoreCase) ||
                    output.Contains("frp-lock: 1", StringComparison.OrdinalIgnoreCase))
                {
                    status.Status = FrpLockStatus.Locked;
                }
                else if (output.Contains("frp: off", StringComparison.OrdinalIgnoreCase) ||
                         output.Contains("frp-lock: 0", StringComparison.OrdinalIgnoreCase))
                {
                    status.Status = FrpLockStatus.Unlocked;
                }
            }
            catch { /* Fall through to unknown */ }

            return status;
        }

        private async Task<FrpStatus> DetectFrpViaAdb(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress,
            CancellationToken ct)
        {
            var status = new FrpStatus { DetectionMethod = FrpDetectionMethod.AdbSettings };
            progress?.Report(ProgressUpdate.Info(30, "Querying device settings..."));

            try
            {
                // Check if user setup is complete
                var setupComplete = await RunAdbCommand("shell settings get secure user_setup_complete", ct);
                
                // Check for registered accounts
                var accounts = await RunAdbCommand("shell pm list packages | grep -E 'google\\.android\\.gms'", ct);

                if (setupComplete.Trim() == "0")
                {
                    status.Status = FrpLockStatus.Locked;
                    status.Notes = "Setup not complete - FRP likely active";
                }
                else if (setupComplete.Trim() == "1")
                {
                    status.Status = FrpLockStatus.Unlocked;
                }
            }
            catch { /* Fall through */ }

            return status;
        }

        private async Task<FrpStatus> DetectFrpViaEdl(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress,
            CancellationToken ct)
        {
            var status = new FrpStatus { DetectionMethod = FrpDetectionMethod.EdlPartitionRead };
            progress?.Report(ProgressUpdate.Info(30, "Analyzing FRP partition via EDL..."));

            if (_firehose == null) return status;

            // Determine FRP partition name for this brand
            var partitions = GetFrpPartitions(device.Brand);
            
            foreach (var partName in partitions)
            {
                try
                {
                    var data = await _firehose.ReadPartitionAsync(partName, null, ct);
                    if (data != null && data.Length > 0)
                    {
                        status.FrpPartitionName = partName;
                        status.FrpPartitionSize = (ulong)data.Length;
                        status.PartitionHasData = !IsPartitionEmpty(data);

                        if (status.PartitionHasData)
                        {
                            status.Status = FrpLockStatus.Locked;
                            status.AccountHint = TryExtractAccountHint(data);
                        }
                        else
                        {
                            status.Status = FrpLockStatus.Unlocked;
                        }
                        break;
                    }
                }
                catch { continue; }
            }

            return status;
        }

        #endregion

        #region Screen Lock Diagnostics

        public async Task<ScreenLockStatus> InspectLockAsync(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress = null,
            CancellationToken ct = default)
        {
            var status = new ScreenLockStatus
            {
                AndroidVersion = device.AndroidVersion,
                SecurityLevel = DetermineSecurityLevel(device.AndroidVersion)
            };

            progress?.Report(ProgressUpdate.Info(20, "Analyzing lock status..."));

            // Can only detect via ADB when device is accessible
            if (device.Mode == ConnectionMode.ADB)
            {
                try
                {
                    var lockType = await RunAdbCommand(
                        "shell settings get secure lockscreen.password_type", ct);
                    
                    status.IsLockEnabled = !string.IsNullOrEmpty(lockType) && lockType.Trim() != "0";
                    status.LockType = ParseLockType(lockType);
                }
                catch
                {
                    status.Warnings.Add("Could not query lock settings");
                }
            }
            else
            {
                status.IsLockEnabled = null;
                status.Warnings.Add($"Lock status cannot be determined in {device.Mode} mode");
            }

            // Data recovery assessment
            status.CanRecoverDataWithoutCredential = 
                status.SecurityLevel == LockSecurityLevel.Legacy;

            status.AvailableOptions = GenerateLockRecoveryOptions(device, status);

            progress?.Report(ProgressUpdate.Info(100, "Lock analysis complete"));
            return status;
        }

        private LockSecurityLevel DetermineSecurityLevel(string? version)
        {
            if (string.IsNullOrEmpty(version)) return LockSecurityLevel.Unknown;

            if (int.TryParse(version.Split('.')[0], out int major))
            {
                return major switch
                {
                    <= 6 => LockSecurityLevel.Legacy,
                    <= 9 => LockSecurityLevel.Gatekeeper,
                    _ => LockSecurityLevel.GatekeeperWeaver
                };
            }
            return LockSecurityLevel.Unknown;
        }

        private LockType ParseLockType(string value)
        {
            // Android lockscreen.password_type values
            return value.Trim() switch
            {
                "65536" => LockType.Pattern,
                "131072" or "196608" => LockType.PIN,
                "262144" or "327680" or "393216" => LockType.Password,
                "0" or "" => LockType.None,
                _ => LockType.Unknown
            };
        }

        #endregion

        #region Combined Diagnostics

        public async Task<LockFrpDiagnostics> FullDiagnosticsAsync(
            DeviceContext device,
            IProgress<ProgressUpdate>? progress = null,
            CancellationToken ct = default)
        {
            progress?.Report(ProgressUpdate.Info(0, "Running full Lock & FRP diagnostics..."));

            var frpStatus = await InspectFrpAsync(device, progress, ct);
            var lockStatus = await InspectLockAsync(device, progress, ct);

            return new LockFrpDiagnostics
            {
                Device = device,
                FrpStatus = frpStatus,
                LockStatus = lockStatus,
                ScanTime = DateTime.UtcNow
            };
        }

        public async Task<string> ExportReportAsync(
            LockFrpDiagnostics diagnostics,
            string outputPath,
            CancellationToken ct = default)
        {
            var report = new
            {
                GeneratedAt = DateTime.UtcNow,
                Device = new
                {
                    diagnostics.Device.Brand,
                    diagnostics.Device.Model,
                    diagnostics.Device.Serial,
                    diagnostics.Device.AndroidVersion,
                    ConnectionMode = diagnostics.Device.Mode.ToString()
                },
                FRP = diagnostics.FrpStatus,
                ScreenLock = diagnostics.LockStatus,
                Summary = diagnostics.Summary
            };

            var json = JsonSerializer.Serialize(report, new JsonSerializerOptions { WriteIndented = true });
            await File.WriteAllTextAsync(outputPath, json, ct);
            return outputPath;
        }

        #endregion

        #region Helper Methods

        private string[] GetFrpPartitions(string? brand)
        {
            if (string.IsNullOrEmpty(brand)) return new[] { "frp" };
            return OemFrpPartitions.TryGetValue(brand, out var parts) ? parts : new[] { "frp" };
        }

        private OemFrpInfo GetOemInfo(string? brand)
        {
            if (string.IsNullOrEmpty(brand)) 
                return new OemFrpInfo { OemName = "Unknown" };
            
            return OemInfoDatabase.TryGetValue(brand, out var info) 
                ? info 
                : new OemFrpInfo { OemName = brand };
        }

        private bool IsPartitionEmpty(byte[] data)
        {
            // Check if partition is all 0x00 or 0xFF
            foreach (var b in data)
            {
                if (b != 0x00 && b != 0xFF) return false;
            }
            return true;
        }

        private string? TryExtractAccountHint(byte[] data)
        {
            // Simple heuristic: look for email-like patterns
            var text = System.Text.Encoding.UTF8.GetString(data);
            var match = System.Text.RegularExpressions.Regex.Match(
                text, @"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}");
            
            if (match.Success)
            {
                // Mask the email
                var email = match.Value;
                var atIdx = email.IndexOf('@');
                if (atIdx > 2)
                {
                    return email[0] + "***" + email[(atIdx - 1)..];
                }
            }
            return null;
        }

        private List<string> GenerateFrpRecommendations(FrpStatus status)
        {
            var actions = new List<string>();

            if (status.Status == FrpLockStatus.Locked)
            {
                actions.Add("Sign in with the Google account previously linked to this device");
                actions.Add("Use Google Account Recovery: accounts.google.com/signin/recovery");

                if (status.OemInfo?.HasOemAccountLock == true)
                {
                    actions.Add($"Check {status.OemInfo.OemAccountType}: {status.OemInfo.OfficialUnlockUrl}");
                }
            }
            else if (status.Status == FrpLockStatus.Unlocked)
            {
                actions.Add("No FRP bypass needed - proceed with normal setup");
            }

            return actions;
        }

        private List<RecoveryOption> GenerateLockRecoveryOptions(DeviceContext device, ScreenLockStatus status)
        {
            var options = new List<RecoveryOption>
            {
                new()
                {
                    Name = "Google Find My Device",
                    Description = "Remote unlock if enabled before lock",
                    Type = RecoveryOptionType.GoogleFindMyDevice,
                    ResultsInDataLoss = false
                },
                new()
                {
                    Name = "Factory Reset (Fastboot)",
                    Description = "Complete data wipe via fastboot -w",
                    Type = RecoveryOptionType.FactoryResetFastboot,
                    ResultsInDataLoss = true
                },
                new()
                {
                    Name = "Factory Reset (Recovery)",
                    Description = "Wipe data via recovery menu",
                    Type = RecoveryOptionType.FactoryResetRecovery,
                    ResultsInDataLoss = true
                }
            };

            // Add OEM-specific options
            if (device.Brand?.ToUpperInvariant() == "SAMSUNG")
            {
                options.Insert(1, new RecoveryOption
                {
                    Name = "Samsung Find My Mobile",
                    Description = "Remote unlock via Samsung account",
                    Type = RecoveryOptionType.OemRemoteUnlock,
                    ResultsInDataLoss = false
                });
            }

            return options;
        }

        private async Task<string> RunAdbCommand(string args, CancellationToken ct)
        {
            var psi = new ProcessStartInfo("adb", args)
            {
                RedirectStandardOutput = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };

            using var proc = Process.Start(psi);
            if (proc == null) return "";

            var output = await proc.StandardOutput.ReadToEndAsync(ct);
            await proc.WaitForExitAsync(ct);
            return output;
        }

        #endregion
    }
}
