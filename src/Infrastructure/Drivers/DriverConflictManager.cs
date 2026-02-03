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

                // 3. LibUsb-Win32 Filter Global Audit
                if (IsFilterPresent("libusb0"))
                {
                    conflicts.Add(new DriverConflict
                    {
                        Name = "LibUsb-Win32 Filter (libusb0)",
                        Reason = "Active filter detected on USB/Ports class. Prevents protocol handshakes.",
                        IsCritical = true
                    });
                }
            });

            return conflicts;
        }

        private bool IsFilterPresent(string filterName)
        {
            // Check Class Upper/Lower Filters for USB and Ports
            string[] classes = { 
                "{36FC9E60-C465-11CF-8056-444553540000}", // USB
                "{4D36E978-E325-11CE-BFC1-08002BE10318}"  // Ports
            };

            foreach (var clsid in classes)
            {
                using (var key = Registry.LocalMachine.OpenSubKey($@"SYSTEM\CurrentControlSet\Control\Class\{clsid}"))
                {
                    if (key == null) continue;
                    
                    var upper = key.GetValue("UpperFilters") as string[];
                    var lower = key.GetValue("LowerFilters") as string[];

                    if (upper != null && upper.Any(f => f.Contains(filterName, StringComparison.OrdinalIgnoreCase))) return true;
                    if (lower != null && lower.Any(f => f.Contains(filterName, StringComparison.OrdinalIgnoreCase))) return true;
                }
            }
            return false;
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
            return await Task.Run(async () =>
            {
                try
                {
                    Core.Logger.Info($"Purging conflicting driver: {conflict.Name}");
                    
                    if (conflict.Name.Contains("Filter"))
                    {
                        return await ResolveFilterConflictAsync(conflict);
                    }

                    var psi = new System.Diagnostics.ProcessStartInfo
                    {
                        FileName = "pnputil.exe",
                        Arguments = "/delete-driver oem*.inf /force",
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

        private async Task<bool> ResolveFilterConflictAsync(DriverConflict conflict)
        {
            string filterToRemove = conflict.Name.Contains("libusb0") ? "libusb0" : "";
            if (string.IsNullOrEmpty(filterToRemove)) return false;

            string[] classes = { 
                "{36FC9E60-C465-11CF-8056-444553540000}", 
                "{4D36E978-E325-11CE-BFC1-08002BE10318}" 
            };

            bool anyChanges = false;
            foreach (var clsid in classes)
            {
                if (RemoveFilterFromClass(clsid, filterToRemove, true)) anyChanges = true;
                if (RemoveFilterFromClass(clsid, filterToRemove, false)) anyChanges = true;
            }

            return anyChanges;
        }

        private bool RemoveFilterFromClass(string clsid, string filter, bool isUpper)
        {
            string valueName = isUpper ? "UpperFilters" : "LowerFilters";
            try
            {
                using (var key = Registry.LocalMachine.OpenSubKey($@"SYSTEM\CurrentControlSet\Control\Class\{clsid}", true))
                {
                    if (key == null) return false;
                    
                    var existing = key.GetValue(valueName) as string[];
                    if (existing == null) return false;

                    var filtered = existing.Where(f => !f.Contains(filter, StringComparison.OrdinalIgnoreCase)).ToArray();
                    if (filtered.Length < existing.Length)
                    {
                        if (filtered.Length > 0)
                            key.SetValue(valueName, filtered, RegistryValueKind.MultiString);
                        else
                            key.DeleteValue(valueName);
                        
                        Core.Logger.Info($"Removed {filter} from {clsid} ({valueName})");
                        return true;
                    }
                }
            }
            catch { }
            return false;
        }
    }
}
