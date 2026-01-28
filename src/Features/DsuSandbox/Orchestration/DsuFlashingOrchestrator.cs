using System;
using System.Diagnostics;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.DsuSandbox.Models;
using DeepEyeUnlocker.Features.DsuSandbox.Validation;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.Features.DsuSandbox.Orchestration
{
    /// <summary>
    /// Orchestrates DSU flashing workflow
    /// </summary>
    public class DsuFlashingOrchestrator
    {
        private readonly IAdbClient _adb;
        private readonly DsuImageValidator _imageValidator;
        private readonly DeviceCapabilityChecker _capabilityChecker;

        private const int DSU_REBOOT_TIMEOUT_SECONDS = 300;
        private const int BOOT_WAIT_INTERVAL_MS = 5000;

        public DsuFlashingOrchestrator(IAdbClient adb)
        {
            _adb = adb;
            _imageValidator = new DsuImageValidator();
            _capabilityChecker = new DeviceCapabilityChecker(adb);
        }

        /// <summary>
        /// Flash DSU image to device
        /// </summary>
        public async Task<BootHealthReport> FlashDsuAsync(
            DeviceContext device,
            DsuImage image,
            DsuTestMethod method,
            IProgress<DsuFlashProgress>? progress,
            CancellationToken ct)
        {
            var stopwatch = Stopwatch.StartNew();
            var totalStages = 5;

            try
            {
                // Stage 1: Pre-flight validation
                ReportProgress(progress, 1, totalStages, "Pre-flight checks", 0);
                Logger.Info($"Starting DSU flash: {image.Name} via {method}");

                var capability = await _capabilityChecker.CheckCapabilityAsync(device, ct);
                var validation = await _imageValidator.ValidateAsync(image.LocalPath, device, capability);
                
                if (!validation.IsValid)
                {
                    throw new DsuFlashException($"Validation failed: {string.Join(", ", validation.Errors)}");
                }

                ct.ThrowIfCancellationRequested();
                ReportProgress(progress, 1, totalStages, "Pre-flight checks complete", 100);

                // Stage 2: Prepare device
                ReportProgress(progress, 2, totalStages, "Preparing device", 0);
                await PrepareDeviceAsync(method, ct);
                ReportProgress(progress, 2, totalStages, "Device ready", 100);

                // Stage 3: Push/Flash image
                ReportProgress(progress, 3, totalStages, "Transferring image", 0);
                await FlashImageAsync(image, method, progress, ct);
                ReportProgress(progress, 3, totalStages, "Image transferred", 100);

                // Stage 4: Reboot to DSU
                ReportProgress(progress, 4, totalStages, "Rebooting to test ROM", 0);
                await RebootToDsuAsync(method, ct);
                ReportProgress(progress, 4, totalStages, "Reboot initiated", 100);

                // Stage 5: Wait for boot and validate
                ReportProgress(progress, 5, totalStages, "Waiting for boot", 0);
                var healthReport = await WaitForBootAndValidateAsync(stopwatch, progress, ct);
                
                ReportProgress(progress, 5, totalStages, 
                    healthReport.IsHealthy ? "Boot successful!" : "Boot completed with issues", 
                    100, isComplete: true);

                return healthReport;
            }
            catch (OperationCanceledException)
            {
                Logger.Warn("DSU flash cancelled by user");
                throw;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "DSU flash failed");
                ReportProgress(progress, 0, totalStages, $"Failed: {ex.Message}", 0, hasError: true, errorMessage: ex.Message);
                throw new DsuFlashException($"DSU flash failed: {ex.Message}", ex);
            }
        }

        /// <summary>
        /// Revert to original system
        /// </summary>
        public async Task RevertToOriginalAsync(CancellationToken ct = default)
        {
            Logger.Info("Reverting to original system...");

            try
            {
                // Clear DSU flag and reboot
                await _adb.ExecuteShellAsync("setprop persist.sys.dsu.enable 0");
                await _adb.ExecuteShellAsync("rm -rf /data/gsi");
                await _adb.ExecuteShellAsync("reboot");

                Logger.Info("Revert initiated - device will boot to original system");
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to revert DSU");
                // Fallback: simple reboot should work due to DSU safety
                await _adb.ExecuteShellAsync("reboot");
            }
        }

        /// <summary>
        /// Set alternate slot as active (for A/B method)
        /// </summary>
        public async Task SetActiveSlotAsync(string slot, CancellationToken ct = default)
        {
            Logger.Info($"Setting active slot to: {slot}");
            
            // Need fastboot for this
            await _adb.ExecuteShellAsync("reboot bootloader");
            await Task.Delay(5000, ct);

            // Use fastboot to set slot
            var fastboot = new Process
            {
                StartInfo = new ProcessStartInfo
                {
                    FileName = "fastboot",
                    Arguments = $"--set-active={slot}",
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                }
            };

            fastboot.Start();
            await fastboot.WaitForExitAsync(ct);

            if (fastboot.ExitCode != 0)
            {
                var error = await fastboot.StandardError.ReadToEndAsync();
                throw new DsuFlashException($"Failed to set active slot: {error}");
            }

            // Reboot
            var reboot = new Process
            {
                StartInfo = new ProcessStartInfo
                {
                    FileName = "fastboot",
                    Arguments = "reboot",
                    UseShellExecute = false,
                    CreateNoWindow = true
                }
            };
            reboot.Start();
            await reboot.WaitForExitAsync(ct);
        }

        private async Task PrepareDeviceAsync(DsuTestMethod method, CancellationToken ct)
        {
            // Ensure device is connected
            if (!_adb.IsConnected())
            {
                throw new DsuFlashException("Device not connected");
            }

            // For A/B slot, check bootloader unlock status
            if (method == DsuTestMethod.ABSlot)
            {
                var lockStatus = await _adb.ExecuteShellAsync("getprop ro.boot.flash.locked");
                if (lockStatus?.Trim() == "1")
                {
                    throw new DsuFlashException("Bootloader is locked. A/B slot method requires unlocked bootloader.");
                }
            }

            // Ensure sufficient battery
            var batteryLevel = await _adb.ExecuteShellAsync("dumpsys battery | grep level");
            if (int.TryParse(batteryLevel?.Replace("level:", "").Trim(), out var level) && level < 30)
            {
                throw new DsuFlashException($"Battery too low ({level}%). Charge to at least 50% before flashing.");
            }
        }

        private async Task FlashImageAsync(DsuImage image, DsuTestMethod method, 
            IProgress<DsuFlashProgress>? progress, CancellationToken ct)
        {
            switch (method)
            {
                case DsuTestMethod.DsuAdb:
                    await FlashViaDsuAdbAsync(image, progress, ct);
                    break;
                case DsuTestMethod.DsuRecovery:
                    await FlashViaDsuRecoveryAsync(image, progress, ct);
                    break;
                case DsuTestMethod.ABSlot:
                    await FlashViaABSlotAsync(image, progress, ct);
                    break;
                default:
                    throw new DsuFlashException($"Unsupported flash method: {method}");
            }
        }

        private async Task FlashViaDsuAdbAsync(DsuImage image, IProgress<DsuFlashProgress>? progress, CancellationToken ct)
        {
            Logger.Info("Flashing via DSU ADB method...");

            // Create DSU directory
            await _adb.ExecuteShellAsync("mkdir -p /data/gsi");

            // Push image
            var fileInfo = new FileInfo(image.LocalPath);
            var totalBytes = fileInfo.Length;

            progress?.Report(new DsuFlashProgress
            {
                Stage = "Transferring image",
                StageNumber = 3,
                TotalStages = 5,
                PercentComplete = 0,
                BytesTransferred = 0,
                TotalBytes = totalBytes,
                Message = $"Pushing: 0 / {totalBytes / (1024 * 1024)} MB"
            });

            // Use adb push (note: progress monitoring would require custom implementation)
            var success = await _adb.PushFileAsync(image.LocalPath, "/data/gsi/system.img");
            if (!success)
            {
                throw new DsuFlashException("Failed to push system image to device");
            }

            progress?.Report(new DsuFlashProgress
            {
                Stage = "Transferring image",
                StageNumber = 3,
                TotalStages = 5,
                PercentComplete = 100,
                BytesTransferred = totalBytes,
                TotalBytes = totalBytes,
                Message = $"Push complete: {totalBytes / (1024 * 1024)} MB"
            });

            // Enable DSU
            await _adb.ExecuteShellAsync("setprop persist.sys.dsu.enable 1");
            await _adb.ExecuteShellAsync("gsi_tool enable -s /data/gsi/system.img");
        }

        private async Task FlashViaDsuRecoveryAsync(DsuImage image, IProgress<DsuFlashProgress>? progress, CancellationToken ct)
        {
            Logger.Info("Flashing via DSU Recovery method...");

            // Reboot to recovery
            await _adb.ExecuteShellAsync("reboot recovery");
            await Task.Delay(10000, ct); // Wait for recovery

            // Use adb sideload
            var process = new Process
            {
                StartInfo = new ProcessStartInfo
                {
                    FileName = "adb",
                    Arguments = $"sideload \"{image.LocalPath}\"",
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                }
            };

            process.Start();

            // Monitor sideload progress
            while (!process.HasExited)
            {
                ct.ThrowIfCancellationRequested();
                await Task.Delay(1000, ct);
                // Sideload progress parsing would go here
            }

            if (process.ExitCode != 0)
            {
                var error = await process.StandardError.ReadToEndAsync();
                throw new DsuFlashException($"Sideload failed: {error}");
            }
        }

        private async Task FlashViaABSlotAsync(DsuImage image, IProgress<DsuFlashProgress>? progress, CancellationToken ct)
        {
            Logger.Info("Flashing via A/B slot method...");

            // Reboot to bootloader
            await _adb.ExecuteShellAsync("reboot bootloader");
            await Task.Delay(5000, ct);

            // Flash to alternate slot
            var currentSlot = await GetCurrentSlotAsync();
            var targetSlot = currentSlot == "a" ? "b" : "a";

            var process = new Process
            {
                StartInfo = new ProcessStartInfo
                {
                    FileName = "fastboot",
                    Arguments = $"flash system --slot={targetSlot} \"{image.LocalPath}\"",
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                }
            };

            process.Start();
            await process.WaitForExitAsync(ct);

            if (process.ExitCode != 0)
            {
                var error = await process.StandardError.ReadToEndAsync();
                throw new DsuFlashException($"Fastboot flash failed: {error}");
            }

            // Set alternate slot as active
            await SetActiveSlotAsync(targetSlot, ct);
        }

        private async Task<string> GetCurrentSlotAsync()
        {
            var slot = await _adb.ExecuteShellAsync("getprop ro.boot.slot_suffix");
            return slot?.Trim()?.TrimStart('_') ?? "a";
        }

        private async Task RebootToDsuAsync(DsuTestMethod method, CancellationToken ct)
        {
            if (method == DsuTestMethod.ABSlot)
            {
                // Already handled in flash method
                return;
            }

            Logger.Info("Rebooting to DSU system...");
            await _adb.ExecuteShellAsync("reboot");
        }

        private async Task<BootHealthReport> WaitForBootAndValidateAsync(
            Stopwatch totalStopwatch,
            IProgress<DsuFlashProgress>? progress,
            CancellationToken ct)
        {
            var bootStopwatch = Stopwatch.StartNew();
            var report = new BootHealthReport();

            Logger.Info("Waiting for device to boot...");

            // Wait for device to come online
            while (bootStopwatch.Elapsed.TotalSeconds < DSU_REBOOT_TIMEOUT_SECONDS)
            {
                ct.ThrowIfCancellationRequested();
                await Task.Delay(BOOT_WAIT_INTERVAL_MS, ct);

                var waitPercent = (int)((bootStopwatch.Elapsed.TotalSeconds * 100) / DSU_REBOOT_TIMEOUT_SECONDS);
                progress?.Report(new DsuFlashProgress
                {
                    Stage = "Waiting for boot",
                    StageNumber = 5,
                    TotalStages = 5,
                    PercentComplete = Math.Min(waitPercent, 99),
                    Message = $"Waiting... {(int)bootStopwatch.Elapsed.TotalSeconds}s"
                });

                if (_adb.IsConnected())
                {
                    // Check if fully booted
                    var bootComplete = await _adb.ExecuteShellAsync("getprop sys.boot_completed");
                    if (bootComplete?.Trim() == "1")
                    {
                        Logger.Info($"Device booted in {bootStopwatch.Elapsed.TotalSeconds:F1}s");
                        break;
                    }
                }
            }

            // Validate boot
            report.AdbResponsive = _adb.IsConnected();
            report.BootTimeSeconds = (int)bootStopwatch.Elapsed.TotalSeconds;
            report.BootTimestamp = DateTime.Now;

            if (report.AdbResponsive)
            {
                report.OsVersion = await _adb.ExecuteShellAsync("getprop ro.build.version.release") ?? "";
                report.BuildFingerprint = await _adb.ExecuteShellAsync("getprop ro.build.fingerprint") ?? "";
                report.SlotSuffix = await _adb.ExecuteShellAsync("getprop ro.boot.slot_suffix") ?? "";
                
                // Check if running DSU
                var dsuStatus = await _adb.ExecuteShellAsync("getprop ro.gsid.image_running");
                report.IsDsuBoot = dsuStatus?.Trim() == "1";

                // Check Play Services
                var gmsVersion = await _adb.ExecuteShellAsync("pm list packages | grep com.google.android.gms");
                report.PlayServicesPresent = !string.IsNullOrEmpty(gmsVersion);

                // Check for crash loops (basic heuristic)
                var logcat = await _adb.ExecuteShellAsync("logcat -d -s AndroidRuntime:E | tail -20");
                report.CrashLoopsDetected = logcat?.Contains("FATAL EXCEPTION") == true;

                if (report.CrashLoopsDetected)
                {
                    report.BootErrors.Add("Crash loops detected in logcat");
                }
            }
            else
            {
                report.BootErrors.Add("Device not responding after boot timeout");
            }

            return report;
        }

        private void ReportProgress(IProgress<DsuFlashProgress>? progress, int stage, int totalStages, 
            string message, int percent, bool isComplete = false, bool hasError = false, string? errorMessage = null)
        {
            progress?.Report(new DsuFlashProgress
            {
                StageNumber = stage,
                TotalStages = totalStages,
                Stage = message,
                Message = message,
                PercentComplete = percent,
                IsComplete = isComplete,
                HasError = hasError,
                ErrorMessage = errorMessage ?? ""
            });
        }
    }

    /// <summary>
    /// DSU flashing exception
    /// </summary>
    public class DsuFlashException : Exception
    {
        public DsuFlashException(string message) : base(message) { }
        public DsuFlashException(string message, Exception inner) : base(message, inner) { }
    }
}
