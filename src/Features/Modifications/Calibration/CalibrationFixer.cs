using System;
using System.IO;
using System.Text;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Features.Modifications.Calibration
{
    public class CalibrationFixer
    {
        /// <summary>
        /// Scans a binary dump (e.g. /persist or /nvdata) for an existing IMEI and replaces it.
        /// Only works on decrypted streams.
        /// </summary>
        public async Task<bool> PatchImeiAsync(string filePath, string oldImei, string newImei)
        {
            Logger.Info($"[CALIBRATION] Attempting to patch IMEI in {Path.GetFileName(filePath)}...");
            
            try
            {
                byte[] data = await File.ReadAllBytesAsync(filePath);
                byte[] oldBytes = Encoding.ASCII.GetBytes(oldImei);
                byte[] newBytes = Encoding.ASCII.GetBytes(newImei);

                int offset = FindBytes(data, oldBytes);
                if (offset == -1)
                {
                    Logger.Warn("[CALIBRATION] Original IMEI not found in the dump.");
                    return false;
                }

                Logger.Info($"[CALIBRATION] Match found at offset 0x{offset:X8}. Injecting new IMEI...");
                Array.Copy(newBytes, 0, data, offset, newBytes.Length);

                await File.WriteAllBytesAsync(filePath, data);
                Logger.Success("[CALIBRATION] Patch applied successfully.");
                return true;
            }
            catch (Exception ex)
            {
                Logger.Error($"[CALIBRATION] Patching failed: {ex.Message}");
                return false;
            }
        }

        private int FindBytes(byte[] source, byte[] target)
        {
            for (int i = 0; i <= source.Length - target.Length; i++)
            {
                bool match = true;
                for (int j = 0; j < target.Length; j++)
                {
                    if (source[i + j] != target[j])
                    {
                        match = false;
                        break;
                    }
                }
                if (match) return i;
            }
            return -1;
        }
    }
}
