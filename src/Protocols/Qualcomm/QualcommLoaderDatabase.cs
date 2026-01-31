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
            
            // 2026 Deep Matching Logic
            // SM8750 (Gen 4) -> 0x001D80E1
            // SM8850 (Gen 5) -> 0x002E90E1
            
            var files = Directory.GetFiles(_loaderPath, "*.elf");
            
            // Priority 1: Match by exact HWID-MSM string in filename
            string exactPattern = $"{hwId:X8}_{msmId:X8}";
            var exactMatch = files.FirstOrDefault(f => f.Contains(exactPattern));
            if (exactMatch != null) return exactMatch;

            // Priority 2: Match by MSM chipset code (e.g. 8750, 8850)
            string msmCode = msmId.ToString("X4");
            var msmMatch = files.FirstOrDefault(f => f.Contains(msmCode));
            
            if (msmMatch != null)
            {
                Logger.Success($"[DB] 2026-Tier programmer matched: {Path.GetFileName(msmMatch)}");
                return msmMatch;
            }

            Logger.Warn("[DB] No specific 2026 match. Attempting legacy fallback.");
            return files.OrderByDescending(f => f).FirstOrDefault(); 
        }
    }
}
