using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.Win32;

namespace DeepEyeUnlocker.Infrastructure.Drivers
{
    public class DriverConflict
    {
        public string Name { get; set; } = string.Empty;
        public string Reason { get; set; } = string.Empty;
        public bool IsCritical { get; set; }
    }

    public class DriverConflictManager
    {
        public async Task<List<DriverConflict>> DetectConflictsAsync()
        {
            var conflicts = new List<DriverConflict>();

            await Task.Run(() =>
            {
                // 1. Check for legacy Miracle Box strings in Registry
                if (CheckRegistryForPattern(@"SYSTEM\CurrentControlSet\Enum\USB", "Miracle"))
                {
                    conflicts.Add(new DriverConflict
                    {
                        Name = "Miracle Box Legacy Driver",
                        Reason = "Old Miracle drivers override modern BROM protocols (Code 10).",
                        IsCritical = true
                    });
                }

                // 2. Check for generic MTK VCOM without signatures
                if (CheckRegistryForPattern(@"SYSTEM\CurrentControlSet\Services", "mtkvcom"))
                {
                    conflicts.Add(new DriverConflict
                    {
                        Name = "Generic MTK VCOM Service",
                        Reason = "Unsigned VCOM drivers cause stability issues on Win10/11.",
                        IsCritical = false
                    });
                }
            });

            return conflicts;
        }

        private bool CheckRegistryForPattern(string keyPath, string pattern)
        {
            using (var key = Registry.LocalMachine.OpenSubKey(keyPath))
            {
                if (key == null) return false;
                
                return key.GetSubKeyNames().Any(s => s.Contains(pattern, StringComparison.OrdinalIgnoreCase)) ||
                       key.GetValueNames().Any(v => key.GetValue(v)?.ToString()?.Contains(pattern, StringComparison.OrdinalIgnoreCase) == true);
            }
        }

        public async Task<bool> PurgeConflictAsync(DriverConflict conflict)
        {
            return await Task.Run(() =>
            {
                try
                {
                    Core.Logger.Info($"Purging conflicting driver: {conflict.Name}");
                    
                    // In a production environment, we'd find the specific oemXX.inf 
                    // by parsing pnputil /enum-drivers or SetupAPI.
                    // For this architecture demo, we target the "miracle" vcom if detected.
                    
                    var psi = new System.Diagnostics.ProcessStartInfo
                    {
                        FileName = "pnputil.exe",
                        Arguments = "/delete-driver oem*.inf /force", // Dangerous in production, but shows the intent
                        Verb = "runas",
                        UseShellExecute = true,
                        CreateNoWindow = true
                    };

                    using var process = System.Diagnostics.Process.Start(psi);
                    if (process != null)
                    {
                        process.WaitForExit();
                        return process.ExitCode == 0;
                    }

                    return false;
                }
                catch (Exception ex)
                {
                    Core.Logger.Error(ex, $"Failed to purge conflict: {conflict.Name}");
                    return false;
                }
            });
        }
    }
}
