using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure.USB;
using LibUsbDotNet;
using LibUsbDotNet.Main;

namespace DeepEyeUnlocker.Core.Engines
{
    /// <summary>
    /// Manages EDL (Emergency Download Mode) operations for Qualcomm devices
    /// </summary>
    public class EdlManager : IEdlManager
    {
        private readonly EdlProfileProvider _profiles;
        private readonly EdlRetryPolicy _retryPolicy;
        private readonly EdlSecurityPolicy _securityPolicy;

        // Known Qualcomm EDL VID:PID combinations
        private static readonly (int Vid, int Pid)[] EdlPids = 
        {
            (0x05C6, 0x9008), // Standard EDL
            (0x05C6, 0x9006), // Xiaomi variant
            (0x05C6, 0x900E), // DIAG (close to EDL)
            (0x05C6, 0xF006), // Legacy HS-USB QDLoader
        };

        public EdlManager() : this(new EdlProfileProvider(), new EdlRetryPolicy(), new EdlSecurityPolicy()) { }

        public EdlManager(EdlProfileProvider profiles, EdlRetryPolicy retryPolicy, EdlSecurityPolicy securityPolicy)
        {
            _profiles = profiles;
            _retryPolicy = retryPolicy;
            _securityPolicy = securityPolicy;
        }

        /// <inheritdoc />
        public async Task<EdlResult> RebootToEdlAsync(DeviceContext device, CancellationToken ct)
        {
            var stopwatch = Stopwatch.StartNew();
            var log = new StringBuilder();
            log.AppendLine($"[{DateTime.Now:HH:mm:ss}] Starting EDL reboot for {device.Brand} {device.Model}");
            log.AppendLine($"Current mode: {device.Mode}, Chipset: {device.Chipset}");

            // Get profile for decision making
            var profile = _profiles.GetProfileFor(device.Brand, device.Model ?? device.Chipset);
            if (profile != null)
            {
                log.AppendLine($"Profile found: {profile.Codename} ({profile.SoC})");
            }
            else
            {
                log.AppendLine("No specific profile found, will attempt standard methods");
            }

            // STEP 1: Try ADB path if device is in ADB mode
            if (device.Mode == ConnectionMode.ADB)
            {
                if (profile?.SupportsAdbRebootEdl != false) // Try if profile allows or unknown
                {
                    log.AppendLine("\n[ADB] Attempting: adb reboot edl");
                    
                    var adbResult = await ExecuteAdbCommandAsync("reboot edl", ct);
                    log.AppendLine($"[ADB] Response: {adbResult}");
                    
                    // Wait for device to start rebooting
                    await Task.Delay(_retryPolicy.InitialDelayMs, ct);
                    
                    // Scan for EDL mode
                    if (await WaitForEdlModeAsync(ct, _retryPolicy.MaxWaitSeconds))
                    {
                        stopwatch.Stop();
                        log.AppendLine($"[SUCCESS] Device entered EDL mode via ADB in {stopwatch.ElapsedMilliseconds}ms");
                        Logger.Success("Device entered EDL mode via ADB", "EDL");
                        
                        return new EdlResult 
                        { 
                            Success = true, 
                            MethodUsed = EdlAttemptMethod.AdbRebootEdl,
                            Log = log.ToString(),
                            ElapsedTime = stopwatch.Elapsed
                        };
                    }
                    
                    log.AppendLine("[ADB] Device did not appear in EDL mode");
                }
                else
                {
                    log.AppendLine("[ADB] Skipped - profile indicates ADB EDL not supported");
                }
            }

            // STEP 2: Try Fastboot path
            if (device.Mode == ConnectionMode.Fastboot || device.Mode == ConnectionMode.DownloadMode)
            {
                if (profile?.SupportsFastbootOemEdl != false)
                {
                    // Try fastboot oem edl
                    log.AppendLine("\n[FASTBOOT] Attempting: fastboot oem edl");
                    
                    var fbResult = await ExecuteFastbootCommandAsync("oem edl", ct);
                    log.AppendLine($"[FASTBOOT] Response: {fbResult}");

                    if (!fbResult.Contains("unknown command") && !fbResult.Contains("FAILED"))
                    {
                        await Task.Delay(_retryPolicy.InitialDelayMs, ct);
                        
                        if (await WaitForEdlModeAsync(ct, _retryPolicy.MaxWaitSeconds))
                        {
                            stopwatch.Stop();
                            log.AppendLine($"[SUCCESS] Device entered EDL mode via fastboot oem edl in {stopwatch.ElapsedMilliseconds}ms");
                            Logger.Success("Device entered EDL mode via fastboot oem edl", "EDL");
                            
                            return new EdlResult 
                            { 
                                Success = true, 
                                MethodUsed = EdlAttemptMethod.FastbootOemEdl,
                                Log = log.ToString(),
                                ElapsedTime = stopwatch.Elapsed
                            };
                        }
                    }

                    // Try alternate command: fastboot reboot-edl
                    log.AppendLine("\n[FASTBOOT] Attempting alternate: fastboot reboot-edl");
                    fbResult = await ExecuteFastbootCommandAsync("reboot-edl", ct);
                    log.AppendLine($"[FASTBOOT] Response: {fbResult}");
                    
                    await Task.Delay(_retryPolicy.InitialDelayMs, ct);
                    
                    if (await WaitForEdlModeAsync(ct, _retryPolicy.MaxWaitSeconds))
                    {
                        stopwatch.Stop();
                        log.AppendLine($"[SUCCESS] Device entered EDL mode via fastboot reboot-edl in {stopwatch.ElapsedMilliseconds}ms");
                        Logger.Success("Device entered EDL mode via fastboot reboot-edl", "EDL");
                        
                        return new EdlResult 
                        { 
                            Success = true, 
                            MethodUsed = EdlAttemptMethod.FastbootRebootEdl,
                            Log = log.ToString(),
                            ElapsedTime = stopwatch.Elapsed
                        };
                    }
                    
                    log.AppendLine("[FASTBOOT] Device did not appear in EDL mode");
                }
                else
                {
                    log.AppendLine("[FASTBOOT] Skipped - profile indicates Fastboot EDL not supported");
                }
            }

            // STEP 3: All software methods failed
            stopwatch.Stop();
            log.AppendLine($"\n[FAILED] All software EDL methods failed after {stopwatch.ElapsedMilliseconds}ms");

            string reason = DetermineFailureReason(profile, log.ToString());
            Logger.Warn($"EDL reboot failed: {reason}", "EDL");
            
            return new EdlResult
            {
                Success = false,
                FailureReason = reason,
                MethodUsed = EdlAttemptMethod.None,
                Log = log.ToString(),
                ElapsedTime = stopwatch.Elapsed
            };
        }

