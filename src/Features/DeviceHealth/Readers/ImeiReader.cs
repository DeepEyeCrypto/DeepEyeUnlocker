using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure;
using DeepEyeUnlocker.Features.DeviceHealth.Interfaces;

namespace DeepEyeUnlocker.Features.DeviceHealth.Readers
{
    public class ImeiReader : IDeviceHealthReader
    {
        private readonly IAdbClient _adb;
        public string Name => "IMEI Reader";

        public ImeiReader(IAdbClient adb)
        {
            _adb = adb;
        }

        public async Task ReadAsync(DeviceHealthReport report, CancellationToken ct = default)
        {
            // Try multiple methods to get IMEI
            report.Imei1 = await TryGetImeiAsync(0, ct);
            report.Imei2 = await TryGetImeiAsync(1, ct);

            ValidateImei(report, report.Imei1, 1);
            ValidateImei(report, report.Imei2, 2);
        }

        private async Task<string> TryGetImeiAsync(int slot, CancellationToken ct)
        {
            // 1. Service call (often requires root or permissive adb)
            var result = await _adb.ExecuteShellAsync($"service call iphonesubinfo {slot + 1}", ct);
            if (!string.IsNullOrEmpty(result) && !result.Contains("Permission denied"))
            {
                var parsed = ParseImeiFromServiceOutput(result);
                if (!string.IsNullOrEmpty(parsed)) return parsed;
            }

            // 2. Fallback to getprop (rarely works on modern Android)
            var prop = slot == 0 ? "ro.ril.oem.imei" : "ro.ril.oem.imei2";
            var propVal = await _adb.ExecuteShellAsync($"getprop {prop}", ct);
            if (!string.IsNullOrEmpty(propVal)) return propVal.Trim();

            return "";
        }

        private string ParseImeiFromServiceOutput(string output)
        {
            try
            {
                // service call output is hex, e.g. 00000000: 00000000 00000003 00000000 00350033 '........3.5.'
                var hexPart = string.Join("", output.Split('\n')
                                    .Select(l => l.Split('\'').FirstOrDefault()?.Split(':').LastOrDefault()?.Trim())
                                    .Where(l => !string.IsNullOrEmpty(l)));
                
                // Extraction logic for the unicode/hex string
                // In practice, this needs robust hex-to-string conversion
                // For now, we use a simple heuristic to find a 15-digit numeric string
                var cleaned = new string(output.Where(char.IsDigit).ToArray());
                if (cleaned.Length >= 15) return cleaned.Substring(0, 15);
            }
            catch { }
            return "";
        }

        private void ValidateImei(DeviceHealthReport report, string imei, int slot)
        {
            if (string.IsNullOrEmpty(imei)) return;

            if (imei.Length != 15)
            {
                report.AuditFindings.Add($"[IMEI{slot}] Invalid length: {imei.Length}");
                return;
            }

            if (!PassesLuhnCheck(imei))
            {
                report.AuditFindings.Add($"[IMEI{slot}] Failed Luhn checksum validation.");
            }
        }

        private bool PassesLuhnCheck(string imei)
        {
            int sum = 0;
            for (int i = 0; i < 15; i++)
            {
                int digit = imei[i] - '0';
                if (i % 2 == 1)
                {
                    digit *= 2;
                    if (digit > 9) digit -= 9;
                }
                sum += digit;
            }
            return sum % 10 == 0;
        }
    }
}
