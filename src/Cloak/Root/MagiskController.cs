using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.Cloak.Root
{
    public class MagiskController
    {
        private readonly IAdbClient _adb;

        public MagiskController(IAdbClient adb)
        {
            _adb = adb;
        }

        public async Task<string> GetVersionAsync()
        {
            var result = await _adb.ExecuteShellAsync("magisk -v");
            return result.Trim();
        }

        public async Task<bool> IsZygiskEnabledAsync()
        {
            var result = await _adb.ExecuteShellAsync("magisk --zygisk");
            return result.Contains("1") || result.Contains("true");
        }

        public async Task SetZygiskEnabledAsync(bool enabled)
        {
            // Note: This usually requires a reboot to take effect
            // In a real implementation, we might use resetprop or edit magisk config files
            await _adb.ExecuteShellAsync($"magisk --zygisk {(enabled ? "1" : "0")}");
        }

        public async Task<bool> IsEnforceDenyListEnabledAsync()
        {
            var result = await _adb.ExecuteShellAsync("magisk --denylist status");
            return result.Contains("Enforce: 1") || result.Contains("Enforce: true");
        }

        public async Task SetEnforceDenyListEnabledAsync(bool enabled)
        {
            if (enabled)
                await _adb.ExecuteShellAsync("magisk --denylist add-enforce");
            else
                await _adb.ExecuteShellAsync("magisk --denylist rm-enforce");
        }

        public async Task AddToDenyListAsync(string packageName)
        {
            await _adb.ExecuteShellAsync($"magisk --denylist add {packageName}");
        }
    }
}