        /// <inheritdoc />
        public async Task<bool> IsInEdlModeAsync(CancellationToken ct)
        {
            try
            {
                foreach (UsbRegistry usb in UsbDevice.AllDevices)
                {
                    var vid = usb.Vid;
                    var pid = usb.Pid;
                    if (EdlPids.Any(e => vid == e.Vid && pid == e.Pid))
                    {
                        Logger.Debug($"EDL device found: {vid:X4}:{pid:X4}", "EDL");
                        return true;
                    }
                }
            }
            catch (Exception ex)
            {
                Logger.Error($"Error scanning for EDL devices: {ex.Message}", "EDL");
            }
            
            return false;
        }

        /// <inheritdoc />
        public async Task<bool> WaitForEdlModeAsync(CancellationToken ct, int timeoutSeconds = 15)
        {
            var deadline = DateTime.Now.AddSeconds(timeoutSeconds);
            int scanCount = 0;
            
            while (DateTime.Now < deadline)
            {
                if (ct.IsCancellationRequested) return false;
                
                scanCount++;
                if (await IsInEdlModeAsync(ct))
                {
                    Logger.Info($"EDL device detected after {scanCount} scans", "EDL");
                    return true;
                }
                
                await Task.Delay(_retryPolicy.ScanIntervalMs, ct);
            }
            
            Logger.Debug($"EDL not detected after {scanCount} scans ({timeoutSeconds}s timeout)", "EDL");
            return false;
        }

        /// <inheritdoc />
        public EdlCapability GetCapabilityFor(DeviceContext device)
        {
            var profile = _profiles.GetProfileFor(device.Brand, device.Model ?? "");
            if (profile != null)
                return profile.Capability;
            
            // Brand-level inference
            return InferCapabilityFromBrand(device);
        }

        /// <inheritdoc />
        public EdlProfile? GetProfileFor(DeviceContext device)
        {
            return _profiles.GetProfileFor(device.Brand, device.Model ?? device.Chipset);
        }

        /// <inheritdoc />
        public TestPointInfo? GetTestPointInfo(DeviceContext device)
        {
            return _profiles.GetTestPointInfo(device.Model ?? "");
        }

        #region Private Helpers

