using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Protocols.Qualcomm
{
    public class QualcommLoaderDatabase
    {
        private readonly string _loaderPath;

        public QualcommLoaderDatabase(string rootDirectory)
        {
            _loaderPath = Path.Combine(rootDirectory, "Resources", "Loaders", "Qualcomm");
            if (!Directory.Exists(_loaderPath))
            {
                Directory.CreateDirectory(_loaderPath);
            }
        }

        /// <summary>
        /// Finds the best matching programmer based on HWID and MSM ID.
        /// </summary>
        public string? FindProgrammer(uint hwId, uint msmId)
        {
            Logger.Info($"[DB] Searching for programmer: HWID=0x{hwId:X8}, MSM=0x{msmId:X8}");
            
            // In a real scenario, this would scan the directory for filenames like:
            // prog_emmc_firehose_8953_ddr.elf
            // For now, we return a default if it exists, or simulated match.
            
            var files = Directory.GetFiles(_loaderPath, "*.elf");
            
            // Simplified matching logic for the MVP
            string targetId = msmId.ToString("X4");
            var match = files.FirstOrDefault(f => f.Contains(targetId));
            
            if (match != null)
            {
                Logger.Success($"[DB] Optimal programmer found: {Path.GetFileName(match)}");
                return match;
            }

            Logger.Warn("[DB] No specific match found. Using generic fail-safe loader.");
            return files.FirstOrDefault(); 
        }
    }
}
