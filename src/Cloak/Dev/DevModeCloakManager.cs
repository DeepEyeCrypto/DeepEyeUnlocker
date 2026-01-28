using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.Cloak.Dev
{
    public class DevModeCloakManager
    {
        private readonly IAdbClient _adb;

        public DevModeCloakManager(IAdbClient adb)
        {
            _adb = adb;
        }

        public async Task<DevModeStatus> InspectAsync()
        {
            var status = new DevModeStatus();
            
            var devSettings = await _adb.ExecuteShellAsync("settings get secure development_settings_enabled");
            status.DeveloperOptionsEnabled = devSettings.Trim() == "1";

            var adbEnabled = await _adb.ExecuteShellAsync("settings get global adb_enabled");
            status.UsbDebuggingEnabled = adbEnabled.Trim() == "1";

            var roDebuggable = await _adb.ExecuteShellAsync("getprop ro.debuggable");
            status.SystemDebuggable = roDebuggable.Trim() == "1";

            return status;
        }

        public async Task ApplyStealthProfileAsync()
        {
            // Note: Disabling ADB will disconnect this session.
            await _adb.ExecuteShellAsync("settings put global adb_enabled 0");
            await _adb.ExecuteShellAsync("settings put secure development_settings_enabled 0");
        }

        public async Task RestoreOriginalDevSettingsAsync()
        {
            await _adb.ExecuteShellAsync("settings put secure development_settings_enabled 1");
            await _adb.ExecuteShellAsync("settings put global adb_enabled 1");
        }
    }
}
