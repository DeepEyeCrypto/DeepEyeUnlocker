using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.Cloak.Root
{
    public class ShamikoController
    {
        private readonly IAdbClient _adb;

        public ShamikoController(IAdbClient adb)
        {
            _adb = adb;
        }

        public async Task<bool> IsInstalledAsync()
        {
            var result = await _adb.ExecuteShellAsync("ls /data/adb/modules/zygisk_shamiko/");
            return !result.Contains("No such file or directory") && !string.IsNullOrEmpty(result);
        }

        public async Task<bool> IsEnabledAsync()
        {
            var result = await _adb.ExecuteShellAsync("ls /data/adb/modules/zygisk_shamiko/disable");
            return result.Contains("No such file or directory");
        }

        public async Task SetWhitelistModeAsync(bool enabled)
        {
            if (enabled)
                await _adb.ExecuteShellAsync("touch /data/adb/shamiko/whitelist");
            else
                await _adb.ExecuteShellAsync("rm /data/adb/shamiko/whitelist");
        }
    }
}
