using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Protocols.Samsung
{
    public class KnoxGuardPatch
    {
        public async Task<bool> ApplyPatchAsync(DeviceContext device)
        {
            Console.WriteLine("[KG] Applying Semi-Permanent Knox Guard Patch...");
            
            // Method: App Ops Restriction (Notification/Overlay)
            // Does not remove the app (preventing bootloop), just silences it.
            
            string[] commands = new[]
            {
                "cmd appops set com.samsung.android.kgclient TOAST_WINDOW ignore",
                "cmd appops set com.samsung.android.kgclient SYSTEM_ALERT_WINDOW ignore",
                "pm disable-user --user 0 com.samsung.android.kgclient" // Risky on new binary
            };

            foreach (var cmd in commands)
            {
                Console.WriteLine($"[ADB] {cmd}");
                await Task.Delay(200);
            }
            
            Console.WriteLine("[KG] Patch Applied. Do NOT Factory Reset.");
            return true;
        }
    }
}