        private EdlCapability InferCapabilityFromBrand(DeviceContext device)
        {
            var brand = device.Brand?.ToLower() ?? "";
            var androidVersion = device.Properties.GetValueOrDefault("ro.build.version.release", "");
            
            return brand switch
            {
                "xiaomi" or "redmi" or "poco" => 
                    androidVersion.CompareTo("12") < 0 
                        ? EdlCapability.SOFTWARE_DIRECT_SUPPORTED 
                        : EdlCapability.SOFTWARE_RESTRICTED,
                    
                "samsung" => EdlCapability.HARDWARE_ONLY,
                
                "google" => EdlCapability.HARDWARE_ONLY,
                
                "oneplus" => 
                    androidVersion.CompareTo("11") < 0
                        ? EdlCapability.SOFTWARE_RESTRICTED
                        : EdlCapability.HARDWARE_ONLY,
                    
                "oppo" or "realme" or "vivo" => EdlCapability.HARDWARE_ONLY,
                
                "motorola" or "lenovo" => EdlCapability.SOFTWARE_RESTRICTED,
                
                "nokia" => EdlCapability.HARDWARE_ONLY,
                
                "huawei" or "honor" => EdlCapability.HARDWARE_ONLY, // Non-Qualcomm mostly
                
                _ => EdlCapability.UNKNOWN
            };
        }

        private string DetermineFailureReason(EdlProfile? profile, string log)
        {
            if (log.Contains("unknown command"))
                return "COMMAND REJECTED: This device's bootloader does not support the EDL reboot command. " +
                       "The OEM has likely removed this functionality.";
            
            if (log.Contains("not allowed") || log.Contains("permission"))
                return "PERMISSION DENIED: The bootloader rejected the EDL command. " +
                       "This may require an unlocked bootloader or authorized service tool.";
            
            if (log.Contains("device is locked") || log.Contains("locked"))
                return "BOOTLOADER LOCKED: EDL reboot requires an unlocked bootloader on this device.";
            
            if (profile?.RequiresTestPoint == true)
                return "HARDWARE EDL REQUIRED: This device only supports test-point EDL entry. " +
                       "Software commands are blocked.";
            
            if (profile?.RequiresAuthTool == true)
                return $"AUTH TOOL REQUIRED: This device requires {profile.AuthToolName ?? "an authorized service tool"} for EDL access.";
            
            return "DEVICE DID NOT ENTER EDL: The command was sent but the device did not " +
                   "enumerate as Qualcomm 9008. Check USB drivers and cable connection.";
        }

        private async Task<string> ExecuteAdbCommandAsync(string command, CancellationToken ct)
        {
            try
            {
                using var process = new Process
                {
                    StartInfo = new ProcessStartInfo
                    {
                        FileName = "adb",
                        Arguments = command,
                        RedirectStandardOutput = true,
                        RedirectStandardError = true,
                        UseShellExecute = false,
                        CreateNoWindow = true
                    }
                };
                
                process.Start();
                
                var output = await process.StandardOutput.ReadToEndAsync();
                var error = await process.StandardError.ReadToEndAsync();
                
                await process.WaitForExitAsync(ct);
                
                return string.IsNullOrEmpty(error) ? output : $"{output}\nError: {error}";
            }
            catch (Exception ex)
            {
                return $"ADB execution failed: {ex.Message}";
            }
        }

        private async Task<string> ExecuteFastbootCommandAsync(string command, CancellationToken ct)
        {
            try
            {
                using var process = new Process
                {
                    StartInfo = new ProcessStartInfo
                    {
                        FileName = "fastboot",
                        Arguments = command,
                        RedirectStandardOutput = true,
                        RedirectStandardError = true,
                        UseShellExecute = false,
                        CreateNoWindow = true
                    }
                };
                
                process.Start();
                
                var output = await process.StandardOutput.ReadToEndAsync();
                var error = await process.StandardError.ReadToEndAsync();
                
                await process.WaitForExitAsync(ct);
                
                // Fastboot often outputs to stderr even on success
                return $"{output}{error}".Trim();
            }
            catch (Exception ex)
            {
                return $"Fastboot execution failed: {ex.Message}";
            }
        }

        #endregion

        #region Static Helpers

        /// <summary>
        /// Get human-readable description of EDL capability
        /// </summary>
        public static string GetCapabilityDescription(EdlCapability capability)
        {
            return capability switch
            {
                EdlCapability.SOFTWARE_DIRECT_SUPPORTED =>
                    "This device likely supports software reboot to EDL. " +
                    "Success depends on firmware version and OEM configuration.",
                    
                EdlCapability.SOFTWARE_RESTRICTED =>
                    "Older firmware versions may support EDL, but recent updates " +
                    "have blocked this feature. Consider downgrading if possible.",
                    
                EdlCapability.HARDWARE_ONLY =>
                    "This device does not support software EDL entry. " +
                    "Physical test-point shorting is required to enter EDL mode.",
                    
                EdlCapability.UNKNOWN =>
                    "No EDL capability data available for this model. " +
                    "DeepEyeUnlocker will attempt standard methods but results are uncertain.",
                    
                _ => "Unknown capability"
            };
        }

        /// <summary>
        /// Check if a USB device is in EDL mode by VID/PID
        /// </summary>
        public static bool IsEdlDevice(int vid, int pid)
        {
            return EdlPids.Any(e => e.Vid == vid && e.Pid == pid);
        }

        #endregion
    }
}
