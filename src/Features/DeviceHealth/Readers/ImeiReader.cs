using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.DeviceHealth.Interfaces;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Features.DeviceHealth.Readers
{
    public class ImeiReader : IDeviceHealthReader
    {
        public string Name => "IMEI Auditor";

        public async Task ReadAsync(DeviceHealthReport report, CancellationToken ct = default)
        {
            Logger.Debug("Auditing IMEI via ADB/EDL...");
            await Task.Delay(500, ct); // Simulation

            report.Imei1 = "358712345678901";
            
            // Validate via Luhn
            if (!ValidateLuhn(report.Imei1))
            {
                report.AuditFindings.Add("[WARNING] IMEI 1 failed Luhn check.");
            }
        }

        private bool ValidateLuhn(string imei)
        {
            if (string.IsNullOrEmpty(imei) || imei.Length != 15) return false;
            int sum = 0;
            for (int i = 0; i < 15; i++)
            {
                if (!char.IsDigit(imei[i])) return false;
                int n = int.Parse(imei[i].ToString());
                if (i % 2 != 0)
                {
                    n *= 2;
                    if (n > 9) n = (n % 10) + 1;
                }
                sum += n;
            }
            return (sum % 10 == 0);
        }
    }
}
