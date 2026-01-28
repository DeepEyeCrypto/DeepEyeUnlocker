using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.Cloak.Root
{
    public class RootCloakManager
    {
        private readonly IAdbClient _adb;
        private readonly MagiskController _magisk;
        private readonly ShamikoController _shamiko;

        public RootCloakManager(IAdbClient adb)
        {
            _adb = adb;
            _magisk = new MagiskController(adb);
            _shamiko = new ShamikoController(adb);
        }

        public async Task<RootCloakStatus> InspectAsync()
        {
            var status = new RootCloakStatus();
            
            // Basic detection
            string suCheck = await _adb.ExecuteShellAsync("which su");
            status.IsRooted = !string.IsNullOrEmpty(suCheck);

            if (status.IsRooted)
            {
                status.MagiskVersion = await _magisk.GetVersionAsync();
                status.IsMagiskInstalled = !string.IsNullOrEmpty(status.MagiskVersion);
                status.ZygiskActive = await _magisk.IsZygiskEnabledAsync();
                status.ShamikoActive = await _shamiko.IsInstalledAsync();
                status.EnforceDenyListOff = !await _magisk.IsEnforceDenyListEnabledAsync();
            }

            return status;
        }

        public async Task<bool> OptimizeForBankingAsync(IEnumerable<string> packageNames)
        {
            try
            {
                // 1. Ensure Zygisk is on
                if (!await _magisk.IsZygiskEnabledAsync())
                {
                    await _magisk.SetZygiskEnabledAsync(true);
                }

                // 2. Ensure Shamiko is handled (Enforce DenyList must be OFF for Shamiko)
                if (await _magisk.IsEnforceDenyListEnabledAsync())
                {
                    await _magisk.SetEnforceDenyListEnabledAsync(false);
                }

                // 3. Add packages to DenyList
                foreach (var pkg in packageNames)
                {
                    await _magisk.AddToDenyListAsync(pkg);
                }

                return true;
            }
            catch
            {
                return false;
            }
        }
    }
}
